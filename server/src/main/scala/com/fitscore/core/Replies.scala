package com.fitscore.core

import java.util.UUID
import cats.effect.*
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import com.fitscore.domain.reply.*
import com.fitscore.domain.reply.Reply
import cats.data.Validated
import cats.data.Validated.*
import com.fitscore.domain.enums.VoteType
import com.fitscore.domain.enums.VoteType.*
import com.fitscore.errors.ReplyUpdateRequestError
import com.fitscore.errors.ReplyUpdateRequestError.*
import doobie.Fragment

import java.util as ju
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor

import java.time.LocalDateTime

trait Replies[F[_]]: // "algebra"
  def create(reply: Reply): F[UUID]
  def getById(id: UUID): F[Option[ReplyDTO]]
  def all: F[List[ReplyDTO]]
  def getReplyKarmaByAccountId(accountId: UUID): F[Long]
  def updateVoteBalance(id: UUID, voteType: VoteType, balanceChange: Int): F[Int]
  def update(reply: ReplyUpdateRequest): F[Validated[ReplyUpdateRequestError, Int]]
  def delete(id: UUID): F[Int]

class RepliesLive[F[_]: Concurrent] private (transactor: Transactor[F]) extends Replies[F]:
  override def create(reply: Reply): F[UUID] =
    sql"""
          INSERT INTO replies(
            account_id,
            account_username,
            post_id,
            reply_parent_id,
            reply_body
          ) VALUES (
            ${reply.accountId},
            ${reply.accountUsername},
            ${reply.postId},
            ${reply.parentId},
            ${reply.body}
          )
      """
      .update
      .withUniqueGeneratedKeys[UUID]("reply_id")
      .transact(transactor)
  
  override def getById(id: UUID): F[Option[ReplyDTO]] =
    sql"""
        SELECT
          reply_id,
          account_id,
          account_username,
          post_id,
          reply_parent_id,
          reply_date_created,
          reply_date_updated,
          reply_body
        FROM replies
        WHERE reply_id=$id
    """
    .query[ReplyDTO]
    .option
    .transact(transactor)
    .map {
      case p@Some(_) => p
      case _ => println(s"[Internal Error] getById: Not found id in replies : $id"); None
    }

  override def all: F[List[ReplyDTO]] =
    sql"""
        SELECT
          reply_id,
          account_id,
          account_username,
          post_id,
          reply_parent_id,
          reply_date_created,
          reply_date_updated,
          reply_body
        FROM replies
    """
      .query[ReplyDTO]
      .stream
      .transact(transactor)
      .compile
      .toList

  override def getReplyKarmaByAccountId(accountId: UUID): F[Long] =
    sql"""
        SELECT
            COALESCE(SUM(reply_vote_balance), 0)
        FROM replies
        WHERE account_id = $accountId
    """
    .query[Long]
    .unique
    .transact(transactor)

  override def updateVoteBalance(id: UUID, voteType: VoteType, balanceChange: Int): F[Int] =
    def updateQuery(voteMath: String): F[Int] =
      sql"""
          UPDATE replies
          SET reply_vote_balance = reply_vote_balance ${Fragment.const(voteMath)},
          WHERE reply_id = $id
        """
        .update
        .run
        .transact(transactor)

    voteType match
      case Upvote => updateQuery(s"+ $balanceChange")
      case Downvote => updateQuery(s"- $balanceChange")
  
  override def update(reply: ReplyUpdateRequest): F[Validated[ReplyUpdateRequestError, Int]] =
    reply.body match
      case "" => Invalid(EmptyReplyBody).pure[F]
      case _ =>
        sql"""
             UPDATE replies
             SET reply_body=${reply.body}, reply_date_updated=${LocalDateTime.now}
             WHERE reply_id = ${reply.id}
        """
          .update
          .run
          .transact(transactor)
          .map{
            case 0 => Invalid(ReplyResourceNotFound(0))
            case i => Valid(i)
          }

  override def delete(id: UUID): F[Int] =
    sql"""
          DELETE
          FROM replies
          WHERE reply_id=$id
    """
      .update
      .run
      .transact(transactor)

//def delete1(id: UUID,id2: UUID): F[Int] = sql""" DELETE FROM replies WHERE reply_id IN ($id,$id2)""".update.run.transact(transactor)

object RepliesLive {
  def make[F[_]: Concurrent](postgres: Transactor[F]): F[RepliesLive[F]] =
    new RepliesLive[F](postgres).pure[F]

  def resource[F[_]: Concurrent](postgres: Transactor[F]): Resource[F, RepliesLive[F]] =
    Resource.pure(new RepliesLive[F](postgres))
}

//object RepliesPlayground extends IOApp.Simple:
//
//  def makeReplygres =
//    for
//      ec          <- ExecutionContexts.fixedThreadPool[IO](32)
//      transactor  <- HikariTransactor.newHikariTransactor[IO](
//        "org.postgresql.Driver",
//        "jdbc:postgresql://localhost:5444/",
//        "docker",
//        "docker",
//        ec
//      )
//    yield transactor
//
//  def program(postgres: Transactor[IO]) =
//    for
//      replies <- RepliesLive.make[IO](postgres)
//      _     <- replies.create(insertDummy)
//      //error    <- replies.getById(UUID.randomUUID())
//      //_     <- IO.println(error)
//      list  <- replies.all
//      _     <- IO.println(list)
//    yield ()
//
//  override def run: IO[Unit] = makeReplygres.use(program)