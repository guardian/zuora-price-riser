package com.gu

trait PriceRiseRequestPreCondition
case object OldRatePlanShouldBeRemovedOnTheSameDateAsTheNewRatePlanIsAdded extends PriceRiseRequestPreCondition
case object PriceIsWithinReasonableBounds extends PriceRiseRequestPreCondition
case object SingleRatePlanIsRemoved extends PriceRiseRequestPreCondition
case object SingleRatePlanIsAdded extends PriceRiseRequestPreCondition
case object HolidayIsNotRemoved extends PriceRiseRequestPreCondition

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
      SingleRatePlanIsRemoved -> (request.remove.size == 1),
      SingleRatePlanIsAdded -> (request.add.size == 1),
      HolidayIsNotRemoved -> true // TODO
    ).partition(_._2)

    unsatisfied.map(_._1)
  }
}
