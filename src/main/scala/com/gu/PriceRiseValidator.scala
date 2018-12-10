package com.gu

import com.gu.FileImporter.PriceRise

/*
PriceRisePreConditions
PriceRisePostConditions
Execute
PriceRisePostDeployConditions
 */

trait PriceRisePreConditions
case object SubscriptionIsAutoRenewable extends PriceRisePreConditions
case object SubscriptionIsActive extends PriceRisePreConditions
case object DeliveryRegionMatchesCurrency extends PriceRisePreConditions
case object PriceRiseDateIsOnInvoicedPeriodEndDate extends PriceRisePreConditions
case object ImportHasCorrectCurrentPrice extends PriceRisePreConditions
case object TargetPriceRiseIsNotMoreThanTheCap extends PriceRisePreConditions
case object TargetPriceRiseIsNotMoreThanDefaultProductRatePlanChargePrice extends PriceRisePreConditions
case object TargetPriceRiseIsNotLessThanOrEqualToTheCurrentPrice extends PriceRisePreConditions
case object TargetPriceRiseIsMoreThanTheCurrentPrice extends PriceRisePreConditions
case object ThereDoesNotExistAFutureAmendmentOnThePriceRiseDate extends PriceRisePreConditions
case object CurrentlyActiveProductRatePlanIsGuardianWeeklyRatePlan extends PriceRisePreConditions
case object TargetPriceRiseIsNotLessThanCataloguePriceWhenRemovingAllDiscountRatePlans extends PriceRisePreConditions
case object BillingPeriodIsQuarterlyOrAnnually extends PriceRisePreConditions

object PriceRiseValidation {
  type UnsatisfiedPriceRisePreConditions = List[PriceRisePreConditions]

  def apply(
      priceRise: PriceRise,
      subscription: Subscription,
      account: Account,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue
  ): UnsatisfiedPriceRisePreConditions = {

    /*
    - if auto-renew == true
    - if status == 'Active'
    - if targetPrice >= default product rate plan charge price
    - if deliveryRegion == currency
    - if given priceRiseDate makes sense (is one day after invoice period end date)
    */

    val currentSubscription = CurrentGuardianWeeklySubscription(subscription, account)
    val futureGuardianWeeklyProducts = Country.toFutureGuardianWeeklyProductId(account.soldToContact.country) match {
      case Config.Zuora.guardianWeeklyDomesticProductId => newGuardianWeeklyProductCatalogue.domestic
      case Config.Zuora.guardianWeeklyRowProductId => newGuardianWeeklyProductCatalogue.restOfTheWorld
    }

    val (_, unsatisfied) = List[(PriceRisePreConditions, Boolean)](
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
