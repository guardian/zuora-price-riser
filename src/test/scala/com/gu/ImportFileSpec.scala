package com.gu

import org.scalatest._

class ImportFileSpec extends FlatSpec with Matchers with ZuoraJsonFormats {
  "ImportFile" should "import a csv file" in {
    FileImporter.importCsv("./src/test/resources/subs.csv").foreach {
      case Left(importError) => fail(importError)
      case Right(priceRise) => priceRise.subscriptionName should be("A-S00048050")
    }
  }

  it should "detect an error in csv file" in {
    FileImporter.importCsv("./src/test/resources/subs-error.csv").foreach {
      case Left(importError) => succeed
      case Right(priceRise) => fail(priceRise.toString)
    }
  }

}
