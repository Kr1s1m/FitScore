package com.fitscore.utils

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

case class Date(year: Int, month: Int, day: Int) extends Ordered[Date]:
  def expand(date: Date): Int = date.year * 10000 + date.month * 100 + date.day
  infix def isBefore(other: Date): Boolean = expand(this) < expand(other)
  def compare(other: Date): Int =
    if this == other then 0
    else if this isBefore other then -1
    else 1

object Date:
  def applyOption(year: Int, month: Int, day: Int): Option[Date] =
    Try {
      LocalDate.of(year, month, day)

      Date(year, month, day)
    }.toOption

  def toIsoString(year: Int, month: Int, day: Int): String =
    def format(x: Int): String = if x < 10 then s"0$x" else s"$x"
    s"${format(year)}-${format(month)}-${format(day)}"
  def toIsoString(date: Date): String = toIsoString(date.year, date.month, date.day)
  
  val current =
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val list = now.format(formatter).split('-').map(_.toInt)
    Date(list.head, list.tail.head, list.tail.tail.head)
  
