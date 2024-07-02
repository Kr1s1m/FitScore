package com.fitscore.errors

import com.fitscore.utils.Date

enum RegistrationRequestError:
  case InvalidEmail(email:String) //sends to backend for check
  case InvalidBirthdayDate(date:Date)

  case PasswordsDoNotMatch
  case PasswordTooShort //frontend maybe

  case PasswordRequiresGreaterSymbolVariety //back/front end

  case BirthdayDateIsInTheFuture(date: Date)
  case NameIsEmpty

  case InvalidHeight(height: String)
  case InvalidWeight(weight: String)






