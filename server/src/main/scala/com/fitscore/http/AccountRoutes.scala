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

import cats.data.{Validated, NonEmptyChain}
import cats.data.Validated.*
import com.fitscore.errors.RegistrationRequestError
import com.fitscore.errors.RegistrationRequestError.*
import com.fitscore.utils.Date
import com.fitscore.validation.AccountValidator

class AccountRoutes[F[_]: Concurrent] private (accounts: Accounts[F]) extends Http4sDsl[F]:
  private val prefix = "/accounts"

  //TODO: maybe move this to a new routes file/class related to new service class Authentication?
  //POST /accounts/register { registrationRequest }
  private val registerAccountRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "register" =>
      request.as[RegistrationRequest].flatMap( regReq =>
        AccountValidator.register(regReq).fold(
          //TODO: errors should be chained errors from the validation and turned into strings with some functionality showErrors?
          errors => BadRequest(s"${errors.toString}"),
          account => Created(accounts.create(account)) //TODO: use additional queries with account. about email and username
        )
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

  //GET /accounts
  private val getAllRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => accounts.all.flatMap(accounts => Ok(accounts))
  }

  //PATCH /accounts/update/stats { AccountStats }
  private val updateStatsByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@PATCH -> Root / "update" / "stats" =>
      for
        accountStats <- request.as[AccountStatsUpdateRequest]
        response <- accounts.updateStats(accountStats).flatMap {
          case 0 => NotFound(s"Stats update failed: Not found account id ${accountStats.id}")
          case i => Ok(s"$i entries modified from accounts")
        }
      yield response
  }
  //PATCH /accounts/update/username { AccountUsername }
  private val updateUsernameByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@PATCH -> Root / "update" / "username" =>
      for
        accountUsername <- request.as[AccountUsernameUpdateRequest]
        response <- accounts.updateUsername(accountUsername).flatMap {
          case 0 => NotFound(s"Username update failed: Not found account id ${accountUsername.id}")
          case i => Ok(s"$i entries modified from accounts")
        }
      yield response
  }
  //PATCH /accounts/update/email { AccountEmail }
  private val updateEmailByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@PATCH -> Root / "update" / "email" =>
      for
        accountEmail <- request.as[AccountEmailUpdateRequest]
        response <- accounts.updateEmail(accountEmail).flatMap {
          case 0 => NotFound(s"Username update failed: Not found account id ${accountEmail.id}")
          case i => Ok(s"$i entries modified from accounts")
        }
      yield response
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
        createAccountRoute <+> 
        getByIdRoute <+> 
        getAllRoute <+> 
        updateStatsByIdRoute <+> 
        updateUsernameByIdRoute <+> 
        updateEmailByIdRoute <+> 
        deleteByIdRoute
      )
  )


object AccountRoutes:
  def resource[F[_]: Concurrent](accounts: Accounts[F]): Resource[F, AccountRoutes[F]] =
    Resource.pure(new AccountRoutes[F](accounts))
