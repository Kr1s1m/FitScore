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
import cats.data.{NonEmptyChain, Validated}
import cats.data.Validated.*
import com.fitscore.errors.RegistrationRequestError
import com.fitscore.errors.RegistrationRequestError.*
import com.fitscore.errors.LoginRequestError
import com.fitscore.errors.LoginRequestError.*
import com.fitscore.utils.PasswordUtils
import com.fitscore.domain.enums.AccessType
import com.fitscore.domain.enums.AccessType.*
import com.fitscore.validation.AccountValidator.validateUpdateStats

import java.time.LocalDate

trait Accounts[F[_]]: // "algebra"
  def create(account: Account): F[UUID]
  def getBy[A :Get :Put](value: A, field: String): F[Option[AccountDTO]]
  def getById(id: UUID): F[Option[AccountDTO]]
  def getByEmail(email: String): F[Option[AccountDTO]]
  def getByUsername(username: String): F[Option[AccountDTO]]
  def existsBy[E, R](f: F[Option[R]], e: E): F[Validated[E, R]]
  def existsByUsername(username: String): F[Validated[NonEmptyChain[RegistrationRequestError], AccountDTO]]
  def existsByEmail(email: String): F[Validated[NonEmptyChain[LoginRequestError], AccountDTO]]
  def notExistsUsername(username: String): F[Validated[NonEmptyChain[RegistrationRequestError], Boolean]]
  def notExistsEmail(email: String): F[Validated[NonEmptyChain[RegistrationRequestError], Boolean]]
  def existsMatchingPassword(email:String, password: String): F[Validated[NonEmptyChain[LoginRequestError], String]]
  def getPasswordByEmail(email:String): F[Option[String]]
  def all: F[List[AccountDTO]]
  def allRole(accessType: AccessType): F[List[AccountDTO]]
  def allUsers: F[List[AccountDTO]]
  def allAdmins: F[List[AccountDTO]]
  def updateStats(updateRequest: AccountStatsUpdate): F[Int]
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
    f.map{
      case Some(x) => Valid(x)
      case None => Invalid(e)
    }
  override def existsByEmail(email: String): F[Validated[NonEmptyChain[LoginRequestError], AccountDTO]] =
    existsBy(getByEmail(email), NonEmptyChain(EmailDoesNotExist))
  
  override def existsByUsername(username: String): F[Validated[NonEmptyChain[RegistrationRequestError], AccountDTO]] =
    existsBy(getByUsername(username), NonEmptyChain(UsernameIsInUse))
  
  def notExistsUsername(username:String): F[Validated[NonEmptyChain[RegistrationRequestError], Boolean]] =
    existsByUsername(username).map(x=>x.fold(
      notFound => Valid(true),
      found => Invalid(NonEmptyChain(UsernameIsInUse))  //should add him
    )
    )

  def notExistsEmail(email: String): F[Validated[NonEmptyChain[RegistrationRequestError], Boolean]] =
    existsByEmail(email).map(x => x.fold(
      notFound => Valid(true),
      found => Invalid(NonEmptyChain(EmailIsInUse)) //should add him
      )
    )

  def existsMatchingPassword(email:String,password:String): F[Validated[NonEmptyChain[LoginRequestError], String]] =
    getPasswordByEmail(email).map{
      case Some(p) =>
        //println(s"$password | ${PasswordUtils.hash(password)} | $p | ${PasswordUtils.check(password,p)}")
        if PasswordUtils.check(password,p) then
        Valid(p)
         else Invalid(NonEmptyChain(WrongPassword))
      case None => Valid(email)
    }

  def getPasswordByEmail(email:String): F[Option[String]] =
    sql"""
     SELECT
     account_password
     FROM accounts
     WHERE account_email = $email
      """
    .query[String]
    .option
    .transact(transactor)

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

  def allRole(accessType: AccessType): F[List[AccountDTO]] =
    def selectJoinQuery(s: String): F[List[AccountDTO]] =
      sql"""
        SELECT
            a.account_id,
            a.account_date_created,
            a.account_email,
            a.account_username,
            a.account_birth_date,
            a.account_height,
            a.account_weight
        FROM accounts a
        INNER JOIN accounts_roles ar ON a.account_id = ar.account_id
        INNER JOIN roles r ON ar.role_id = r.role_id
        WHERE r.role_access_type = $s
      """
      .query[AccountDTO]
      .stream
      .transact(transactor)
      .compile
      .toList

    selectJoinQuery(accessType.toString.toLowerCase)

  override def allUsers: F[List[AccountDTO]] = allRole(User)

  override def allAdmins: F[List[AccountDTO]] = allRole(Admin)

  override def updateStats(account: AccountStatsUpdate): F[Int] =
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
