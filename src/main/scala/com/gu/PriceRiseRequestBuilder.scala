package com.gu

import com.gu.FileImporter.PriceRise

/**
  * Build the body of the request that will actually apply the price rise.
  */
object PriceRiseRequestBuilder {
  def apply(
      subscription: Subscription,
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue,
      priceRise: PriceRise
  ): PriceRiseRequest = {

    require(currentGuardianWeeklySubscription.subscriptionNumber == priceRise.subscriptionName, "Price rise applied to a wrong subscription.")

    val newGuardianWeeklyProduct =
      NewGuardianWeeklyProduct(currentGuardianWeeklySubscription, newGuardianWeeklyProductCatalogue)

    val chargeOverride = ChargeOverride(
      newGuardianWeeklyProduct.productRatePlanChargeId,
      priceRise.newPrice
    )

    val addProductRatePlan = AddProductRatePlan(
      newGuardianWeeklyProduct.productRatePlanId,
      priceRise.priceRiseDate,
      Some(List(chargeOverride))
    )

    PriceRiseRequest(
      remove = RemoveRatePlans(subscription, currentGuardianWeeklySubscription, priceRise),
      add = List(addProductRatePlan)
    )
  }
}


