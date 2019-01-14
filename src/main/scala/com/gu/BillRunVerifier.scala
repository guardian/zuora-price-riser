package com.gu

import com.typesafe.scalalogging.LazyLogging

object BillRunVerifier extends App with LazyLogging {
  if (args.length == 0)
    Abort("Please provide import filename")
  val filename = args(0)
  val billRunFilename = args(1)

  val csvImport = FileImporter.importCsv(filename)
  val billRunImport = BillRunFileImporter.importCsv(billRunFilename)

  var verified = true

  logger.info(s"Start verifying bill run $billRunFilename against $filename...")
  csvImport.foreach {
    case Left(importError) =>
      Abort(s"Bad import file: $importError")

    case Right(priceRise) =>

      billRunImport
        .find(_.SubscriptionNumber == priceRise.subscriptionName)
        .foreach { invoice =>
          if (invoice.InvoiceTotalAmount == priceRise.newPrice) {
            //            logger.info(s"$invoice === $priceRise")
          }
          else {
            verified = false
            logger.warn(s"$invoice =/= $priceRise")
          }
        }



//      assert {
//        billRunImport
//          .exists(invoice => (invoice.SubscriptionNumber == priceRise.subscriptionName) && (invoice.InvoiceTotalAmount == priceRise.newPrice))
//      }
  }

  logger.info(s"Finished verifying bill run $billRunFilename against $filename")
  if (verified)
    logger.info(Console.GREEN + s"Bill run verified.")
}
