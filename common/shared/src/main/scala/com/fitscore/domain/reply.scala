package com.fitscore.domain

import java.time.LocalDateTime
import java.util.UUID


//create table replies(
//  reply_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
//  account_id uuid NOT NULL REFERENCES accounts(account_id),
//  post_id uuid NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
//  reply_parent_id uuid REFERENCES replies(reply_id) ON DELETE CASCADE,
//  reply_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
//  reply_date_updated timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
//  reply_body text
//);

object reply:
  case class Reply( //WRITING MOSTLY
                   //dateCreated: LocalDateTime = LocalDateTime.now,
                   //dateUpdated: LocalDateTime = LocalDateTime.now,
                   accountId: UUID,
                   accountUsername:String,
                   postId: UUID,
                   parentId: Option[UUID],
                   body: String
                 )

  case class ReplyDTO( //FOR OUTPUT MOSTLY
                       id: UUID = UUID.randomUUID,
                       accountId: UUID = UUID.randomUUID,
                       accountUsername:String ="",
                       postId: UUID = UUID.randomUUID,
                       parentId: Option[UUID] = Some(UUID.randomUUID),
                       dateCreated: LocalDateTime = LocalDateTime.now,
                       dateUpdated: LocalDateTime = LocalDateTime.now,
                       body: String = ""
                    )
  case class ReplyUpdateRequest(
                                id: UUID,
                                body: String
                              )

  case class ReplyFrontEnd(
                           id: UUID = UUID.randomUUID,
                           accountId: UUID = UUID.randomUUID,
                           accountUsername:String ="",
                           postId: UUID = UUID.randomUUID,
                           parentId: Option[UUID] = Some(UUID.randomUUID),
                           dateCreated: LocalDateTime = LocalDateTime.now,
                           dateUpdated: LocalDateTime = LocalDateTime.now,
                           body: String = ""
                         )

  case class SendReplyFrontEnd(
                               accountId: UUID,
                               accountUsername:String,
                               postId: UUID,
                               parentId: Option[String],
                               body: String
                             )

//  object Reply:
//
//    val accountid = UUID.fromString("f170bb9c-bfd4-483f-9357-9859009169f6")
//    val postid = UUID.fromString("95437f60-aec5-4ea0-926b-2d2d54158d11")
//    val parentid = None//Some(UUID.fromString("4a6df968-50c4-4eb2-875f-87f22d734a06"))
//
//    val insertDummy =
//      Reply(
//        accountId = accountid,
//        postId = postid,
//        parentId = parentid,
//        body = "insertDummyTestBody"
//      )
//
//    val replyid = UUID.fromString("4a6df968-50c4-4eb2-875f-87f22d734a06")
//
//    val dummyDTO = fromReplyToDTO(insertDummy, replyid)
//
//    def fromReplyToDTO(reply: Reply, replyId: UUID): ReplyDTO =
//      ReplyDTO(
//        id = replyId,
//        //dateCreated = reply.dateCreated,
//        //dateUpdated = reply.dateUpdated,
//        accountId = reply.accountId,
//        postId = reply.postId,
//        parentId = reply.parentId,
//        body = reply.body
//      )
//
//    def fromDTOtoReply(replyDTO:ReplyDTO): Reply =
//      Reply(
//
//        accountId=replyDTO.accountId,
//        postId=replyDTO.postId,
//        parentId=replyDTO.parentId,
//        //dateCreated=replyDTO.dateCreated,
//        //dateUpdated=replyDTO.dateUpdated,
//        body=replyDTO.body
//      )
