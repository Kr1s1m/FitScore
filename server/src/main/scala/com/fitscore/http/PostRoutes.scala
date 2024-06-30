package com.fitscore.http

import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.fitscore.core.*
import com.fitscore.domain.post.{Post, PostDTO, PostUpdateRequest}
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.Router

class PostRoutes[F[_]: Concurrent] private (posts: Posts[F]) extends Http4sDsl[F]:
  private val prefix = "/posts"

  //POST /posts/create { Post }
  private val createPostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "create" =>
      for
        post      <- request.as[Post]
        id        <- posts.create(post)
        response  <- Created(id)
      yield response
  }

  //GET /posts/{id}
  private val getByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(postId) => posts.getById(postId).flatMap{
      case Some(post) => Ok(post)
      case None => NotFound(s"Not found post id : $postId")
    }
  }

  //GET /posts
  private val getAllRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => posts.all.flatMap(posts => Ok(posts))
  }

  //PATCH /posts/update { Post }
  private val updateByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ PATCH -> Root / "update" =>
      for
        post <- request.as[PostUpdateRequest]
        response <- posts.update(post).flatMap {
          case 0 => NotModified()
          case i => Ok(s"$i entries modified from accounts")
        }
      yield response
  }

  //DELETE /posts/{id}
  private val deleteByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(postId) =>  posts.delete(postId).flatMap{
        case 0 => NotFound(s"Not found post id : $postId")
        case i => NoContent()
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefix -> (createPostRoute <+> getByIdRoute <+> getAllRoute <+> updateByIdRoute <+> deleteByIdRoute)
  )

object PostRoutes:
  def resource[F[_]: Concurrent](posts: Posts[F]): Resource[F, PostRoutes[F]] =
    Resource.pure(new PostRoutes[F](posts))
