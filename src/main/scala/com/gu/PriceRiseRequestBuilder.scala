package com.gu

import com.gu.FileImporter.PriceRise
import org.joda.time.LocalDate

/**
  * Build the body of the request that will actually apply the price rise.
  */
object PriceRiseRequestBuilder {
  def apply(
      currentSubscription: CurrentGuardianWeeklySubscription,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue,
      priceRise: PriceRise
  ): PriceRiseRequest = {

    require(currentSubscription.subscriptionNumber == priceRise.subscriptionName, "")

    val newGuardianWeeklyProducts =
      Country.toFutureGuardianWeeklyProductId(currentSubscription.country) match {
        case Config.Zuora.guardianWeeklyDomesticProductId => newGuardianWeeklyProductCatalogue.domestic
        case Config.Zuora.guardianWeeklyRowProductId => newGuardianWeeklyProductCatalogue.restOfTheWorld
      }

    val request = newGuardianWeeklyProducts
      .find(_.billingPeriod == currentSubscription.billingPeriod)
      .map { newGuardianWeeklyProduct =>
        val removeRatePlan = RemoveRatePlan(
          currentSubscription.ratePlanId,
          priceRise.priceRiseDate
        )

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
          remove = List(removeRatePlan),
          add = List(addProductRatePlan)
        )
      }
      .getOrElse(throw new RuntimeException(s"${currentSubscription.subscriptionNumber} failed to build PriceRiseRequest: $currentSubscription; $priceRise; $newGuardianWeeklyProducts $currentSubscription"))

//    assert(request.remove.head.contractEffectiveDate == request.add.head.contractEffectiveDate)
//    assert(request.add.head.chargeOverrides.get.head.price <= Config.priceRiseFactorCap)

    request
  }

}


