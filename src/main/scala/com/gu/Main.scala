package com.gu

import org.joda.time.LocalDate

object Main extends App with ZuoraJsonFormats {

  val priceRises = FileImporter.importCsv()
  priceRises.foreach {
    case Left(error) =>
      println(error)
      // ErrorLogger.write("bad import row")

    case Right(priceRise) =>
      val newGuardianWeeklyProductCatalogue = ZuoraClient.getNewGuardianWeeklyProductCatalogue()
      val subscription = ZuoraClient.getSubscription(priceRise.subscriptionName)
      val account = ZuoraClient.getAccount(subscription.accountNumber)
      val currentSubscription = CurrentGuardianWeeklySubscription(subscription, account)
      val priceRiseRequest = PriceRiseRequestBuilder(currentSubscription, newGuardianWeeklyProductCatalogue, priceRise)

      val unsatisfiedPriceRisePreConditions = PriceRiseValidator.validate(priceRise, subscription, account, newGuardianWeeklyProductCatalogue)
      val unsatisfiedPriceRiseRequestConditions = PriceRiseRequestValidation(priceRiseRequest, currentSubscription)

      if (unsatisfiedPriceRisePreConditions.isEmpty && unsatisfiedPriceRiseRequestConditions.isEmpty) {

        println("validation successful")
        println(priceRise.subscriptionName)
        println(priceRiseRequest)
        val priceRiseResponse = ZuoraClient.removeAndAddAProductRatePlan(priceRise.subscriptionName, priceRiseRequest)

        if (priceRiseResponse.success)
          println(s"Successfully applied price rise to ${priceRise.subscriptionName}")
        else
          throw new RuntimeException(s"Failed to apply price rice to ${priceRise.subscriptionName}: $priceRiseResponse")

      } else {
        // ErrorLogger.write("price rise failed validation")
        println(s"${priceRise.subscriptionName} failed because of unsatisfied conditions: $unsatisfiedPriceRisePreConditions; $unsatisfiedPriceRiseRequestConditions")
      }
  }

}

