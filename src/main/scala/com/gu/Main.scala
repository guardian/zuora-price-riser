package com.gu

object Main extends App {
  val priceRises = FileImporter.importCsv()
  priceRises.foreach {
    case Left(error) =>
      println(error)
      // ErrorLogger.write("bad import row")

    case Right(priceRise) =>
      val subscription = ZuoraClient.getSubscription("A-S00047799")
      val account = ZuoraClient.getAccount(subscription.accountNumber)
      if (PriceRiseValidator.validate(priceRise, subscription, account)) {

      } else {
        // ErrorLogger.write("price rise failed validation")
      }
      println(priceRise.subscriptionName)
  }



  /*
  Scenario 1:
   */
}

