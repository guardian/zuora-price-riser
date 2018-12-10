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

    require(currentGuardianWeeklySubscription.subscriptionNumber == priceRise.subscriptionName, "")

    val newGuardianWeeklyProducts =
      Country.toFutureGuardianWeeklyProductId(currentGuardianWeeklySubscription.country) match {
        case Config.Zuora.guardianWeeklyDomesticProductId => newGuardianWeeklyProductCatalogue.domestic
        case Config.Zuora.guardianWeeklyRowProductId => newGuardianWeeklyProductCatalogue.restOfTheWorld
      }

    newGuardianWeeklyProducts
      .find(_.billingPeriod == currentGuardianWeeklySubscription.billingPeriod)
      .map { newGuardianWeeklyProduct =>
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
      .getOrElse(throw new RuntimeException(s"${currentGuardianWeeklySubscription.subscriptionNumber} failed to build PriceRiseRequest: $currentGuardianWeeklySubscription; $priceRise; $newGuardianWeeklyProducts $currentGuardianWeeklySubscription"))
  }

}


