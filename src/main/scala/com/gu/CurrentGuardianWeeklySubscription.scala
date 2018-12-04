package com.gu

import org.joda.time.LocalDate
import scala.util.Try

/**
  * Conditions defining what Guardian Weekly subscription the customer has today.
  */
sealed trait CurrentGuardianWeeklyRatePlanPredicate extends (RatePlan => Boolean)
case object RatePlanIsGuardianWeekly extends CurrentGuardianWeeklyRatePlanPredicate {
  def apply(ratePlan: RatePlan): Boolean = Config.Zuora.guardianWeeklyProductRatePlanIds.contains(ratePlan.productRatePlanId)
}
case object RatePlanHasNotBeenRemoved extends CurrentGuardianWeeklyRatePlanPredicate {
  def apply(ratePlan: RatePlan): Boolean =
    ratePlan.lastChangeType.isEmpty || !ratePlan.lastChangeType.contains("Remove")
}
case object TodayHasBeenInvoiced extends CurrentGuardianWeeklyRatePlanPredicate { // FIXME: Invoiced raised today but after running the script?
  def apply(ratePlan: RatePlan): Boolean =
    Try {
      PeriodContainsDate(
        startPeriodInclusive = ratePlan.ratePlanCharges.head.processedThroughDate.get,
        endPeriodExcluding = ratePlan.ratePlanCharges.head.chargedThroughDate.get,
        date = LocalDate.now()
      )
    }.getOrElse(false)
}
case object RatePlanHasACharge extends CurrentGuardianWeeklyRatePlanPredicate {
  def apply(ratePlan: RatePlan): Boolean = ratePlan.ratePlanCharges.nonEmpty
}
case object RatePlanHasOnlyOneCharge extends CurrentGuardianWeeklyRatePlanPredicate {
  def apply(ratePlan: RatePlan): Boolean = ratePlan.ratePlanCharges.size == 1
}
case object ChargeIsQuarterlyOrAnnual extends CurrentGuardianWeeklyRatePlanPredicate {
  def apply(ratePlan: RatePlan): Boolean =
    Try(List("Annual", "Quarterly").contains(ratePlan.ratePlanCharges.head.billingPeriod)).getOrElse(false)
}


/**
  * What Guardian Weekly does the customer have today?
  *
  * Zuora subscription can have multiple rate plans so this function selects just the one representing
  * current Guardian Weekly subscription. Given a Zuora subscription return a single current rate plan
  * attached to Guardian Weekly product that satisfies all of the CurrentGuardianWeeklyRatePlanPredicates.
  *
  * @return current RatePlan representing Guardian Weekly
  */
object CurrentGuardianWeeklyRatePlan extends (Subscription => Option[RatePlan]) {
  def apply(subscription: Subscription): Option[RatePlan] =
    subscription.ratePlans.find { ratePlan =>
      List[CurrentGuardianWeeklyRatePlanPredicate](
        RatePlanIsGuardianWeekly,
        RatePlanHasNotBeenRemoved,
        RatePlanHasACharge,
        RatePlanHasOnlyOneCharge,
        TodayHasBeenInvoiced,
        ChargeIsQuarterlyOrAnnual
      ).forall(P => P(ratePlan))
    }
}

/**
  * Model representing 'What Guardian Weekly does the customer have today?'
  *
  * The idea is to have a single unified object as an answer to this question because Zuora's answer is
  * scattered across multiple objects such as Subscription, RatePlan, RatePlanCharge.
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
  * Given a subscription and account Zuora objects it constructs CurrentGuardianWeeklySubscription representing
  * and unified view of what Guardian Weekly customer has today in a single object.
  */
object CurrentGuardianWeeklySubscription extends ((Subscription, Account) => CurrentGuardianWeeklySubscription) {
  def apply(subscription: Subscription, account: Account): CurrentGuardianWeeklySubscription =
    CurrentGuardianWeeklyRatePlan(subscription).map { currentGuardianWeeklyRatePlan =>
      assert(currentGuardianWeeklyRatePlan.ratePlanCharges.size == 1, "RatePlanHasOnlyOneCharge not satisfied")
      assert(currentGuardianWeeklyRatePlan.ratePlanCharges.head.processedThroughDate.isDefined, "TodayHasBeenInvoiced not satisfied")

      CurrentGuardianWeeklySubscription(
        subscriptionNumber = subscription.subscriptionNumber,
        billingPeriod = currentGuardianWeeklyRatePlan.ratePlanCharges.head.billingPeriod,
        price = currentGuardianWeeklyRatePlan.ratePlanCharges.head.price,
        currency = account.billingAndPayment.currency,
        country = account.soldToContact.country,
        invoicedPeriod = CurrentInvoicedPeriod(
          startDateIncluding = currentGuardianWeeklyRatePlan.ratePlanCharges.head.processedThroughDate.get,
          endDateExcluding = currentGuardianWeeklyRatePlan.ratePlanCharges.head.chargedThroughDate.get)
      )
    }.getOrElse(throw new AssertionError(s"Subscription does not have a current Guardian Weekly rate plan: $subscription; $account"))
}

