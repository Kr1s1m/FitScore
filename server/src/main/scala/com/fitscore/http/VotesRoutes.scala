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
import cats.data.{NonEmptyChain, Validated}
import cats.data.Validated.*
import com.fitscore.errors.RegistrationRequestError
import com.fitscore.errors.RegistrationRequestError.*
import com.fitscore.utils.Date
import com.fitscore.validation.AccountValidator
import com.fitscore.core.AccountsRoles
import com.fitscore.domain.enums.AccessType
import com.fitscore.domain.enums.AccessType.*
import com.fitscore.domain.enums.VoteType
import com.fitscore.domain.enums.VoteType.*
import com.fitscore.domain.enums.VoteTarget
import com.fitscore.domain.enums.VoteTarget.*
import com.fitscore.domain.vote.{VoteDTO, VoteJson, VoteJsonDTO}
import io.circe.*

import java.util.UUID

//val genderDecoder: Decoder[AccessType] = Decoder.
//
//def encode_AccessType[T <: Enumeration#Value]: Encoder[T] =
//  (x: T) =>
//    Json.obj(
//      ("name", Json.fromString(x.toString)),
//      ("id", Json.fromInt(x.id))
//    )
//  def decode_AccessType[T <: Enumeration#Value](obj: Enumeration): Decoder[T] =
//       (x: HCursor) =>
//         for {
//           name <- x.downField("name").as[String]
//           id <- x.downField("id").as[Int]
//        } yield {
//           val a = obj.withName(name)
//           val b = obj.apply(id)
//           assert(a == b)
//           a.asInstanceOf[T]
//        }
//
//implicit val genderEncoder: Encoder[AccessTarget] = Encoder.encodeEnumeration(Gender)
class VotesRoutes[F[_]: Concurrent] private (votes: Votes[F], posts: Posts[F], replies: Replies[F]) extends Http4sDsl[F]:
  private val prefix = "/votes"
  
  //POST /votes/vote { voteDTO }
  private val voteRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "vote" =>
      request.as[VoteJsonDTO].flatMap(voteDTO =>
        votes.vote(voteDTO).flatMap{
          case Some((id, voteType, voteTarget, balanceChange)) => 
            voteTarget match
              case Post => posts.updateVoteBalance(voteDTO.postId, voteType, balanceChange).flatMap{
                case 0 => NotFound(s"Post balance update failed: Not found post id ${voteDTO.postId}")
                case i => Ok(s"$i entries modified from posts")  
              }
              case Reply => voteDTO.replyId match {
                case Some(replyId) => replies.updateVoteBalance(replyId, voteType, balanceChange).flatMap{
                  case 0 => NotFound(s"Post balance update failed: Not found post id $replyId")
                  case i => Ok(s"$i entries modified from replies")
                }
                case None => InternalServerError("Something went very wrong with your vote1")
              }
          case None => InternalServerError("Something went very wrong with your vote2")
        }
      )
  }
  

  val routes: HttpRoutes[F] = Router(
    prefix -> (
      voteRoute
      )
  )


object VotesRoutes:
  def resource[F[_]: Concurrent](votes: Votes[F], posts: Posts[F], replies: Replies[F]): Resource[F, VotesRoutes[F]] =
    Resource.pure(new VotesRoutes[F](votes, posts, replies))