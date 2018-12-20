package com.gu

import com.typesafe.scalalogging.LazyLogging

/**
  * Check pre-conditions for all import records, and outputs stats on failed conditions.
  * Do not write to Zuora.
  */
object DryRunner extends App with LazyLogging {
  if (args.length == 0)
    Abort("Please provide import filename")
  val filename = args(0)

  logger.info(s"Start dry run processing $filename...")

  var unsatisfiedPreConditionsCount = List.empty[Any]
  var alreadyAppliedCount = 0
  var termExtensionCount = 0

  FileImporter.importCsv(filename).foreach {
    case Left(importError) =>
      Abort(s"Bad import file: $importError")

    case Right(priceRise) =>
      // **********************************************************************************************
      // 1. GET CURRENT ZUORA DATA
      // **********************************************************************************************
      val newGuardianWeeklyProductCatalogue = ZuoraClient.getNewGuardianWeeklyProductCatalogue()
      val subscriptionBefore = ZuoraClient.getSubscription(priceRise.subscriptionName)
      val accountBefore = ZuoraClient.getAccount(subscriptionBefore.accountNumber)

      // **********************************************************************************************
      // 2. CHECK PRE-CONDITIONS
      // **********************************************************************************************
      if (PriceRiseAlreadyApplied(subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue))
        alreadyAppliedCount = alreadyAppliedCount + 1
      else {
        val currentSubscription = CurrentGuardianWeeklySubscription(subscriptionBefore, accountBefore)
        val priceRiseRequest = PriceRiseRequestBuilder(subscriptionBefore, currentSubscription, newGuardianWeeklyProductCatalogue, priceRise)
        val extendTermRequestOpt = ExtendTermRequestBuilder(subscriptionBefore, currentSubscription)

        val unsatisfiedPreConditions =
          CheckPriceRisePreConditions(priceRise, subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue
          ) ++ CheckPriceRiseRequestPreConditions(priceRiseRequest, currentSubscription)

        if (unsatisfiedPreConditions.nonEmpty)
          unsatisfiedPreConditionsCount = unsatisfiedPreConditionsCount ++ unsatisfiedPreConditions

        if (extendTermRequestOpt.isDefined)
          termExtensionCount = termExtensionCount + 1
      }
  }

  logger.info(s"Dry run completed for $filename")
  logger.info(s"--------------------------------------------------------------")
  logger.info(s"Results (count):")
  logger.info(s"--------------------------------------------------------------")
  unsatisfiedPreConditionsCount
    .groupBy(identity).mapValues(_.size) // https://stackoverflow.com/a/28495085/5205022
    .foreach { case (precondition, count) => logger.info(s"Unsatisfied $precondition: $count") }
  logger.info(s"Price rise already applied: $alreadyAppliedCount")
  logger.info(s"Term extension applied: $termExtensionCount")
}
