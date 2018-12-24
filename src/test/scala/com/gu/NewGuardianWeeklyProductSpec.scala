package com.gu

import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, Matchers}

class NewGuardianWeeklyProductSpec extends FlatSpec with Matchers {

  val genericSubscription = CurrentGuardianWeeklySubscription(
    subscriptionNumber = "A-S123456789",
    billingPeriod = "",
    price = 10,
    currency = "",
    country = "",
    invoicedPeriod = CurrentInvoicedPeriod(LocalDate.now.minusMonths(2), LocalDate.now.plusMonths(1)),
    ratePlanId = "rpId1",
    productRatePlanId = "prRpId1"
  )

  "NewGuardianWeeklyProduct" should "select Domestic (Quarterly) for a subscriber who currently pays quarterly in GBP and has the magazine delivered to the UK" in {
    val currentGuardianWeeklySubscription = genericSubscription.copy(billingPeriod = "Quarter", currency = "GBP", country = "United Kingdom")
    val selection = NewGuardianWeeklyProduct(currentGuardianWeeklySubscription, DummyCatalog.catalog)
    assert(selection == DummyCatalog.domesticQuarterly)
  }

  "NewGuardianWeeklyProduct" should "select Domestic (Annual) for a subscriber who currently pays annually in USD and has the magazine delivered to the US" in {
    val currentGuardianWeeklySubscription = genericSubscription.copy(billingPeriod = "Annual", currency = "USD", country = "United States")
    val selection = NewGuardianWeeklyProduct(currentGuardianWeeklySubscription, DummyCatalog.catalog)
    assert(selection == DummyCatalog.domesticAnnual)
  }

  "NewGuardianWeeklyProduct" should "select ROW (Quarterly) for a subscriber who currently pays quarterly in USD and has the magazine delivered to Afghanistan" in {
    val currentGuardianWeeklySubscription = genericSubscription.copy(billingPeriod = "Quarter", currency = "USD", country = "Afghanistan")
    val selection = NewGuardianWeeklyProduct(currentGuardianWeeklySubscription, DummyCatalog.catalog)
    assert(selection == DummyCatalog.rowQuarterly)
  }

  "NewGuardianWeeklyProduct" should "select ROW (Annual) for a subscriber who currently pays annually in GBP and has the magazine delivered to Bahrain" in {
    val currentGuardianWeeklySubscription = genericSubscription.copy(billingPeriod = "Annual", currency = "GBP", country = "Bahrain")
    val selection = NewGuardianWeeklyProduct(currentGuardianWeeklySubscription, DummyCatalog.catalog)
    assert(selection == DummyCatalog.rowAnnual)
  }

  "NewGuardianWeeklyProduct" should "select ROW (Quarterly) for a subscriber who currently pays quarterly in GBP and has the magazine delivered to Germany" in {
    val currentGuardianWeeklySubscription = genericSubscription.copy(billingPeriod = "Quarter", currency = "GBP", country = "Germany")
    val selection = NewGuardianWeeklyProduct(currentGuardianWeeklySubscription, DummyCatalog.catalog)
    assert(selection == DummyCatalog.rowQuarterly)
  }

  "NewGuardianWeeklyProduct" should "select ROW (Annual) for a subscriber who currently pays annually in AUD and has the magazine delivered to the US" in {
    val currentGuardianWeeklySubscription = genericSubscription.copy(billingPeriod = "Annual", currency = "AUD", country = "United States")
    val selection = NewGuardianWeeklyProduct(currentGuardianWeeklySubscription, DummyCatalog.catalog)
    assert(selection == DummyCatalog.rowAnnual)
  }

}

object DummyCatalog {

  val genericWeeklyProduct = NewGuardianWeeklyProduct(
    productRatePlanName = "GW",
    billingPeriod = "Quarter",
    productRatePlanId = "id1",
    productRatePlanChargeId = "id2",
    pricing = List(),
    taxCode = "Guardian Weekly"
  )

  val domesticQuarterly = genericWeeklyProduct.copy(productRatePlanName = "GW Oct 18 - Quarterly - Domestic")
  val domesticAnnual = genericWeeklyProduct.copy(productRatePlanName = "GW Oct 18 - Annual - Domestic", billingPeriod = "Annual")
  val domestic = List(domesticQuarterly, domesticAnnual)

  val rowQuarterly = genericWeeklyProduct.copy(productRatePlanName = "GW Oct 18 - Quarterly - ROW")
  val rowAnnual = genericWeeklyProduct.copy(productRatePlanName = "GW Oct 18 - Annual - ROW", billingPeriod = "Annual")
  val restOfWorld = List(rowQuarterly, rowAnnual)

  val catalog = NewGuardianWeeklyProductCatalogue(domestic, restOfWorld)

}
