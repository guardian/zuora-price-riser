package com.gu

import org.scalatest._
import scala.io.Source
import org.json4s.native.JsonMethods.parse

class ZuoraJsonFormatsSpec extends FlatSpec with Matchers with ZuoraJsonFormats {
  "ZuoraJsonFormats" should "parse Zuora subscription" in {
    val file = Source.fromURL(getClass.getResource("/current-subscription.json")).mkString
    parse(file).extract[Subscription].subscriptionNumber should be ("A-S00045676")
  }

  it should "parse Zuora account" in {
    val file = Source.fromURL(getClass.getResource("/current-subscription-account.json")).mkString
    parse(file).extract[Account].basicInfo.accountNumber should be ("A00018226")
  }
}
