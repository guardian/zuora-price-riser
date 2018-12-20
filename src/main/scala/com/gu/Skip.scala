package com.gu

sealed trait SkipReason
case object OneOff extends SkipReason
case object Cancelled extends SkipReason
case object PriceRiseApplied extends SkipReason

/**
  * Check if the price rise should be skipped.
  */
object Skip {
  def apply(
      subscriptionBefore: Subscription,
      accountBefore: Account,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue,
  ): List[SkipReason] = {
    val (satisfied, _) = List[(SkipReason, Boolean)](
      OneOff -> (!subscriptionBefore.autoRenew),
      Cancelled -> (subscriptionBefore.status == "Cancelled"),
      PriceRiseApplied -> PriceRiseAlreadyApplied(subscriptionBefore, accountBefore, newGuardianWeeklyProductCatalogue),
    ).partition(_._2)

    satisfied.map(_._1)
  }
}
