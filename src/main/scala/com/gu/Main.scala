package com.gu

object Main extends App {
  val newGuardianWeeklyProductCatalogue = ZuoraClient.getNewGuardianWeeklyProductCatalogue()

  val priceRises = FileImporter.importCsv()
  priceRises.foreach {
    case Left(error) =>
      println(error)
      // ErrorLogger.write("bad import row")

    case Right(priceRise) =>
//      val subscription = ZuoraClient.getSubscription("A-S00047206")
      val subscription = ZuoraClient.getSubscription(priceRise.subscriptionName)
      val account = ZuoraClient.getAccount(subscription.accountNumber)
      val unsatisfiedPriceRisePreConditions =
        PriceRiseValidator.validate(priceRise, subscription, account, newGuardianWeeklyProductCatalogue)

      if (unsatisfiedPriceRisePreConditions.isEmpty) {
        println("validation successful")
        println(priceRise.subscriptionName)
      } else {
        // ErrorLogger.write("price rise failed validation")
        println(s"${priceRise.subscriptionName} failed because of unsatisfied conditions: $unsatisfiedPriceRisePreConditions")
      }
  }
}

