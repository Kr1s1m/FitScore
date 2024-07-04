package com.fitscore.core

import java.util.UUID
import cats.effect.*
import cats.syntax.all.*
import com.fitscore.domain.enums.AccessType
import com.fitscore.domain.enums.AccessType.*
import com.fitscore.domain.enums.VoteTarget
import com.fitscore.domain.enums.VoteTarget.*
import com.fitscore.domain.enums.VoteType
import com.fitscore.domain.enums.VoteType.*
import com.fitscore.domain.vote.{Vote, VoteDTO}
import doobie.Fragment
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor

import java.util as ju
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor

trait Votes[F[_]]: // "algebra"
  def vote(voteDTO: VoteDTO): F[Option[(UUID, VoteType, VoteTarget, Int)]]
  def getByAccountAndPostOrReplyIds(accountId: UUID, postId: UUID, replyId: Option[UUID]): F[Option[Vote]]
  def delete(id: UUID): F[Int]
class VotesLive[F[_]: Concurrent] private (transactor: Transactor[F]) extends Votes[F]:
  override def vote(voteDTO: VoteDTO): F[Option[(UUID, VoteType, VoteTarget, Int)]] =
    def invert(voteType: VoteType): VoteType =
      voteType match
        case Upvote => Downvote
        case Downvote => Upvote
        
    def insertQuery(voteType: VoteType, voteTarget: VoteTarget): F[UUID] =
      sql"""
        INSERT INTO votes(
          account_id,
          post_id,
          reply_id,
          vote_type,
          vote_target
        ) VALUES (
          ${voteDTO.accountId},
          ${voteDTO.postId},
          ${voteDTO.replyId},
          ${voteType.toString.toLowerCase},
          ${voteTarget.toString.toLowerCase}
        )
      """
      .update
      .withUniqueGeneratedKeys[UUID]("vote_id")
      .transact(transactor)
    
    getByAccountAndPostOrReplyIds(voteDTO.accountId, voteDTO.postId, voteDTO.replyId).flatMap{
      case Some(existingVote) =>
        (existingVote.voteType, existingVote.voteTarget, voteDTO.voteType, voteDTO.voteTarget) match
          case (exType, exTarget, vType, vTarget) if exType == vType && exTarget == vTarget =>
            delete(existingVote.id).map(_ => Some((existingVote.id, invert(existingVote.voteType), existingVote.voteTarget, 1)))
          case (exType, exTarget, vType, vTarget) if exType != vType && exTarget == vTarget =>
            for
              _   <- delete(existingVote.id)
              id  <- insertQuery(voteDTO.voteType, voteDTO.voteTarget)
            yield Some((id, voteDTO.voteType, voteDTO.voteTarget, 2))
          case _ => None.pure[F]
      case None => insertQuery(voteDTO.voteType, voteDTO.voteTarget).map(id => Some((id, voteDTO.voteType, voteDTO.voteTarget, 1)))
    }


  override def getByAccountAndPostOrReplyIds(accountId: UUID, postId: UUID, replyId: Option[UUID]): F[Option[Vote]] =
    def selectQuery(aid: UUID, pid: UUID, ridStatement: String) =
      sql"""
             SELECT
               vote_id,
               account_id,
               post_id,
               reply_id,
               post_vote_type,
               post_vote_target
             FROM votes
             WHERE ${Fragment.const(ridStatement)} account_id = $aid, post_id = $pid
       """
      .query[Vote]
      .option
      .transact(transactor)
      .map {
        case a@Some(_) => a
        case _ =>
          println(s"[Internal Error] getVoteInfoByIds: " +
                  s"Not found vote information by (account_id, post_id, Option[reply_id]): " +
                  s"($aid, $pid, $replyId)"
          )
          None
      }

    replyId match
      case Some(rid) => selectQuery(accountId, postId, s"reply_id = $rid,")
      case None      => selectQuery(accountId, postId, "")

  override def delete(id: UUID): F[Int] =
    sql"""
            DELETE
            FROM votes
            WHERE votes_id=$id
      """
      .update
      .run
      .transact(transactor)


object VotesLive:
  def make[F[_]: Concurrent](postgres: Transactor[F]): F[VotesLive[F]] =
    new VotesLive[F](postgres).pure[F]

  def resource[F[_]: Concurrent](postgres: Transactor[F]): Resource[F, VotesLive[F]] =
    Resource.pure(new VotesLive[F](postgres))


object VotesPlayground extends IOApp.Simple:
  def makePostgres =
    for
      ec          <- ExecutionContexts.fixedThreadPool[IO](32)
      transactor  <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql://localhost:5444/",
        "docker",
        "docker",
        ec
      )
    yield transactor

  def program(postgres: Transactor[IO]) =
    val accountid = UUID.fromString("610f04be-8966-4951-8f94-c37a796e49f9")
    val postid    = UUID.fromString("610f04be-8966-4951-8f94-c37a796e49f9")
    val replyid   = None //Some(UUID.fromString("2c4e0bd0-61db-4730-8c00-1ee065a6c171"))
    val votetype  = Upvote //Downvote
    val votetar   = Post //Reply
    val voteDTO   = VoteDTO(accountid, postid, replyid, votetype, votetar)
    for
      votes  <- VotesLive.make[IO](postgres)
      output <- votes.vote(voteDTO)
      _      <- IO.println(votes)
    yield ()

  override def run: IO[Unit] = makePostgres.use(program)
