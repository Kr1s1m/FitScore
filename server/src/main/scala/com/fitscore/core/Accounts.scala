package com.fitscore.core


import java.util.UUID
import cats.effect.*
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import com.fitscore.domain.account.*
import doobie.Fragment

import java.util as ju
import doobie.util.{ExecutionContexts, Get, Put}
import doobie.hikari.HikariTransactor
import doobie.syntax.SqlInterpolator.SingleFragment

import cats.data.{Validated, NonEmptyChain}
import cats.data.Validated.*
import com.fitscore.errors.RegistrationRequestError
import com.fitscore.errors.RegistrationRequestError.*


trait Accounts[F[_]]: // "algebra"
  def create(account: Account): F[UUID]
  def getBy[A :Get :Put](value: A, field: String): F[Option[AccountDTO]]
  def getById(id: UUID): F[Option[AccountDTO]]
  def getByEmail(email: String): F[Option[AccountDTO]]
  def getByUsername(username: String): F[Option[AccountDTO]]
  def existsBy[E, R](f: F[Option[R]], e: E): F[Validated[E, R]]
  def existsByUsername(username: String): F[Validated[NonEmptyChain[RegistrationRequestError], AccountDTO]]
  def existsByEmail(username: String): F[Validated[NonEmptyChain[RegistrationRequestError], AccountDTO]]
  def all: F[List[AccountDTO]]
  def updateStats(updateRequest: AccountStatsUpdateRequest): F[Int]
  def updateUsername(updateRequest: AccountUsernameUpdateRequest): F[Int]
  def updateEmail(updateRequest: AccountEmailUpdateRequest): F[Int]
  def delete(id: UUID): F[Int]


class AccountsLive[F[_]: Concurrent] private (transactor: Transactor[F]) extends Accounts[F]:
    
  override def create(account: Account): F[UUID] =
    sql"""
      INSERT INTO accounts(
        account_email,
        account_username,
        account_password,
        account_birth_date,
        account_height,
        account_weight
      ) VALUES (
        ${account.email},
        ${account.username},
        ${account.passwordHash},
        ${account.birthDate},
        ${account.height},
        ${account.weight}
      )
    """
      .update
      .withUniqueGeneratedKeys[UUID]("account_id")
      .transact(transactor)

  def getBy[A :Get :Put](value: A, field: String): F[Option[AccountDTO]] =
    sql"""
            SELECT
              account_id,
              account_date_created,
              account_email,
              account_username,
              account_birth_date,
              account_height,
              account_weight
            FROM accounts
            WHERE ${Fragment.const(field)}=$value
      """
      .query[AccountDTO]
      .option
      .transact(transactor)
      .map {
        case a@Some(_) => a
        case _ => println(s"[Internal Error] getBy: Not found $field in accounts : $value"); None
      }
  override def getById(id: UUID): F[Option[AccountDTO]] =
    getBy(id, "account_id")

  override def getByEmail(email: String): F[Option[AccountDTO]] =
    getBy(email, "account_email")

  override def getByUsername(username: String): F[Option[AccountDTO]] =
    getBy(username, "account_username")
  
  override def existsBy[E, R](f: F[Option[R]], e: E): F[Validated[E, R]] =
    f.flatMap{
      case Some(x) => Valid(x).pure[F]
      case None => Invalid(e).pure[F]
    }
  override def existsByEmail(username: String): F[Validated[NonEmptyChain[RegistrationRequestError], AccountDTO]] =
    existsBy(getByUsername(username), NonEmptyChain(UsernameIsInUse))
//    getByUsername(username).flatMap {
//      case Some(x) => Valid(x).pure[F]
//      case None => Invalid(NonEmptyChain(UsernameIsInUse)).pure[F]
//    }
  
  override def existsByUsername(username: String): F[Validated[NonEmptyChain[RegistrationRequestError], AccountDTO]] =
    existsBy(getByUsername(username), NonEmptyChain(UsernameIsInUse))
//    getByUsername(username).flatMap{
//      case Some(x) => Valid(x).pure[F]
//      case None    => Invalid(NonEmptyChain(UsernameIsInUse)).pure[F]
//    }

  override def all: F[List[AccountDTO]] =
    sql"""
        SELECT
          account_id,
          account_date_created,
          account_email,
          account_username,
          account_birth_date,
          account_height,
          account_weight
        FROM accounts
      """
      .query[AccountDTO]
      .stream
      .transact(transactor)
      .compile
      .toList

  override def updateStats(account: AccountStatsUpdateRequest): F[Int] =
        sql"""
             UPDATE accounts
             SET
               account_birth_date = ${account.birthDate},
               account_height = ${account.height},
               account_weight = ${account.weight}
             WHERE account_id = ${account.id}
        """
          .update
          .run
          .transact(transactor)

  override def updateUsername(account: AccountUsernameUpdateRequest): F[Int] =
    sql"""
               UPDATE accounts
               SET
                 account_username = ${account.username}
               WHERE account_id = ${account.id}
          """
      .update
      .run
      .transact(transactor)


  override def updateEmail(account: AccountEmailUpdateRequest): F[Int] =
    sql"""
               UPDATE accounts
               SET
                 account_email = ${account.email}
               WHERE account_id = ${account.id}
          """
      .update
      .run
      .transact(transactor)


  override def delete(id: UUID): F[Int] =
    sql"""
          DELETE 
          FROM accounts
          WHERE account_id=$id
    """
    .update
    .run
    .transact(transactor)


object AccountsLive:
  def make[F[_]: Concurrent](postgres: Transactor[F]): F[AccountsLive[F]] =
    new AccountsLive[F](postgres).pure[F]

  def resource[F[_]: Concurrent](postgres: Transactor[F]): Resource[F, AccountsLive[F]] =
    Resource.pure(new AccountsLive[F](postgres))


object AccountsPlayground extends IOApp.Simple:
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
      accounts  <- AccountsLive.make[IO](postgres)
      _         <- accounts.create(Account.dummy)
      list      <- accounts.all
      _         <- IO.println(list)
    yield ()

  override def run: IO[Unit] = makePostgres.use(program)
