package com.gu

import org.joda.time.{LocalDate, Years}

object AtLeastOneYearPassed {
  def apply(start: LocalDate, end: LocalDate): Boolean =
    Years.yearsBetween(start, end).getYears >= 1
}
