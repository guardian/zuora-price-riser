package com.gu

import com.gu.FileImporter.PriceRise
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import scalaj.http.{BaseHttp, Http, HttpOptions}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write

import scala.annotation.tailrec

case class RatePlanCharge(
  id: String,
  productRatePlanChargeId: String,
  currency: String,
  price: Option[Float],
  billingPeriod: String,
  effectiveStartDate: LocalDate,
  effectiveEndDate: LocalDate,
  processedThroughDate: Option[LocalDate],
  chargedThroughDate: Option[LocalDate],
  priceChangeOption: String,
)

case class RatePlan(
  id: String,
  lastChangeType: Option[String],
  productRatePlanId: String,
  ratePlanName: String,
  ratePlanCharges: List[RatePlanCharge],
  productName: String
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
  workEmail: String,
  state: String
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
case class ChargeOverride(productRatePlanChargeId: String, price: Float, priceChangeOption: String = "NoChange")
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

case class InvoiceItem(
  id: String,
  subscriptionNumber: String,
  serviceStartDate: LocalDate,
  serviceEndDate: LocalDate,
  chargeAmount: Float,
  chargeName: String,
  productName: String
)

case class BillingPreviewBody(
  accountId: String,
  targetDate: LocalDate,
  assumeRenewal: String = "All"
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

  object HttpWithLongTimeout extends BaseHttp(
    options = Seq(
      HttpOptions.connTimeout(5000),
      HttpOptions.readTimeout(30000),
      HttpOptions.followRedirects(false)
    )
  )

  def getSubscription(subscriptionName: String): Subscription = {
    val response =
      HttpWithLongTimeout(s"$host/v1/subscriptions/$subscriptionName")
        .header("Authorization", s"Bearer $accessToken")
        .asString

    response.code match {
      case 200 => parse(response.body).extract[Subscription]
      case _ => throw new RuntimeException(s"Failed to getSubscription $subscriptionName: $response")
    }
  }

  def getAccount(accountNumber: String): Account = {
    val response =
      HttpWithLongTimeout(s"$host/v1/accounts/$accountNumber")
        .header("Authorization", s"Bearer $accessToken")
        .asString

    response.code match {
      case 200 => parse(response.body).extract[Account]
      case _ => throw new RuntimeException(s"Failed to getAccount $accountNumber: $response")
    }
  }

  private def getProductRatePlans(productId: String): List[ProductRatePlan] = {
    val response =
      HttpWithLongTimeout(s"$host/v1/rateplan/$productId/productRatePlan")
        .header("Authorization", s"Bearer $accessToken")
        .asString

    //pprint.pprintln(productRatePlans, height = 1000)
    response.code match {
      case 200 => (parse(response.body) \ "productRatePlans").extract[List[ProductRatePlan]]
      case _ => throw new RuntimeException(s"Failed to getProductRatePlans for productId $productId: $response")
    }
  }

  // FIXME: Once we enable all currencies we could hardcode this object
  lazy val getNewGuardianWeeklyProductCatalogue = NewGuardianWeeklyProductCatalogue(
    domestic = NewGuardianWeeklyProducts(getProductRatePlans(Config.Zuora.New.guardianWeeklyDomesticProductId)),
    restOfTheWorld = NewGuardianWeeklyProducts(getProductRatePlans(Config.Zuora.New.guardianWeeklyRowProductId))
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
    val response = HttpWithLongTimeout(s"$host/v1/subscriptions/$subscriptionName")
      .header("Authorization", s"Bearer $accessToken")
      .header("content-type", "application/json")
      .postData(write(body))
      .method("PUT")
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
      body: ExtendTerm
  ): PriceRiseResponse = {
    val response = HttpWithLongTimeout(s"$host/v1/subscriptions/$subscriptionName")
      .header("Authorization", s"Bearer $accessToken")
      .header("content-type", "application/json")
      .postData(write(body))
      .method("PUT")
      .asString

    response.code match {
      case 200 => parse(response.body).extract[PriceRiseResponse]
      case _ => throw new RuntimeException(s"$subscriptionName failed to raise price due to Zuora networking issue: $response")
    }
  }

  // https://stackoverflow.com/a/7931459/5205022
  @tailrec
  def retry[T](n: Int)(fn: => T): T = {
    util.Try { fn } match {
      case util.Success(x) => x
      case _ if n > 1 => retry(n - 1)(fn)
      case util.Failure(e) => throw e
    }
  }

  def newGuardianWeeklyInvoicePreview(
    account: Account,
    priceRise: PriceRise,
  ): InvoiceItem = {

    def newGuardianWeeklyInvoicePreviewImpl(): InvoiceItem = {
      val body = BillingPreviewBody(account.basicInfo.id, priceRise.priceRiseDate)
      val response = HttpWithLongTimeout(s"$host/v1/operations/billing-preview")
        .header("Authorization", s"Bearer $accessToken")
        .header("content-type", "application/json")
        .postData(write(body))
        .method("POST")
        .asString

      response.code match {
        case 200 => (parse(response.body) \ "invoiceItems").extract[List[InvoiceItem]].filter(_.productName != "Discounts") match {
          case List(singleInvoiceItem) => singleInvoiceItem
          case _ => throw new RuntimeException(s"Expected to find a single invoice item after excluding Discounts, but got $body: $response")
        }
        case _ => throw new RuntimeException(s"${account.basicInfo.id} failed to get billing preview due to Zuora networking issue: $response")
      }
    }

    retry[InvoiceItem](3)(newGuardianWeeklyInvoicePreviewImpl())
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

    response.code match {
      case 200 => parse(response.body).extract[Token].access_token
      case _ => throw new RuntimeException(s"Failed to authenticate with Zuora: $response")
    }
  }

  private val timer = new Timer()

  timer.schedule(
    new TimerTask { def run(): Unit = accessToken = getAccessToken() },
    0, 1 * 60 * 1000 // refresh token every 1 min
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

