package com.gu

import com.gu.FileImporter.PriceRise
import org.joda.time.LocalDate

trait PriceRiseResponseCondition
case object NewGuardianWeeklyRatePlanExists extends PriceRiseResponseCondition
case object NewGuardianWeeklyRatePlanHasOnlyOneCharge extends PriceRiseResponseCondition
case object CustomerAcceptanceDateIsOnTheBeginningOfNextInvoicePeriod extends PriceRiseResponseCondition // which is in the future
// case object OldRatePlanIsRemoved extends PriceRiseResponseCondition
case object AllOtherRatePlansAreRemovedApartFromHolidaysAndRetentionDiscounts extends PriceRiseResponseCondition
case object CurrencyDidNotChange extends PriceRiseResponseCondition
case object PriceHasBeenRaised extends PriceRiseResponseCondition

object PriceRiseResponseValidation {
  type UnsatisfiedPriceRiseResponseConditions = List[PriceRiseResponseCondition]

  def apply(
      subscriptionAfterPriceRise: Subscription,
      accountBeforeThePriceRise: Account,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue,
      priceRise: PriceRise
  ): UnsatisfiedPriceRiseResponseConditions = {
    val newGuardianWeeklyRatePlans =
      subscriptionAfterPriceRise.ratePlans.filter { ratePlan =>
        newGuardianWeeklyProductCatalogue.getAllProductRatePlanIds.contains(ratePlan.productRatePlanId)
      }
    assert(newGuardianWeeklyRatePlans.size == 1)
    assert(newGuardianWeeklyRatePlans.head.ratePlanCharges.size == 1)
    val newRatePlan = newGuardianWeeklyRatePlans.head
    val newRatePlanCharge = newRatePlan.ratePlanCharges.head
    val newProductRatePlanId = newRatePlan.productRatePlanId
    val newProductRatePlanChargeId = newRatePlanCharge.productRatePlanChargeId
    val price = newRatePlanCharge.price

    // AllOtherRatePlansAreRemovedApartFromHolidays
    val removedRatePlans = subscriptionAfterPriceRise.ratePlans.filterNot(_.lastChangeType.contains("Remove"))

    val (_, unsatisfied) = List[(PriceRiseResponseCondition, Boolean)](
      NewGuardianWeeklyRatePlanExists -> newGuardianWeeklyRatePlans.nonEmpty,
      NewGuardianWeeklyRatePlanHasOnlyOneCharge -> (newRatePlan.ratePlanCharges.size == 1),
      CustomerAcceptanceDateIsOnTheBeginningOfNextInvoicePeriod ->
        List(
          subscriptionAfterPriceRise.customerAcceptanceDate.isEqual(priceRise.priceRiseDate),
          newRatePlanCharge.effectiveStartDate.isEqual(priceRise.priceRiseDate),
          newRatePlanCharge.effectiveStartDate.isAfter(LocalDate.now())
        ).forall(_ == true),
      AllOtherRatePlansAreRemovedApartFromHolidaysAndRetentionDiscounts -> true, // TODO: Do not remove holiday and retention discounts
      CurrencyDidNotChange -> (newRatePlanCharge.currency == accountBeforeThePriceRise.billingAndPayment.currency),
      PriceHasBeenRaised -> (newRatePlanCharge.price == priceRise.newPrice)
    ).partition(_._2)

    unsatisfied.map(_._1)
  }

}
