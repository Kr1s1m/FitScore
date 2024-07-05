package com.fitscore.domain

import com.fitscore.domain.enums.{VoteTarget, VoteType}

import java.util.UUID
import scala.language.implicitConversions

object vote:

  case class Vote(    
                      id: UUID,
                      accountId: UUID,
                      postId: UUID,
                      replyId: Option[UUID],
                      voteType: VoteType,
                      voteTarget: VoteTarget
                    )
  case class VoteDTO(
                      accountId: UUID,
                      postId: UUID,
                      replyId: Option[UUID],
                      voteType: VoteType,
                      voteTarget: VoteTarget
                    )

  case class FrontEndVote(
                           accountId: String,
                           postId: String,
                           replyId: Option[String],
                           voteType: String,
                           voteTarget: String
                         )

  case class VoteJsonDTO(
                       accountId: UUID,
                       postId: UUID,
                       replyId: Option[UUID],
                       voteType: String,
                       voteTarget: String
                     )

  case class VoteJson(
                       id: UUID,
                       accountId: UUID,
                       postId: UUID,
                       replyId: Option[UUID],
                       voteType: String,
                       voteTarget: String
                     )
  implicit def dtoToJson(voteDTO: VoteDTO): VoteJsonDTO =
    val (svType,svTarget) = (voteDTO.voteType.toString.toLowerCase,voteDTO.voteType.toString.toLowerCase)
    VoteJsonDTO(voteDTO.accountId,voteDTO.postId,voteDTO.replyId,svType,svTarget)

  implicit def jsonToDTO(voteJson: VoteJsonDTO): VoteDTO =
    (voteJson.voteType,voteJson.voteTarget) match
      case ("upvote","post") => VoteDTO(voteJson.accountId, voteJson.postId, voteJson.replyId, VoteType.Upvote, VoteTarget.Post)
      case ("downvote","post") => VoteDTO(voteJson.accountId, voteJson.postId, voteJson.replyId, VoteType.Downvote, VoteTarget.Post)
      case ("upvote", "reply") => VoteDTO(voteJson.accountId, voteJson.postId, voteJson.replyId, VoteType.Upvote, VoteTarget.Reply)
      case ("downvote", "reply") => VoteDTO(voteJson.accountId, voteJson.postId, voteJson.replyId, VoteType.Downvote, VoteTarget.Reply)
      case _ => VoteDTO(UUID.fromString("?"), voteJson.postId, voteJson.replyId, VoteType.Downvote, VoteTarget.Reply)

  implicit def voteToJson(vote: Vote): VoteJson =
    val (svType, svTarget) = (vote.voteType.toString.toLowerCase, vote.voteType.toString.toLowerCase)
    VoteJson(vote.id,vote.accountId, vote.postId, vote.replyId, svType, svTarget)

  implicit def jsonToVote(voteJson: VoteJson): Vote =
    (voteJson.voteType, voteJson.voteTarget) match
      case ("upvote", "post") => Vote(voteJson.id,voteJson.accountId, voteJson.postId, voteJson.replyId, VoteType.Upvote, VoteTarget.Post)
      case ("downvote", "post") => Vote(voteJson.id,voteJson.accountId, voteJson.postId, voteJson.replyId, VoteType.Downvote, VoteTarget.Post)
      case ("upvote", "reply") => Vote(voteJson.id,voteJson.accountId, voteJson.postId, voteJson.replyId, VoteType.Upvote, VoteTarget.Reply)
      case ("downvote", "reply") => Vote(voteJson.id,voteJson.accountId, voteJson.postId, voteJson.replyId, VoteType.Downvote, VoteTarget.Reply)
      case _ => Vote(voteJson.id,UUID.fromString("?"), voteJson.postId, voteJson.replyId, VoteType.Downvote, VoteTarget.Reply)
