package com.fitscore.core

import java.util.UUID
import cats.effect.*
import cats.syntax.all.*
import com.fitscore.domain.enums.AccessType
import com.fitscore.domain.enums.AccessType.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor

import java.util as ju
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor

trait AccountsRoles[F[_]]: // "algebra"
  def assign(accountId: UUID, roleId: UUID): F[Unit]
  def spawn(accessType: AccessType): F[UUID]
  def getIdByAccessType(accessType: AccessType): F[Option[UUID]]
class AccountsRolesLive[F[_]: Concurrent] private (transactor: Transactor[F]) extends AccountsRoles[F]:
  override def assign(accountId: UUID, roleId: UUID): F[Unit] =
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

  def spawn(accessType: AccessType): F[UUID] =
    def insertQuery(s: String): F[UUID] =
      sql"""
          INSERT INTO roles(
            role_access_type
          ) VALUES (
            $s
          )
      """
      .update
      .withUniqueGeneratedKeys[UUID]("role_id")
      .transact(transactor)

    getIdByAccessType(accessType).flatMap{
      case Some(id) => id.pure[F]
      case None     => insertQuery(accessType.toString.toLowerCase)
    }
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

    selectQuery(accessType.toString.toLowerCase)


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
