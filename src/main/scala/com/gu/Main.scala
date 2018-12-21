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

  if (Config.Zuora.stage == "PROD") {
    logger.warn(Console.RED + "WARNING: Are you sure you want to run against Zuora PROD? (Y/N)" + Console.RESET)
    StdIn.readLine() match {
      case "Y" =>
      case _ => Abort("User aborted the script.")
    }
  }

  logger.info(s"Start processing $filename...")

  FileImporter.importCsv(filename).foreach {
    case Left(importError) =>
      Abort(s"Bad import file: $importError")

    case Right(priceRise) =>
      // **************************************************************************************************************
      // 1. GET CURRENT ZUORA DATA
      // **************************************************************************************************************
      val newGuardianWeeklyProductCatalogue = ZuoraClient.getNewGuardianWeeklyProductCatalogue()
      val subscriptionBefore = ZuoraClient.getSubscription(priceRise.subscriptionName)
      val accountBefore = ZuoraClient.getAccount(subscriptionBefore.accountNumber)

      // **************************************************************************************************************
      // 2. CHECK PRE-CONDITIONS
      // **************************************************************************************************************

      val skipReasons = Skip(subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue)
      if (skipReasons.nonEmpty) {
        logger.info(s"${priceRise.subscriptionName} skipped because $skipReasons")
      } else {

        val currentSubscription = CurrentGuardianWeeklySubscription(subscriptionBefore, accountBefore)
        val priceRiseRequest = PriceRiseRequestBuilder(subscriptionBefore, currentSubscription, newGuardianWeeklyProductCatalogue, priceRise)
        val extendTermRequestOpt = ExtendTermRequestBuilder(subscriptionBefore, currentSubscription)

        val unsatisfiedPreConditions =
          CheckPriceRisePreConditions(priceRise, subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue
          ) ++ CheckPriceRiseRequestPreConditions(priceRiseRequest, currentSubscription)

        if (unsatisfiedPreConditions.nonEmpty)
          Abort(s"${priceRise.subscriptionName} failed because of unsatisfied pre-conditions: $unsatisfiedPreConditions")

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

