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

    import Config.Zuora._
    val removeRatePlans: List[RemoveRatePlan] =
      subscription
        .ratePlans
        .filterNot(ratePlan => doNotRemoveProductRatePlanIds.contains(ratePlan.productRatePlanId))
        .map(ratePlan => RemoveRatePlan(ratePlan.id, priceRise.priceRiseDate))

    assert(removeRatePlans.map(_.ratePlanId).contains(currentGuardianWeeklySubscription.ratePlanId), "Current Guardian Weekly rate plan should be removed.")
    removeRatePlans
  }

}
