package com.fitscore.core

import java.util.UUID
import cats.effect.*
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor

import com.fitscore.enums.AccessType
import com.fitscore.enums.AccessType.*

import java.util as ju
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor

trait AccountsRoles[F[_]]: // "algebra"
  def create(accountId: UUID, roleId: UUID): F[Unit]
  def getIdByAccessType(accessType: AccessType): F[Option[UUID]]
class AccountsRolesLive[F[_]: Concurrent] private (transactor: Transactor[F]) extends AccountsRoles[F]:
  override def create(accountId: UUID, roleId: UUID): F[Unit] =

    sql"""
      INSERT INTO accounts_roles(
        account_id, role_id
      ) VALUES (
          $accountId, $roleId
      )
      """
      .update
      .withUniqueGeneratedKeys("account_id", "role_id")
      .transact(transactor)

  override def getIdByAccessType(accessType: AccessType = User): F[Option[UUID]] =
    def selectQuery(x: String) =
        sql"""
             SELECT
               role_id
             FROM roles
             WHERE role_access_type=$x
       """
      .query[UUID]
      .option
      .transact(transactor)
      .map {
        case a@Some(_) => a
        case _ => println(s"[Internal Error] getIdByAccessType: Not found access type in roles : $x"); None
      }

    accessType match
      case User   => selectQuery("user")
      case Admin  => selectQuery("admin")

//(accountId, roleId).pure[F]

//  create table accounts_roles(
//    PRIMARY KEY(account_id, role_id),
//    account_id uuid REFERENCES accounts (account_id),
//    role_id uuid REFERENCES roles (role_id)
//  );

object AccountsRolesLive:
  def make[F[_]: Concurrent](postgres: Transactor[F]): F[AccountsRolesLive[F]] =
    new AccountsRolesLive[F](postgres).pure[F]

  def resource[F[_]: Concurrent](postgres: Transactor[F]): Resource[F, AccountsRolesLive[F]] =
    Resource.pure(new AccountsRolesLive[F](postgres))


object AccountsRolesPlayground extends IOApp.Simple:
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
    val roleid    = UUID.fromString("2c4e0bd0-61db-4730-8c00-1ee065a6c171")
    for
      accountsRoles  <- AccountsRolesLive.make[IO](postgres)
      //output         <- accountsRoles.create(accountid, roleid)
      userId         <- accountsRoles.getIdByAccessType()
      adminId        <- accountsRoles.getIdByAccessType(Admin)
      //_         <- IO.println(output)
      _         <- IO.println((userId, adminId))
    yield ()

  override def run: IO[Unit] = makePostgres.use(program)
