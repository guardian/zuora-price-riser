package com.gu

/**
  * https://{{zuoraUrlPrefix}}/v1/operations/billing-preview returns price without tax applied
  * so this adds the tax.
  */
object BillingPreview {
  def apply(
    account: Account,
    invoiceItem: InvoiceItem
  ): Float = {
    if (taxedCountries.contains(account.soldToContact.country)) {
      val country = account.soldToContact.country
      val state =  country match {
        case "Canada" => Some(account.soldToContact.state)
        case _ => None
      }
      val tax = taxBy(country -> state)
      val chargeAmount = invoiceItem.chargeAmount
      val chargeWithTax = chargeAmount * (1 + tax)
      val roundedPrice = BigDecimal(chargeWithTax).setScale(2, BigDecimal.RoundingMode.HALF_UP).toFloat // https://stackoverflow.com/a/11107005/5205022
      roundedPrice
    }
    else {
      invoiceItem.chargeAmount
    }

  }

  type Country = String
  type State = String
  type TaxRate = Double
  private val taxBy: Map[(Country, Option[State]), TaxRate] = Map(
    ("Australia", None) -> 0.1,
    ("Canada", Some("Alberta")) -> 0.05,
    ("Canada", Some("British Columbia")) -> 0.05,
    ("Canada", Some("Manitoba")) -> 0.05,
    ("Canada", Some("New Brunswick")) -> 0.15,
    ("Canada", Some("Newfoundland and Labrador")) -> 0.15,
    ("Canada", Some("Nova Scotia")) -> 0.15,
    ("Canada", Some("Northwest Territories")) -> 0.05,
    ("Canada", Some("Nunavut")) -> 0.05,
    ("Canada", Some("Ontario")) -> 0.13,
    ("Canada", Some("Prince Edward Island")) -> 0.14,
    ("Canada", Some("Quebec")) -> 0.14975,
    ("Canada", Some("Saskatchewan")) -> 0.05,
    ("Canada", Some("Yukon")) -> 0.05,
    ("France", None) -> 0.021,
    ("Germany", None) -> 0.07,
    ("Italy", None) -> 0.008,
    ("Spain", None) -> 0.04,
    ("Sweden", None) -> 0.06,
  )
  private val taxedCountries: List[Country] = taxBy.keys.map(_._1).toList
}
