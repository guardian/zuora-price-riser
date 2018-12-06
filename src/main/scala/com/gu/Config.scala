package com.gu

import com.typesafe.config.ConfigFactory

object Config {
  val conf = ConfigFactory.load
  object Zuora {
    val stage = conf.getString("zuora.stage")
    val client_id = conf.getString("zuora.client_id")
    val client_secret = conf.getString("zuora.client_secret")

    /**
        Contains list of all Product Rate Plan IDs for Guardian Weekly obtained manually
        using the following query:

        POST /query/jobs HTTP/1.1
        Host: rest.apisandbox.zuora.com
        Accept: application/json
        Content-Type: application/json
        Authorization: Bearer *****************
        cache-control: no-cache
        {
          "query": "select id, name from productrateplan",
          "outputFormat": "JSON",
          "compression": "NONE",
          "retries": 3,
          "output": {
            "target": "API_RESPONSE"
          }
        }
      */
    val guardianWeeklyProductRatePlanIds = {
      val dev = List(
        "2c92c0f8574b2b8101574c4a9480068d", // "name":"Guardian Weekly Annual"
        "2c92c0f8574b2b8101574c4a957706be", // "name":"Guardian Weekly Quarterly"
        "2c92c0f8574b2be601574c323ca15c7e", // "name":"Guardian Weekly Quarterly"
        "2c92c0f8574b2be601574c39888d6850", // "name":"Guardian Weekly Annual"
        "2c92c0f858aa38af0158da325cec0b2e", // "name":"Guardian Weekly Quarterly"
        "2c92c0f858aa38af0158da325d2f0b3d", // "name":"Guardian Weekly Annual"}
        "2c92c0f86716796f016717d9e1465e03", // "name":"GW Nov 19 - Quarterly - Domestic"
        "2c92c0f965d280590165f16b1b9946c2", // "name":"GW Oct 18 - Annual - Domestic"
        "2c92c0f965dc30640165f150c0956859", // "name":"GW Oct 18 - Quarterly - Domestic"
        "2c92c0f965f2122101660fb33ed24a45", // "name":"GW Oct 18 - Annual - ROW"
        "2c92c0f965f2122101660fb81b745a06", // "name":"GW Oct 18 - Quarterly - ROW"
      )

      val prod = List(
        "",
      )

      Config.Zuora.stage match {
        case "DEV" | "dev" => dev
        case "PROD" | "prod" => prod
        case _ => dev
      }
    }

    val guardianWeeklyProductIds = {
      val dev = List(
        "2c92c0f8574b2b8101574c4a9473068b", // "name":"Guardian Weekly Zone A"
        "2c92c0f95703468a015704e268635dd5", // "name":"Guardian Weekly Zone B"
        "2c92c0f858aa38af0158da325ce00b2c", // "name":"Guardian Weekly Zone C"
        "2c92c0f865d272ef0165f14cc19d238a", // "name":"Guardian Weekly - Domestic"
        "2c92c0f965f2121e01660fb1f1057b1a", // "name":"Guardian Weekly - ROW"
      )
      val prod = List()

      Config.Zuora.stage match {
        case "DEV" | "dev" => dev
        case "PROD" | "prod" => prod
        case _ => dev
      }
    }

    // "2c92c0f865d272ef0165f14cc19d238a", // "name":"Guardian Weekly - Domestic"
    val guardianWeeklyDomesticProductId: String =
      Config.Zuora.stage match {
        case "DEV" | "dev" => "2c92c0f865d272ef0165f14cc19d238a"
        case "PROD" | "prod" => ""
        case _ => "2c92c0f865d272ef0165f14cc19d238a"
      }

    // "2c92c0f965f2121e01660fb1f1057b1a", // "name":"Guardian Weekly - ROW"
    val guardianWeeklyRowProductId: String =
      Config.Zuora.stage match {
        case "DEV" | "dev" => "2c92c0f965f2121e01660fb1f1057b1a"
        case "PROD" | "prod" => ""
        case _ => "2c92c0f965f2121e01660fb1f1057b1a"
      }

  }

  val priceRiseFactorCap = 1.5 // cap is 50% rise


}
