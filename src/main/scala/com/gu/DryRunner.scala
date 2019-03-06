package com.gu

import com.gu.Main.{args, csvImport, resumeSubscriptionNameOpt}
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

/**
  * Check pre-conditions for all import records, and outputs stats on failed conditions.
  * Do not write to Zuora.
  */
object DryRunner extends App with LazyLogging {
  if (args.length == 0)
    Abort("Please provide import filename")
  val filename = args(0)
  val resumeSubscriptionNameOpt = Try(args(1)).toOption
  val csvImport = FileImporter.importCsv(filename)

  val importSize = csvImport.size
  var unsatisfiedPreConditionsCount = List.empty[Any]
  var skipReasonsCount = List.empty[SkipReason]
  var termExtensionCount = 0
  var readyToApplyCount = 0 // pre-conditions passed

  logger.info(s"Start dry run processing $importSize records from $filename...")
  ResumeProcessing(csvImport, resumeSubscriptionNameOpt).foreach {
    case Left(importError) =>
      Abort(s"Bad import file: $importError")

    case Right(priceRise) =>
      // **********************************************************************************************
      // 1. GET CURRENT ZUORA DATA
      // **********************************************************************************************
      val newGuardianWeeklyProductCatalogue = ZuoraClient.getNewGuardianWeeklyProductCatalogue
      val subscriptionBefore = ZuoraClient.getSubscription(priceRise.subscriptionName)
      val accountBefore = ZuoraClient.getAccount(subscriptionBefore.accountNumber)

      // **********************************************************************************************
      // 2. CHECK PRE-CONDITIONS
      // **********************************************************************************************
      val skipReasons = Skip(subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue, priceRise)
      if (skipReasons.nonEmpty) {
        skipReasonsCount = skipReasonsCount ++ skipReasons
        logger.info(s"${subscriptionBefore.subscriptionNumber} skipped because $skipReasons")
        priceRise.logOutputRow(subscriptionBefore.autoRenew, skipReasons)
      }
      else {
        val currentSubscription = CurrentGuardianWeeklySubscription(subscriptionBefore, accountBefore)
        val priceRiseRequest = PriceRiseRequestBuilder(subscriptionBefore, currentSubscription, newGuardianWeeklyProductCatalogue, priceRise)
        val projectedInvoiceItems = ZuoraClient.guardianWeeklyInvoicePreview(accountBefore)

        val unsatisfiedPreConditions =
          CheckPriceRisePreConditions(priceRise, subscriptionBefore, accountBefore, projectedInvoiceItems, newGuardianWeeklyProductCatalogue
          ) ++ CheckPriceRiseRequestPreConditions(priceRiseRequest, currentSubscription)

        if(unsatisfiedPreConditions.nonEmpty) {
          unsatisfiedPreConditionsCount = unsatisfiedPreConditionsCount ++ unsatisfiedPreConditions
          logger.warn(s"${subscriptionBefore.subscriptionNumber} would NOT apply because of unsatisfied preconditions: $unsatisfiedPreConditions")
        }
        else {
          readyToApplyCount = readyToApplyCount + 1
          logger.info(s"${subscriptionBefore.subscriptionNumber} ready to apply")

          if (ExtendTermRequestBuilder(subscriptionBefore, priceRise.priceRiseDate).isDefined)
            termExtensionCount = termExtensionCount + 1
        }
      }
  }

  logger.info(s"Dry run completed for $filename")
  logger.info(s"--------------------------------------------------------------")
  logger.info(s"Results (count):")
  logger.info(s"--------------------------------------------------------------")
  logger.info(s"Import size: $importSize")
  logger.info(s"Ready to apply: $readyToApplyCount")

  unsatisfiedPreConditionsCount
    .groupBy(identity).mapValues(_.size) // https://stackoverflow.com/a/28495085/5205022
    .foreach { case (precondition, count) => logger.warn(s"Unsatisfied $precondition: $count") }

  skipReasonsCount
    .groupBy(identity).mapValues(_.size)
    .foreach { case (skipReason, count) => logger.info(s"Skip because $skipReason: $count") }

  logger.info(s"Term extension applied: $termExtensionCount")
}
