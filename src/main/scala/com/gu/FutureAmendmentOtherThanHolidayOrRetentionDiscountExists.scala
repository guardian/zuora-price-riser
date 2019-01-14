package com.gu

import com.gu.FileImporter.PriceRise

/**
  * Return list of rate plans on or after price rise date.
  */
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
