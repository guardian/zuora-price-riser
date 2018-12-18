package com.gu

import com.gu.FileImporter.PriceRise

import scala.util.Try

trait PriceRisePreCondition
case object SubscriptionIsAutoRenewable extends PriceRisePreCondition
case object SubscriptionIsActive extends PriceRisePreCondition
case object DeliveryRegionMatchesCurrency extends PriceRisePreCondition
case object PriceRiseDateIsOnInvoicedPeriodEndDate extends PriceRisePreCondition
case object ImportHasCorrectCurrentPrice extends PriceRisePreCondition
case object TargetPriceRiseIsNotMoreThanTheCap extends PriceRisePreCondition
case object TargetPriceRiseIsNotMoreThanDefaultProductRatePlanChargePrice extends PriceRisePreCondition
case object TargetPriceRiseIsMoreThanTheCurrentPrice extends PriceRisePreCondition
case object ThereDoesNotExistAFutureAmendmentOnThePriceRiseDate extends PriceRisePreCondition
case object CurrentlyActiveProductRatePlanIsGuardianWeeklyRatePlan extends PriceRisePreCondition
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

    val currentGuardianWeeklySubscription = CurrentGuardianWeeklySubscription(subscription, account)
    val futureGuardianWeeklyProducts = GuardianWeeklyProduct(currentGuardianWeeklySubscription, newGuardianWeeklyProductCatalogue)

    val (_, unsatisfied) = List[(PriceRisePreCondition, Boolean)](
      SubscriptionIsAutoRenewable -> subscription.autoRenew,
      SubscriptionIsActive -> (subscription.status == "Active"),
      DeliveryRegionMatchesCurrency -> (Country.toCurrency(account.soldToContact.country) == account.billingAndPayment.currency),
      PriceRiseDateIsOnInvoicedPeriodEndDate -> currentGuardianWeeklySubscription.invoicedPeriod.endDateExcluding.isEqual(priceRise.priceRiseDate),
      ImportHasCorrectCurrentPrice -> (currentGuardianWeeklySubscription.price == priceRise.currentPrice),
      TargetPriceRiseIsNotMoreThanTheCap -> (priceRise.newPrice <= priceRise.currentPrice * Config.priceRiseFactorCap),
      TargetPriceRiseIsNotMoreThanDefaultProductRatePlanChargePrice -> (priceRise.newPrice <= DefaultCataloguePrice(futureGuardianWeeklyProducts, currentGuardianWeeklySubscription)),
      TargetPriceRiseIsMoreThanTheCurrentPrice -> (priceRise.newPrice > currentGuardianWeeklySubscription.price),
      CurrentlyActiveProductRatePlanIsGuardianWeeklyRatePlan -> Config.Zuora.guardianWeeklyProductRatePlanIds.contains(currentGuardianWeeklySubscription.productRatePlanId),
      BillingPeriodIsQuarterlyOrAnnually -> List("Annual", "Quarter").contains(currentGuardianWeeklySubscription.billingPeriod),
      ThereDoesNotExistAFutureAmendmentOnThePriceRiseDate ->
        Try {
          val futureRatePlanExists = subscription.ratePlans.exists { ratePlan =>
              val effectiveStartDate = ratePlan.ratePlanCharges.head.effectiveStartDate
              effectiveStartDate.isEqual(priceRise.priceRiseDate) || effectiveStartDate.isAfter(priceRise.priceRiseDate)
          }
          !futureRatePlanExists
        }.getOrElse(false),
    ).partition(_._2)

    unsatisfied.map(_._1)
  }

}
