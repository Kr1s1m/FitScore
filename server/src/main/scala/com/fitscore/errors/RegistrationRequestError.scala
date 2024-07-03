package com.fitscore.errors

import com.fitscore.utils.Date
import cats.data.NonEmptyChain

enum RegistrationRequestError:
  case UsernameIsEmpty
  case UsernameIsInUse
  case EmailIsInUse
  case InvalidEmail(email:String) //sends to backend for check
  case PasswordsDoNotMatch
  case PasswordTooShort //frontend maybe
  case PasswordRequiresGreaterSymbolVariety //back/front end
  case InvalidBirthdayDate(dateErrors: NonEmptyChain[DateError])
  case BirthdayDateIsInTheFuture(date: Date)
  case HeightIsNotShort(height: String)
  case WeightIsNotDouble(weight: String)
  case InvalidHeight(height: Short)
  case InvalidWeight(weight: Double)
