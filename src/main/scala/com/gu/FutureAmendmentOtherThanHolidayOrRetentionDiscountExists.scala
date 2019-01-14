package com.gu

import com.gu.FileImporter.PriceRise

object FutureAmendmentOtherThanHolidayOrRetentionDiscountExists {
  def apply(
      subscription: Subscription,
      priceRise: PriceRise
  ): Boolean = {

    !subscription
      .ratePlans
      .filter { ratePlan =>
        val effectiveStartDate = ratePlan.ratePlanCharges.head.effectiveStartDate // Let it crash if no head
        effectiveStartDate.isEqual(priceRise.priceRiseDate) || effectiveStartDate.isAfter(priceRise.priceRiseDate)
      }
      .map(_.productRatePlanId)
      .forall(Config.Zuora.doNotRemoveProductRatePlanIds.contains)
  }
}
