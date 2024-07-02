package com.fitscore.validation

import scala.annotation.tailrec

enum Chain[+A]:
  case Singleton(a: A)
  case Append(left: Chain[A], right: Chain[A])

  @tailrec
  final def head: A = this match
    case Singleton(a) => a
    case Append(left, _) => left.head

  def tail: Chain[A] = this match
    case Append(Singleton(_), right) => right
    case c @ Append(_, _) => c.listify.tail
    case Singleton(_) => throw new UnsupportedOperationException

  def isEmpty: Boolean = false

  def +:[B >: A](front: B): Chain[B] = Singleton(front) ++ this

  def :+[B >: A](back: B): Chain[B] = this ++ Singleton(back)

  def ++[B >: A](right: Chain[B]): Chain[B] = Append(this, right)

  def reverse: Chain[A] = singletons.reduceLeft(_ ++ _) // O(2*n)

  def foldLeft[B](initial: B)(f: (B, A) => B): B =
    @tailrec
    def loop(chain: Chain[A], helper: Chain[A], result: B): B =
      (chain, helper) match
        case (Singleton(a), Singleton(b)) => f(f(result, a), b)
        case (Singleton(a), Append(l, r)) => loop(l, r, f(result, a))
        case (Append(l, r), rest) => loop(l, Append(r, rest), result)

    this match
      case Singleton(a) => f(initial, a)
      case Append(l, r) => loop(l, r, initial) // O(n)

  def map[B](f: A => B): Chain[B] = flatMap(a => Singleton(f(a))) // O(3*n)

  def flatMap[B](f: A => Chain[B]): Chain[B] =
    singletons.map(s => f(s.head)).reduceLeft((result, chain) => chain ++ result) // O(3*n)

  def singletons: List[Chain[A]] =
    @tailrec // the extracted singletons will be in reverse order inside the resulting list (intended)
    def extractSingletons(chain: Chain[A], helper: Chain[A], result: List[Chain[A]]): List[Chain[A]] =
      (chain, helper) match
        case (s1 @ Singleton(_), s2 @ Singleton(_)) => s2 :: s1 :: result
        case (s @ Singleton(_), Append(l, r)) => extractSingletons(l, r, s :: result)
        case (Append(s @ Singleton(_), r), rest) => extractSingletons(r, rest, s :: result)
        case (Append(l, r), rest) => extractSingletons(l, Append(r, rest), result)

    this match
      case s @ Singleton(_) => List(s)
      case Append(l, r) => extractSingletons(l, r, Nil) // O(n)

  def listify: Chain[A] = this.singletons.reduceLeft((result, singleton) => singleton ++ result) // O(2*n)

  def foreach(f: A => Unit): Unit = foldLeft(())((_, next) => f(next))

  override def equals(that: Any): Boolean = that match
    case c: Chain[?] => this.toList == c.toList
    case _ => false

  override def hashCode: Int = foldLeft(0)(_ * 31 + _.hashCode)

  override def toString: String = toList.mkString("Chain(", ",", ")")

  def toList: List[A] = foldLeft(List.empty[A])((acc, next) => next :: acc).reverse
  def toSet[B >: A]: Set[B] = foldLeft(Set.empty[B])((acc, next) => acc + next)

object Chain:
  def apply[A](head: A, rest: A*): Chain[A] =
    (head +: rest).reverse.map(Singleton(_)).reduceLeft((result, singleton) => singleton ++ result) // O(3*n)

  // Allows Chain to be used in pattern matching
  //
  // As an alternative implementation we can make Chain[A] implement Seq[A] and return it directly,
  // but that requires implementing a couple of more operations which are related to the way
  // Scala collections operate behind the scenes
  def unapplySeq[A](chain: Chain[A]): Option[Seq[A]] = Some(chain.toList)
