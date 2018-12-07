package com.gu

import com.typesafe.scalalogging.LazyLogging

object Main extends App with LazyLogging {

  val priceRises = FileImporter.importCsv()
  priceRises.foreach {
    case Left(error) =>
      logger.error(s"Bad import: $error")

    case Right(priceRise) =>
      val newGuardianWeeklyProductCatalogue = ZuoraClient.getNewGuardianWeeklyProductCatalogue()
      val subscription = ZuoraClient.getSubscription(priceRise.subscriptionName)
      val account = ZuoraClient.getAccount(subscription.accountNumber)
      val currentSubscription = CurrentGuardianWeeklySubscription(subscription, account)
      val priceRiseRequest = PriceRiseRequestBuilder(currentSubscription, newGuardianWeeklyProductCatalogue, priceRise)

      val unsatisfiedPriceRisePreConditions = PriceRiseValidator.validate(priceRise, subscription, account, newGuardianWeeklyProductCatalogue)
      val unsatisfiedPriceRiseRequestConditions = PriceRiseRequestValidation(priceRiseRequest, currentSubscription)

      if (unsatisfiedPriceRisePreConditions.isEmpty && unsatisfiedPriceRiseRequestConditions.isEmpty) {
        logger.info(s"validation successful for ${priceRise.subscriptionName}")
        val priceRiseResponse = ZuoraClient.removeAndAddAProductRatePlan(priceRise.subscriptionName, priceRiseRequest)

        if (priceRiseResponse.success)
          logger.info(s"Successfully applied price rise to ${priceRise.subscriptionName}")
        else
          throw new RuntimeException(s"Failed to apply price rice to ${priceRise.subscriptionName}: $priceRiseResponse")
      } else {
        logger.error(s"${priceRise.subscriptionName} failed because of unsatisfied conditions: $unsatisfiedPriceRisePreConditions; $unsatisfiedPriceRiseRequestConditions")
      }
  }

}

