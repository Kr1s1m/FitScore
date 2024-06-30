package com.fitscore

import cats.effect.*
import com.fitscore.domain.account.Account
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*

enum Msg:
  case NoMsg
  case LoadAccounts(accounts: List[Account])
  case Error(e: String)

case class Model(accounts: List[Account] = List())

@JSExportTopLevel("FitScoreApp")
object App extends TyrianApp[Msg, Model]:

  private def backendCall: Cmd[IO, Msg] =
    Http.send(
      Request.get("http://localhost:8080/accounts"),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[List[Account]]) match {
            case Left(e)     => Msg.Error(e.getMessage)
            case Right(list) => Msg.LoadAccounts(list)
          },
        err => Msg.Error(err.toString)
      )
    )

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(), backendCall)

  override def view(model: Model): Html[Msg] =
    div(`class` := "row")(
      p("This is the first ScalaJS app by FitScore"),
      div(`class` := "contents ")(
        model.accounts.map { account =>
          div(account.toString)
        }
      )
    )

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.NoMsg => (model, Cmd.None)
    case Msg.Error(e) => (model, Cmd.None)
    case Msg.LoadAccounts(list) => (model.copy(accounts = model.accounts ++ list), Cmd.None)
  }

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None
