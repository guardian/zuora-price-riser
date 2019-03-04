package com.gu

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalatest._

class PriceRiseFallsOnAcceptableDaySpec extends FlatSpec with Matchers {

  val zuoraDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  def invoiceItem(
    start: LocalDate
  ) = InvoiceItem(
    "invItem123",
    "A-S12345678",
    start,
    start.plusMonths(3).minusDays(1),
    10,
    "Weekly Charge",
    "Guardian Weekly"
  )

  val standardQuarterlyPaymentSchedule = List(
    invoiceItem(LocalDate.parse("2019-03-25", zuoraDateFormat)),
    invoiceItem(LocalDate.parse("2019-06-25", zuoraDateFormat)),
    invoiceItem(LocalDate.parse("2019-09-25", zuoraDateFormat))
  )

  "PriceRiseFallsOnAcceptableDay" should "return true if the price will go up on the next invoice date" in {
    PriceRiseFallsOnAcceptableDay(
      LocalDate.parse("2019-03-25", zuoraDateFormat),
      standardQuarterlyPaymentSchedule
    ) should be(true)
  }

  "PriceRiseFallsOnAcceptableDay" should "return true if the price will go up 2 quarters after the next invoice date" in {
    PriceRiseFallsOnAcceptableDay(
      LocalDate.parse("2019-09-25", zuoraDateFormat),
      standardQuarterlyPaymentSchedule
    ) should be(true)
  }

  "PriceRiseFallsOnAcceptableDay" should "return false if the price will go up before the next expected invoice" in {
    PriceRiseFallsOnAcceptableDay(
      LocalDate.parse("2019-03-24", zuoraDateFormat),
      standardQuarterlyPaymentSchedule
    ) should be(false)
  }

  "PriceRiseFallsOnAcceptableDay" should "return false if the price will go up on a date which doesn't align with the normal quarterly payment schedule" in {
    PriceRiseFallsOnAcceptableDay(
      LocalDate.parse("2019-03-27", zuoraDateFormat),
      standardQuarterlyPaymentSchedule
    ) should be(false)
  }

}
