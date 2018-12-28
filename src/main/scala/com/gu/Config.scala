package com.gu

import com.typesafe.config.ConfigFactory

object Config {
  private val conf = ConfigFactory.load
  object Zuora {
    lazy val stage: String = conf.getString("zuora.stage")
    lazy val client_id: String = conf.getString("zuora.client_id")
    lazy val client_secret: String = conf.getString("zuora.client_secret")

    object Old { // Pre-price rise Guardian Weekly
      val guardianWeeklyProductRatePlanIds: List[String] = stage match {
          case "DEV" => List(
            "2c92c0f8574b2b8101574c4a9480068d", // "name":"Guardian Weekly Annual"
            "2c92c0f8574b2b8101574c4a957706be", // "name":"Guardian Weekly Quarterly"
            "2c92c0f8574b2be601574c323ca15c7e", // "name":"Guardian Weekly Quarterly"
            "2c92c0f8574b2be601574c39888d6850", // "name":"Guardian Weekly Annual"
            "2c92c0f858aa38af0158da325cec0b2e", // "name":"Guardian Weekly Quarterly"
            "2c92c0f858aa38af0158da325d2f0b3d", // "name":"Guardian Weekly Annual"
          )
          case "PROD" => List(
            // Product: {"id":"2c92a0fd-57d0-a987-0157-d73fa27c3de1","name":"Guardian Weekly Zone A"}
            "2c92a0fd57d0a9870157d7412f19424f", // "name":"Guardian Weekly Quarterly"
            "2c92a0ff57d0a0b60157d741e722439a", // "name":"Guardian Weekly Annual"

            // Product: {"id":"2c92a0fe-57d0-a0c4-0157-d74240d35541","name":"Guardian Weekly Zone B"}
            "2c92a0fe57d0a0c40157d74241005544", // "name":"Guardian Weekly Quarterly"
            "2c92a0fe57d0a0c40157d74240de5543", // "name":"Guardian Weekly Annual"

            // Product: {"id":"2c92a0ff-58bd-f4eb-0158-f307ecc102ad","name":"Guardian Weekly Zone C"}
            "2c92a0ff58bdf4eb0158f307ed0e02be", // "name":"Guardian Weekly Quarterly"
            "2c92a0ff58bdf4eb0158f307eccf02af", // "name":"Guardian Weekly Annual"
          )
        }
    }

    object New {
      val guardianWeeklyDomesticProductId: String =
        Config.Zuora.stage match {
          case "DEV" | "dev" => "2c92c0f865d272ef0165f14cc19d238a"   // "name":"Guardian Weekly - Domestic"
          case "PROD" | "prod" => "2c92a0ff6619bf8901661aa3247c4b1d" // "name":"Guardian Weekly - Domestic"
        }

      val guardianWeeklyRowProductId: String =
        Config.Zuora.stage match {
          case "DEV" | "dev" => "2c92c0f965f2121e01660fb1f1057b1a"    // "name":"Guardian Weekly - ROW"
          case "PROD" | "prod" => "2c92a0fe6619b4b901661aaf826435de"  // "name":"Guardian Weekly - ROW"
        }

      val guardianWeeklyProductRatePlanIds: List[String] = stage match {
        case "DEV" => List(
          // Product: {"id":"2c92c0f8-65d2-72ef-0165-f14cc19d238a", "name":"Guardian Weekly - Domestic"}
          "2c92c0f965d280590165f16b1b9946c2", // "name": "GW Oct 18 - Annual - Domestic"
          "2c92c0f965dc30640165f150c0956859", // "name": "GW Oct 18 - Quarterly - Domestic"

            // Product: {"2c92c0f9-65f2-121e-0166-0fb1f1057b1a", "name":"Guardian Weekly - ROW"}
          "2c92c0f965f2122101660fb33ed24a45", // "name":"GW Oct 18 - Annual - ROW"
          "2c92c0f965f2122101660fb81b745a06", // "name":"GW Oct 18 - Quarterly - ROW"
        )
        case "PROD" => List(
          // Product: {"id":"2c92a0ff-6619-bf89-0166-1aa3247c4b1d", "name":"Guardian Weekly - Domestic"}
          "2c92a0fe6619b4b901661aa8e66c1692", // "name": "GW Oct 18 - Annual - Domestic"
          "2c92a0fe6619b4b301661aa494392ee2", // "name": "GW Oct 18 - Quarterly - Domestic"

          // Product: {"2c92a0fe-6619-b4b9-0166-1aaf826435de", "name":"Guardian Weekly - ROW"}
          "2c92a0fe6619b4b601661ab300222651", // "name":"GW Oct 18 - Annual - ROW"
          "2c92a0086619bf8901661ab02752722f", // "name":"GW Oct 18 - Quarterly - ROW"
        )
      }

      val guardianWeeklyProductCatalogue = stage match {
        case "DEV" =>
          NewGuardianWeeklyProductCatalogue(
            List(
              NewGuardianWeeklyProduct(
                "GW Oct 18 - Annual - Domestic",
                "Annual",
                "2c92c0f965d280590165f16b1b9946c2",
                "2c92c0f965d280590165f16b1ba946c4",
                List(
                  Price("USD", 300.0F),
                  Price("NZD", 492.0F),
                  Price("EUR", 245.2F),
                  Price("GBP", 150.0F),
                  Price("CAD", 320.0F),
                  Price("AUD", 390.0F)
                ),
                "Guardian Weekly"
              ),
              NewGuardianWeeklyProduct(
                "GW Oct 18 - Quarterly - Domestic",
                "Quarter",
                "2c92c0f965dc30640165f150c0956859",
                "2c92c0f865d273010165f16ada0a4346",
                List(
                  Price("USD", 75.0F),
                  Price("NZD", 123.0F),
                  Price("EUR", 61.3F),
                  Price("GBP", 37.5F),
                  Price("CAD", 80.0F),
                  Price("AUD", 97.5F)
                ),
                "Guardian Weekly"
              )
            ),
            List(
              NewGuardianWeeklyProduct(
                "GW Oct 18 - Quarterly - ROW",
                "Quarter",
                "2c92c0f965f2122101660fb81b745a06",
                "2c92c0f965f2122101660fb81b875a0b",
                List(
                  Price("USD", 81.3F),
                  Price("NZD", 132.5F),
                  Price("EUR", 67.5F),
                  Price("GBP", 60.0F),
                  Price("CAD", 86.25F),
                  Price("AUD", 106.0F)
                ),
                "Guardian Weekly"
              ),
              NewGuardianWeeklyProduct(
                "GW Oct 18 - Annual - ROW",
                "Annual",
                "2c92c0f965f2122101660fb33ed24a45",
                "2c92c0f965f2122101660fb6ac46550e",
                List(
                  Price("USD", 325.2F),
                  Price("NZD", 530.0F),
                  Price("EUR", 270.0F),
                  Price("GBP", 240.0F),
                  Price("CAD", 345.0F),
                  Price("AUD", 424.0F)
                ),
                "Guardian Weekly"
              )
            )
          )

        case "PROD" =>
          NewGuardianWeeklyProductCatalogue(
            List(
              NewGuardianWeeklyProduct(
                "GW Oct 18 - Annual - Domestic",
                "Annual",
                "2c92a0fe6619b4b901661aa8e66c1692",
                "2c92a0fe6619b4b901661aa8e6811695",
                List(
                  Price("USD", 300.0F),
                  Price("NZD", 492.0F),
                  Price("EUR", 245.2F),
                  Price("GBP", 150.0F),
                  Price("CAD", 320.0F),
                  Price("AUD", 390.0F)
                ),
                "Guardian Weekly"
              ),
              NewGuardianWeeklyProduct(
                "GW Oct 18 - Quarterly - Domestic",
                "Quarter",
                "2c92a0fe6619b4b301661aa494392ee2",
                "2c92a0fe6619b4b601661aa8b74e623f",
                List(
                  Price("USD", 75.0F),
                  Price("NZD", 123.0F),
                  Price("EUR", 61.3F),
                  Price("GBP", 37.5F),
                  Price("CAD", 80.0F),
                  Price("AUD", 97.5F)
                ),
                "Guardian Weekly"
              )
            ),
            List(
              NewGuardianWeeklyProduct(
                "GW Oct 18 - Annual - ROW",
                "Annual",
                "2c92a0fe6619b4b601661ab300222651",
                "2c92a0fe6619b4b601661ab3002f2653",
                List(
                  Price("USD", 325.2F),
                  Price("NZD", 530.0F),
                  Price("EUR", 270.0F),
                  Price("GBP", 240.0F),
                  Price("CAD", 345.0F),
                  Price("AUD", 424.0F)
                ),
                "Guardian Weekly"
              ),
              NewGuardianWeeklyProduct(
                "GW Oct 18 - Quarterly - ROW",
                "Quarter",
                "2c92a0086619bf8901661ab02752722f",
                "2c92a0ff6619bf8b01661ab2d0396eb2",
                List(
                  Price("USD", 81.3F),
                  Price("NZD", 132.5F),
                  Price("EUR", 67.5F),
                  Price("GBP", 60.0F),
                  Price("CAD", 86.25F),
                  Price("AUD", 106.0F)
                ),
                "Guardian Weekly"
              )
            )
          )

      }
    }

    // Do not remove Holiday and Retention Discounts (Cancellation Save Discount)
    val doNotRemoveProductRatePlanIds: List[String] =
      Config.Zuora.stage match {
        case "DEV" | "dev" => List(
          "2c92c0f9671686a201671d14b5e5771e", // "name":"Guardian Weekly Holiday Credit"
          "2c92c0f962cec7990162d3882afc52dd", // "name":"Cancellation Save Discount - 25% off for 3 months"
          "2c92c0f862ceb7050162d393b0ff6df7", // "name":"Cancellation Save Discount - 50% off for 3 months"
        )
        case "PROD" | "prod" => List(
          "2c92a0fc5b42d2c9015b6259f7f40040", // "name":"Guardian Weekly Holiday Credit"
          "2c92a0ff64176cd40164232c8ec97661", // "name":"Cancellation Save Discount - 25% off for 3 months"
          "2c92a00864176ce90164232ac0d90fc1", // "name":"Cancellation Save Discount - 50% off for 3 months"
        )
      }
  }

  val priceRiseFactorCap = 1.99 // cap is 99% rise;
}
