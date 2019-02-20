package com.gu

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalatest._

class AtLeastOneYearPassedSpec extends FlatSpec with Matchers {
  val zuoraDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  "AtLeastOneYearPassed" should "include the bounds" in {
    val start = LocalDate.parse("2018-01-01", zuoraDateFormat)
    val end = LocalDate.parse("2019-01-01", zuoraDateFormat)
    AtLeastOneYearPassed(start, end) should be(true)
  }

  it should "return false if one day before" in {
    val start = LocalDate.parse("2018-01-01", zuoraDateFormat)
    val end = LocalDate.parse("2018-12-31", zuoraDateFormat)
    AtLeastOneYearPassed(start, end) should be(false)
  }

  it should "return true if few years passed" in {
    val start = LocalDate.parse("2018-01-01", zuoraDateFormat)
    val end = LocalDate.parse("2069-12-31", zuoraDateFormat)
    AtLeastOneYearPassed(start, end) should be(true)
  }
}
