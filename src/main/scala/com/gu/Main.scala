package com.gu

import com.typesafe.scalalogging.LazyLogging

/**
  * The script will stop on the first error it encounters.
  * It is safe to re-run the script on same input file.
  */
object Main extends App with LazyLogging {
  if (args.length == 0)
    Abort("Please provide import filename")
  val filename = args(0)

  logger.info(s"Start processing $filename...")

  FileImporter.importCsv(filename).foreach {
    case Left(importError) =>
      Abort(s"Bad import file: $importError")

    case Right(priceRise) =>
      // **************************************************************************************************************
      // 1. PREPARE DATA
      // **************************************************************************************************************
      val newGuardianWeeklyProductCatalogue = ZuoraClient.getNewGuardianWeeklyProductCatalogue()
      val subscriptionBefore = ZuoraClient.getSubscription(priceRise.subscriptionName)
      val accountBefore = ZuoraClient.getAccount(subscriptionBefore.accountNumber)
      val currentSubscription = CurrentGuardianWeeklySubscription(subscriptionBefore, accountBefore)
      val priceRiseRequest = PriceRiseRequestBuilder(subscriptionBefore, currentSubscription, newGuardianWeeklyProductCatalogue, priceRise)
      val extendTermRequestOpt = ExtendTermRequestBuilder(subscriptionBefore, currentSubscription)

      // **************************************************************************************************************
      // 2. CHECK PRE-CONDITIONS
      // **************************************************************************************************************
      val unsatisfiedPreConditions =
        CheckPriceRisePreConditions(priceRise, subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue
        ) ++ CheckPriceRiseRequestPreConditions(priceRiseRequest, currentSubscription)

      if (unsatisfiedPreConditions.nonEmpty)
        Abort(s"${priceRise.subscriptionName} failed because of unsatisfied pre-conditions: $unsatisfiedPreConditions")

      if (PriceRiseAlreadyApplied(subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue)) {
        logger.info(s"${priceRise.subscriptionName} skipped because price rise already applied")
      } else {
        // ************************************************************************************************************
        // 3. MUTATE
        // ************************************************************************************************************
        extendTermRequestOpt.map(extendTerm => ZuoraClient.extendTerm(priceRise.subscriptionName, extendTerm))
        val priceRiseResponse = ZuoraClient.removeAndAddAProductRatePlan(priceRise.subscriptionName, priceRiseRequest)

        // ************************************************************************************************************
        // 4. CHECK POST-CONDITIONS
        // ************************************************************************************************************
        if (!priceRiseResponse.success)
          Abort(s"Failed price rise request for ${priceRise.subscriptionName}: $priceRiseResponse")
        val subscriptionAfter = ZuoraClient.getSubscription(priceRise.subscriptionName)
        val accountAfter = ZuoraClient.getAccount(subscriptionBefore.accountNumber)
        val unsatisfiedPostConditions = CheckPriceRisePostConditions(subscriptionAfter, accountBefore, accountAfter, newGuardianWeeklyProductCatalogue, priceRise, currentSubscription)
        if (unsatisfiedPostConditions.nonEmpty)
          Abort(s"${priceRise.subscriptionName} failed because of unsatisfied post-conditions: $unsatisfiedPostConditions")
        val newGuardianWeeklySubscription = NewGuardianWeeklySubscription(subscriptionAfter, accountAfter, newGuardianWeeklyProductCatalogue)

        // ************************************************************************************************************
        // 5. LOG SUCCESS
        // ************************************************************************************************************
        logger.info(s"${priceRise.subscriptionName} successfully applied price rise: $newGuardianWeeklySubscription")
      }
  }

  logger.info(s"Successfully completed $filename")
}

