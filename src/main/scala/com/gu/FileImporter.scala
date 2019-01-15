package com.gu

import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import kantan.csv.joda.time._
import java.io.File

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object FileImporter {
  val format = DateTimeFormat.forPattern("dd/MM/yyyy")
  implicit val decoder: CellDecoder[LocalDate] = localDateDecoder(format)

  type subscription_name = String
  type campaign_name = String
  type date_letter_sent = String // we do not care about this so no LocalDate
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
  )

  def importCsv(filename: String = "subs.csv"): List[ReadResult[PriceRise]] =
    new File(filename).asCsvReader[PriceRise](rfc.withHeader).toList

}
