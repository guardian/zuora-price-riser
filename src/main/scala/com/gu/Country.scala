package com.gu

import com.gu.i18n.CountryGroup

object Country {
  def standardCurrency(country: String): String =
    country match {
      case _ if UK.contains(country) => "GBP"
      case _ if US.contains(country) => "USD"
      case _ if Canada.contains(country) => "CAD"
      case _ if Australia.contains(country) => "AUD"
      case _ if NewZealand.contains(country) => "NZD"
      case _ if Europe.contains(country) => "EUR"
      case _ if RestOfTheWorld.contains(country) => "USD"
    }

  def toFutureGuardianWeeklyProductId(country: String, currency: String): String = country match {
    case _ if RestOfTheWorld.contains(country) => Config.Zuora.New.guardianWeeklyRowProductId
    case _ if standardCurrency(country) == currency => Config.Zuora.New.guardianWeeklyDomesticProductId
    case _ => Config.Zuora.New.guardianWeeklyRowProductId
  }

  private val UK = CountryGroup.UK.countries.map(_.name)
  private val US = CountryGroup.US.countries.map(_.name)
  private val Canada = CountryGroup.Canada.countries.map(_.name)
  private val Australia = CountryGroup.Australia.countries.map(_.name)
  private val NewZealand = CountryGroup.NewZealand.countries.map(_.name)
  private val Europe = CountryGroup.Europe.countries.map(_.name)
  private val RestOfTheWorld = CountryGroup.RestOfTheWorld.countries.map(_.name)
}
