package com.fitscore.http

import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.fitscore.core.*
import com.fitscore.domain.account.Account
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.Router

class AccountRoutes[F[_]: Concurrent] private (accounts: Accounts[F]) extends Http4sDsl[F]:
  private val prefix = "/accounts"

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
      case None => NotFound(s"Not found post id : $accountId")
    }
  }

  //GET /accounts
  private val getAllRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => accounts.all.flatMap(accounts => Ok(accounts))
  }

  //DELETE /accounts/{id}
  private val deleteByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(accountId) => accounts.delete(accountId).flatMap {
      case 0 => NotFound(s"Not found post id : $accountId")
      case i => NoContent()
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefix -> (createAccountRoute <+> getByIdRoute <+> getAllRoute <+> deleteByIdRoute)
  )


object AccountRoutes:
  def resource[F[_]: Concurrent](accounts: Accounts[F]): Resource[F, AccountRoutes[F]] =
    Resource.pure(new AccountRoutes[F](accounts))
