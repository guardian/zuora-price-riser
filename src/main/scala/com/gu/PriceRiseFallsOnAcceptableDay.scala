package com.gu

import org.joda.time.LocalDate

object PriceRiseFallsOnAcceptableDay {

  def apply(priceRiseDate: LocalDate, firstDayOfNextInvoicePeriod: LocalDate, billingPeriod: String) = {
    val acceptablePriceRiseDates = if (billingPeriod == "Quarter") {
      List(firstDayOfNextInvoicePeriod, firstDayOfNextInvoicePeriod.plusMonths(3), firstDayOfNextInvoicePeriod.plusMonths(6), firstDayOfNextInvoicePeriod.plusMonths(9))
    } else {
      List(firstDayOfNextInvoicePeriod)
    }
    acceptablePriceRiseDates.contains(priceRiseDate)
  }

}
