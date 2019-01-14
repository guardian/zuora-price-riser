package com.gu

import java.io.File

import kantan.csv._
import kantan.csv.joda.time._
import kantan.csv.ops._
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object BillRunFileImporter {
  case class BillRunInvoice(
    InvoiceTotalAmount: Float,
    SubscriptionNumber: String,
    invoiceNumber: String
  )

  implicit val billRunInvoiceDecoder: HeaderDecoder[BillRunInvoice] =
    HeaderDecoder.decoder("InvoiceTotalAmount", "Subscription Number", "InvoiceNumber")(BillRunInvoice.apply _)

  def importCsv(filename: String = "billrun.csv"): List[BillRunInvoice] =
    new File(filename).asUnsafeCsvReader[BillRunInvoice](rfc.withHeader).toList
}
