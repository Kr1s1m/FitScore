package com.fitscore.http

import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.fitscore.core.*
import com.fitscore.domain.reply.{Reply, ReplyDTO, ReplyUpdateRequest}
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.Router

import cats.data.Validated
import cats.data.Validated.*
import com.fitscore.errors.ReplyUpdateRequestError
import com.fitscore.errors.ReplyUpdateRequestError.*

class ReplyRoutes[F[_]: Concurrent] private (replies: Replies[F]) extends Http4sDsl[F]:
  private val prefix = "/replies"

  //POST /replies/create { Reply }
  private val createReplyRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "create" =>
      for
        reply     <- request.as[Reply]
        id        <- replies.create(reply)
        response  <- Created(id)
      yield response
  }

  //GET /replies/{id}
  private val getByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(replyId) => replies.getById(replyId).flatMap{
      case Some(reply) => Ok(reply)
      case None => NotFound(s"Fetch failed: Not found reply id $replyId")
    }
  }

  //GET /replies
  private val getAllRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => replies.all.flatMap(replies => Ok(replies))
  }

  //PATCH /replies/update { Reply }
  private val updateByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ PATCH -> Root / "update" =>
      for
        reply <- request.as[ReplyUpdateRequest]
        response <- replies.update(reply).flatMap {
          case Invalid(EmptyReplyBody) => BadRequest("Update failed: Bad request - reply body must not be empty")
          case Invalid(ReplyResourceNotFound(_))  => NotFound(s"Update failed: Not found reply id ${reply.id}")
          case Valid(i)  => Ok(s"$i entries modified from accounts")
        }
      yield response
  }

  //DELETE /replies/{id}
  private val deleteByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(replyId) =>  replies.delete(replyId).flatMap{
      case 0 => NotFound(s"Delete failed: Not found reply id $replyId")
      case i => NoContent()
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefix -> (
      createReplyRoute <+>
      getByIdRoute <+>
      getAllRoute <+>
      updateByIdRoute <+>
      deleteByIdRoute
    )
  )

object ReplyRoutes:
  def resource[F[_]: Concurrent](replies: Replies[F]): Resource[F, ReplyRoutes[F]] =
    Resource.pure(new ReplyRoutes[F](replies))
