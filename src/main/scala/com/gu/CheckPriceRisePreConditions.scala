package com.gu

import com.gu.FileImporter.PriceRise

import scala.util.Try

trait PriceRisePreCondition
case object SubscriptionIsAutoRenewable extends PriceRisePreCondition
case object SubscriptionIsActive extends PriceRisePreCondition
case object PriceRiseDateIsOnInvoicedPeriodEndDate extends PriceRisePreCondition
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
  import Config.Zuora._

  def apply(
      priceRise: PriceRise,
      subscription: Subscription,
      account: Account,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue
  ): UnsatisfiedPriceRisePreConditions = {

    val currentGuardianWeeklySubscription = CurrentGuardianWeeklySubscription(subscription, account)
    val futureGuardianWeeklyProducts = NewGuardianWeeklyProduct(currentGuardianWeeklySubscription, newGuardianWeeklyProductCatalogue)

    val (_, unsatisfied) = List[(PriceRisePreCondition, Boolean)](
      SubscriptionIsAutoRenewable -> subscription.autoRenew,
      SubscriptionIsActive -> (subscription.status == "Active"),
      PriceRiseDateIsOnInvoicedPeriodEndDate -> currentGuardianWeeklySubscription.invoicedPeriod.endDateExcluding.isEqual(priceRise.priceRiseDate),
      TargetPriceRiseIsNotMoreThanTheCap -> (priceRise.newPrice < currentGuardianWeeklySubscription.price * Config.priceRiseFactorCap),
      TargetPriceRiseIsNotMoreThanDefaultProductRatePlanChargePrice -> (priceRise.newPrice <= CatalogPriceExceptNZ(futureGuardianWeeklyProducts, currentGuardianWeeklySubscription)),
      TargetPriceRiseIsMoreThanTheCurrentPrice -> (priceRise.newPrice > currentGuardianWeeklySubscription.price),
      CurrentlyActiveProductRatePlanIsGuardianWeeklyRatePlan -> Config.Zuora.Old.guardianWeeklyProductRatePlanIds.contains(currentGuardianWeeklySubscription.productRatePlanId),
      BillingPeriodIsQuarterlyOrAnnually -> List("Annual", "Quarter").contains(currentGuardianWeeklySubscription.billingPeriod),
      ThereDoesNotExistAFutureAmendmentOnThePriceRiseDate ->
        Try {
          FutureAmendmentsOnOrAfterPriceRiseDate(subscription, priceRise)
            .map(_.productRatePlanId)
            .forall(doNotRemoveProductRatePlanIds.contains)
        }.getOrElse(false),
    ).partition(_._2)

    unsatisfied.map(_._1)
  }

}
