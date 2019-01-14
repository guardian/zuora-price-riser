package com.gu

import java.io.File

import kantan.csv._
import kantan.csv.joda.time._
import kantan.csv.ops._
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object BillRunFileImporter {
  val format = DateTimeFormat.forPattern("dd/MM/yyyy")
  implicit val decoder: CellDecoder[LocalDate] = localDateDecoder(format)

  case class BillRunInvoice(
    InvoiceTotalAmount: Float,
    SubscriptionNumber: String,
    invoiceNumber: String,
    productName: String,
    invoiceDate: LocalDate
  )

  implicit val billRunInvoiceDecoder: HeaderDecoder[BillRunInvoice] =
    HeaderDecoder.decoder("InvoiceTotalAmount", "Subscription Number", "InvoiceNumber", "ProductName", "InvoiceDate")(BillRunInvoice.apply _)

  def importCsv(filename: String = "billrun.csv"): List[BillRunInvoice] =
    new File(filename).asUnsafeCsvReader[BillRunInvoice](rfc.withHeader).toList
}
