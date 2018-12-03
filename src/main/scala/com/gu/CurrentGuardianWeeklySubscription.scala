package com.gu

import org.joda.time.LocalDate

/**
  * Conditions defining what Guardian Weekly subscription the customer has today
  */
sealed trait CurrentGuardianWeeklyRatePlanConditions
case object RatePlanIsGuardianWeekly extends CurrentGuardianWeeklyRatePlanConditions
case object RatePlanHasNotBeenRemoved extends CurrentGuardianWeeklyRatePlanConditions
case object TodayHasBeenInvoiced extends CurrentGuardianWeeklyRatePlanConditions // FIXME: Invoiced raised today but after running the script?
case object RatePlanHasACharge extends CurrentGuardianWeeklyRatePlanConditions
case object RatePlanHasOnlyOneCharge extends CurrentGuardianWeeklyRatePlanConditions
case object ChargeIsQuarterlyOrAnnual extends CurrentGuardianWeeklyRatePlanConditions

/**
  * Model representing 'What Guardian Weekly does the customer have today?'
  *
  * FIXME: override flag?
  */
case class CurrentGuardianWeeklySubscription(
  subscriptionNumber: String,
  billingPeriod: String,
  price: Float,
  currency: String,
  country: String,
  invoicedPeriod: CurrentInvoicedPeriod
)

/**
  * Invoiced period defined by [startDateIncluding, endDateExcluding) specifies the current period for which
  * the customer has been billed. Today must be within this period.
  *
  * @param startDateIncluding service active on startDateIncluding; corresponds to processedThroughDate
  * @param endDateExcluding service stops one endDateExcluding; corresponds to chargedThroughDate
  */
case class CurrentInvoicedPeriod(
  startDateIncluding: LocalDate,
  endDateExcluding: LocalDate
) {
  private val todayIsWithinCurrentInvoicedPeriod: Boolean = PeriodContainsDate(
    startPeriodInclusive = startDateIncluding,
    endPeriodExcluding = endDateExcluding,
    date = LocalDate.now()
  )
  require(todayIsWithinCurrentInvoicedPeriod, "Today should be within [startDateIncluding, endDateExcluding)")
}

/**
  * What Guardian Weekly does the customer have today?
  *
  * Zuora subscription can have multiple rate plans so this function selects just the one representing
  * current Guardian Weekly subscription. Given a Zuora subscription and account return a single current rate plan
  * attached to Guardian Weekly product that satisfies all of the CurrentGuardianWeeklyRatePlanConditions.
  *
  * @return CurrentGuardianWeeklySubscription current rate plan attached to a Guardian Weekly product
  */
object CurrentGuardianWeeklySubscription extends ((Subscription, Account) => CurrentGuardianWeeklySubscription) {
  def apply(subscription: Subscription, account: Account): CurrentGuardianWeeklySubscription = {
    val currentRatePlans = subscription.ratePlans.filter { ratePlan =>
      List[(CurrentGuardianWeeklyRatePlanConditions, Boolean)](
        RatePlanIsGuardianWeekly -> IsGuardianWeeklyProductRatePlanId(ratePlan),
        RatePlanHasNotBeenRemoved -> (ratePlan.lastChangeType.isEmpty || !ratePlan.lastChangeType.contains("Remove")),
        RatePlanHasACharge -> ratePlan.ratePlanCharges.nonEmpty,
        RatePlanHasOnlyOneCharge -> (ratePlan.ratePlanCharges.size == 1),
        TodayHasBeenInvoiced -> todayIsWithinInvoicedPeriod(ratePlan.ratePlanCharges.head),
        ChargeIsQuarterlyOrAnnual -> List("Annual", "Quarterly").contains(ratePlan.ratePlanCharges.head.billingPeriod)
      ).forall(_._2 == true)
    }

    assert(currentRatePlans.size == 1, s"There should be exactly one current plan: $subscription") // FIXME: Can this be handled?

    val currentRatePlan = currentRatePlans.head
    val currentRatePlanCharge = currentRatePlan.ratePlanCharges.head

    CurrentGuardianWeeklySubscription(
      subscriptionNumber = subscription.subscriptionNumber,
      billingPeriod = currentRatePlanCharge.billingPeriod,
      price = currentRatePlanCharge.price,
      currency = account.billingAndPayment.currency,
      country = account.soldToContact.country,
      invoicedPeriod = CurrentInvoicedPeriod(
        startDateIncluding = currentRatePlanCharge.processedThroughDate.get,
        endDateExcluding = currentRatePlanCharge.chargedThroughDate.get)
    )
  }

  // Invoiced period = [processedThroughDate, chargedThroughDate)
  private def todayIsWithinInvoicedPeriod(ratePlanCharge: RatePlanCharge): Boolean =
    if (notInvoiced(ratePlanCharge))
      false
    else
      PeriodContainsDate(
        startPeriodInclusive = ratePlanCharge.processedThroughDate.get,
        endPeriodExcluding = ratePlanCharge.chargedThroughDate.get,
        date = LocalDate.now()
      )

  private def notInvoiced(ratePlanCharge: RatePlanCharge): Boolean =
    ratePlanCharge.processedThroughDate.isEmpty || ratePlanCharge.chargedThroughDate.isEmpty
}
