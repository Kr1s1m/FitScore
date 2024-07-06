package com.fitscore

import org.scalajs.dom.{MouseEvent, window}
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
import com.fitscore.domain.reply.{ReplyFrontEnd, SendReplyFrontEnd}



enum DynPage:
  case Login
  case Register
  case Home
  case Leader
  case Profile
enum Msg:
  case Send
  case Reset
  case MouseDown
  case MouseUp
  case MouseOver(buttonId: Int)
  case SortBy(s:String)
  case CloseOtherProfile
  case GotKarmaPosts(karma: Long)
  case GotKarmaReplies(karma: Long)
  case GotKarmaPostsOther(karma: Long,accountId: String)
  case GotKarmaRepliesOther(karma: Long)
  case None
  case LoadReplies(replies: List[ReplyFrontEnd])
  case CheckProfile(account: AccountInfo)
  case AddReply
  case ContentInputReply(content:String)
  case CreateReply(parentId:Option[String],postId:String)
  case Upvote(postId:String,target:VoteTarget,replyId:String="")
  case Downvote(postId:String,target:VoteTarget,replyId:String="")
  case OpenPosts
  case LoadLeaderboard
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
  case CloseFullPost

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
                  currentPost: Option[PostFrontEnd] = None,
                  currentPostReplies: List[ReplyFrontEnd] = Nil,

                  replyBody: String = "",
                  createReply: Boolean = false,
                  replyParent: Option[String] = None,
                  replyPost: String = "",
                  isMouseDown: Boolean = false,
                  activeButtons: Set[Int] = Set.empty,

                  currentUserKarmaPosts: Long = 0,
                  currentUserKarmaReplies: Long = 0,

                  otherUserKarmaPosts: Long = 0 ,
                  otherUserKarmaReplies: Long = 0

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
          case Left(e)     => Msg.Error(e.getMessage)
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
            case Left(e) => Msg.Error(e.toString)
            case Right(r) => Msg.StoreCookie(r)
          },
        err => Msg.Error(err.toString)))
  private def getAccountByUsername(username: String): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"http://localhost:8080/accounts/username/$username"),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[AccountInfo]) match {
            case Left(e) => Msg.Error(e.getMessage)//+e.getMessage)
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
            case Left(e) => Msg.Error(e.getMessage)//+e.getMessage)
            case Right(acc) => Msg.NoMsg
          },
        err => Msg.Error(err.toString)
      )
    )

  private def addReplyCall(reply: SendReplyFrontEnd,post: PostFrontEnd): Cmd[IO, Msg] =
    val json = reply.asJson.toString
    Http.send(
      Request.post("http://localhost:8080/replies/create", tyrian.http.Body.json(json)),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[String]) match {
            case Left(e) => Msg.Error(e.getMessage)
            case Right(r) => Msg.FullPost(post)
          },
        err => Msg.Error(err.toString)))
    ///posts/create
  private def addPostCall(post: SendPostFrontEnd) : Cmd[IO,Msg] =
   val json = post.asJson.toString
   Http.send(
     Request.post("http://localhost:8080/posts/create", tyrian.http.Body.json(json)),
     Decoder[Msg](
       resp =>
         parse(resp.body).flatMap(_.as[String]) match {
           case Left(e) => Msg.Error(e.getMessage)
           case Right(r) => Msg.OpenPosts
         },
       err => Msg.Error(err.toString)))

  private def castVote(vote: FrontEndVote): Cmd[IO, Msg] =
    val json = vote.asJson.toString
    Http.send(
      Request.post("http://localhost:8080/votes/vote", tyrian.http.Body.json(json)),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[String]) match {
            case Left(e) => Msg.Error(e.getMessage)
            case Right(r) => Msg.None
          },
        err => Msg.Error(err.toString)))
  private def getKarmaPosts(account: String): Cmd[IO,Msg] =
    Http.send(
      Request.get(s"http://localhost:8080/posts/karma/${account}"),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[Long]) match {
            case Left(e) => Msg.Error(s"${resp.body}")//+e.getMessage)
            case Right(karma) => Msg.GotKarmaPosts(karma)
          },
        err => Msg.Error(err.toString)
      )
    )

  private def getKarmaReplies(account: String): Cmd[IO, Msg] =
      Http.send(
        Request.get(s"http://localhost:8080/replies/karma/${account}"),
        Decoder[Msg](
          resp =>
            parse(resp.body).flatMap(_.as[Long]) match {
              case Left(e) => Msg.Error(s"${resp.body}") //+e.getMessage)
              case Right(karma) => Msg.GotKarmaReplies(karma)
            },
          err => Msg.Error(err.toString)
        )
      )
//  private def getPostsKarma(posts:List[PostFrontEnd]): Cmd[IO,Msg] = ???
  private def getRepliesByPost(postId: String): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"http://localhost:8080/replies/postId/$postId"),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[List[ReplyFrontEnd]]) match {
            case Left(e) => Msg.Error(s"${resp.body}") //+e.getMessage)
            case Right(replies) => Msg.LoadReplies(replies)
          },
        err => Msg.Error(err.toString)
      )
    )

  private def backendCallGetOtherProfile(name: String): Cmd[IO,Msg] =
    Http.send(
      Request.get(s"http://localhost:8080/accounts/username/$name"),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[AccountInfo]) match {
            case Left(e) => Msg.Error(e.getMessage) //+e.getMessage)
            case Right(acc) => Msg.CheckProfile(acc)
          },
        err => Msg.Error(err.toString)
      )
    )

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
  private def leaderboardHtml(model: Model):Html[Msg] =
    div(cls:="leaderboard")(
      multipleButtons(model),
      text(s"${model.activeButtons}"),
      lbButton("Reset",Msg.Reset),lbButton("Apply",Msg.Send),
      br,
      lbButton("Name",Msg.NoMsg),
      lbButton("Height",Msg.SortBy("Height")),
      lbButton("Weight",Msg.SortBy("Weight")),
    div()(
      model.accounts.map(leaderboardEntry)
      )
    )

  def multipleButtons(model: Model): Html[Msg] =
    div()(
      button(
        onMouseDown(_ => Msg.MouseDown),
        onMouseUp(_ => Msg.MouseUp),
        onMouseOver(_ => Msg.MouseOver(1)),
        style := (if model.activeButtons.contains(1) then "background-color: yellow;" else "")
      )("Button 1"),
      button(
        onMouseDown(_ => Msg.MouseDown),
        onMouseUp(_ => Msg.MouseUp),
        onMouseOver(_ => Msg.MouseOver(2)),
        style := (if model.activeButtons.contains(2) then "background-color: yellow;" else "")
      )("Button 2"),
      button(
        onMouseDown(_ => Msg.MouseDown),
        onMouseUp(_ => Msg.MouseUp),
        onMouseOver(_ => Msg.MouseOver(3)),
        style := (if model.activeButtons.contains(3) then "background-color: yellow;" else "")
      )("Button 3")
    )
  def keyComponent(t:String,show: String): Html[Msg] =
    div(cls:=s"$show")(text(t))
  def leaderboardEntry(account: AccountPrint): Html[Msg] =
    div(cls:="leaderboard-entry")(
      div(cls:="leaderboard-entry")(List(keyComponent("Rank ?","rank"),clickableProfile(account.username,""),keyComponent(s"${account.height}","score"),keyComponent(s"${account.weight}","score"))
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
                  clickableProfile(post.accountUsername),
                  lbButton("/\\",Msg.Upvote(post.id,Post),"green active"),text(post.balance.toString),lbButton("\\/",Msg.Downvote(post.id,Post),"red active"),br,
                  lbButton("+",Msg.CreateReply(None,post.id)),button(cls:= "cool-button",onClick(Msg.CloseFullPost))("Close"),
                  if model.createReply then createReplyHtlm(model) else div()(),
                  div()(
                    model.currentPostReplies.map(x=>replyHtml(x,model))
                  ),
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
          span(s"${model.profile.email}")
        ),
          div(
          cls := "profile-field")(
          label(s"Height: "),
          span(s"${model.profile.height}")
        ),
          div(
          cls := "profile-field")(
          label(s"Weight: "),
          span(s"${model.profile.weight}"),
          div(
           cls := "profile-field")(br,
           label(s"Karma: "),br,
           span(cls := (if model.currentUserKarmaPosts >= 0 then "text-green" else "text-red"))(s"Posts karma:${model.currentUserKarmaPosts}"),br,
           span(cls := (if model.currentUserKarmaReplies >= 0 then "text-green" else "text-red"))(s"Replies karma: ${model.currentUserKarmaReplies}") ),

        )
    )
  )

  private def otherProfileHtml(user: AccountInfo): Html[Msg] =
    div(
      cls := "profile-container",`style` := s"position: absolute; top: ${0}px; left: ${0}px")(
      h1("Profile: "),
      div(
        cls := "profile-info")(
        div(
          cls := "profile-field")(
          label("Name: "),
          span(user.username)
        ),
        div(
          cls := "profile-field")(
          label(s"Id: "),
          span(s"${user.id}")
        ),
        div(
          cls := "profile-field")(
          label(s"Height: "),
          span(s"${user.height}")
        ),
        div(
          cls := "profile-field")(
          label(s"Weight: "),
          span(s"${user.weight}")
        ),
        lbButton("close",Msg.CloseOtherProfile)
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
        br,//add limiter for empty post
        button(onClick(Msg.AddPost))("Add Post")
      )
    )

  def createReplyHtlm(model: Model): Html[Msg] =
      div()(
        h1("Reply:"),
        div(id := "post-form")(
          input(placeholder := "Content", value := model.replyBody, onInput(e => Msg.ContentInputReply(e))),
          br,
          button(onClick(Msg.AddReply))("reply")
        )
      )

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
              clickableProfile(post.accountUsername),
              lbButton("/\\",Msg.Upvote(post.id,Post),"green active"),text(post.balance.toString),lbButton("\\/",Msg.Downvote(post.id,Post),"red active"),br,
              lbButton("Read Replies",Msg.FullPost(post))

            )
          )
        )
      )
  def clickableProfile(s: String,by:String ="by "): Html[Msg]  =  span(`class` := "post-author",style := "cursor: pointer; color: blue; text-decoration: underline;",
    onClick(Msg.OpenProfile(s)))(by + s)
  def replyHtml(reply:ReplyFrontEnd,model:Model): Html[Msg] =
    model.currentPost match
      case None => div()()
      case Some(currPost) =>
        div(`class` := "post")(
          span(`class` := "post-id")("reply id:"+reply.id),
          p(`class` := "post-content")(reply.body),
          span(`class` := "post-author")(reply.dateCreated),
          span(`class` := "post-author")(reply.dateUpdated),
          clickableProfile(reply.accountUsername),
          span(`class` := "post-author")("replied to " + (if reply.parentId.isEmpty then "OP" else reply.parentId.getOrElse(""))),
          lbButton("/\\",Msg.Upvote(currPost.id,Reply,reply.id),"green"),text(reply.balance.toString),lbButton("\\/",Msg.Downvote(currPost.id,Reply,reply.id),"red"),br,
          lbButton("+",Msg.CreateReply(Some(reply.id),currPost.id))
          )

  //val testCompetitors = List(("190","63","12.5"),("180","79","15.0"),("160","80","20.0"))
  override def view(model: Model): Html[Msg] =
    div(cls:="leaderboard",id := "Home page")(
        if model.leaderBoardIsOpen then
        leaderboardHtml(model) else div()(),
        button(cls := "cool-button",onClick(Msg.ToDo))("Errors"),br,
        text(s"${model.accountId}"),
        lbButton("Leaderboard",Msg.LoadLeaderboard),
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
        (model.storeCookie match
          case Some(_) => div()(button(cls := "cool-button",onClick(Msg.LogOut))("Logout"),button(cls := "green",onClick(Msg.Open))("Register"))
          case _ => div()(button(cls := "cool-button",onClick(Msg.LogInOpen))("Login"),button(cls := "cool-button",onClick(Msg.Open))("Register"))),
        postsHtml(model),
        if model.pages.contains(DynPage.Profile) then otherProfileHtml(model.otherProfile) else div()(),
        div(`class` := "contents ")(
          model.accounts.map { account =>
            div(account.toString)
          }

    )
    )
  private val minPasswordLength = 8
  private def sdecode:Decoder[Msg] =
   Decoder[Msg](r => Msg.HoldInformation("sauz1"),e => Msg.HoldInformation("saauz"))

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.ToDo => (model.copy(error=""), Cmd.None)
    case Msg.None => (model,Cmd.None)
    case Msg.NoMsg =>
      val cookie = entries(dom.window.localStorage).head
      (model.copy(storeCookie = Some(LoginResponse(cookie._1,cookie._2.toString))), getAccountByUsername(cookie._1))

    case Msg.Error(e) => (model.copy(error=e),Cmd.None)
    case Msg.LoadLeaderboard =>
      (model.copy(leaderBoardIsOpen = true),Cmd.None) //TODO: Not sure on the design yet.

    case Msg.InputPassword(x) => if model.password.length < minPasswordLength then
      (model.copy(password=x),Cmd.None) else
      (model.copy(password=x),Cmd.None)
    case Msg.InputPasswordConfirm(x) => (model.copy(passwordConfirmation=x),Cmd.None)

    case Msg.AgeInput(_) => (model, Cmd.None)

    case Msg.ShowAll => (model,backendCall)
    case Msg.HideAll => (model.copy(pages=List(DynPage.Home),accounts=Nil),Cmd.None)

    case Msg.Close => (model.copy(pages=List(DynPage.Home),password=""),Cmd.None)
    case Msg.Open => (model.copy(pages=List(DynPage.Register),password=""),Cmd.None)

    case Msg.GotProfile(acc) => (model.copy(profile=acc,loadProfile=true),getKarmaPosts(acc.id))

    case Msg.GotKarmaPosts(karmaPosts) => (model.copy(currentUserKarmaPosts = karmaPosts),getKarmaReplies(model.profile.id))
    case Msg.GotKarmaReplies(karmaReplies) => (model.copy(currentUserKarmaReplies = karmaReplies),Cmd.None)

    case Msg.GotKarmaPostsOther(karmaPosts,acc) => (model.copy(currentUserKarmaPosts = karmaPosts),getKarmaPosts(acc))

    case Msg.GotKarmaRepliesOther(karmaReplies) => (model.copy(currentUserKarmaReplies = karmaReplies),Cmd.None)

    case Msg.StoreCookie(r) =>
      dom.window.localStorage.setItem(r.username,r.sessionId)
      (model.copy(storeCookie=Some(r),pages=List(DynPage.Home)),getAccountByUsername(r.username))

    case Msg.LoadProfile =>
      val storeCookie = model.storeCookie.getOrElse(LoginResponse("",""))
      if !model.loadProfile then (model.copy(loadProfile = !model.loadProfile),getAccountByUsername(storeCookie.username))
      else (model.copy(loadProfile = !model.loadProfile),Cmd.None)

    case Msg.OpenProfile(name) => (model.copy(pages=Profile+:model.pages),backendCallGetOtherProfile(name))
    case Msg.CloseOtherProfile => (model.copy(pages=(model.pages.toSet-Profile).toList),Cmd.None)
    case Msg.CheckProfile(acc) => (model.copy(otherProfile=acc),Cmd.None)

    case Msg.TryToGetProfile(username) => (model.copy(loadProfile = !model.loadProfile),Cmd.None)

    case Msg.Upvote(postId,Post,_) =>
      val voteBuilder = FrontEndVote(model.profile.id,postId,None,Upvote.toString.toLowerCase,Post.toString.toLowerCase)
      (model,castVote(voteBuilder))
    case Msg.Downvote(postId,Post,_) =>
      val voteBuilder = FrontEndVote(model.profile.id,postId,None,Downvote.toString.toLowerCase,Post.toString.toLowerCase)
      (model,castVote(voteBuilder))
    case Msg.Upvote(postId,Reply,id) =>
      val voteBuilder = FrontEndVote(model.profile.id,postId,Some(id),Upvote.toString.toLowerCase,Reply.toString.toLowerCase)
      (model,castVote(voteBuilder))
    case Msg.Downvote(postId,Reply,id) =>
      val voteBuilder = FrontEndVote(model.profile.id,postId,Some(id),Downvote.toString.toLowerCase,Reply.toString.toLowerCase)
      (model,castVote(voteBuilder))

    case Msg.LogOut =>
      dom.window.localStorage.clear()//removeItem(storeCookie.sessionId)
      (model.copy(storeCookie=None,loadProfile = false,pages=List(DynPage.Home)),Cmd.None)


    case Msg.TitleInput(title) => (model.copy(postTitle=title),Cmd.None)
    case Msg.ContentInput(content) => (model.copy(postBody=content),Cmd.None)
    case Msg.OpenPosts => (model,backendCallPosts)
    case Msg.LoadPosts(posts) => (model.copy(posts=posts,loadProfile = false),Cmd.None)
    case Msg.CreatePost =>
      val storeCookie = model.storeCookie.getOrElse(LoginResponse("",""))
      (model.copy(createPost = !model.createPost),backendCallPosts)
    case Msg.AddPost => (model,addPostCall(SendPostFrontEnd(model.profile.id,model.profile.username,model.postTitle,model.postBody)))
    case Msg.FullPost(post:PostFrontEnd) => (model.copy(currentPost = Some(post)),getRepliesByPost(post.id))

    case Msg.ContentInputReply(content) => (model.copy(replyBody = content),Cmd.None)
    case Msg.AddReply => (model,addReplyCall(SendReplyFrontEnd(model.profile.id,model.profile.username,model.replyPost,model.replyParent,model.replyBody),model.currentPost.getOrElse(PostFrontEnd())))
    case Msg.CreateReply(parent,post) =>
      val storeCookie = model.storeCookie.getOrElse(LoginResponse("",""))
      (model.copy(createReply = !model.createReply,replyParent = parent,replyPost= post),getAccountByUsername(storeCookie.username))
    case Msg.CloseFullPost => (model.copy(currentPost = None),Cmd.None)
    case Msg.LoadReplies(replies) => (model.copy(currentPostReplies = replies),Cmd.None)


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
    case Msg.MouseDown =>
      (model.copy(isMouseDown = true), Cmd.None)

    case Msg.MouseUp =>
      (model.copy(isMouseDown = false), Cmd.None)

    case Msg.MouseOver(buttonId) if model.isMouseDown =>
      (model.copy(activeButtons = model.activeButtons + buttonId), Cmd.None)
    case Msg.Reset => (model.copy(activeButtons=Set.empty),Cmd.None)
    case Msg.SortBy(x) => x match
      case "Height" =>
        val sort=model.accounts.sortBy(_.height).reverse
        (model.copy(accounts=sort),Cmd.None)
      case "Weight" =>
        val sort=model.accounts.sortBy(_.weight).reverse
        (model.copy(accounts=sort),Cmd.None)
      case "Name" =>
        val sort=model.accounts.sortBy(_.username).reverse
        (model.copy(accounts=sort),Cmd.None)

    case Msg.Send =>
      val choices=model.activeButtons
      (choices.contains(1),choices.contains(1),choices.contains(1)) match
        case (true,true,true) =>
          val sort=model.accounts.sortBy(x=>(x.username,x.height,x.weight))
          (model.copy(accounts=sort),Cmd.None)
        case (false,true,true) =>
          val sort = model.accounts.sortBy(x => (x.height, x.weight))
          (model.copy(accounts = sort), Cmd.None)
        case (false, false, true) =>
          val sort = model.accounts.sortBy(x => (x.height, x.weight))
          (model.copy(accounts = sort), Cmd.None)
        case (false, true, false) =>
          val sort = model.accounts.sortBy(x => (x.height, x.weight))
          (model.copy(accounts = sort), Cmd.None)
        case _ => (model,Cmd.None)
        
    case _=> (model,Cmd.None)


  }

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None
