package com.fitscore.domain

object account:
  case class Account(
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
  

