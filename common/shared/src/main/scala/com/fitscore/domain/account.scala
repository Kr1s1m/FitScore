package com.fitscore.domain


import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
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

  case class RegistrationRequest(
                                  //credentials
                                  email: String,
                                  username: String,
                                  //password
                                  password: String,
                                  passwordConfirmation: String,
                                  //date of birth
                                  birthDay: String,
                                  birthMonth: String,
                                  birthYear: String,
                                  //stats
                                  height: String,
                                  weight: String
                                  //...rest with update queries? shorter registration.
                                )
  case class LoginRequest(
                           email: String,
                           password: String
                         )
  case class Account(
                      email: String,
                      username: String,
                      passwordHash: String,
                      birthDate: String,
                      height: Short,
                      weight: Double
                    )
  
  case class AccountDTO(
                        id: UUID,
                        dateCreated: LocalDateTime,
                        email: String,
                        username: String,
                        birthDate: String,
                        height: Short,
                        weight: Double
                       )

  case class AccountPrint(
                         dateCreated: LocalDateTime,
                         email: String,
                         username: String,
                         birthDate: String,
                         height: Short,
                         weight: Double
                       )
  case class AccountStatsUpdateRequest(
                                 id: UUID,
                                 birthDate: String,
                                 height: Short,
                                 weight: Double
                               )
  case class AccountUsernameUpdateRequest(
                                 id: UUID,
                                 username: String
                               )

  case class AccountEmailUpdateRequest(
                                  id: UUID,
                                  email: String
                                )

  object Account:
    val dummy = Account(
      "dummy@fitscore.com",
      "dummy",
      "dummy",
      "2002-03-12",
      2,
      3.3
    )
    val uuidDefault: UUID = UUID.randomUUID()
