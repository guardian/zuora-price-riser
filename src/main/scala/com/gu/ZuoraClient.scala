package com.gu

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import scalaj.http.{Http, HttpResponse}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write

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
  lastChangeType: Option[String],
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

case class ProductRatePlan(
  id: String,
  status: String,
  name: String,
  productRatePlanCharges: List[ProductRatePlanCharge]
)

case class ProductRatePlanCharge(
  id: String,
  name: String,
  pricing: List[Price],
  billingPeriod: String,
  taxCode: String
)

case class Price(
  currency: String,
  price: Float
)

case class RemoveRatePlan(ratePlanId: String, contractEffectiveDate: LocalDate)
case class ChargeOverride(productRatePlanChargeId: String, price: Float)
case class AddProductRatePlan(productRatePlanId: String, contractEffectiveDate: LocalDate, chargeOverrides: Option[List[ChargeOverride]])

case class PriceRiseRequest(
  remove: List[RemoveRatePlan],
  add: List[AddProductRatePlan]
)


case class Reason(
  code: Int,
  message: String
)
case class PriceRiseResponse(
  success: Boolean,
  reasons: Option[List[Reason]]
)

object LocalDateSerializer extends CustomSerializer[LocalDate](format => ({
  case JString(str) => LocalDate.parse(str)
  case JNull => null
}, {
  case value: LocalDate  =>
    val formatter = DateTimeFormat.forPattern("YYYY-MM-dd")
    JString(formatter.print(value))
}
))

trait ZuoraJsonFormats {
  implicit val codec = DefaultFormats ++ List(LocalDateSerializer)
}

object ZuoraClient extends ZuoraJsonFormats {
  import ZuoraOauth._
  import ZuoraHostSelector._

  def getSubscription(subscriptionName: String): Subscription = {
    val response =
      Http(s"$host/v1/subscriptions/$subscriptionName")
        .header("Authorization", s"Bearer $accessToken")
        .asString
        .body


    val subscription = parse(response).extract[Subscription]
    subscription
  }

  def getAccount(accountNumber: String): Account = {
    val responseAccount =
      Http(s"$host/v1/accounts/$accountNumber")
        .header("Authorization", s"Bearer $accessToken")
        .asString
        .body

    val account = parse(responseAccount).extract[Account]
    account
  }

  def getProductRatePlans(productId: String): List[ProductRatePlan] = {
    val response =
      Http(s"$host/v1/rateplan/$productId/productRatePlan")
        .header("Authorization", s"Bearer $accessToken")
        .asString
        .body

    //pprint.pprintln(productRatePlans, height = 1000)
    (parse(response) \ "productRatePlans").extract[List[ProductRatePlan]]
  }

  private def getGuardianWeeklyProducts(guardianWeeklyProductId: String): List[GuardianWeeklyProduct] = {
    import Config.Zuora._
    require(
      List(guardianWeeklyDomesticProductId, guardianWeeklyRowProductId).contains(guardianWeeklyProductId),
      "Product ID should represent either 'Guardian Weekly - ROW' or 'Guardian Weekly - Domestic'"
    )
    GuardianWeeklyProducts(
      getProductRatePlans(guardianWeeklyProductId)
    )
  }

  def getNewGuardianWeeklyProductCatalogue() = NewGuardianWeeklyProductCatalogue(
    domestic = getGuardianWeeklyProducts(Config.Zuora.guardianWeeklyDomesticProductId),
    restOfTheWorld = getGuardianWeeklyProducts(Config.Zuora.guardianWeeklyRowProductId)
  )


  /*
  PUT /v1/subscriptions/A-S00047834 HTTP/1.1
  Host: rest.apisandbox.zuora.com
  apiAccessKeyId: mario.galic_SB@guardian.co.uk
  apiSecretAccessKey: ************
  Accept: application/json
  Content-Type: application/json
  cache-control: no-cache
  {
      "remove": [
          {
              "ratePlanId": "2c92c0fb6736875d0167552b1a3a5058",
              "contractEffectiveDate": "2019-03-07"
          }
      ],
      "add": [
          {
              "productRatePlanId": "2c92c0f965dc30640165f150c0956859",
              "contractEffectiveDate": "2019-03-07",
              "chargeOverrides": [
                  {
                      "productRatePlanChargeId": "2c92c0f865d273010165f16ada0a4346",
                      "price": 99
                  }
              ]
          }
      ]
  }
    */
  def removeAndAddAProductRatePlan(
      subscriptionName: String,
      body: PriceRiseRequest): PriceRiseResponse = {
    val response = Http(s"$host/v1/subscriptions/$subscriptionName")
      .method("PUT")
      .header("Authorization", s"Bearer $accessToken")
      .header("content-type", "application/json")
      .postData(write(body))
      .asString

    response.code match {
      case 200 => parse(response.body).extract[PriceRiseResponse]
      case _ => throw new RuntimeException(s"$subscriptionName failed to raise price due to Zuora networking issue: $response")
    }
  }

  case class ExtendTerm(
    currentTerm: String,
    currentTermPeriodType: String
  )

  def extendTerm(
    subscriptionName: String,
    body: ExtendTerm): PriceRiseResponse = {
    val response = Http(s"$host/v1/subscriptions/$subscriptionName")
      .method("PUT")
      .header("Authorization", s"Bearer $accessToken")
      .header("content-type", "application/json")
      .postData(write(body))
      .asString

    response.code match {
      case 200 => parse(response.body).extract[PriceRiseResponse]
      case _ => throw new RuntimeException(s"$subscriptionName failed to raise price due to Zuora networking issue: $response")
    }
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
object ZuoraOauth extends ZuoraJsonFormats {
  import java.util.{Timer, TimerTask}
  import ZuoraHostSelector._

  var accessToken: String = null

  private def getAccessToken(): String = {
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

