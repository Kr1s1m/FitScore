package com.fitscore


import cats.effect.*
import com.fitscore.DynPage.Profile
import com.fitscore.domain.account.*
import com.fitscore.domain.post.{PostFrontEnd, SendPostFrontEnd}
import com.fitscore.domain.vote.FrontEndVote
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

import scala.scalajs.js.Object.entries
import com.fitscore.domain.enums.VoteType
import com.fitscore.domain.enums.VoteType.*
import com.fitscore.domain.enums.VoteTarget
import com.fitscore.domain.enums.VoteTarget.*



enum DynPage:
  case Login
  case Register
  case Home
  case Leader
  case Profile(name:String)
enum Msg:
  case Upvote(postId:String,target:VoteTarget)
  case Downvote(postId:String,target:VoteTarget)
  case OpenPosts
  case LoadLeaderBoard
  case LogOut
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
  case LogInOpen
  case StoreCookie(loginResponse:LoginResponse)
  case InputPasswordConfirm(p:String)
  case InputPassword(p:String) //8 min
  case HoldInformation(s:String)
  case Error(e: String)
  case OpenProfile(name:String)
  case LoadProfile
  case CloseProfile
  case TryToGetProfile(email: String)
  case GotProfile(profile: AccountInfo)
  case LoadPosts(posts:List[PostFrontEnd])
  case ContentInput(content: String)
  case TitleInput(title: String)
  case AddPost
  case CreatePost
  case FullPost(post:PostFrontEnd)

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
                  storeCookie: Option[LoginResponse] = None,
                  profile: AccountInfo = AccountInfo("","","","","",0,0.0),
                  loadProfile: Boolean = false,
                  otherProfile: AccountInfo = AccountInfo("","","","","",0,0.0),
                  leaderBoardIsOpen :Boolean = false,
                  posts: List[PostFrontEnd] = Nil,
                  post: SendPostFrontEnd = SendPostFrontEnd("","","",""),
                  postTitle: String ="",
                  postBody: String ="",
                  createPost: Boolean = false,
                  accountId: String = "",
                  vote : FrontEndVote = FrontEndVote("","",None,"",""),
                  currentPost: Option[PostFrontEnd] = None
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
          case Right(r) => Msg.LogIn
        },
      err => Msg.Error(err.toString)))

  private def backendCallLogin(account: LoginRequest): Cmd[IO, Msg] =
    val json = account.asJson.toString
    Http.send(
      Request.post("http://localhost:8080/accounts/login", tyrian.http.Body.json(json)),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[LoginResponse]) match {
            case Left(e) => Msg.Error(e.toString+s"${resp.body}")
            case Right(r) => Msg.StoreCookie(r)
          },
        err => Msg.Error(err.toString)))
  private def getAccountByUsername(username: String): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"http://localhost:8080/accounts/username/$username"),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[AccountInfo]) match {
            case Left(e) => Msg.Error(s"${resp.body}")//+e.getMessage)
            case Right(acc) => Msg.GotProfile(acc)
          },
        err => Msg.Error(err.toString)
      )
    )

  private def backendCallPosts: Cmd[IO, Msg] =
      Http.send(
        Request.get("http://localhost:8080/posts"),
        Decoder[Msg](
          resp =>
            parse(resp.body).flatMap(_.as[List[PostFrontEnd]]) match {
              case Left(e) => Msg.Error(e.getMessage)
              case Right(list) => Msg.LoadPosts(list)
            },
          err => Msg.Error(err.toString)
        )
      )

  private def initCall(username:String): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"http://localhost:8080/accounts/username/$username"),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[AccountInfo]) match {
            case Left(e) => Msg.Error(s"${resp.body}")//+e.getMessage)
            case Right(acc) => Msg.NoMsg
          },
        err => Msg.Error(err.toString)
      )
    )
    ///posts/create
  private def addPostCall(post: SendPostFrontEnd) : Cmd[IO,Msg] =
   val json = post.asJson.toString
   Http.send(
     Request.post("http://localhost:8080/posts/create", tyrian.http.Body.json(json)),
     Decoder[Msg](
       resp =>
         parse(resp.body).flatMap(_.as[String]) match {
           case Left(e) => Msg.Error(e.toString+s"${resp.body}")
           case Right(r) => Msg.NoMsg
         },
       err => Msg.Error(err.toString)))

  private def castVote(vote: FrontEndVote): Cmd[IO, Msg] =
    val json = vote.asJson.toString
    Http.send(
      Request.post("http://localhost:8080/votes/vote", tyrian.http.Body.json(json)),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[String]) match {
            case Left(e) => Msg.Error(e.toString + s"${resp.body}")
            case Right(r) => Msg.NoMsg
          },
        err => Msg.Error(err.toString)))
//  private def getPostKarma(post: PostFrontEnd): Cmd[IO,Msg] =
//    Http.send(
//      Request.get(s"http://localhost:8080/posts/karma/${post.id}"),
//      Decoder[Msg](
//        resp =>
//          parse(resp.body).flatMap(_.as[Long]) match {
//            case Left(e) => Msg.Error(s"${resp.body}")//+e.getMessage)
//            case Right(karma) => Msg.GotKarma(post,karma)
//          },
//        err => Msg.Error(err.toString)
//      )
//    )
//  private def getPostsKarma(posts:List[PostFrontEnd]): Cmd[IO,Msg] = ???
  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(), initCall(dom.window.localStorage.key(0)))
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

  def lbButton(attribute:String,msg: Msg = Msg.ToDo,style:String="cool-button"): Html[Msg] = button(onClick(msg),cls:=s"${style}")(attribute) //leaderboarddbuttons
  private def leaderboardHtml(competitors:List[(String,String,String)]):Html[Msg] =
    div(cls:="leaderboard")(
    lbButton("Name"),lbButton("Height"),lbButton("Weight"),lbButton("Bodyfat"),
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
            button(onClick(Msg.LogInOpen))("Login")), br,
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

  private def fullPostHtml(model: Model): Html[Msg] =
     (model.currentPost) match
       case None => div()()
       case Some(post) =>
        div(cls:="post-popup show-popup")(
          div(id := "post-overlay")(
            div(id := "post-popup")(
              div(id := "post-popup-content")(
                div(`class` := "post")(
                  h2(`class` := "post-title")(post.title),
                  p(`class` := "post-content")(post.body),
                  span(`class` := "post-author")(post.dateCreated),
                  span(`class` := "post-author")(post.dateCreated),
                  span(`class` := "post-author")("by " + post.accountUsername),
                  lbButton("/\\",Msg.Upvote(post.id,Post),"green active"),text(post.balance.toString),lbButton("\\/",Msg.Downvote(post.id,Post),"red active"),br,
                  lbButton("+",Msg.NoMsg)
              )
            )
          )
        )
        )
  private def profileHtml(model: Model): Html[Msg] =
    div(
    cls := "profile-container")(
    h1("Profile:"),
    div(
      cls := "profile-info")(
      div(
        cls := "profile-field")(
        label("Name: "),
        span(model.profile.username)
      ),
        div(
        cls := "profile-field")(
        label(s"Id: "),
        span(s"${model.profile.id}")
        ),
        div(
          cls := "profile-field")(
          label(s"Email: "),
          span(s"${model.email}")
        ),
          div(
          cls := "profile-field")(
          label(s"Height: "),
          span(s"${model.profile.height}")
        ),
          div(
          cls := "profile-field")(
          label(s"Weight: "),
          span(s"${model.profile.weight}")
        )
    )
  )
  def createPostHtlm(model: Model): Html[Msg] =
    div()(
      h1("Create a Post"),
      div(id := "post-form")(
        input(`type` := "text", placeholder := "Title", value := model.post.title, onInput(e => Msg.TitleInput(e))),
        br,
        input(placeholder := "Content", value := model.post.body, onInput(e => Msg.ContentInput(e))),
        br,
        br,
        button(onClick(Msg.AddPost))("Add Post")
      )
    )
  def calculatePostKarma(post:PostFrontEnd): Int = 0
  def postsHtml(model: Model): Html[Msg] =
      div()(
        h1("Posts"),
        div(id := "posts-container")(
          model.posts.map(
            post => div(`class` := "post")(
              h2(`class` := "post-title")(post.title),
              p(`class` := "post-content")(post.body),
              span(`class` := "post-author")(post.dateCreated),
              span(`class` := "post-author")(post.dateCreated),
              span(`class` := "post-author")("by " + post.accountUsername),
              lbButton("/\\",Msg.Upvote(post.id,Post),"green active"),text(post.balance.toString),lbButton("\\/",Msg.Downvote(post.id,Post),"red active"),br,
              lbButton("Read Replies",Msg.FullPost(post))

            )
          )
        )
      )

  val testCompetitors = List(("190","63","12.5"),("180","79","15.0"),("160","80","20.0"))
  override def view(model: Model): Html[Msg] =
    div(cls:="leaderboard",id := "Home page")(
        text(s"${model.accountId}"),
        lbButton("Leaderboard",Msg.LoadLeaderBoard),
        lbButton("Posts",Msg.OpenPosts),
        lbButton("Create Post",Msg.CreatePost),
        if model.createPost then createPostHtlm(model) else div()(),
        button(onClick(Msg.ShowAll))("Show all"),
        button(onClick(Msg.HideAll))("Hide all"), br,
        div(title:= "profile")(model.storeCookie match
          case Some(_) => div(cls:="button-container")(button(cls := "cool-button", onClick(Msg.LoadProfile))("Profile"))
          case None => div()()),
        div()(
        if model.loadProfile then
          profileHtml(model)
        else
          div()()
        ),
        text(model.error),
        registerHtml(model),
        loginHtml(model),
        fullPostHtml(model),
        button(cls := "cool-button",onClick(Msg.ToDo))("Errors"),
        (model.storeCookie match
          case Some(_) => div()(button(cls := "cool-button",onClick(Msg.LogOut))("Logout"),button(cls := "green",onClick(Msg.Open))("Register"))
          case _ => div()(button(cls := "cool-button",onClick(Msg.LogInOpen))("Login"),button(cls := "cool-button",onClick(Msg.Open))("Register"))),
        postsHtml(model),
        //leaderboardHtml(testCompetitors),
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
    case Msg.ToDo => (model.copy(error=s"${model.storeCookie}"), Cmd.None)

    case Msg.NoMsg =>
      val cookie = entries(dom.window.localStorage).head
      (model.copy(error="",storeCookie = Some(LoginResponse(cookie._1,cookie._2.toString))), Cmd.None)

    case Msg.Error(e) => (model.copy(error=e),Cmd.None)
    case Msg.LoadLeaderBoard =>
      (model.copy(leaderBoardIsOpen = true),Cmd.None) //TODO: Not sure on the design yet.

    case Msg.InputPassword(x) => if model.password.length < minPasswordLength then
      (model.copy(password=x),Cmd.None) else
      (model.copy(password=x),Cmd.None)
    case Msg.InputPasswordConfirm(x) => (model.copy(passwordConfirmation=x),Cmd.None)

    case Msg.AgeInput(_) => (model, Cmd.None)

    case Msg.ShowAll => (model,backendCall)
    case Msg.HideAll => (Model(),Cmd.None)

    case Msg.Close => (model.copy(pages=List(DynPage.Home),password=""),Cmd.None)
    case Msg.Open => (model.copy(pages=List(DynPage.Register),password=""),Cmd.None)

    case Msg.GotProfile(acc) => (model.copy(profile=acc,loadProfile=true),Cmd.None)

    case Msg.StoreCookie(r) =>
      dom.window.localStorage.setItem(r.username,r.sessionId)
      (model.copy(storeCookie=Some(r),pages=List(DynPage.Home)),getAccountByUsername(r.username))

    case Msg.LoadProfile =>
      val storeCookie = model.storeCookie.getOrElse(LoginResponse("",""))
      if !model.loadProfile then (model.copy(loadProfile = !model.loadProfile),getAccountByUsername(storeCookie.username))
      else (model.copy(loadProfile = !model.loadProfile),Cmd.None)

    case Msg.TryToGetProfile(username) => (model.copy(loadProfile = !model.loadProfile),Cmd.None)

    case Msg.Upvote(postId,Post) =>
      val voteBuilder = FrontEndVote(model.profile.id,postId,None,Upvote.toString.toLowerCase,Post.toString.toLowerCase)
      (model,castVote(voteBuilder))
    case Msg.Downvote(postId,Post) =>
      val voteBuilder = FrontEndVote(model.profile.id,postId,None,Downvote.toString.toLowerCase,Post.toString.toLowerCase)
      (model,castVote(voteBuilder))
    case Msg.Upvote(id,Reply) => (model,Cmd.None)
    case Msg.Downvote(id,Reply) => (model,Cmd.None)

    case Msg.LogOut =>
      dom.window.localStorage.clear()//removeItem(storeCookie.sessionId)
      (model.copy(storeCookie=None,loadProfile = false,pages=List(DynPage.Home)),Cmd.None)


    case Msg.TitleInput(title) => (model.copy(postTitle=title),Cmd.None)
    case Msg.ContentInput(content) => (model.copy(postBody=content),Cmd.None)
    case Msg.OpenPosts => (model,backendCallPosts)
    case Msg.LoadPosts(posts) => (model.copy(posts=posts,loadProfile = false),Cmd.None)
    case Msg.CreatePost =>
      val storeCookie = model.storeCookie.getOrElse(LoginResponse("",""))
      (model.copy(createPost = !model.createPost),getAccountByUsername(storeCookie.username))
    case Msg.AddPost => (model,addPostCall(SendPostFrontEnd(model.profile.id,model.profile.username,model.postTitle,model.postBody)))
    case Msg.FullPost(post:PostFrontEnd) => (model.copy(currentPost = Some(post)),Cmd.None)

    case Msg.LogIn =>
      val login = LoginRequest(model.email,model.password)
      model.storeCookie match
        case Some(_)=>(model.copy(pages=List(DynPage.Home)),backendCallLogin(login))
        case None => (model,backendCallLogin(login))

    case Msg.LogInOpen => (model.copy(pages=List(DynPage.Login)),Cmd.None)

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
