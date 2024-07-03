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
import cats.Invariant.catsApplicativeForArrow
import cats.effect.{IO, IOApp}
import com.fitscore.utils.Date.toIsoString
import com.fitscore.validation.AccountValidator.validateBirthDate

given Semigroup[RegistrationRequestError] with
  def combine(x: RegistrationRequestError, y: RegistrationRequestError): RegistrationRequestError = (x, y) match {
    case (a, b) => b
  }

given Semigroup[DateError] with
  def combine(x: DateError, y: DateError): DateError = (x, y) match {
    case (a, b) => a
  }


  extension [A](opt: Option[A])
    def toValidated[E](onEmpty: => E): Validated[E, A] =
      opt.fold(Invalid(onEmpty))(Valid(_))

object AccountValidator:

  //TODO: Maybe errors arent actually being chained and need to be wrapped in NonEmptyChain
  def register(regReq: RegistrationRequest): Validated[NonEmptyChain[RegistrationRequestError], Account] =
    (
      AccountValidator.validateEmail(regReq.email),
      AccountValidator.validateUsername(regReq.username),
      AccountValidator.validatePassword(regReq.password, regReq.passwordConfirmation),
      AccountValidator.validateBirthDateFinal(regReq.birthYear, regReq.birthMonth, regReq.birthDay),
      AccountValidator.validateHeight(regReq.height),
      AccountValidator.validateWeight(regReq.weight)
    ).mapN(Account.apply)

  private def validate[E,A](value: A, p: A => Boolean, e: E): Validated[E,A] = Some(value).filter(p).toValidated(e)
  
  private def validateUsername(username:String): Validated[NonEmptyChain[RegistrationRequestError],String] =
    validate(username,_.nonEmpty,NonEmptyChain(NameIsEmpty))

  def validateBirthDateFinal(year: String,month:String,day:String ): Validated[NonEmptyChain[RegistrationRequestError], String] =
    validateBirthDateLastCheck(year,month,day) match
      case Valid(d) => Valid(toIsoString(d))
      case Invalid(chain) => Invalid(NonEmptyChain(RegistrationRequestError.InvalidBirthdayDate(chain)))

  private def validateEmail(email: String): Validated[NonEmptyChain[RegistrationRequestError], String] =
    val regex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
    if regex.r.matches(email) then
      def checkEmpty(e: String): Boolean =
        val tokens = e.split("@"); tokens(0).nonEmpty && tokens(1).nonEmpty
      validate(
        email,
        checkEmpty,
        NonEmptyChain(InvalidEmail(email))
      )
    else Validated.invalid(NonEmptyChain(InvalidEmail(email)))

  private def validatePassword(password: String, passwordConfirmation: String): Validated[NonEmptyChain[RegistrationRequestError], String] =
    def hasSymbolVariety(s: String): Boolean = s.exists(_.isDigit) && s.exists(_.isLetter) && s.exists(!_.isLetterOrDigit)
    (
      validate(password, _.length >= 8, NonEmptyChain(PasswordTooShort)),
      validate(password, hasSymbolVariety, NonEmptyChain(PasswordRequiresGreaterSymbolVariety)),
      validate(password, _ == passwordConfirmation, NonEmptyChain(PasswordsDoNotMatch))
    ).mapN((_,_,p) => PasswordUtils.hash(p))

  def validateBirthDate(birthYear: String, birthMonth: String, birthDay: String): Validated[NonEmptyChain[DateError], Date] =
      val validatedYear: Validated[NonEmptyChain[DateError], Int] = birthYear.toIntOption match
        case Some(d) => Valid(d)
        case _ => Validated.invalid(NonEmptyChain(DateError.YearIsNotAnInteger(birthYear)))


      val validatedMonth: Validated[NonEmptyChain[DateError], Int] = birthMonth.toIntOption match
        case Some(d) => if d>12 || d<1 then Validated.invalid(NonEmptyChain(DateError.MonthOutOfRange(d))) else Valid(d)
        case _ => Validated.invalid(NonEmptyChain(DateError.MonthIsNotAnInteger(birthMonth)))


      val validatedDay: Validated[NonEmptyChain[DateError], Int] = birthDay.toIntOption match
        case Some(d) => if d>31 || d<1 then Validated.invalid(NonEmptyChain(DateError.DayOutOfRange(d))) else Valid(d)
        case _ => Validated.invalid(NonEmptyChain(DateError.DayIsNotAnInteger(birthDay)))

      (
        validatedYear,
        validatedMonth,
        validatedDay
      ).mapN(Date.apply)

  def validateBirthDateLastCheck(year:String,month:String,day:String): Validated[NonEmptyChain[DateError], Date] =
    validateBirthDate(year,month,day) match
      case Valid(d) => Date.applyOption(year.toInt,month.toInt,day.toInt).toValidated(NonEmptyChain(DateError.InvalidDate(year.toInt,month.toInt,day.toInt)))
      case errors => errors

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
      
  private def validateHeight(height: String): Validated[NonEmptyChain[RegistrationRequestError], Short] = height.toShortOption match
      case Some(d) => if d<30 || d>300 then Validated.invalid(NonEmptyChain(RegistrationRequestError.InvalidHeight(d))) else Valid(d)
      case _ => Validated.invalid(NonEmptyChain(RegistrationRequestError.HeightIsNotShort(height)))

  private def validateWeight(height: String): Validated[NonEmptyChain[RegistrationRequestError], Double] = height.toDoubleOption match
    case Some(d) => if d < 1.0 || d > 1000.0 then Validated.invalid(NonEmptyChain(RegistrationRequestError.InvalidWeight(d))) else Valid(d)
    case _ => Validated.invalid(NonEmptyChain(RegistrationRequestError.WeightIsNotDouble(height)))

object AccountsPlayground extends IOApp.Simple:

  def program:IO[Unit] =
    for
      test  <- IO.println(AccountValidator.register(RegistrationRequest("","","2000","3000","29","2","19000","w","w")))
      _  <- IO.println(AccountValidator.validateBirthDateFinal("1986","2","29"))
      _  <- IO.println(Date.applyOption(3985,2,29))
    yield ()

  override def run: IO[Unit] = program
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