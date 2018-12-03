package com.gu

import org.joda.time.{DateTimeUtils, Instant}
import org.scalatest.{FlatSpec, Matchers}
import scala.io.Source
import org.json4s.native.JsonMethods.parse

class CurrentGuardianWeeklySubscriptionSpec extends FlatSpec with Matchers with ZuoraJsonFormats {
  "CurrentGuardianWeeklySubscription" should "have been invoiced for the current period (TodayHasBeenInvoiced)" in {
    // "processedThroughDate": null,
    // "chargedThroughDate": null,
    val subscripitonRaw = Source.fromURL(getClass.getResource("/subscription.json")).mkString
    val accountRaw = Source.fromURL(getClass.getResource("/account.json")).mkString
    val subscription = parse(subscripitonRaw).extract[Subscription]
    val account = parse(accountRaw).extract[Account]

    intercept[AssertionError] {
      CurrentGuardianWeeklySubscription(subscription, account)
    }
  }

  // How to mock joda time: https://stackoverflow.com/a/26344048/5205022
  it should "satisfy all the CurrentGuardianWeeklyRatePlanConditions" in {
    DateTimeUtils.setCurrentMillisFixed(Instant.parse("2018-12-15").getMillis)

      val subscripitonRaw = Source.fromURL(getClass.getResource("/subscription-valid.json")).mkString
      val accountRaw = Source.fromURL(getClass.getResource("/account-valid.json")).mkString
      val subscription = parse(subscripitonRaw).extract[Subscription]
      val account = parse(accountRaw).extract[Account]
      CurrentGuardianWeeklySubscription(subscription, account).subscriptionNumber should be ("A-S00047886")

    DateTimeUtils.setCurrentMillisSystem()
  }
}
