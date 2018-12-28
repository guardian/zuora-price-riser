package com.gu

import com.gu.ZuoraClient.ExtendTerm
import org.joda.time.{DateTimeUtils, Instant}
import org.json4s.native.JsonMethods.parse
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class ExtendTermRequestBuilderSpec extends FlatSpec with Matchers with ZuoraJsonFormats {
  DateTimeUtils.setCurrentMillisFixed(Instant.parse("2018-12-15").getMillis)
  val subscriptionRaw = Source.fromURL(getClass.getResource("/current-subscription.json")).mkString
  val accountRaw = Source.fromURL(getClass.getResource("/current-subscription-account.json")).mkString
  val subscription = parse(subscriptionRaw).extract[Subscription]
  val account = parse(accountRaw).extract[Account]
  val currentGuardianWeeklySubscription = CurrentGuardianWeeklySubscription(subscription, account)

  "ExtendTerm" should "should be created if invoiced period is outside term" in {
    ExtendTermRequestBuilder(
      subscription,
      currentGuardianWeeklySubscription
    ) should be (Some(ExtendTerm((365 + 9).toString, "Day"))) // "chargedThroughDate": "2019-07-27", "termEndDate": "2019-07-18"
  }

  it should "not extend term if invoice period end date is equal to term end date" in {
    ExtendTermRequestBuilder(
      subscription,
      currentGuardianWeeklySubscription.copy(
        invoicedPeriod = currentGuardianWeeklySubscription.invoicedPeriod.copy(endDateExcluding = subscription.termEndDate)
      )
    ) should be (None)
  }
}
