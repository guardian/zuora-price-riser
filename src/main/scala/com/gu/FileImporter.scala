package com.gu

import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import kantan.csv.joda.time._
import java.io.File

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object FileImporter extends LazyLogging {
  val format = DateTimeFormat.forPattern("dd/MM/yyyy")
  implicit val decoder: CellDecoder[LocalDate] = localDateDecoder(format)

  type subscription_name = String
  type campaign_name = String
  type date_letter_sent = LocalDate
  type price_rise_date = LocalDate
  type current_price_today = Float
  type guardian_weekly_new_price = Float
  type term_end_date = Option[LocalDate]

  case class PriceRise(
    subscriptionName: subscription_name,
    campaignName: campaign_name,
    dateLetterSent: date_letter_sent,
    priceRiseDate: price_rise_date,
    _unsafeCurrentPrice: current_price_today, // WARNING: Do not trust this value.
    newPrice: guardian_weekly_new_price,
    termEndDate: term_end_date
  ) {
    def logOutputRow(autoRenew: Boolean, skipReasons: List[SkipReason]) = {
      val priceRiseOutputCsvRow = s"$subscriptionName,$campaignName,$dateLetterSent,$priceRiseDate,${_unsafeCurrentPrice},$newPrice,${termEndDate.getOrElse("")},${autoRenew}"
      skipReasons match {
        case list if list.contains(PriceRiseApplied) => logger.info(s"PRICE RISE APPLIED:$priceRiseOutputCsvRow")
        case list if list.contains(OneOff) => logger.info(s"ONE-OFF:$priceRiseOutputCsvRow")
        case list if list.contains(Cancelled) => logger.info(s"CANCELLED:$priceRiseOutputCsvRow")
        case _ => logger.error(s"Unexpected skip reason detected:$priceRiseOutputCsvRow")
      }
    }

  }

  def importCsv(filename: String = "subs.csv"): List[ReadResult[PriceRise]] =
    new File(filename).asCsvReader[PriceRise](rfc.withHeader).toList

}
