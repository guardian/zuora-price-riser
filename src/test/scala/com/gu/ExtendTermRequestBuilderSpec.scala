package com.gu

import com.gu.ZuoraClient.ExtendTerm
import org.joda.time.{DateTimeUtils, Instant, LocalDate}
import org.json4s.native.JsonMethods.parse
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class ExtendTermRequestBuilderSpec extends FlatSpec with Matchers with ZuoraJsonFormats {
  DateTimeUtils.setCurrentMillisFixed(Instant.parse("2018-12-15").getMillis)
  val subscriptionRaw = Source.fromURL(getClass.getResource("/current-subscription.json")).mkString
  val subscription = parse(subscriptionRaw).extract[Subscription]

  "ExtendTerm" should "be created if the price rise date is outside of the term end date" in {
    ExtendTermRequestBuilder(
      subscription, // "termEndDate": "2019-07-18",
      LocalDate.parse("2019-07-28")
    ) should be (Some(ExtendTerm((365 + 10).toString, "Day")))
  }

  it should "not extend term if price rise date is equal to term end date" in {
    ExtendTermRequestBuilder(
      subscription,
      LocalDate.parse("2019-07-18")
    ) should be (None)
  }
}
