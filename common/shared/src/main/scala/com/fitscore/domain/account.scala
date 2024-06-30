package com.fitscore.domain

import java.time.LocalDateTime
import java.util.UUID

//create table accounts(
//  account_id uuid primary key NOT NULL DEFAULT gen_random_uuid(),
//  account_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
//  account_email character varying(255),
//  account_username character varying(255),
//  account_age smallint,
//  account_height smallint,
//  account_weight numeric(4, 1)
//);

object account:
  case class Account(
                      email: String,
                      username: String,
                      age: Short,
                      height: Short,
                      weight: Double
                    )
  
  case class AccountDTO(
                        id: UUID,
                        dateCreated: LocalDateTime,
                        email: String,
                        username: String,
                        age: Short,
                        height: Short,
                        weight: Double         
                       )

  object Account:
    val dummy = Account(
      "dummy@fitscore.com",
      "dummy",
      1,
      2,
      3.3
    )
  

