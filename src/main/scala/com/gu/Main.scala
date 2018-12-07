package com.gu

import com.typesafe.scalalogging.LazyLogging

/**
  * The script will stop on the first error it encounters.
  */
object Main extends App with LazyLogging {
  try {
    val priceRises = FileImporter.importCsv()

    priceRises.foreach {
      case Left(error) =>
        Abort(s"Bad import: $error")

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
          else {
            Abort(s"Failed price rise request for ${priceRise.subscriptionName}: $priceRiseResponse")
          }

        } else {
          Abort(s"${priceRise.subscriptionName} failed because of unsatisfied conditions: $unsatisfiedPriceRisePreConditions; $unsatisfiedPriceRiseRequestConditions")
        }

    }

  } catch {
    case e: Exception => Abort(s"Failed because of unknown reason: $e")
  }
}

