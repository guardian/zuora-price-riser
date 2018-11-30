package com.gu

import java.time.LocalDate

case class SubscriptionDates(
  invoiceDate: LocalDate,
  dueDate: LocalDate,
  servicePeriodStartDate: LocalDate,
  servicePeriodEndDate: LocalDate,
  termStartDate: LocalDate,
  termEndDate: LocalDate,
  processedThroughDate: LocalDate,
  chargedThroughDate: LocalDate,
  effectiveStartDate: LocalDate,
  effectiveEndDate: LocalDate
) {
  def todayIsWithinInvoicedBillingPeriod: Boolean = {
    val today = LocalDate.now()
    (today.isEqual(processedThroughDate) || today.isAfter(processedThroughDate)) && today.isBefore(chargedThroughDate)
  }
}

