package com.gu

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import kantan.csv.java8._

import scalaj.http.{Http, HttpResponse}
import org.json4s._
import org.json4s.native.JsonMethods._

case class RatePlanCharge(
  id: String,
  productRatePlanChargeId: String,
  currency: String,
  price: Float,
  billingPeriod: String,
  effectiveStartDate: LocalDate,
  effectiveEndDate: LocalDate,
  processedThroughDate: Option[LocalDate],
  chargedThroughDate: Option[LocalDate],
)

case class RatePlan(
  id: String,
  productRatePlanId: String,
  ratePlanName: String,
  ratePlanCharges: List[RatePlanCharge]
)

case class Subscription(
  id: String,
  accountId: String,
  accountNumber: String,
  subscriptionNumber: String,
  contractEffectiveDate: LocalDate,
  customerAcceptanceDate: LocalDate,
  termStartDate: LocalDate,
  termEndDate: LocalDate,
  autoRenew: Boolean,
  status: String,
  ratePlans: List[RatePlan]
)

case class BasicInfo(
  id: String,
  name: String,
  accountNumber: String,
  status: String,
  IdentityId__c: String,
  sfContactId__c: String
)

case class SoldToContact(
  country: String,
  workEmail: String
)

case class Account(
  basicInfo: BasicInfo,
  soldToContact: SoldToContact
)

object LocalDateSerializer extends CustomSerializer[LocalDate](format => ({
  case JString(str) => LocalDate.parse(str)
  case JNull => null
}, {
  case value: LocalDate  =>
    val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd")
    JString(formatter.format(value))
}
))

object Main extends Greeting with App {
  // JSON
  implicit val codec = DefaultFormats ++ List(LocalDateSerializer)
  val string =
    """
      |{
      |                    "id": "2c92c0fa67367a3d016740b48a0456b3",
      |                    "productRatePlanChargeId": "2c92c0f865d273010165f16ada0a4346",
      |                    "currency": "GBP",
      |                    "price": 37.5,
      |                    "billingPeriod": "Quarter",
      |                    "effectiveStartDate": "2018-12-07",
      |                    "effectiveEndDate": "2019-03-07",
      |                    "processedThroughDate": null,
      |                    "chargedThroughDate": null,
      |}
    """.stripMargin
  val json = parse(string)
  val ratePlanCharge = json.extract[RatePlanCharge]
  println(ratePlanCharge)

  println(greeting)
  val format = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  implicit val decoder: CellDecoder[LocalDate] = localDateDecoder(format)

  type subscription_name = String
  type campaign_name = String
  type date_letter_sent = LocalDate
  type price_rise_date = LocalDate
  type current_price_today = Float
  type guardian_weekly_new_price = Float
  type term_end_date = Option[LocalDate]

  type Row =
    (
      subscription_name,
      campaign_name,
      date_letter_sent,
      price_rise_date,
      current_price_today,
      guardian_weekly_new_price,
      term_end_date
    )

  final case class PriceRise(
    subscriptionName: subscription_name,
    campaignName: campaign_name,
    dateLetterSent: date_letter_sent,
    priceRiseDate: price_rise_date,
    currentPrice: current_price_today,
    newPrice: guardian_weekly_new_price,
    termEndDate: term_end_date
  )

  val csvReader = new File("subs.csv").asCsvReader[PriceRise](rfc.withHeader)
//  csvReader.foreach(println)
//  println(csvReader.forall(_.isRight))
//  println(csvReader.filter(_.isLeft).size)

  val zuoraOauthAccessToken = "f91ea94ab44640a19c2d31d1ed18ff5e"

//  csvReader.foreach {
//    case Left(error) =>
//      println(error)
//
//    case Right(row) =>
//      println(row.subscriptionName)
//      val response: HttpResponse[String] =
//        Http("https://rest.apisandbox.zuora.com/v1/subscriptions/A-S00047799")
//          .header("Authorization", s"Bearer ${zuoraOauthAccessToken}")
//          .asString
//
//      println(response.body)
//  }

  val response =
    Http("https://rest.apisandbox.zuora.com/v1/subscriptions/A-S00047799")
      .header("Authorization", s"Bearer ${zuoraOauthAccessToken}")
      .asString
      .body

  println(response)

  val subscription = parse(response).extract[Subscription]
  println(subscription)

  val responseAccount =
    Http(s"https://rest.apisandbox.zuora.com/v1/accounts/${subscription.accountNumber}")
      .header("Authorization", s"Bearer ${zuoraOauthAccessToken}")
      .asString
      .body

  val account = parse(responseAccount).extract[Account]
  println(account)

  /*
  Scenario 1:
  - if auto-renew == true
  - if status == 'Active'
  - if targetPrice >= default product rate plan charge price
  - if deliveryRegion == currency
  - if given priceRiseDate makes sense (is one day after invoice period end date)
   */

}

trait Greeting {
  lazy val greeting: String = "hello"
}
