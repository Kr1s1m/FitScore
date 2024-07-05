package com.fitscore.http

import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.fitscore.core.*
import com.fitscore.domain.account.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.Router
import cats.data.{NonEmptyChain, Validated}
import cats.data.Validated.*
import com.fitscore.errors.RegistrationRequestError
import com.fitscore.errors.RegistrationRequestError.*
import com.fitscore.utils.Date
import com.fitscore.validation.AccountValidator
import com.fitscore.core.AccountsRoles
import com.fitscore.domain.enums.AccessType
import com.fitscore.domain.enums.AccessType.*

import java.util.UUID

class AccountRoutes[F[_]: Concurrent] private (accounts: Accounts[F], accountsRoles: AccountsRoles[F]) extends Http4sDsl[F]:
  private val prefix = "/accounts"

  //TODO: move this to a new routes file/class related to new service class Authentication?
  //POST /accounts/register { registrationRequest }
  private val registerAccountRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "register" =>
      request.as[RegistrationRequest].flatMap( regReq =>
        AccountValidator.register(regReq).fold(
          //TODO: chained errors from the validation should be turned into strings with some functionality showErrors?
          errors => BadRequest(s"${errors.toString}"),
          account =>
            for
               emailNotExists <- accounts.notExistsEmail(account.email)
               usernameNotExists <- accounts.notExistsUsername(account.username)
               response <- (emailNotExists, usernameNotExists).mapN((_,_) => true).fold(
                 errors => BadRequest(s"${errors.toString}"),
                 _ =>
                   accounts.create(account).flatMap( accountId =>
                     for
                       roleId   <- accountsRoles.spawn(User)
                       _        <- accountsRoles.assign(accountId, roleId)
                       r        <- Created(accountId)
                     yield r
                   )
               )
            yield response
        )
      )
  }

  //TODO: move this to a new routes file/class Authentication routes related to new core class Authentication
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@POST -> Root / "login" =>
      request.as[LoginRequest].flatMap(logReq =>
        for
           email <- accounts.existsByEmail(logReq.email)
           matchingPassword <- accounts.existsMatchingPassword(logReq.email,logReq.password)
           response <- (email,matchingPassword).mapN((a, _) => a).fold(
             errors => BadRequest(s"${errors.toString}"),
             account =>
               val sessionId = UUID.randomUUID().toString
               Response(Status.Ok)
                 .withEntity(LoginResponse(account.username, sessionId))
                 .addCookie(ResponseCookie("sessionId", sessionId))
                 .pure[F]
           )
        yield response
      )
  }

  //POST /accounts/create { Account }
  private val createAccountRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "create" =>
      for
        account   <- request.as[Account]
        id        <- accounts.create(account)
        response  <- Created(id)
      yield response
  }

  //GET /accounts/{id}
  private val getByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(accountId) => accounts.getById(accountId).flatMap {
      case Some(account) => Ok(account)
      case None => NotFound(s"Fetch failed: Not found account id $accountId")
    }
  }

  //GET /accounts/{email}
  private val getByEmailRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "email" / accountEmail => accounts.getByEmail(accountEmail).flatMap {
      case Some(account) => Ok(account)
      case None => NotFound(s"Fetch failed: Not found account email $accountEmail")
    }
  }

  //GET /accounts/{username}
  private val getByUsernameRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "username" / accountUsername => accounts.getByUsername(accountUsername).flatMap {
      case Some(account) => Ok(account)
      case None => NotFound(s"Fetch failed: Not found account username $accountUsername")
    }
  }

  //GET /accounts
  private val getAllRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => accounts.all.flatMap(accounts => Ok(accounts))
  }

  //GET /accounts/users
  private val getAllUsers: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "users" => accounts.allUsers.flatMap(accounts => Ok(accounts))
  }

  //GET /accounts/admins
  private val getAllAdmins: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "admins" => accounts.allAdmins.flatMap(accounts => Ok(accounts))
  }

  //PATCH /accounts/update/stats { AccountStats }
  private val updateStatsByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@PATCH -> Root / "update" / "stats" => request.as[AccountStatsUpdateRequest].flatMap(
      updReq => AccountValidator.validateUpdateStats(updReq).fold(
        errors => BadRequest(s"${errors.toString}"),
        accountStats => accounts.updateStats(accountStats).flatMap {
          case 0 => NotFound(s"Stats update failed: Not found account id ${accountStats.id}")
          case i => Ok(s"$i entries modified from accounts")
        }
      )
    )

  }
  //PATCH /accounts/update/username { AccountUsername }
  private val updateUsernameByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@PATCH -> Root / "update" / "username" => request.as[AccountUsernameUpdateRequest].flatMap(
      updReq => AccountValidator.validateUsername(updReq.username).fold(
        errors => BadRequest(s"$errors"),
        validUsername => accounts.notExistsUsername(validUsername).flatMap(_.fold(
          error => BadRequest("Account with that username already exists"),
          _ => accounts.updateUsername(updReq).flatMap {
            case 0 => NotFound(s"Username update failed: Not found account id ${updReq.id}")
            case i => Ok(s"$i entries modified from accounts")
          }
        )
        )
      )
    )
  }
  //PATCH /accounts/update/email { AccountEmail }
  private val updateEmailByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@PATCH -> Root / "update" / "email" => request.as[AccountEmailUpdateRequest].flatMap(
      updReq => AccountValidator.validateEmail(updReq.email).fold(
        errors => BadRequest(s"$errors"),
        validEmail => accounts.notExistsEmail(validEmail).flatMap(_.fold(
          error => BadRequest("Account with that email already exists"),
          _ => accounts.updateEmail(updReq).flatMap {
            case 0 => NotFound(s"Email update failed: Not found account id ${updReq.id}")
            case i => Ok(s"$i entries modified from accounts")
          }
        )
        )
      )
    )
  }

  //DELETE /accounts/{id}
  private val deleteByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(accountId) => accounts.delete(accountId).flatMap {
      case 0 => NotFound(s"Delete failed: Not found account id $accountId")
      case i => NoContent()
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefix -> (
        registerAccountRoute <+> 
        loginRoute <+>
        createAccountRoute <+> 
        getByIdRoute <+>
        getByEmailRoute <+>
        getByUsernameRoute <+>
        getAllRoute <+>
        getAllUsers <+>
        getAllAdmins <+>
        updateStatsByIdRoute <+> 
        updateUsernameByIdRoute <+> 
        updateEmailByIdRoute <+> 
        deleteByIdRoute
      )
  )


object AccountRoutes:
  def resource[F[_]: Concurrent](accounts: Accounts[F], accountsRoles: AccountsRoles[F]): Resource[F, AccountRoutes[F]] =
    Resource.pure(new AccountRoutes[F](accounts, accountsRoles))
