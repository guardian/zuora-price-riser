package com.gu

import com.gu.FileImporter.PriceRise
import org.joda.time.LocalDate

import scala.util.Try

trait PriceRisePostCondition
case object NewGuardianWeeklyRatePlanExists extends PriceRisePostCondition
case object NewGuardianWeeklyRatePlanHasOnlyOneCharge extends PriceRisePostCondition
case object CustomerAcceptanceDateIsOnTheBeginningOfNextInvoicePeriod extends PriceRisePostCondition // which is in the future
case object OldRatePlanIsRemoved extends PriceRisePostCondition
case object AllOtherRatePlansAreRemovedApartFromHolidaysAndRetentionDiscounts extends PriceRisePostCondition
case object CurrencyDidNotChange extends PriceRisePostCondition
case object PriceHasBeenRaised extends PriceRisePostCondition
case object AccountDidNotChange extends PriceRisePostCondition

/**
  * Checks post-conditions after the price rise has been writen to Zuora.
  */
object CheckPriceRisePostConditions {
  import Config.Zuora._
  type UnsatisfiedPriceRisePostConditions = List[PriceRisePostCondition]

  def apply(
      subscriptionAfter: Subscription,
      accountBefore: Account,
      accountAfter: Account,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue,
      priceRise: PriceRise,
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription
  ): UnsatisfiedPriceRisePostConditions = {

    val newGuardianWeeklyRatePlans =
      subscriptionAfter.ratePlans.filter { ratePlan =>
        newGuardianWeeklyProductCatalogue.getAllProductRatePlanIds.contains(ratePlan.productRatePlanId)
      }

    val (_, unsatisfied) = List[(PriceRisePostCondition, Boolean)](
      NewGuardianWeeklyRatePlanExists -> (newGuardianWeeklyRatePlans.size == 1),
      NewGuardianWeeklyRatePlanHasOnlyOneCharge -> Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.size == 1).getOrElse(false),
      CustomerAcceptanceDateIsOnTheBeginningOfNextInvoicePeriod ->
        List(
          subscriptionAfter.customerAcceptanceDate.isEqual(priceRise.priceRiseDate),
          Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.head.effectiveStartDate.isEqual(priceRise.priceRiseDate)).getOrElse(false),
          Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.head.effectiveStartDate.isAfter(LocalDate.now())).getOrElse(false)
        ).forall(_ == true),
      AllOtherRatePlansAreRemovedApartFromHolidaysAndRetentionDiscounts ->
        subscriptionAfter
          .ratePlans
          .filterNot(_.lastChangeType.contains("Remove"))
          .map(_.productRatePlanId)
          .forall(doNotRemoveProductRatePlanIds.contains),
      OldRatePlanIsRemoved -> subscriptionAfter.ratePlans.exists(_.id == currentGuardianWeeklySubscription.ratePlanId),
      CurrencyDidNotChange -> Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.head.currency == accountBefore.billingAndPayment.currency).getOrElse(false),
      PriceHasBeenRaised -> Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.head.price == priceRise.newPrice).getOrElse(false),
      AccountDidNotChange -> true // TODO:
    ).partition(_._2)

    unsatisfied.map(_._1)
  }

}