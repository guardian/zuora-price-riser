package com.gu

import com.gu.FileImporter.PriceRise

trait PriceRisePreCondition
case object SubscriptionIsAutoRenewable extends PriceRisePreCondition
case object SubscriptionIsActive extends PriceRisePreCondition
case object DeliveryRegionMatchesCurrency extends PriceRisePreCondition
case object PriceRiseDateIsOnInvoicedPeriodEndDate extends PriceRisePreCondition
case object ImportHasCorrectCurrentPrice extends PriceRisePreCondition
case object TargetPriceRiseIsNotMoreThanTheCap extends PriceRisePreCondition
case object TargetPriceRiseIsNotMoreThanDefaultProductRatePlanChargePrice extends PriceRisePreCondition
case object TargetPriceRiseIsNotLessThanOrEqualToTheCurrentPrice extends PriceRisePreCondition
case object TargetPriceRiseIsMoreThanTheCurrentPrice extends PriceRisePreCondition
case object ThereDoesNotExistAFutureAmendmentOnThePriceRiseDate extends PriceRisePreCondition
case object CurrentlyActiveProductRatePlanIsGuardianWeeklyRatePlan extends PriceRisePreCondition
case object TargetPriceRiseIsNotLessThanCataloguePriceWhenRemovingAllDiscountRatePlans extends PriceRisePreCondition
case object BillingPeriodIsQuarterlyOrAnnually extends PriceRisePreCondition

/**
  * Check pre-conditions, before price rise is written to Zuora, by cross-referencing
  * data from input file with current state of Zuora. Essentially we do not trust the given input file.
  */
object CheckPriceRisePreConditions {
  type UnsatisfiedPriceRisePreConditions = List[PriceRisePreCondition]

  def apply(
      priceRise: PriceRise,
      subscription: Subscription,
      account: Account,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue
  ): UnsatisfiedPriceRisePreConditions = {

    val currentSubscription = CurrentGuardianWeeklySubscription(subscription, account)
    val futureGuardianWeeklyProducts = Country.toFutureGuardianWeeklyProductId(account.soldToContact.country) match {
      case Config.Zuora.guardianWeeklyDomesticProductId => newGuardianWeeklyProductCatalogue.domestic
      case Config.Zuora.guardianWeeklyRowProductId => newGuardianWeeklyProductCatalogue.restOfTheWorld
    }

    val (_, unsatisfied) = List[(PriceRisePreCondition, Boolean)](
      SubscriptionIsAutoRenewable -> subscription.autoRenew,
      SubscriptionIsActive -> (subscription.status == "Active"),
      DeliveryRegionMatchesCurrency -> (Country.toCurrency(account.soldToContact.country) == account.billingAndPayment.currency),
      PriceRiseDateIsOnInvoicedPeriodEndDate -> currentSubscription.invoicedPeriod.endDateExcluding.isEqual(priceRise.priceRiseDate),
      ImportHasCorrectCurrentPrice -> (currentSubscription.price == priceRise.currentPrice),
      TargetPriceRiseIsNotMoreThanTheCap -> (priceRise.currentPrice * Config.priceRiseFactorCap <= priceRise.newPrice),
      TargetPriceRiseIsNotMoreThanDefaultProductRatePlanChargePrice -> (DefaultCataloguePrice(futureGuardianWeeklyProducts, currentSubscription) == priceRise.newPrice),
    ).partition(_._2)

    unsatisfied.map(_._1)
  }

}
