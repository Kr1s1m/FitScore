package com.fitscore.core

import java.util.UUID
import cats.effect.*
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import com.fitscore.domain.account.{Account, AccountDTO}

import java.util as ju
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor

import java.time.LocalDateTime

trait Accounts[F[_]]: // "algebra"
  def create(account: Account): F[UUID]
  def getById(id: UUID): F[Option[AccountDTO]]
  def all: F[List[AccountDTO]]
  //def update(post: AccountDTO): F[Int]
  def delete(id: UUID): F[Int]

class AccountsLive[F[_]: Concurrent] private (transactor: Transactor[F]) extends Accounts[F]:
  override def create(account: Account): F[UUID] =
    sql"""
      INSERT INTO accounts(
        account_email,
        account_username,
        account_age,
        account_height,
        account_weight
      ) VALUES (
        ${account.email},
        ${account.username},
        ${account.age},
        ${account.height},
        ${account.weight}
      )
    """
      .update
      .withUniqueGeneratedKeys[UUID]("account_id")
      .transact(transactor)

  override def getById(id: UUID): F[Option[AccountDTO]] =
    sql"""
          SELECT
            account_id,
            account_date_created,
            account_email,
            account_username,
            account_age,
            account_height,
            account_weight
          FROM accounts
          WHERE account_id=$id
    """
    .query[AccountDTO]
    .option
    .transact(transactor)
    .map {
      case a@Some(_) => a
      case _ => println(s"[Internal Error] getById: Not found id in accounts : $id"); None
    }
  override def all: F[List[AccountDTO]] =
    sql"""
        SELECT
          account_id,
          account_date_created,
          account_email,
          account_username,
          account_age,
          account_height,
          account_weight
        FROM accounts
      """
      .query[AccountDTO]
      .stream
      .transact(transactor)
      .compile
      .toList

  //override def update(post: AccountDTO): F[Int] = ???

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
