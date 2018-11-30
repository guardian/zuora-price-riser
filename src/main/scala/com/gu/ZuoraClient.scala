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
  lastChangeType: String,
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

case class BillingAndPayment(
  currency: String,
  paymentGateway: String
)

case class SoldToContact(
  country: String,
  workEmail: String
)

case class Account(
  basicInfo: BasicInfo,
  billingAndPayment: BillingAndPayment,
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

trait MyJson4sFormats {
  implicit val codec = DefaultFormats ++ List(LocalDateSerializer)
}

object ZuoraClient extends MyJson4sFormats {
  import ZuoraOauth._
  import ZuoraHostSelector._

  def getSubscription(subscriptionName: String): Subscription = {
    val response =
      Http(s"$host/v1/subscriptions/$subscriptionName")
        .header("Authorization", s"Bearer $accessToken")
        .asString
        .body

    println(response)

    val subscription = parse(response).extract[Subscription]
    println(subscription)
    subscription
  }

  def getAccount(accountNumber: String): Account = {
    val responseAccount =
      Http(s"$host/v1/accounts/$accountNumber")
        .header("Authorization", s"Bearer $accessToken")
        .asString
        .body

    val account = parse(responseAccount).extract[Account]
    println(account)
    account
  }
}

case class Token(
  access_token: String,
  token_type: String,
  expires_in: String,
  scope: String,
  jti: String
)

// https://www.zuora.com/developer/api-reference/#operation/createToken
object ZuoraOauth extends MyJson4sFormats {
  import java.util.{Timer, TimerTask}
  import ZuoraHostSelector._

  var accessToken: String = null

  private def getAccessToken(): String = {
    println("Getting token")
    val response = Http(s"$host/oauth/token")
      .postForm(Seq(
        "client_id" -> Config.Zuora.client_id,
        "client_secret" -> Config.Zuora.client_secret,
        "grant_type" -> "client_credentials"
      ))
      .asString
      .body

    parse(response).extract[Token].access_token
  }

  private val timer = new Timer()

  timer.schedule(
    new TimerTask { def run(): Unit = accessToken = getAccessToken() },
    0, 55 * 60 * 1000 // refresh token every 55 min
  )
  accessToken = getAccessToken() // set token on initialization
}

object ZuoraHostSelector {
  val host: String =
    Config.Zuora.stage match {
      case "DEV" | "dev" => "https://rest.apisandbox.zuora.com"
      case "PROD" | "prod" => "https://rest.zuora.com"
      case _ => "https://rest.apisandbox.zuora.com"
    }
}

