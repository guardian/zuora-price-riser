package com.gu

import com.typesafe.scalalogging.LazyLogging
import scala.io.StdIn

/**
  * The script will stop on the first error it encounters.
  * It is safe to re-run the script on same input file.
  */
object Main extends App with LazyLogging {
  if (args.length == 0)
    Abort("Please provide import filename")
  val filename = args(0)

  val csvImport = FileImporter.importCsv(filename)

  if (Config.Zuora.stage == "PROD") {
    logger.warn(Console.RED + "WARNING: Are you sure you want to run against Zuora PROD? (Y/N)" + Console.RESET)
    StdIn.readLine() match {
      case "Y" =>
      case _ => Abort("User aborted the script.")
    }
  }

  val importSize = csvImport.size
  var unsatisfiedPreConditionsCount = List.empty[Any]
  var skipReasonsCount = List.empty[SkipReason]
  var successfullyAppliedCount = 0

  logger.info(s"Start processing $importSize records from $filename...")
  csvImport.foreach {
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
        logger.info(s"${priceRise.subscriptionName} skipped because $skipReasons")
        skipReasonsCount = skipReasonsCount ++ skipReasons
        if (skipReasons.contains(PriceRiseApplied)) successfullyAppliedCount = successfullyAppliedCount + 1
      }
      else {
        val currentSubscription = CurrentGuardianWeeklySubscription(subscriptionBefore, accountBefore)
        val priceRiseRequest = PriceRiseRequestBuilder(subscriptionBefore, currentSubscription, newGuardianWeeklyProductCatalogue, priceRise)

        val unsatisfiedPreConditions =
          CheckPriceRisePreConditions(priceRise, subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue
          ) ++ CheckPriceRiseRequestPreConditions(priceRiseRequest, currentSubscription)

        if(unsatisfiedPreConditions.nonEmpty) {
          unsatisfiedPreConditionsCount = unsatisfiedPreConditionsCount ++ unsatisfiedPreConditions
          logger.warn(s"${subscriptionBefore.subscriptionNumber} skipped because of unsatisfied preconditions: $unsatisfiedPreConditions")
        }
        else {
          // **********************************************************************************************
          // 3. MUTATE
          // **********************************************************************************************
          ExtendTermRequestBuilder(subscriptionBefore, currentSubscription).map(extendTerm => ZuoraClient.extendTerm(priceRise.subscriptionName, extendTerm))
          val priceRiseResponse = ZuoraClient.removeAndAddAProductRatePlan(priceRise.subscriptionName, priceRiseRequest)

          // **********************************************************************************************
          // 4. CHECK POST-CONDITIONS
          // **********************************************************************************************
          if (!priceRiseResponse.success)
            Abort(s"Failed price rise request for ${priceRise.subscriptionName}: $priceRiseResponse")
          val subscriptionAfter = ZuoraClient.getSubscription(priceRise.subscriptionName)
          val accountAfter = ZuoraClient.getAccount(subscriptionBefore.accountNumber)
          val unsatisfiedPostConditions = CheckPriceRisePostConditions(subscriptionAfter, accountBefore, accountAfter, newGuardianWeeklyProductCatalogue, priceRise, currentSubscription)
          if (unsatisfiedPostConditions.nonEmpty)
            Abort(s"${priceRise.subscriptionName} failed because of unsatisfied post-conditions: $unsatisfiedPostConditions")
          val newGuardianWeeklySubscription = NewGuardianWeeklySubscription(subscriptionAfter, accountAfter, newGuardianWeeklyProductCatalogue)

          // **********************************************************************************************
          // 5. LOG SUCCESS
          // **********************************************************************************************
          successfullyAppliedCount = successfullyAppliedCount + 1
          logger.info(s"${priceRise.subscriptionName} successfully applied price rise: $newGuardianWeeklySubscription")
        }
      }
  }

  logger.info(s"Processing completed of $importSize records from $filename")
  logger.info(s"--------------------------------------------------------------")
  logger.info(s"RESULTS:")
  logger.info(s"--------------------------------------------------------------")
  logger.info(s"Successfully applied: $successfullyAppliedCount (${math.floor(successfullyAppliedCount/importSize)}%)")
  logger.info(s"Skipped: ${skipReasonsCount.size + unsatisfiedPreConditionsCount.size}")
  if (unsatisfiedPreConditionsCount.nonEmpty)
    logger.warn(s"Skipped due to unsatisfied preconditions: ${unsatisfiedPreConditionsCount.size}")
  else
    logger.info(Console.GREEN + s"DONE.")
}

