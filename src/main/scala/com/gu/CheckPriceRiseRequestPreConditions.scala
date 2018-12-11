package com.gu

trait PriceRiseRequestPreCondition
case object OldRatePlanShouldBeRemovedOnTheSameDateAsTheNewRatePlanIsAdded extends PriceRiseRequestPreCondition
case object PriceIsWithinReasonableBounds extends PriceRiseRequestPreCondition
case object SingleRatePlanIsAdded extends PriceRiseRequestPreCondition
case object HolidayAndRetentionDiscountRatePlansShouldNotBeRemoved extends PriceRiseRequestPreCondition

/**
  * Checks pre-conditions on the Zuora API request body before serialization to JSON.
  */
object CheckPriceRiseRequestPreConditions {
  type UnsatisfiedPriceRiseRequestConditions = List[PriceRiseRequestPreCondition]

  def apply(
      request: PriceRiseRequest,
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription
  ): UnsatisfiedPriceRiseRequestConditions = {

    val (_, unsatisfied) = List[(PriceRiseRequestPreCondition, Boolean)](
      OldRatePlanShouldBeRemovedOnTheSameDateAsTheNewRatePlanIsAdded -> (request.remove.head.contractEffectiveDate == request.add.head.contractEffectiveDate),
      PriceIsWithinReasonableBounds -> (request.add.head.chargeOverrides.get.head.price > currentGuardianWeeklySubscription.price),
      SingleRatePlanIsAdded -> (request.add.size == 1),
      HolidayAndRetentionDiscountRatePlansShouldNotBeRemoved -> true, // TODO: Implement HolidayAndRetentionDiscountRatePlansShouldNotBeRemoved - request.remove.map(_.) Config.Zuora.doNotRemoveProductRatePlanIds.contains
    ).partition(_._2)

    unsatisfied.map(_._1)
  }
}
