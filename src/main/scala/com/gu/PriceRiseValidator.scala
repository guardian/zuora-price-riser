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
case object TargetPriceRiseIsNotMoreThanTheCap extends PriceRisePreConditions
case object TargetPriceRiseIsNotMoreThanDefaultProductRatePlanChargePrice extends PriceRisePreConditions
case object TargetPriceRiseIsNotLessThanOrEqualToTheCurrentPrice extends PriceRisePreConditions
case object TargetPriceRiseIsMoreThanTheCurrentPrice extends PriceRisePreConditions
case object ThereDoesNotExistAFutureAmendmentOnThePriceRiseDate extends PriceRisePreConditions
case object CurrentlyActiveProductRatePlanIsGuardianWeeklyRatePlan extends PriceRisePreConditions
case object TargetPriceRiseIsNotLessThanCataloguePriceWhenRemovingAllDiscountRatePlans extends PriceRisePreConditions
case object BillingPeriodIsQuarterlyOrAnnually extends PriceRisePreConditions

object PriceRiseValidator {
  def validate(
      priceRise: PriceRise,
      subscription: Subscription,
      account: Account
  ): Boolean = {

    /*
    - if auto-renew == true
    - if status == 'Active'
    - if targetPrice >= default product rate plan charge price
    - if deliveryRegion == currency
    - if given priceRiseDate makes sense (is one day after invoice period end date)
    */

    List[(PriceRisePreConditions, Boolean)](
      SubscriptionIsAutoRenewable -> subscription.autoRenew,
      SubscriptionIsActive -> (subscription.status == "Active"),
      DeliveryRegionMatchesCurrency -> (Country.toCurrency(account.soldToContact.country) == account.billingAndPayment.currency),
      PriceRiseDateIsOnInvoicedPeriodEndDate -> CurrentGuardianWeeklySubscription(subscription, account).invoicedPeriod.endDateExcluding.isEqual(priceRise.priceRiseDate)
    ).forall(_._2 == true)
  }

}
