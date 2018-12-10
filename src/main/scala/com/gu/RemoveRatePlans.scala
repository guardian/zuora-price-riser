package com.gu

import com.gu.FileImporter.PriceRise

/**
  * Remove all rate plans but Holiday and Retention Discount
  */
object RemoveRatePlans {
  def apply(
      subscription: Subscription,
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription,
      priceRise: PriceRise
  ): List[RemoveRatePlan] = {
    val removeRatePlans: List[RemoveRatePlan] =
      subscription.ratePlans
        .map(_.id)
        .filterNot(Config.Zuora.doNotRemoveProductRatePlanIds.contains)
        .map(ratePlanId => RemoveRatePlan(ratePlanId, priceRise.priceRiseDate))

    assert(removeRatePlans.map(_.ratePlanId).contains(currentGuardianWeeklySubscription.ratePlanId), "Current Guardian Weekly rate plan should be removed.")
    removeRatePlans
  }

}
