package com.gu

import org.joda.time.LocalDate

/**
  * Is date between two dates where including the start while excluding the end?
  */
object PeriodContainsDate extends ((LocalDate, LocalDate, LocalDate) => Boolean) {
  def apply(
      startPeriodInclusive: LocalDate,
      endPeriodExcluding: LocalDate,
      date: LocalDate
  ): Boolean =
    (date.isEqual(startPeriodInclusive) || date.isAfter(startPeriodInclusive)) && date.isBefore(endPeriodExcluding)
}
