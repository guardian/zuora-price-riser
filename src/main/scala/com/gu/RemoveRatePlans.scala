package com.gu

import com.gu.FileImporter.PriceRise

/**
  * Remove current guardian weekly (only), and all discounts except holiday and retention.
  */
object RemoveRatePlans {
  def apply(
      subscription: Subscription,
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription,
      priceRise: PriceRise
  ): List[RemoveRatePlan] = {

    val removeCurrentGuardianWeeklySubscriptionRatePlan =
      List(RemoveRatePlan(currentGuardianWeeklySubscription.ratePlanId, priceRise.priceRiseDate))

    import Config.Zuora._
    val removeDiscountsOtherThanHolidayAndRetention: List[RemoveRatePlan] =
      subscription
        .ratePlans
        .filterNot(ratePlan => doNotRemoveProductRatePlanIds.contains(ratePlan.productRatePlanId))
        .filter(_.productName == "Discounts")
        .map(ratePlan => RemoveRatePlan(ratePlan.id, priceRise.priceRiseDate))

    removeCurrentGuardianWeeklySubscriptionRatePlan ++ removeDiscountsOtherThanHolidayAndRetention
  }

}
