package com.gu

import org.scalatest._
import scala.io.Source
import org.json4s.native.JsonMethods.parse

class ZuoraJsonFormatsSpec extends FlatSpec with Matchers with ZuoraJsonFormats {
  "ZuoraJsonFormats" should "parse Zuora subscription" in {
    val file = Source.fromURL(getClass.getResource("/subscription.json")).mkString
    parse(file).extract[Subscription].subscriptionNumber should be ("A-S00047799")
  }

  it should "parse Zuora account" in {
    val file = Source.fromURL(getClass.getResource("/account.json")).mkString
    parse(file).extract[Account].basicInfo.accountNumber should be ("A00020405")
  }
}
