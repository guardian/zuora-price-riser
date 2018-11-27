package com.gu

import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

object ZuoraClient {
  implicit val codec = DefaultFormats ++ List(LocalDateSerializer)

  // expires in 60 min
  val zuoraOauthAccessToken = "11dcbc9bbafc45aa8a9b6d1b3effb864"

  def getSubscription(subscriptionName: String): Subscription = {
    val response =
      Http(s"https://rest.apisandbox.zuora.com/v1/subscriptions/${subscriptionName}")
        .header("Authorization", s"Bearer ${zuoraOauthAccessToken}")
        .asString
        .body

    println(response)

    val subscription = parse(response).extract[Subscription]
    println(subscription)
    subscription
  }

  def getAccount(accountNumber: String): Account = {
    val responseAccount =
      Http(s"https://rest.apisandbox.zuora.com/v1/accounts/${accountNumber}")
        .header("Authorization", s"Bearer ${zuoraOauthAccessToken}")
        .asString
        .body

    val account = parse(responseAccount).extract[Account]
    println(account)
    account
  }
}
