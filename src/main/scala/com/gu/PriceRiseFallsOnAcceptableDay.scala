package com.gu

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.LocalDate

object PriceRiseFallsOnAcceptableDay extends LazyLogging {

  def apply(priceRiseDate: LocalDate, projectedInvoiceItems: List[InvoiceItem]) = {
    val acceptablePriceRiseDates = projectedInvoiceItems.map(_.serviceStartDate)
    val dateIsAcceptable = acceptablePriceRiseDates.contains(priceRiseDate)
    if (!dateIsAcceptable) logger.warn(s"Price rise date in input file ($priceRiseDate) is unacceptable. Acceptable dates are: $acceptablePriceRiseDates")
    dateIsAcceptable
  }

}
