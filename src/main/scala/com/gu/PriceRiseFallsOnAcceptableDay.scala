package com.gu

import org.joda.time.LocalDate

object PriceRiseFallsOnAcceptableDay {

  def apply(priceRiseDate: LocalDate, projectedInvoiceItems: List[InvoiceItem]) = {
    projectedInvoiceItems.map(_.serviceStartDate).contains(priceRiseDate)
  }

}
