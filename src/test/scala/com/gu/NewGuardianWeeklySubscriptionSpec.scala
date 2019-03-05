package com.gu

import com.gu.FileImporter.PriceRise
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTimeUtils, Instant, LocalDate}
import org.json4s.native.JsonMethods.parse
import org.scalatest.{FlatSpec, Matchers}
import scala.io.Source

class NewGuardianWeeklySubscriptionSpec extends FlatSpec with Matchers with ZuoraJsonFormats {
  val zuoraDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  DateTimeUtils.setCurrentMillisFixed(Instant.parse("2018-12-15").getMillis)

  val subscriptionAfter = parse(Source.fromURL(getClass.getResource("/new-subscription.json")).mkString).extract[Subscription]
  val subscriptionBefore = parse(Source.fromURL(getClass.getResource("/current-subscription.json")).mkString).extract[Subscription]
  val accountBefore = parse(Source.fromURL(getClass.getResource("/current-subscription-account.json")).mkString).extract[Account]
  val accountAfter = accountBefore
  val newGuardianWeeklyProductCatalogue = Config.Zuora.New.guardianWeeklyProductCatalogue
  val priceRise = PriceRise("A-S00045676", "", LocalDate.parse("2019-01-14", zuoraDateFormat), LocalDate.parse("2019-07-27", zuoraDateFormat), 312.0f, 390.0f, None)
  val currentSubscription = CurrentGuardianWeeklySubscription(subscriptionBefore, accountBefore)
  val invoiceItem = InvoiceItem("", "", LocalDate.parse("2019-07-27", zuoraDateFormat), LocalDate.parse("2019-07-27", zuoraDateFormat), (390 / 1.1).toFloat, "", "")

  val priceRiseRequest = PriceRiseRequestBuilder(subscriptionBefore, currentSubscription, newGuardianWeeklyProductCatalogue, priceRise)

  "NewGuardianWeeklySubscription" should "should satisfy all CheckPriceRisePostConditions" in {
    val unsatisfiedPostConditions = CheckPriceRisePostConditions(
        subscriptionAfter, accountBefore, accountAfter, newGuardianWeeklyProductCatalogue, priceRise, currentSubscription, invoiceItem)

    val newGuardianWeeklySubscription = NewGuardianWeeklySubscription(subscriptionAfter, accountAfter, newGuardianWeeklyProductCatalogue)

    unsatisfiedPostConditions should be (empty)
    priceRiseRequest.remove.head.ratePlanId should be ("2c92c0f9649cc85e0164ad3581856e5d")
    priceRiseRequest.add.head.productRatePlanId should be ("2c92c0f965d280590165f16b1b9946c2") // GW Oct 18 - Annual - Domestic"
    newGuardianWeeklySubscription.subscriptionNumber should be ("A-S00045676")
  }

  it should "list failed postconditions" in {
    val unsatisfiedPostConditions = CheckPriceRisePostConditions(
        subscriptionAfter, accountBefore, accountAfter, newGuardianWeeklyProductCatalogue, priceRise.copy(newPrice = 1000), currentSubscription, invoiceItem)

    unsatisfiedPostConditions should be (List(PriceHasBeenRaised, InvoiceShouldHaveTheNewPrice))
  }
}
