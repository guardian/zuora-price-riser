package com.gu

import org.scalatest._

class CountrySpec extends FlatSpec with Matchers with ZuoraJsonFormats {
  "Country" should "map country name to country ISO code" in {
    Country.zuoraCodeByCountry("Afghanistan") should be ("AF")
    Country.zuoraCodeByCountry("Macedonia, the former Yugoslav Republic of") should be ("MK")
    Country.zuoraCodeByCountry("Korea, Republic of") should be ("KR")
    Country.zuoraCodeByCountry("Zimbabwe") should be ("ZW")
  }

  it should "map country name to currency" in {
    Country.standardCurrency("Afghanistan") should be ("USD")
    Country.standardCurrency("Macedonia, the former Yugoslav Republic of") should be ("USD")
    Country.standardCurrency("Korea, Republic of") should be ("USD")
    Country.standardCurrency("Zimbabwe") should be ("USD")

    Country.standardCurrency("Germany") should be ("EUR")
    Country.standardCurrency("United Kingdom") should be ("GBP")
  }
}
