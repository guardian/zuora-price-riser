package com.gu

object Main extends App {
  val priceRises = FileImporter.importCsv()
  priceRises.foreach {
    case Left(error) =>
      println(error)
      // ErrorLogger.write

    case Right(priceRise) =>
      println(priceRise.subscriptionName)
  }

  val subscription = ZuoraClient.getSubscription("A-S00047799")
  val account = ZuoraClient.getAccount(subscription.accountNumber)

  /*
  Scenario 1:
  - if auto-renew == true
  - if status == 'Active'
  - if targetPrice >= default product rate plan charge price
  - if deliveryRegion == currency
  - if given priceRiseDate makes sense (is one day after invoice period end date)
   */
}

