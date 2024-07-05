package com.fitscore.validation

//import com.fitscore.validation.Validated
//import com.fitscore.validation.Validated.toValidated

import cats.Semigroup
import cats.data.{NonEmptyChain, Validated}
import cats.data.Validated.*
import cats.syntax.all.*
import com.fitscore.domain.account.{Account, AccountStatsUpdate, AccountStatsUpdateRequest, RegistrationRequest}
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

import java.time.{LocalDate, LocalDateTime}

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

  def register(regReq: RegistrationRequest): Validated[NonEmptyChain[RegistrationRequestError], Account] =
    (
      validateEmail(regReq.email),
      validateUsername(regReq.username),
      validatePassword(regReq.password, regReq.passwordConfirmation),
      validateBirthDate(regReq.birthYear, regReq.birthMonth, regReq.birthDay),
      validateHeight(regReq.height),
      validateWeight(regReq.weight)
    ).mapN(Account.apply)
  def validateUpdateStats(updReq: AccountStatsUpdateRequest): Validated[NonEmptyChain[RegistrationRequestError], AccountStatsUpdate] =
    (
      Valid(updReq.id),
      validateBirthDate(updReq.birthYear,updReq.birthMonth,updReq.birthDay),
      validateHeight(updReq.height),
      validateWeight(updReq.weight)
    ).mapN(AccountStatsUpdate.apply)
  private def validate[E,A](value: A, p: A => Boolean, e: E): Validated[E,A] = Some(value).filter(p).toValidated(e)
  
  private def validateUsername(username:String): Validated[NonEmptyChain[RegistrationRequestError],String] =
    validate(username,_.nonEmpty,NonEmptyChain(UsernameIsEmpty))

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
    else Invalid(NonEmptyChain(InvalidEmail(email)))

  private def validatePassword(password: String, passwordConfirmation: String): Validated[NonEmptyChain[RegistrationRequestError], String] =
    def hasSymbolVariety(s: String): Boolean = s.exists(_.isDigit) && s.exists(_.isLetter) && s.exists(!_.isLetterOrDigit)
    (
      validate(password, _.length >= 8, NonEmptyChain(PasswordTooShort)),
      validate(password, hasSymbolVariety, NonEmptyChain(PasswordRequiresGreaterSymbolVariety)),
      validate(password, _ == passwordConfirmation, NonEmptyChain(PasswordsDoNotMatch))
    ).mapN((_,_,p) => PasswordUtils.hash(p))

  def validateBirthDate(birthYear: String, birthMonth: String, birthDay: String): Validated[NonEmptyChain[RegistrationRequestError], LocalDate] =
      val validatedYear: Validated[NonEmptyChain[DateError], Int] =
        birthYear.toIntOption match
          case Some(d) => Valid(d)
          case _ => Invalid(NonEmptyChain(YearIsNotAnInteger(birthYear)))


      val validatedMonth: Validated[NonEmptyChain[DateError], Int] =
        birthMonth.toIntOption match
          case Some(d) => if d > 12 || d < 1 then Invalid(NonEmptyChain(MonthOutOfRange(d))) else Valid(d)
          case _ => Invalid(NonEmptyChain(MonthIsNotAnInteger(birthMonth)))


      val validatedDay: Validated[NonEmptyChain[DateError], Int] =
        birthDay.toIntOption match
          case Some(d) => if d > 31 || d < 1 then Invalid(NonEmptyChain(DayOutOfRange(d))) else Valid(d)
          case _ => Invalid(NonEmptyChain(DayIsNotAnInteger(birthDay)))

      (
        validatedYear,
        validatedMonth,
        validatedDay
      ).mapN((y, m, d) => Date.applyOption(y, m, d).toValidated(NonEmptyChain(InvalidDate(y, m, d))))
        .fold(
          errors => Invalid(NonEmptyChain(InvalidBirthdayDate(errors))),
          valid => valid.fold(
            chain => Invalid(NonEmptyChain(InvalidBirthdayDate(chain))),
            date => validate(date, _ <= Date.current, NonEmptyChain(BirthdayDateIsInTheFuture(date))).fold(
              e => Invalid(e),
              d => Valid(LocalDate.of(d.year, d.month, d.day))
            )
          )
        )

  private def validateHeight(height: String): Validated[NonEmptyChain[RegistrationRequestError], Short] =
    height.toShortOption match
      case Some(d) => if d < 30 || d > 300 then Invalid(NonEmptyChain(InvalidHeight(d))) else Valid(d)
      case _ => Invalid(NonEmptyChain(HeightIsNotShort(height)))

  private def validateWeight(height: String): Validated[NonEmptyChain[RegistrationRequestError], Double] =
    height.toDoubleOption match
      case Some(d) => if d < 1.0 || d > 1000.0 then Invalid(NonEmptyChain(InvalidWeight(d))) else Valid(d)
      case _ => Invalid(NonEmptyChain(WeightIsNotDouble(height)))

object AccountsPlayground extends IOApp.Simple:

  def program:IO[Unit] =
    for
      test  <- IO.println(AccountValidator.register(RegistrationRequest("","","2000","3000","29","2","19000","w","w")))
      _  <- IO.println(AccountValidator.validateBirthDate("asdfg","13","32"))
      _  <- IO.println(AccountValidator.validateBirthDate("2889","4","20"))
      _  <- IO.println(Date.applyOption(3985,2,29))
    yield ()

  override def run: IO[Unit] = program
