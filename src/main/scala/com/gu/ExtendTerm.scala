package com.gu

import com.gu.ZuoraClient.ExtendTerm
import org.joda.time.Days

sealed trait ExtendTermPreCondition
case object BillingTermIsAnnual extends ExtendTermPreCondition
case object TermEndDateIsBeforeInvoicePeriodEndDate extends ExtendTermPreCondition

/**
  * Optionally extend term if subscriptions is annual and invoice period is outside term.
  */
object ExtendTermBuilder {
  def apply(
      subscription: Subscription,
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription
  ): Option[ExtendTerm] = {

    val (_, unsatisfied) = List(
      BillingTermIsAnnual -> (currentGuardianWeeklySubscription.billingPeriod == "Annual"),
      TermEndDateIsBeforeInvoicePeriodEndDate -> subscription.termEndDate.isBefore(currentGuardianWeeklySubscription.invoicedPeriod.endDateExcluding)
    ).partition(_._2)

    if (unsatisfied.isEmpty) {
      val extensionInDays: Int = Days.daysBetween(
        subscription.termEndDate,
        currentGuardianWeeklySubscription.invoicedPeriod.endDateExcluding
      ).getDays

      Some(ExtendTerm(
        currentTerm = (365 + extensionInDays).toString,
        currentTermPeriodType = "Day"
      ))
    } else None
  }
}
