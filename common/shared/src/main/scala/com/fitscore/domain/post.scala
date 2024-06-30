package com.fitscore.domain

import java.time.LocalDateTime
import java.util.UUID


//create table posts(
//  post_id uuid primary key NOT NULL DEFAULT gen_random_uuid(),
//  account_id uuid NOT NULL REFERENCES accounts (account_id),
//  post_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
//  post_date_updated timestamp without time zone,
//  post_title character varying(100),
//  post_body text

//{LocalDate, LocalTime, LocalDateTime, ZonedDateTime}

//);

object post:
  case class Post( //WRITING MOSTLY
//                   dateCreated: Option[LocalDateTime] = LocalDateTime.now,
//                   dateUpdated: LocalDateTime = LocalDateTime.now,
                   accountId: UUID = UUID.randomUUID,
                   title: String,
                   body: String
                 )

  case class PostDTO( //FOR OUTPUT MOSTLY
                      id: UUID = UUID.randomUUID,
                      dateCreated: LocalDateTime = LocalDateTime.now,
                      dateUpdated: LocalDateTime = LocalDateTime.now,
                      accountId: UUID = UUID.randomUUID,
                      title: String = "",
                      body: String = ""
                    )
  case class PostUpdateRequest(
                              id: UUID,
                              title: String,
                              body: String
                              )

  object Post:

    val uuidvalid = UUID.fromString("4a6df968-50c4-4eb2-875f-87f22d734a06")
    val postid = UUID.fromString("4a6df968-50c4-4eb2-875f-87f22d734a06")

    val insertDummy =
      Post(
        accountId = uuidvalid,
        title = "&&&&",
        body = "****"
      )

    val dummyDTO = fromPostToDTO(insertDummy, postid)

    def fromPostToDTO(post: Post, postId: UUID): PostDTO =
      PostDTO(
        id = postId,
        //dateCreated = post.dateCreated,
        //dateUpdated = post.dateUpdated,
        accountId = post.accountId,
        title = post.title,
        body = post.body
      )

    def fromDTOtoPost(postDTO:PostDTO): Post =
      Post(
        //dateCreated=postDTO.dateCreated,
        //dateUpdated=postDTO.dateUpdated,
        accountId=postDTO.accountId,
        title=postDTO.title,
        body=postDTO.body
      )
