package com.fitscore.http

import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.fitscore.core.*
import com.fitscore.domain.post.Post
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.Router

class PostRoutes[F[_]: Concurrent] private (posts: Posts[F]) extends Http4sDsl[F] {
  private val prefix = "/posts"

  // post /posts/create { Post }
  private val createPostRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        post  <- req.as[Post]
        id   <- posts.create(post)
        resp <- Created(id)
      } yield resp
  }

  private val getByIdRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(postId) => posts.getById(postId).flatMap{
      case Some(x) => Ok(x)
      case None => NotFound(s"Not found post id : $postId")
    }

  }

  // get /posts
  private val getAllRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    posts.all.flatMap(posts => Ok(posts))
  }

  val routes: HttpRoutes[F] = Router(
    prefix -> (createPostRoute <+> getAllRoute <+> getByIdRoute)
  )
}

object PostRoutes {
  def resource[F[_]: Concurrent](posts: Posts[F]): Resource[F, PostRoutes[F]] =
    Resource.pure(new PostRoutes[F](posts))
}
