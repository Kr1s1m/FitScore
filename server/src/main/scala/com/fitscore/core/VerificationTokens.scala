//package com.fitscore.core
//
//import java.util.UUID
//import cats.effect.*
//import cats.syntax.all.*
//import com.fitscore.domain.token.VerificationToken
//import doobie.implicits.*
//import doobie.postgres.implicits.*
//import doobie.util.transactor.Transactor
//import doobie.Fragment
//import doobie.util.ExecutionContexts
//import doobie.hikari.HikariTransactor
//
//import java.util.UUID
//import java.util as ju
//
//import java.time.{Duration, LocalDateTime, Instant}
//
//trait VerificationTokens[F[_]]: // "algebra"
//  def create(verificationToken: VerificationToken): F[UUID]
//  def getById(id: UUID): F[Option[VerificationTokenDTO]]
//  def all: F[List[VerificationToken]]
//  def update(verificationToken: VerificationTokenUpdateRequest): F[Validated[VerificationTokenUpdateRequestError, Int]]
//  def delete(id: UUID): F[Int]
//
//class VerificationTokensLive[F[_]: Concurrent] private (transactor: Transactor[F]) extends VerificationTokens[F]:
//
//  import doobie._
//  import doobie.implicits._
//  import doobie.util.transactor.Transactor
//  import java.util.UUID
//  import java.time.Instant
//
//  class VerificationTokenRepository(transactor: Transactor[IO]) {
//
//    // TODO: IDEAS:
//    //    def insert(token: VerificationToken): IO[Int] = {
//    //      val query =
//    //        sql"""
//    //        INSERT INTO verification_tokens (token_id, user_id, token, created_at, expires_at)
//    //        VALUES (${token.tokenId}, ${token.userId}, ${token.token}, ${token.createdAt}, ${token.expiresAt})
//    //      """.update.run
//    //      query.transact(transactor)
//    //    }
//    //
//    //    def findByToken(token: String): IO[Option[VerificationToken]] = {
//    //      val query = sql"""
//    //        SELECT token_id, user_id, token, created_at, expires_at
//    //        FROM verification_tokens
//    //        WHERE token = $token
//    //      """.query[VerificationToken].option
//    //      query.transact(transactor)
//    //    }
//    //
//    //    def deleteByUserId(userId: UUID): IO[Int] = {
//    //      val query =
//    //        sql"""
//    //        DELETE FROM verification_tokens
//    //        WHERE user_id = $userId
//    //      """.update.run
//    //      query.transact(transactor)
//    //    }
//    //  }
//
//
//    //TODO: fix refactored copy paste from posts by crafting the needed functions from ideas above
//    override def create(verificationToken: VerificationToken): F[UUID] =
//      sql"""
//          INSERT INTO verification_tokens(
//            account_id,
//            verification_token_title,
//            verification_token_body
//          ) VALUES (
//            ${verificationToken.accountId},
//            ${verificationToken.title},
//            ${verificationToken.body}
//          )
//      """
//        .update
//        .withUniqueGeneratedKeys[UUID]("verification_token_id")
//        .transact(transactor)
//
//    override def getById(id: UUID): F[Option[VerificationTokenDTO]] =
//      sql"""
//          SELECT
//            verification_token_id,
//            verification_token_date_created,
//            verification_token_date_updated,
//            account_id,
//            verification_token_title,
//            verification_token_body
//          FROM verificationTokens
//          WHERE verification_token_id=$id
//    """
//        .query[VerificationTokenDTO]
//        .option
//        .transact(transactor)
//        .map {
//          case p@Some(_) => p
//          case _ => println(s"[Internal Error] getById: Not found id in verificationTokens : $id"); None
//        }
//
//    override def all: F[List[VerificationTokenDTO]] =
//      sql"""
//        SELECT
//          verification_token_id,
//          verification_token_date_created,
//          verification_token_date_updated,
//          account_id,
//          verification_token_title,
//          verification_token_body
//        FROM verificationTokens
//    """
//        .query[VerificationTokenDTO]
//        .stream
//        .transact(transactor)
//        .compile
//        .toList
//
//    override def update(verificationToken: VerificationTokenUpdateRequest): F[Validated[VerificationTokenUpdateRequestError, Int]] =
//      val runUpdateQuery =
//        sql"""
//            UPDATE verificationTokens
//            SET
//              verification_token_title=${verificationToken.newTitle},
//              verification_token_body=${verificationToken.newBody},
//              verification_token_date_updated=${LocalDateTime.now}
//            WHERE verification_token_id = ${verificationToken.id}
//        """
//          .update
//          .run
//          .transact(transactor)
//          .map {
//            case 0 => Invalid(VerificationTokenResourceNotFound(0))
//            case i => Valid(i)
//          }
//
//      (verificationToken.oldTitle, verificationToken.newTitle, verificationToken.newBody) match
//        case (_, "", _) => Invalid(EmptyVerificationTokenTitle).pure[F]
//        case (_, _, "") => Invalid(EmptyVerificationTokenBody).pure[F]
//        case (t1, t2, _) if t1 != t2 =>
//          val createdElapsedMinutes = Duration.between(verificationToken.dateCreated, LocalDateTime.now).toMinutes
//          if createdElapsedMinutes >= 15 then Invalid(VerificationTokenCreatedTimeElapsed).pure[F] else runUpdateQuery
//        case _ => runUpdateQuery
//
//    override def delete(id: UUID): F[Int] =
//      sql"""
//          DELETE
//          FROM verification_tokens
//          WHERE verification_token_id=$id
//    """
//        .update
//        .run
//        .transact(transactor)
//
//
//    object VerificationTokensLive {
//      def make[F[_] : Concurrent](postgres: Transactor[F]): F[VerificationTokensLive[F]] =
//        new VerificationTokensLive[F](postgres).pure[F]
//
//      def resource[F[_] : Concurrent](postgres: Transactor[F]): Resource[F, VerificationTokensLive[F]] =
//        Resource.pure(new VerificationTokensLive[F](postgres))
//    }
//
//    object VerificationTokensPlayground extends IOApp.Simple:
//
//    def makePostgres =
//      for
//        ec <- ExecutionContexts.fixedThreadPool[IO](32)
//        transactor <- HikariTransactor.newHikariTransactor[IO](
//          "org.postgresql.Driver",
//          "jdbc:postgresql://localhost:5444/",
//          "docker",
//          "docker",
//          ec
//        )
//      yield transactor
//
//    def program(postgres: Transactor[IO]) =
//      for
//        verificationTokens <- VerificationTokensLive.make[IO](postgres)
//      // _     <- verificationTokens.create(fromDTOtoVerificationToken(dummyDTO))
//      //_     <- verificationTokens.update
//      // v     <- verificationTokens.create(fromDTOtoVerificationToken(dummyDTO))
//      //  s     <- verificationTokens.create(fromDTOtoVerificationToken(dummyDTO))
//      //   az    <- verificationTokens.getById(UUID.randomUUID())
//      // _     <- IO.println(az)
//      //  list  <- verificationTokens.all
//      // _     <- IO.println(list)
//      yield ()
//
//    override def run: IO[Unit] = makePostgres.use(program)
//  }
