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
  type date_letter_sent = LocalDate
  type price_rise_date = LocalDate
  type current_price_today = Float
  type guardian_weekly_new_price = Float
  type term_end_date = Option[LocalDate]

  type Row =
    (
      subscription_name,
        campaign_name,
        date_letter_sent,
        price_rise_date,
        current_price_today,
        guardian_weekly_new_price,
        term_end_date
      )

  case class PriceRise(
    subscriptionName: subscription_name,
    campaignName: campaign_name,
    dateLetterSent: date_letter_sent,
    priceRiseDate: price_rise_date,
    currentPrice: current_price_today,
    newPrice: guardian_weekly_new_price,
    termEndDate: term_end_date
  )

  private lazy val csvReader: CsvReader[ReadResult[PriceRise]] =
    new File("subs.csv").asCsvReader[PriceRise](rfc.withHeader)

  def importCsv(filename: String = "subs.scv"): CsvReader[ReadResult[PriceRise]] =
    csvReader

}
