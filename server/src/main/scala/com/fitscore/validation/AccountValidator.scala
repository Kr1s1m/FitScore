package com.fitscore.validation

//import com.fitscore.validation.Validated
//import com.fitscore.validation.Validated.toValidated

import cats.Semigroup
import cats.data.{NonEmptyChain, Validated}
import cats.data.Validated.*
import cats.syntax.all.*
import com.fitscore.domain.account.{Account, RegistrationRequest}
import com.fitscore.errors.RegistrationRequestError
import com.fitscore.errors.RegistrationRequestError.*
import com.fitscore.errors.DateError
import com.fitscore.errors.DateError.*
import com.fitscore.utils.PasswordUtils
import com.fitscore.utils.Date

given Semigroup[RegistrationRequestError] with
  def combine(x: RegistrationRequestError, y: RegistrationRequestError): NonEmptyChain[RegistrationRequestError] = NonEmptyChain(x, y)

given Semigroup[DateError] with
  def combine(x: DateError, y: DateError): NonEmptyChain[DateError] = NonEmptyChain(x, y)

  extension [A](opt: Option[A])
    def toValidated[E](onEmpty: => E): Validated[E, A] =
      opt.fold(Invalid(onEmpty))(Valid(_))

object AccountValidator:

  //TODO: Maybe errors arent actually being chained and need to be wrapped in NonEmptyChain
  def register(regReq: RegistrationRequest): Validated[RegistrationRequestError, Account] =
    (
      AccountValidator.validateEmail(regReq.email),
      AccountValidator.validateUsername(regReq.username),
      AccountValidator.validatePassword(regReq.password, regReq.passwordConfirmation),
      AccountValidator.validateBirthDate(regReq.birthYear, regReq.birthMonth, regReq.birthDay).map(Date.toIsoString),
      AccountValidator.validateHeight(regReq.height),
      AccountValidator.validateWeight(regReq.weight)
    ).mapN(Account.apply)

  private def validate[E,A](value: A, p: A => Boolean, e: E): Validated[E,A] = Some(value).filter(p).toValidated(e)
  
  private def validateUsername(username:String): Validated[RegistrationRequestError,String] = validate(username,_.nonEmpty,NameIsEmpty)

  private def validateEmail(email: String): Validated[RegistrationRequestError, String] =
    val regex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
    if regex.r.matches(email) then
      def checkEmpty(e: String): Boolean =
        val tokens = e.split("@"); tokens(0).nonEmpty && tokens(1).nonEmpty
      validate(
        email,
        checkEmpty,
        InvalidEmail(email)
      )
    else Invalid(InvalidEmail(email))

  private def validatePassword(password: String, passwordConfirmation: String): Validated[RegistrationRequestError, String] =
    def hasSymbolVariety(s: String): Boolean = s.exists(_.isDigit) && s.exists(_.isLetter) && s.exists(!_.isLetterOrDigit)
    (
      validate(password, _.length >= 8, PasswordTooShort),
      validate(password, hasSymbolVariety, PasswordRequiresGreaterSymbolVariety),
      validate(password, _ == passwordConfirmation, PasswordsDoNotMatch)
    ).mapN((_, _, p) => PasswordUtils.hash(p))

  private def validateBirthDate(birthYear: String, birthMonth: String, birthDay: String): Validated[RegistrationRequestError, Date] =
      val validatedYear: Validated[DateError, Int] =
        birthYear.toIntOption.toValidated(YearIsNotAnInteger(birthYear))

      val validatedMonth: Validated[DateError, Int] =
        (for
          monthInt <- birthMonth.toIntOption.toValidated(MonthIsNotAnInteger(birthMonth)).toEither
          _        <- validate(monthInt, m => m >= 1 && m <= 12, MonthOutOfRange(monthInt)).toEither
        yield monthInt).toValidated

      val validatedDay: Validated[DateError, Int] =
        (for
          dayInt <- birthDay.toIntOption.toValidated(DayIsNotAnInteger(birthDay)).toEither
          _      <- validate(dayInt, d => d >= 1 && d <= 31, DayOutOfRange(dayInt)).toEither
        yield dayInt).toValidated


      (
        validatedYear,
        validatedMonth,
        validatedDay
      ).mapN((y, m, d) =>
        Date.applyOption(y, m, d).toValidated(InvalidDate(y, m, d))
          .leftMap(errors => Invalid(InvalidBirthdayDate(errors)))
          .andThen(date => validate(date, _ <= Date.current, BirthdayDateIsInTheFuture(date)))
      )

//      (
//        validatedYear,
//        validatedMonth,
//        validatedDay
//      ).zipN
//        .flatMap((y, m, d) => Date.applyOption(y, m, d).toValidated(InvalidDate(y, m, d)))
//        .fold(
//          errors => Invalid(InvalidBirthdayDate(errors)),
//          date => validate(date, _ <= Date.current, BirthdayDateIsInTheFuture(date))
//        )
      
  private def validateHeight(height: String): Validated[RegistrationRequestError, Short] =
    (for
      heightShort <- height.toShortOption.toValidated(HeightIsNotShort(height)).toEither
      _           <- validate(heightShort, h => h >= 30 && h <= 300, InvalidHeight(heightShort)).toEither
    yield heightShort).toValidated

  private def validateWeight(weight: String): Validated[RegistrationRequestError, Double] =
    (for
      weightDouble <- weight.toDoubleOption.toValidated(HeightIsNotShort(weight)).toEither
      _ <- validate(weightDouble, h => h >= 1 && h <= 1000, InvalidWeight(weightDouble)).toEither
    yield weightDouble).toValidated


//  import cats.data.{Validated, NonEmptyChain}
//  import cats.implicits._
//
//  object AccountValidator {
//    private def validate[E, A](value: A, p: A => Boolean, e: E): Validated[NonEmptyChain[E], A] =
//      if (p(value)) Validated.valid(value) else Validated.invalid(NonEmptyChain.one(e))
//
//    def validateUsername(username: String): Validated[NonEmptyChain[RegistrationRequestError], String] =
//      validate(username, _.nonEmpty, RegistrationRequestError.NameIsEmpty)
//
//    def validateEmail(email: String): Validated[NonEmptyChain[RegistrationRequestError], String] = {
//      val regex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
//      if (regex.r.matches(email)) {
//        val tokens = email.split("@")
//        validate(email, _ => tokens(0).nonEmpty && tokens(1).nonEmpty, RegistrationRequestError.InvalidEmail(email))
//      } else Validated.invalid(NonEmptyChain.one(RegistrationRequestError.InvalidEmail(email)))
//    }
//
//    def validatePassword(password: String, passwordConfirmation: String): Validated[NonEmptyChain[RegistrationRequestError], String] = {
//      def hasSymbolVariety(s: String): Boolean = s.exists(_.isDigit) && s.exists(_.isLetter) && s.exists(!_.isLetterOrDigit)
//
//      (
//        validate(password, _.length >= 8, RegistrationRequestError.PasswordTooShort),
//        validate(password, hasSymbolVariety, RegistrationRequestError.PasswordRequiresGreaterSymbolVariety),
//        validate(password, _ == passwordConfirmation, RegistrationRequestError.PasswordsDoNotMatch)
//      ).mapN((_, _, p) => p) // Modify this if you want to hash the password or do other processing
//    }
//
//    def validateBirthDate(birthYear: String, birthMonth: String, birthDay: String): Validated[NonEmptyChain[RegistrationRequestError], Date] = {
//      val validatedYear: Validated[NonEmptyChain[DateError], Int] =
//        birthYear.toIntOption.toValidated(NonEmptyChain.one(DateError.YearIsNotAnInteger(birthYear)))
//
//      val validatedMonth: Validated[NonEmptyChain[DateError], Int] =
//        for {
//          monthInt <- birthMonth.toIntOption.toValidated(NonEmptyChain.one(DateError.MonthIsNotAnInteger(birthMonth)))
//          _ <- validate(monthInt, m => m >= 1 && m <= 12, DateError.MonthOutOfRange(monthInt))
//        } yield monthInt
//
//      val validatedDay: Validated[NonEmptyChain[DateError], Int] =
//        for {
//          dayInt <- birthDay.toIntOption.toValidated(NonEmptyChain.one(DateError.DayIsNotAnInteger(birthDay)))
//          _ <- validate(dayInt, d => d >= 1 && d <= 31, DateError.DayOutOfRange(dayInt))
//        } yield dayInt
//
//      (
//        validatedYear,
//        validatedMonth,
//        validatedDay
//      ).mapN((y, m, d) => Date(y, m, d))
//        .leftMap(errors => NonEmptyChain.one(RegistrationRequestError.InvalidBirthdayDate(errors)))
//        .andThen(date =>
//          validate(date, _ <= Date.current, RegistrationRequestError.BirthdayDateIsInTheFuture(date))
//        )
//    }
//
//    def validateHeight(height: String): Validated[NonEmptyChain[RegistrationRequestError], Short] =
//      for {
//        heightShort <- height.toShortOption.toValidated(NonEmptyChain.one(RegistrationRequestError.HeightIsNotShort(height)))
//        _ <- validate(heightShort, h => h >= 30 && h <= 300, RegistrationRequestError.InvalidHeight(heightShort))
//      } yield heightShort
//
//    def validateWeight(weight: String): Validated[NonEmptyChain[RegistrationRequestError], Double] =
//      for {
//        weightDouble <- weight.toDoubleOption.toValidated(NonEmptyChain.one(RegistrationRequestError.WeightIsNotDouble(weight)))
//        _ <- validate(weightDouble, w => w >= 30 && w <= 300, RegistrationRequestError.InvalidWeight(weightDouble))
//      } yield weightDouble
//  }