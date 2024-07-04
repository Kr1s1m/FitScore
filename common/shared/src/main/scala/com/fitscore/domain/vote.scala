package com.fitscore.domain

import com.fitscore.domain.enums.{VoteTarget, VoteType}

import java.util.UUID

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