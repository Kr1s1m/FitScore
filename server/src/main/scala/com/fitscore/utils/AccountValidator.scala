package com.fitscore.utils

import cats.data.Validated
import cats.data.Validated.*
import com.fitscore.errors.RegistrationRequestError
import com.fitscore.errors.RegistrationRequestError.*

case class Email(user: String, domain: String)
object AccountValidator:
  def validate[E,A](value:A,p: A=>Boolean,e:E): Validated[E,A] = Some(value).filter(p).fold(Invalid(e))(Valid(_))
  
  def validateName(name:String): Validated[RegistrationRequestError,String] = validate(name,_.nonEmpty,NameIsEmpty)

  def validateEmail(email: String): Validated[RegistrationRequestError, Email] =
    val regex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
    if regex.r.matches(email) then
      val tokens = email.split("@")
      validate(
        Email(tokens(0), tokens(1)),
        e => e.user.nonEmpty && e.domain.nonEmpty,
        InvalidEmail(email)
      )
    else Invalid(InvalidEmail(email))