package com.gu

import com.gu.FileImporter.PriceRise
import org.joda.time.LocalDate
import org.scalactic.Tolerance._
import org.scalactic.TripleEquals._
import scala.util.Try

trait PriceRisePostCondition
case object NewGuardianWeeklyRatePlanExists extends PriceRisePostCondition
case object NewGuardianWeeklyRatePlanHasOnlyOneCharge extends PriceRisePostCondition
case object CustomerAcceptanceDateIsOnTheBeginningOfNextInvoicePeriod extends PriceRisePostCondition // which is in the future
case object OldRatePlanIsRemoved extends PriceRisePostCondition
case object HolidaysAndRetentionDiscountsWereNotRemoved extends PriceRisePostCondition
case object CurrencyDidNotChange extends PriceRisePostCondition
case object PriceHasBeenRaised extends PriceRisePostCondition
case object DeliveryCountryDidNotChange extends PriceRisePostCondition
case object PriceShouldNotChangeOnSubsequentRenewals extends PriceRisePostCondition
case object InvoiceShouldHaveTheNewPrice extends PriceRisePostCondition
case object InvoiceStartDateShouldBeOnThePriceRiseDate extends PriceRisePostCondition

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
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription,
      invoiceItem: InvoiceItem,
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
          Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.head.effectiveStartDate.isEqual(priceRise.priceRiseDate)).getOrElse(false),
          Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.head.effectiveStartDate.isAfter(LocalDate.now())).getOrElse(false)
        ).forall(_ == true),
      HolidaysAndRetentionDiscountsWereNotRemoved ->
        !subscriptionAfter
          .ratePlans
          .filter(_.lastChangeType.contains("Remove"))
          .map(_.productRatePlanId)
          .exists(doNotRemoveProductRatePlanIds.contains),
      OldRatePlanIsRemoved ->
        subscriptionAfter
          .ratePlans
          .filter(_.lastChangeType.contains("Remove"))
          .exists(_.productRatePlanId == currentGuardianWeeklySubscription.productRatePlanId),
      CurrencyDidNotChange -> Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.head.currency == accountBefore.billingAndPayment.currency).getOrElse(false),
      PriceHasBeenRaised -> Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.head.price.get == priceRise.newPrice).getOrElse(false),
      DeliveryCountryDidNotChange -> (accountBefore.soldToContact.country == accountAfter.soldToContact.country),
      PriceShouldNotChangeOnSubsequentRenewals -> Try(newGuardianWeeklyRatePlans.head.ratePlanCharges.head.priceChangeOption == "NoChange").getOrElse(false),
      InvoiceShouldHaveTheNewPrice -> (BillingPreview(accountAfter, invoiceItem) === priceRise.newPrice +- 0.01f),
      InvoiceStartDateShouldBeOnThePriceRiseDate -> invoiceItem.serviceStartDate.isEqual(priceRise.priceRiseDate),
    ).partition(_._2)

    unsatisfied.map(_._1)
  }

}
