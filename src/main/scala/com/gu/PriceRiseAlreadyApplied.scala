package com.gu

import scala.util.Try

/**
  * Condition to skip mutating Zuora if the price rise has already been applied.
  * Makes script idempotent, that is, the script can be safely re-run on the same input.
  */
object PriceRiseAlreadyApplied {
  def apply(
      subscription: Subscription,
      account: Account,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue,
  ): Boolean = {
    Try(NewGuardianWeeklySubscription(subscription, account, newGuardianWeeklyProductCatalogue)).isSuccess
  }
}
