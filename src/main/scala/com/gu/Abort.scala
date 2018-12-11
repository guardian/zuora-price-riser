package com.gu

import com.typesafe.scalalogging.LazyLogging

/**
  * Log an error and exit the program.
  *
  * The application is designed to stop on the first error it encounters.
  * After the error:
  *  1. examine the logs to determine the cause,
  *  2. fix the cause
  *  3. re-run the script (the script should be idempotent)
  */
object Abort extends LazyLogging {
  def apply(errorMessage: String): Unit = {
    logger.error(errorMessage)
    logger.error("Aborted due to error. Please examine the logs, fix the error, and re-run the script.")
    System.exit(1)
  }
}
