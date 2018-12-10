package com.gu

import com.typesafe.scalalogging.LazyLogging

/**
  * The script will stop on the first error it encounters.
  */
object Main extends App with LazyLogging {
  FileImporter.importCsv().foreach {
    case Left(importError) =>
      Abort(s"Bad import file: $importError")

    case Right(priceRise) =>
      // **************************************************************************************************************
      // 1. PREPARE DATA
      // **************************************************************************************************************
      val newGuardianWeeklyProductCatalogue = ZuoraClient.getNewGuardianWeeklyProductCatalogue()
      val subscription = ZuoraClient.getSubscription(priceRise.subscriptionName)
      val account = ZuoraClient.getAccount(subscription.accountNumber)
      val currentSubscription = CurrentGuardianWeeklySubscription(subscription, account)
      val priceRiseRequest = PriceRiseRequestBuilder(subscription, currentSubscription, newGuardianWeeklyProductCatalogue, priceRise)

      // **************************************************************************************************************
      // 2. CHECK PRE-CONDITIONS
      // **************************************************************************************************************
      val unsatisfiedPriceRisePreConditions = PriceRiseValidation(priceRise, subscription, account, newGuardianWeeklyProductCatalogue)
      val unsatisfiedPriceRiseRequestConditions = PriceRiseRequestValidation(priceRiseRequest, currentSubscription)
      val preConditions = unsatisfiedPriceRisePreConditions ++ unsatisfiedPriceRiseRequestConditions
      if (preConditions.nonEmpty)
        Abort(s"${priceRise.subscriptionName} failed because of unsatisfied pre-conditions: $preConditions")

      if (PriceRiseAlreadyApplied(subscription, account, newGuardianWeeklyProductCatalogue)) {
        logger.info(s"${priceRise.subscriptionName} skipped because price rise already applied")
      } else {
        // **************************************************************************************************************
        // 3. MUTATE
        // **************************************************************************************************************
        val priceRiseResponse = ZuoraClient.removeAndAddAProductRatePlan(priceRise.subscriptionName, priceRiseRequest)

        // **************************************************************************************************************
        // 4. CHECK POST-CONDITIONS
        // **************************************************************************************************************
        if (!priceRiseResponse.success)
          Abort(s"Failed price rise request for ${priceRise.subscriptionName}: $priceRiseResponse")
        val subscriptionAfterPriceRise = ZuoraClient.getSubscription(priceRise.subscriptionName)
        val accountAfterPriceRise = ZuoraClient.getAccount(subscription.accountNumber)
        val unsatisfiedPriceRiseResponseConditions = PriceRiseResponseValidation(subscriptionAfterPriceRise, account, newGuardianWeeklyProductCatalogue, priceRise)
        if (unsatisfiedPriceRiseResponseConditions.nonEmpty)
          Abort(s"${priceRise.subscriptionName} failed because of unsatisfied post-conditions: $unsatisfiedPriceRiseResponseConditions")
        val newGuardianWeeklySubscription = NewGuardianWeeklySubscription(subscriptionAfterPriceRise, accountAfterPriceRise, newGuardianWeeklyProductCatalogue)

        // **************************************************************************************************************
        // 5. LOG SUCCESS
        // **************************************************************************************************************
        logger.info(s"${priceRise.subscriptionName} successfully applied price rise: $newGuardianWeeklySubscription")
      }

  }
}

