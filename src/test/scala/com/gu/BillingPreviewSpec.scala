package com.gu

import com.gu.FileImporter.PriceRise
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.json4s.native.JsonMethods.parse
import org.scalactic.TolerantNumerics
import org.scalatest._

class BillingPreviewSpec extends FlatSpec with Matchers with ZuoraJsonFormats {

  val rawInvoiceItem =
    """
      |        {
      |            "id": "",
      |            "subscriptionName": "11111",
      |            "subscriptionId": "",
      |            "subscriptionNumber": "11111",
      |            "serviceStartDate": "2019-01-14",
      |            "serviceEndDate": "2019-04-13",
      |            "chargeAmount": 76.19,
      |            "chargeDescription": "",
      |            "chargeName": "GW Oct 18 - Quarterly - Domestic",
      |            "chargeNumber": "",
      |            "chargeId": "",
      |            "productName": "Guardian Weekly - Domestic",
      |            "quantity": 1,
      |            "taxAmount": 0,
      |            "unitOfMeasure": "",
      |            "chargeDate": "2018-12-31 16:23:19",
      |            "chargeType": "Recurring",
      |            "processingType": "Charge",
      |            "appliedToItemId": null
      |        }
    """.stripMargin

  val rawAccount =
    """
      |{
      |    "basicInfo": {
      |        "id": "",
      |        "name": "11111",
      |        "accountNumber": "11111",
      |        "notes": null,
      |        "status": "Active",
      |        "crmId": "",
      |        "batch": "Batch1",
      |        "invoiceTemplateId": "",
      |        "communicationProfileId": "",
      |        "IdentityId__c": "",
      |        "sfContactId__c": "",
      |        "CCURN__c": null,
      |        "SpecialDeliveryInstructions__c": null,
      |        "NonStandardDataReason__c": null,
      |        "ProcessingAdvice__c": null,
      |        "CreatedRequestId__c": null,
      |        "salesRep": null
      |    },
      |    "billingAndPayment": {
      |        "billCycleDay": 13,
      |        "currency": "CAD",
      |        "paymentTerm": "Due Upon Receipt",
      |        "paymentGateway": "Stripe Gateway 1",
      |        "invoiceDeliveryPrefsPrint": false,
      |        "invoiceDeliveryPrefsEmail": true,
      |        "additionalEmailAddresses": []
      |    },
      |    "metrics": {
      |        "balance": -12,
      |        "totalInvoiceBalance": 0,
      |        "creditBalance": 12,
      |        "contractedMrr": 26.67
      |    },
      |    "billToContact": {
      |        "address1": "",
      |        "address2": null,
      |        "city": "",
      |        "country": "Canada",
      |        "county": "BC",
      |        "fax": null,
      |        "firstName": "",
      |        "homePhone": null,
      |        "lastName": "",
      |        "mobilePhone": null,
      |        "nickname": null,
      |        "otherPhone": null,
      |        "otherPhoneType": null,
      |        "personalEmail": null,
      |        "state": "British Columbia",
      |        "taxRegion": null,
      |        "workEmail": "",
      |        "workPhone": "",
      |        "zipCode": "",
      |        "Company_Name__c": null,
      |        "SpecialDeliveryInstructions__c": null,
      |        "Title__c": "",
      |        "contactDescription": null
      |    },
      |    "soldToContact": {
      |        "address1": "",
      |        "address2": null,
      |        "city": "VICTORIA",
      |        "country": "Canada",
      |        "county": "BC",
      |        "fax": null,
      |        "firstName": "",
      |        "homePhone": null,
      |        "lastName": "",
      |        "mobilePhone": null,
      |        "nickname": null,
      |        "otherPhone": null,
      |        "otherPhoneType": null,
      |        "personalEmail": null,
      |        "state": "British Columbia",
      |        "taxRegion": null,
      |        "workEmail": "",
      |        "workPhone": "",
      |        "zipCode": "",
      |        "Company_Name__c": null,
      |        "SpecialDeliveryInstructions__c": null,
      |        "Title__c": "",
      |        "contactDescription": null
      |    },
      |    "taxInfo": null,
      |    "success": true
      |}
    """.stripMargin

  implicit val floatEq = TolerantNumerics.tolerantFloatEquality(0.01f)

  "BillingPreview" should "calculate tax for countries with states" in {
    val zuoraDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
    val priceRise = PriceRise("11111", "", "", LocalDate.parse("2019-01-14", zuoraDateFormat), 60.0f, 80.0f, None)
    BillingPreview(
      parse(rawAccount).extract[Account],
      parse(rawInvoiceItem).extract[InvoiceItem]
    ) should === (priceRise.newPrice)
  }

  it should "calculate tax for countries where rounding is not precise (Canada, Nova Scotia)" in {
    val zuoraDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
    val priceRise = PriceRise("11111", "", "", LocalDate.parse("2019-01-14", zuoraDateFormat), 60.0f, 80.0f, None)
    val account = parse(rawAccount).extract[Account]
    val accountCanNovaScotia = account.copy(soldToContact = account.soldToContact.copy(country = "Canada", state = "Nova Scotia"))

    // without triple equals it is 80.01 vs 80.00
    BillingPreview(
      accountCanNovaScotia,
      parse(rawInvoiceItem).extract[InvoiceItem].copy(chargeAmount = 69.57f)
    ) should === (priceRise.newPrice)
  }

  it should "calculate tax for countries without states" in {
    val zuoraDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
    val newPrice = 76.19f * (1 + 0.07f)
    val newPriceRounded = (math rint newPrice * 100) / 100
    val priceRise = PriceRise("11111", "", "", LocalDate.parse("2019-01-14", zuoraDateFormat), 60.0f, newPriceRounded.toFloat, None)
    val account = parse(rawAccount).extract[Account]
    val accountGermanyNoState = account.copy(soldToContact = account.soldToContact.copy(country = "Germany", state = ""))

    BillingPreview(
      accountGermanyNoState,
      parse(rawInvoiceItem).extract[InvoiceItem]
    ) should === (priceRise.newPrice)
  }

  it should "not apply tax for countries without tax" in {
    val zuoraDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
    val priceRise = PriceRise("11111", "", "", LocalDate.parse("2019-01-14", zuoraDateFormat), 60.0f, 76.19f, None)
    val account = parse(rawAccount).extract[Account]
    val accountLatviaNoTax = account.copy(soldToContact = account.soldToContact.copy(country = "Latvia", state = ""))

    BillingPreview(
      accountLatviaNoTax,
      parse(rawInvoiceItem).extract[InvoiceItem]
    ) should === (priceRise.newPrice)
  }
}
