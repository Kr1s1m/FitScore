package com.fitscore


import cats.effect.*
import com.fitscore.domain.account.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.*
import tyrian.Html.{button, p, text, *}
import tyrian.http.*
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import io.circe.syntax.*


enum DynPage:
  case Login
  case Register
  case Home
enum Msg:
  case NoMsg
  case ShowAll
  case HideAll
  case Close
  case Open
  case ToDo
  case LoadAccounts(accounts: List[AccountPrint])
  case RegisterAccount
  case UsernameInput(username:String)
  case EmailInput(username:String)
  case AgeInput(username:String)
  case HeightInput(username:String)
  case WeightInput(username:String)
  case DayInput(username: String)
  case MonthInput(username: String)
  case YearInput(username: String)
  case LogIn
  case StoreCookie(loginResponse:LoginResponse)
  case InputPasswordConfirm(p:String)
  case InputPassword(p:String) //8 min
  case HoldInformation(s:String)
  case Error(e: String)

case class Model(
                  accounts: List[AccountPrint] = List(),
                  email: String="",
                  username: String="",
                  //password
                  password:String="",
                  passwordConfirmation:String="",
                  //date of birth
                  birthDay: String="",
                  birthMonth: String="",
                  birthYear: String="",

                  height: String="",
                  weight: String="",
                  pages:List[DynPage]=List(DynPage.Home),

                  error: String="",
                  storeCookie: LoginResponse = LoginResponse("","")
                )

@JSExportTopLevel("FitScoreApp")
object App extends TyrianApp[Msg, Model]:

  private def backendCall: Cmd[IO, Msg] =
    Http.send(
      Request.get("http://localhost:8080/accounts"),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[List[AccountPrint]]) match {
            case Left(e)     => Msg.Error(e.getMessage)
            case Right(list) => Msg.LoadAccounts(list)
          },
        err => Msg.Error(err.toString)
      )
    )

  private def backendCallRegister(account: RegistrationRequest): Cmd[IO, Msg] =
    val json = account.asJson.toString
    Http.send(
      Request.post("http://localhost:8080/accounts/register",tyrian.http.Body.json(json)),
      Decoder[Msg](
      resp =>
        parse(resp.body) match {
          case Left(e)     => Msg.Error(e.getMessage+s"${resp.toString}")
          case Right(r) => Msg.Error(r.toString)
        },
      err => Msg.Error(err.toString)))

  private def backendCallLogin(account: LoginRequest): Cmd[IO, Msg] =
    val json = account.asJson.toString
    Http.send(
      Request.post("http://localhost:8080/accounts/login", tyrian.http.Body.json(json)),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[LoginResponse]) match {
            case Left(e) => Msg.Error(s"${resp.body}")
            case Right(r) => Msg.StoreCookie(r)
          },
        err => Msg.Error(err.toString)))
  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(), Cmd.None)
  private def birthDate(model: Model):Html[Msg] =
    div(cls := "birthdate-container")(
      input(
        cls := "birthdate-input",
        `type` := "text",
        placeholder := "DD",
        value := model.birthDay,
        onInput(Msg.DayInput(_))
      ),
      input(
        cls := "birthdate-input",
        `type` := "text",
        placeholder := "MM",
        value := model.birthMonth,
        onInput(Msg.MonthInput(_))
      ),
      input(
        cls := "birthdate-input",
        `type` := "text",
        placeholder := "YYYY",
        value := model.birthYear,
        onInput(Msg.YearInput(_))
      )
    )
  def quickButtonCheck(p:Boolean): String = if p then "red" else "green"

  def lbButton(attribute:String): Html[Msg] = button(onToggle(Msg.NoMsg),cls:="cool-button active")(attribute) //leaderboarddbuttons
  private def leaderboardHtml(competitors:List[(String,String,String)]):Html[Msg] =
    div(cls:="leaderboard")(
    lbButton("Leaderboard"),br,lbButton("Name"),lbButton("Height"),lbButton("Weight"),lbButton("Bodyfat"),
    div()(
      competitors.map(leaderboardEntry)
        //leaderboardEntry(model.accounts(2)),

        // Add more entries as needed
      )
    )

  def keyComponent(t:String,show: String): Html[Msg] =
    div(cls:=s"${show}")(text(t))
  def leaderboardEntry(height:String,weight:String,bodyfat:String): Html[Msg] =
    div(cls:="leaderboard-entry")(
      div(cls:="leaderboard-entry")(List(keyComponent("Rank ?","rank"),keyComponent("Dero","name"),keyComponent("randomStat1","score"),keyComponent("randomStat2","score"))
    ))

  private def registerHtml(model: Model): Html[Msg] =
    div(cls := (if (model.pages.head == DynPage.Register) "login-popup show-popup" else "hide-popup login-popup"))(model.pages.map{case DynPage.Register => div()(
    div(id := "register-overlay")(
      div(id := "register-popup")(
        div(id := "register-popup-content")(
          div(id := "background"
          )(div()(
            div(cls := quickButtonCheck(model.email.isEmpty))(input(value := model.email,`type` := "text", placeholder := "Email", onInput(email => Msg.EmailInput(email)))), br, //15
            div(cls := quickButtonCheck(model.username.isEmpty || model.username.length > maxUserLength))(input(maxLength := maxUserLength,value := model.username,`type` := "text", placeholder := "Username", onInput(username => Msg.UsernameInput(username))))),
            div(cls :="text")(if model.username.length < maxUserLength-4 then text("") else text(s"Maximum username length:${model.username.length.toString}/$maxUserLength")),br,
            birthDate(model),
            div(cls := (if model.password.length < minPasswordLength then "red" else "green"))(input(`type` := "password", placeholder := "Password", onInput(password => Msg.InputPassword(password)))), br,
            div(cls := quickButtonCheck(!(model.password == model.passwordConfirmation && model.passwordConfirmation.nonEmpty)))(input(`type` := "text", placeholder := "ConfirmPassword", onInput(password => Msg.InputPasswordConfirm(password)))), br,
            div()(if model.password == model.passwordConfirmation then text("") else text(s"Passwords do not match!")),br,
            div(cls:= quickButtonCheck(model.height.isEmpty))(input(value := model.height,`type` := "number",min:="0",step:="1", placeholder := "Height", onInput(height => Msg.HeightInput(height)))),
            div(cls:=quickButtonCheck(model.weight.isEmpty))(input(value := model.weight,`type` := "number",min:="0",step:="0.5",placeholder := "Weight", onInput(weight => Msg.WeightInput(weight)))), br,
            div()(if absoluteCheck(model) then
              div(cls:="green")(button(onClick(Msg.RegisterAccount))("Register"))
            else div(cls:= "red")(button(onClick(Msg.NoMsg))("Please fill in the form"))),
            button(onClick(Msg.LogIn))("Login")), br,
            text(s"absolute check:${model.password.length}," +
              s"${model.username.length < maxUserLength}," +
              s"${(model.password == model.passwordConfirmation)}," +
              s"${model.username.nonEmpty &&
                model.email.nonEmpty && model.password.nonEmpty}"),
            button(onClick(Msg.Close))("Close")))))


    case _ => div()()})
  val maxUserLength = 15
  def absoluteCheck(model: Model): Boolean =
    (model.password.length >= minPasswordLength) &&
      (model.username.length <= maxUserLength) &&
      (model.password == model.passwordConfirmation) &&
      model.username.nonEmpty &&
      model.email.nonEmpty &&
      model.password.nonEmpty &&
      model.height.nonEmpty &&
      model.weight.nonEmpty
  private def loginHtml(model: Model): Html[Msg] =
    div(cls := (if (model.pages.head == DynPage.Login) "login-popup show-popup" else "hide-popup login-popup"))(model.pages.map { case DynPage.Login => div()(
      div(id := "login-overlay")(
        div(id := "login-popup")(
          div(id := "login-popup-content")(
            div(id := "background"
            )(div()(
              input(`type` := "text",value := model.email ,placeholder := "Email", onInput(email => Msg.EmailInput(email))), br,
              input(`type` := "password", placeholder := "Password", onInput(password => Msg.InputPassword(password))), br,
              button(onClick(Msg.LogIn))("Login"),button(onClick(Msg.Open))("Register"),br,
              button(onClick(Msg.Close))("Close")))))))
    case _ => div()()
    })
  val testCompetitors = List(("190","63","12.5"),("180","79","15.0"),("160","80","20.0"))
  override def view(model: Model): Html[Msg] =
    div(cls:="leaderboard",id := "Home page")(
        leaderboardHtml(testCompetitors),
        button(onClick(Msg.ShowAll))("Show all"), br,
        button(onClick(Msg.HideAll))("Hide all"), br,
        text(model.error),
        registerHtml(model),
        loginHtml(model),
        div()(button(onClick(Msg.Open))("Register")),
        div()(button(onClick(Msg.LogIn))("Login")),
        div(`class` := "contents ")(
          model.accounts.map { account =>
            div(account.toString)
          }
    )
    )
  val minPasswordLength = 8
  private def sdecode:Decoder[Msg] =
   Decoder[Msg](r => Msg.HoldInformation("sauz1"),e => Msg.HoldInformation("saauz"))

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.ToDo => (model, Cmd.None)
    case Msg.NoMsg => (model.copy(error=""), Cmd.None)
    case Msg.Error(e) => (model.copy(error=e),Cmd.None)

    case Msg.InputPassword(x) => if model.password.length < minPasswordLength then
      (model.copy(password=x),Cmd.None) else
      (model.copy(password=x),Cmd.None)
    case Msg.InputPasswordConfirm(x) => (model.copy(passwordConfirmation=x),Cmd.None)

    case Msg.AgeInput(_) => (model, Cmd.None)

    case Msg.ShowAll => (model,backendCall)
    case Msg.HideAll => (Model(),Cmd.None)

    case Msg.Close => (model.copy(pages=List(DynPage.Home),password=""),Cmd.None)
    case Msg.Open => (model.copy(pages=List(DynPage.Register),password=""),Cmd.None)

    case Msg.StoreCookie(r) =>
      dom.window.sessionStorage.setItem(model.storeCookie.username, model.storeCookie.sessionId)
      (model.copy(storeCookie=r),Cmd.None)

    case Msg.LogIn =>
      val login = LoginRequest(model.email,model.password)
      (model.copy(pages=List(DynPage.Login)),backendCallLogin(login))

    case Msg.UsernameInput(x)  => if model.username.length < maxUserLength then
      (model.copy(username=x),Cmd.None) else
      (model.copy(username=model.username.init),Cmd.None)

    case Msg.DayInput(x)  => (model.copy(birthDay=x),Cmd.None)
    case Msg.MonthInput(x)  => (model.copy(birthMonth=x),Cmd.None)
    case Msg.YearInput(x)  => (model.copy(birthYear=x),Cmd.None)
    case Msg.EmailInput(x)  => (model.copy(email=x),Cmd.None)

    case Msg.HeightInput(x)  => if model.height.forall(_.isDigit) then
      (model.copy(height=x),Cmd.None)
    else
      (model, Cmd.None)
   // case Msg.HeightInput(x)  => (model.copy(height=x),Cmd.None)

    case Msg.WeightInput(x)  => (model.copy(weight=x),Cmd.None)

    case Msg.LoadAccounts(list) =>
      if model.accounts == list
      then (model.copy(accounts = model.accounts ), Cmd.None)
      else (model.copy(accounts = model.accounts ++ list), Cmd.None)
    case Msg.HoldInformation(s:String) => (model, Cmd.None)
    case Msg.RegisterAccount =>
      //  val account = RegistrationRequest(model.email,model.username,model.height.toShort,model.weight.toDouble)
        val regAccount = RegistrationRequest(model.email,model.username,model.password,model.passwordConfirmation,model.birthDay,model.birthMonth,model.birthYear,model.height,model.weight)
        (model, backendCallRegister(regAccount))
  }

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None
