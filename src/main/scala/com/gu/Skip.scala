package com.gu

import com.gu.FileImporter.PriceRise

sealed trait SkipReason
case object OneOff extends SkipReason
case object Cancelled extends SkipReason
case object PriceRiseApplied extends SkipReason
case object FutureAmendmentExists extends SkipReason

/**
  * Check if the price rise should be skipped.
  */
object Skip {
  def apply(
      subscriptionBefore: Subscription,
      accountBefore: Account,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue,
      priceRise: PriceRise
  ): List[SkipReason] = {
    val (satisfied, _) = List[(SkipReason, Boolean)](
      OneOff -> (!subscriptionBefore.autoRenew),
      Cancelled -> (subscriptionBefore.status == "Cancelled"),
      PriceRiseApplied -> PriceRiseAlreadyApplied(subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue),
      FutureAmendmentExists ->
        subscriptionBefore
          .ratePlans
          .exists { ratePlan =>
            val effectiveStartDate = ratePlan.ratePlanCharges.head.effectiveStartDate // Let it crash if no head
            effectiveStartDate.isEqual(priceRise.priceRiseDate) || effectiveStartDate.isAfter(priceRise.priceRiseDate)
          }
    ).partition(_._2)

    satisfied.map(_._1)
  }
}
