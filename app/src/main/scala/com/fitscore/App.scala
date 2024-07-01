package com.fitscore

import cats.effect.*
import com.fitscore.domain.account.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.*
import tyrian.Html.*
import tyrian.http.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import io.circe.syntax.*

enum Msg:
  case NoMsg
  case ShowAll
  case HideAll
  case LoadAccounts(accounts: List[Account])
  case RegisterAccount
  case UsernameInput(username:String)
  case EmailInput(username:String)
  case AgeInput(username:String)
  case HeightInput(username:String)
  case WeightInput(username:String)

  case HoldInformation(s:String)
  case Error(e: String)

case class Model(
                  accounts: List[Account] = List(),
                  email:String="",
                  username:String="",
                  age:String="",
                  height:String="",
                  weight:String=""
                )

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

  private def backendCallRegister(account: Account): Cmd[IO, Msg] =
    val json = account.asJson.toString
    Http.send(Request.post("http://localhost:8080/accounts/create",tyrian.http.Body.json(json)),sdecode)
  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(), Cmd.None)

  override def view(model: Model): Html[Msg] =
    div(id := "login-overlay")(
      div(id := "login-popup")(
        div(id := "login-popup-content",style :=
          "position: fixed; " +
            "top: 0; left: 0; width: 100%; height: 100%; " +
            "background-color: rgba(0, 0, 0, 0.2); display: " +
            "flex; justify-content: center; " +
            "align-items: center;")(
          div(id := "background",style := "width: 500px; height: 700px; background-color: rgba(0, 0, 0.2, 0.1); border: 1px solid black;"+"display: " +
    "flex; justify-content: center; " +
      "align-items: center;"
          )(div()(
            input(`type` := "text", placeholder := "Email",onInput(email =>Msg.EmailInput(email))),br,
            input(`type` := "text", placeholder := "Username",onInput(username =>Msg.UsernameInput(username))),br,
            input(`type` := "text", placeholder := "Age",onInput(age =>Msg.AgeInput(age))),br,
            input(`type` := "text", placeholder := "Height",onInput(height =>Msg.HeightInput(height))),br,
            input(`type` := "text", placeholder := "Weight",onInput(weight =>Msg.WeightInput(weight))),br,
            button(onClick(Msg.RegisterAccount))("Register"),br,
            button(onClick(Msg.ShowAll))("Show all"),br,
            button(onClick(Msg.HideAll))("Hide all")))
        ),
        div(`class` := "contents ")(
          model.accounts.map { account =>
            div(account.toString)
          }
        ),
      )
    )
  def sdecode:Decoder[Msg] =
   Decoder[Msg](r => Msg.HoldInformation("sauz1"),e => Msg.HoldInformation("saauz"))

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.NoMsg => (model, Cmd.None)
    case Msg.ShowAll => (model,backendCall)
    case Msg.HideAll => (Model(),Cmd.None)

    case Msg.UsernameInput(x)  => (model.copy(username=x),Cmd.None)
    case Msg.AgeInput(x)  => (model.copy(age=x),Cmd.None)
    case Msg.EmailInput(x)  => (model.copy(email=x),Cmd.None)
    case Msg.HeightInput(x)  => (model.copy(height=x),Cmd.None)
    case Msg.WeightInput(x)  => (model.copy(weight=x),Cmd.None)
    case Msg.Error(e) => (model, Cmd.None)


    case Msg.LoadAccounts(list) =>
      if model.accounts == list
      then (model.copy(accounts = model.accounts ), Cmd.None)
      else (model.copy(accounts = model.accounts ++ list), Cmd.None)
    case Msg.HoldInformation(s:String) => (model, Cmd.None)
    case Msg.RegisterAccount =>
        val accounts = (model.email,model.username,model.age,model.height,model.weight)
        val account = Account(accounts._1,accounts._2,accounts._3.toShort,accounts._4.toShort,accounts._5.toDouble)
        (model.copy(accounts = account+: model.accounts), backendCallRegister(account))
  }

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None
