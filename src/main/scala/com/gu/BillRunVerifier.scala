package com.gu

import com.typesafe.scalalogging.LazyLogging

/**
  * Cross reference 'Zuora | Bill Run | Export invoice CSV file' against price rise import file
  * https://knowledgecenter.zuora.com/CD_Reporting/D_Data_Sources_and_Exports/E_Data_Exports
  */
object BillRunVerifier extends App with LazyLogging {
  if (args.length != 2)
    Abort("Please provide price rise import file and bill run import file")
  val priceRiseFilename = args(0)
  val billRunFilename = args(1)

  val csvImport = FileImporter.importCsv(priceRiseFilename)
  val billRunImport = BillRunFileImporter.importCsv(billRunFilename)

  var verified = true

  logger.info(s"Start verifying bill run $billRunFilename against $priceRiseFilename...")
  csvImport.foreach {
    case Left(importError) =>
      Abort(s"Bad import file: $importError")

    case Right(priceRise) =>
      billRunImport
        .find(_.SubscriptionNumber == priceRise.subscriptionName)
        .foreach { invoice =>
          if (invoice.InvoiceTotalAmount == priceRise.newPrice && invoice.invoiceDate.isEqual(priceRise.priceRiseDate)) {
            //            logger.info(s"$invoice === $priceRise")
          }
          else {
            verified = false
            logger.warn(s"$invoice =/= $priceRise")
          }
        }
  }

  logger.info(s"Finished verifying bill run $billRunFilename against $priceRiseFilename")
  if (verified)
    logger.info(Console.GREEN + s"All OK.")
}
