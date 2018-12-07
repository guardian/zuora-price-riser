package com.gu

trait PriceRiseRequestCondition
case object OldRatePlanShouldBeRemovedOnTheSameDateAsTheNewRatePlanIsAdded extends PriceRiseRequestCondition
case object PriceIsWithinReasonableBounds extends PriceRiseRequestCondition
case object SingleRatePlanIsRemoved extends PriceRiseRequestCondition
case object SingleRatePlanIsAdded extends PriceRiseRequestCondition
case object HolidayIsNotRemoved extends PriceRiseRequestCondition

object PriceRiseRequestValidation {
  type UnsatisfiedPriceRiseRequestConditions = List[PriceRiseRequestCondition]

  def apply(
      request: PriceRiseRequest,
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription
  ): UnsatisfiedPriceRiseRequestConditions = {

    val (_, unsatisfied) = List[(PriceRiseRequestCondition, Boolean)](
      OldRatePlanShouldBeRemovedOnTheSameDateAsTheNewRatePlanIsAdded -> (request.remove.head.contractEffectiveDate == request.add.head.contractEffectiveDate),
      PriceIsWithinReasonableBounds -> (request.add.head.chargeOverrides.get.head.price > currentGuardianWeeklySubscription.price),
      SingleRatePlanIsRemoved -> (request.remove.size == 1),
      SingleRatePlanIsAdded -> (request.add.size == 1),
      HolidayIsNotRemoved -> true // TODO
    ).partition(_._2)

    unsatisfied.map(_._1)
  }
}
