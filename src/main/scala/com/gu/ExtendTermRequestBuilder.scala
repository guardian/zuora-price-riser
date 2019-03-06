package com.gu

import com.gu.ZuoraClient.ExtendTerm
import org.joda.time.{Days, LocalDate}

sealed trait ExtendTermPreCondition
case object TermEndDateIsBeforePriceRiseDate extends ExtendTermPreCondition

/**
  * Optionally extend term if subscriptions is annual and invoice period is beyond term end date.
  */
object ExtendTermRequestBuilder {
  def apply(
    subscription: Subscription,
    priceRiseDate: LocalDate
  ): Option[ExtendTerm] = {

    val (_, unsatisfied) = List(
      TermEndDateIsBeforePriceRiseDate -> subscription.termEndDate.isBefore(priceRiseDate)
    ).partition(_._2)

    if (unsatisfied.isEmpty) {
      val extensionInDays =
        Days.daysBetween(subscription.termEndDate, priceRiseDate).getDays

      Some(ExtendTerm(
        currentTerm = (365 + extensionInDays).toString,
        currentTermPeriodType = "Day"
      ))
    } else None
  }
}
