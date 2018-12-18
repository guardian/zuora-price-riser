package com.gu

import org.joda.time.LocalDate
import org.scalatest._
import org.joda.time.format.DateTimeFormat

class PeriodContainsDateSpec extends FlatSpec with Matchers {
  val zuoraDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  "PeriodContainsDate" should "include the start bound" in {
    val start = LocalDate.parse("2018-09-01", zuoraDateFormat)
    val end = LocalDate.parse("2018-12-01", zuoraDateFormat)
    val date = LocalDate.parse("2018-09-01", zuoraDateFormat)
    PeriodContainsDate(start, end, date) should be(true)
  }

  it should "exclude the end bound" in {
    val start = LocalDate.parse("2018-09-01", zuoraDateFormat)
    val end = LocalDate.parse("2018-12-01", zuoraDateFormat)
    val date = LocalDate.parse("2018-12-01", zuoraDateFormat)
    PeriodContainsDate(start, end, date) should be(false)
  }

  it should "not contain dates before the start of the period" in {
    val start = LocalDate.parse("2018-09-01", zuoraDateFormat)
    val end = LocalDate.parse("2018-12-01", zuoraDateFormat)
    val date = LocalDate.parse("2018-08-01", zuoraDateFormat)
    PeriodContainsDate(start, end, date) should be(false)
  }

  it should "not contain dates after the end of the period" in {
    val start = LocalDate.parse("2018-09-01", zuoraDateFormat)
    val end = LocalDate.parse("2018-12-01", zuoraDateFormat)
    val date = LocalDate.parse("2018-12-02", zuoraDateFormat)
    PeriodContainsDate(start, end, date) should be(false)
  }
}
