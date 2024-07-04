package com.fitscore.core

import java.util.UUID
import cats.effect.*
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import com.fitscore.domain.post.*
import com.fitscore.domain.post.Post.{dummyDTO, fromDTOtoPost, fromPostToDTO}
import com.fitscore.domain.enums.VoteType
import com.fitscore.domain.enums.VoteType.*
import cats.data.Validated
import cats.data.Validated.*
import com.fitscore.errors.PostUpdateRequestError
import com.fitscore.errors.PostUpdateRequestError.*
import doobie.Fragment

import java.util as ju
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor

import java.time.{Duration, LocalDateTime}

trait Posts[F[_]]: // "algebra"
  def create(post: Post): F[UUID]
  def getById(id: UUID): F[Option[PostDTO]]
  def all: F[List[PostDTO]]
  def getPostKarmaByAccountId(accountId: UUID): F[Long]
  def updateVoteBalance(id: UUID, voteType: VoteType, balanceChange: Int): F[Int]
  def update(post: PostUpdateRequest): F[Validated[PostUpdateRequestError, Int]]
  def delete(id: UUID): F[Int]

class PostsLive[F[_]: Concurrent] private (transactor: Transactor[F]) extends Posts[F]:
  override def create(post: Post): F[UUID] =
    sql"""
          INSERT INTO posts(
            account_id,
            post_title,
            post_body
          ) VALUES (
            ${post.accountId},
            ${post.title},
            ${post.body}
          )
      """
      .update
      .withUniqueGeneratedKeys[UUID]("post_id")
      .transact(transactor)

  override def getById(id: UUID): F[Option[PostDTO]] =
    sql"""
          SELECT
            post_id,
            post_date_created,
            post_date_updated,
            account_id,
            post_title,
            post_body
          FROM posts
          WHERE post_id=$id
    """
    .query[PostDTO]
    .option
    .transact(transactor)
    .map {
      case p@Some(_) => p
      case _ => println(s"[Internal Error] getById: Not found id in posts : $id"); None
    }

  override def all: F[List[PostDTO]] =
    sql"""
        SELECT
          post_id,
          post_date_created,
          post_date_updated,
          account_id,
          post_title,
          post_body
        FROM posts
    """
    .query[PostDTO]
    .stream
    .transact(transactor)
    .compile
    .toList

  override def getPostKarmaByAccountId(accountId: UUID): F[Long] =
    sql"""
        SELECT
            COALESCE(SUM(post_vote_balance), 0)
        FROM posts
        WHERE account_id = $accountId
    """
      .query[Long]
      .unique
      .transact(transactor)

  override def updateVoteBalance(id: UUID, voteType: VoteType, balanceChange: Int): F[Int] =
    def updateQuery(voteMath: String): F[Int] =
      sql"""
          UPDATE posts
          SET post_vote_balance = post_vote_balance ${Fragment.const(voteMath)},
          WHERE post_id = $id
        """
        .update
        .run
        .transact(transactor)

    voteType match
      case Upvote => updateQuery(s"+ $balanceChange")
      case Downvote => updateQuery(s"- $balanceChange")

  override def update(post: PostUpdateRequest): F[Validated[PostUpdateRequestError, Int]] =
    val runUpdateQuery =
      sql"""
            UPDATE posts
            SET
              post_title=${post.newTitle},
              post_body=${post.newBody},
              post_date_updated=${LocalDateTime.now}
            WHERE post_id = ${post.id}
        """
        .update
        .run
        .transact(transactor)
        .map{
          case 0 => Invalid(PostResourceNotFound(0))
          case i => Valid(i)
        }

    (post.oldTitle, post.newTitle, post.newBody) match
      case (_, "", _) => Invalid(EmptyPostTitle).pure[F]
      case (_, _, "") => Invalid(EmptyPostBody).pure[F]
      case (t1, t2, _) if t1 != t2 =>
        val createdElapsedMinutes = Duration.between(post.dateCreated, LocalDateTime.now).toMinutes
        if createdElapsedMinutes >= 15 then Invalid(PostCreatedTimeElapsed).pure[F] else runUpdateQuery
      case _ => runUpdateQuery
  
  override def delete(id: UUID): F[Int] =
    sql"""
          DELETE
          FROM posts
          WHERE post_id=$id
    """
    .update
    .run
    .transact(transactor)

  //def delete1(id: UUID,id2: UUID): F[Int] = sql""" DELETE FROM posts WHERE post_id IN ($id,$id2)""".update.run.transact(transactor)

object PostsLive {
  def make[F[_]: Concurrent](postgres: Transactor[F]): F[PostsLive[F]] =
    new PostsLive[F](postgres).pure[F]

  def resource[F[_]: Concurrent](postgres: Transactor[F]): Resource[F, PostsLive[F]] =
    Resource.pure(new PostsLive[F](postgres))
}

object PostsPlayground extends IOApp.Simple:

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
    for
      posts <- PostsLive.make[IO](postgres)
     // _     <- posts.create(fromDTOtoPost(dummyDTO))
      //_     <- posts.update
     // v     <- posts.create(fromDTOtoPost(dummyDTO))
    //  s     <- posts.create(fromDTOtoPost(dummyDTO))
   //   az    <- posts.getById(UUID.randomUUID())
     // _     <- IO.println(az)
    //  list  <- posts.all
     // _     <- IO.println(list)
    yield ()

  override def run: IO[Unit] = makePostgres.use(program)
