package com.gu

import com.gu.FileImporter.PriceRise
import com.typesafe.scalalogging.LazyLogging
import kantan.csv.ReadResult

object ResumeProcessing extends LazyLogging{
  def apply(
      csvImport: List[ReadResult[PriceRise]],
      resumeSubscriptionNameOpt: Option[String]
  ): List[ReadResult[PriceRise]] = {

    resumeSubscriptionNameOpt match {
      case None =>
        csvImport

      case Some(resumeSubscriptionName) =>
        val resumeIndex = csvImport.indexWhere { priceRiseRecord =>
          priceRiseRecord match {
            case Left(_) => false
            case Right(priceRise) => priceRise.subscriptionName == resumeSubscriptionName
          }
        }

        assert(resumeIndex != -1, "Subscriptions name must exists in import file to resume processing")

        logger.info(s"Resuming processing from $resumeSubscriptionName...")
        csvImport.drop(resumeIndex)
    }
  }

}
