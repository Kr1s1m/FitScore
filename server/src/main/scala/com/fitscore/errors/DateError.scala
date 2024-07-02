package com.fitscore.errors

enum DateError:
  case YearIsNotAnInteger(year: String)
  case MonthIsNotAnInteger(month: String)
  case DayIsNotAnInteger(day: String)
  case MonthOutOfRange(month: Int)
  case DayOutOfRange(day: Int)
  case InvalidDate(year: Int, month: Int, day: Int)