package com.fitscore.validation

sealed trait Validated[+E, +A]:
  def isValid: Boolean = fold(_ => false, _ => true)
//    this match
//      case Valid(_) => true
//      case _ => false

  def getOrElse[B >: A](default: => B): B = fold(_ => default, identity)
//    this match
//      case Valid(valid) => valid
//      case _ => default

  def orElse[F >: E, B >: A](default: => Validated[F, B]): Validated[F, B] = fold(_ => default, _ => this)
//    this match
//      case Valid(_) => this
//      case _ => default

  infix def zip[EE >: E, B](vb: Validated[EE, B]): Validated[EE, (A, B)] =
    (this, vb) match
      case (Valid(v1), Valid(v2)) => Valid((v1, v2))
      case (Valid(_), invalid @ Invalid(_)) => invalid
      case (invalid @ Invalid(_), Valid(_)) => invalid
      case (Invalid(e1), Invalid(e2)) => Invalid(e1 ++ e2)

  def map[B](f: A => B): Validated[E, B] = flatMap(v => Valid(f(v)))

  def zipMap[EE >: E, B, R](vb: Validated[EE, B])(f: (A, B) => R): Validated[EE, R] = zip(vb).map(f.tupled)

  def flatMap[EE >: E, B](f: A => Validated[EE, B]): Validated[EE, B] = fold(Invalid(_), f)
//    this match
//      case Valid(value) => f(value)
//      case invalid @ Invalid(_) => invalid

  def fold[B](fInvalid: Chain[E] => B, f: A => B): B =
    this match
      case Valid(value) => f(value)
      case Invalid(chain) => fInvalid(chain)

  def foreach(f: A => Unit): Unit = fold(_ => (), f)

case class Valid[+A](value: A) extends Validated[Nothing, A]
case class Invalid[+E](errors: Chain[E]) extends Validated[E, Nothing]

object Invalid:
  def apply[E](error: E): Invalid[E] = Invalid(Chain(error))

object Validated:
  extension [EE, A, B](
    tuple: (
      Validated[EE, A],
      Validated[EE, B]
    )
  )
    def zipN: Validated[EE, (A, B)] = tuple._1 zip tuple._2

    def mapN[R](f: (A, B) => R): Validated[EE, R] = tuple.zipN.map(f.tupled)

  extension [EE, A, B, C](
    tuple: (
      Validated[EE, A],
      Validated[EE, B],
      Validated[EE, C]
    )
  )
    def zipN: Validated[EE, (A, B, C)] = tuple.head.zip(tuple.tail.zipN).map(t => t.head *: t.last)
//      ((tuple._1, tuple._2).zipN zip tuple._3)
//        .map { case ((a, b), c) => (a, b, c) }
    def mapN[R](f: (A, B, C) => R): Validated[EE, R] = tuple.zipN.map(f.tupled)

  extension [EE, A, B, C, D](
    tuple: (
      Validated[EE, A],
      Validated[EE, B],
      Validated[EE, C],
      Validated[EE, D]
    )
  )
    def zipN: Validated[EE, (A, B, C, D)] = tuple.head.zip(tuple.tail.zipN).map(t => t.head *: t.last)
//      ((tuple._1, tuple._2, tuple._3).zipN zip tuple._4)
//        .map { case ((a, b, c), d) => (a, b, c, d) }
    def mapN[R](f: (A, B, C, D) => R): Validated[EE, R] = tuple.zipN.map(f.tupled)

  extension [EE, A, B, C, D, E](
    tuple: (
      Validated[EE, A],
      Validated[EE, B],
      Validated[EE, C],
      Validated[EE, D],
      Validated[EE, E]
    )
  )
    def zipN: Validated[EE, (A, B, C, D, E)] = tuple.head.zip(tuple.tail.zipN).map(t => t.head *: t.last)
//      ((tuple._1, tuple._2, tuple._3, tuple._4).zipN zip tuple._5)
//        .map { case ((a, b, c, d), e) => (a, b, c, d, e) }
    def mapN[R](f: (A, B, C, D, E) => R): Validated[EE, R] = tuple.zipN.map(f.tupled)
  extension [EE, A, B, C, D, E, F](
    tuple: (
      Validated[EE, A],
      Validated[EE, B],
      Validated[EE, C],
      Validated[EE, D],
      Validated[EE, E],
      Validated[EE, F]
    )
  )
    def zipN: Validated[EE, (A, B, C, D, E, F)] = tuple.head.zip(tuple.tail.zipN).map(t => t.head *: t.last)
//      ((tuple._1, tuple._2, tuple._3, tuple._4).zipN zip tuple._5)
//        .map { case ((a, b, c, d), e) => (a, b, c, d, e) }
    def mapN[R](f: (A, B, C, D, E, F) => R): Validated[EE, R] = tuple.zipN.map(f.tupled)

  def sequence[E, A](xs: List[Validated[E, A]]): Validated[E, List[A]] =
    val init: Validated[E, List[A]] = Valid(List.empty)
    xs.reverse.foldLeft(init)((res, va) => va.zipMap(res)(_ :: _))
//    val errors = for case Invalid(error) <- xs yield error
//    errors match
//      case Nil => val values = for case Valid(value) <- xs yield value; Valid(values)
//      case _ => val chainedErrors = errors.reduceLeft(_ ++ _); Invalid(chainedErrors)

  extension [A](opt: Option[A])
    def toValidated[E](onEmpty: => E): Validated[E, A] =
      opt.fold(Invalid(onEmpty))(Valid(_))
//      opt match
//        case Some(value) => Valid(value)
//        case None => Invalid(onEmpty)
