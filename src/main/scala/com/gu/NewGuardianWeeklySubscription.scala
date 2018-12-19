package com.gu

/**
  * Model representing subscription after the price rise.
  */
case class NewGuardianWeeklySubscription(
  subscriptionNumber: String,
  price: Float,
  currency: String,
  country: String,
  productRatePlanId: String,
  productRatePlanChargeId: String
)

object DefaultCataloguePrice {
  def apply(
      guardianWeeklyProduct: GuardianWeeklyProduct,
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription
  ): Float = {
    guardianWeeklyProduct
      .pricing
      .find(_.currency == currentGuardianWeeklySubscription.currency)
      .map(_.price)
      .getOrElse(throw new RuntimeException(s"Guardian Weekly product should have a default price: $guardianWeeklyProduct, $currentGuardianWeeklySubscription"))
  }
}

/**
  * Flattened representation of Guardian Weekly subscription after the price rise has been applied.
  *
  * Note this does only a quick and simple check on the basis of Product Rate Plan ID. Please see
  * PriceRiseResponseValidation for full validation of successfully applied price rise.
  */
object NewGuardianWeeklySubscription {
  def apply(
      subscriptionAfterPriceRise: Subscription,
      accountAfterPriceRise: Account,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue,
  ): NewGuardianWeeklySubscription = {
    val newRatePlans =
      subscriptionAfterPriceRise.ratePlans.filter { ratePlan =>
        newGuardianWeeklyProductCatalogue.getAllProductRatePlanIds.contains(ratePlan.productRatePlanId)
      }

    assert(newRatePlans.size == 1, "NewGuardianWeeklyRatePlanExists not satisfied")
    assert(newRatePlans.head.ratePlanCharges.size == 1, "NewGuardianWeeklyRatePlanHasOnlyOneCharge not satisfied ")
    assert(newRatePlans.head.ratePlanCharges.head.price.isDefined, "Price should exist")

    val newRatePlan = newRatePlans.head
    val newRatePlanCharge = newRatePlan.ratePlanCharges.head
    val newProductRatePlanId = newRatePlan.productRatePlanId
    val newProductRatePlanChargeId = newRatePlanCharge.productRatePlanChargeId

    NewGuardianWeeklySubscription(
      subscriptionAfterPriceRise.subscriptionNumber,
      newRatePlanCharge.price.get,
      newRatePlanCharge.currency,
      accountAfterPriceRise.soldToContact.country,
      newProductRatePlanId,
      newProductRatePlanChargeId
    )
  }
}
/**
  * Single flattened model representing Guardian Weekly product.
  *
  * Because Guardian Weekly (quarter, annual) has one-to-one mapping between productRatePlanId
  * and productRatePlanChargeId, we can flatten the model.
  */
case class GuardianWeeklyProduct(
  productRatePlanName: String,
  billingPeriod: String,
  productRatePlanId: String,
  productRatePlanChargeId: String,
  pricing: List[Price],
  taxCode: String
) {
  require(taxCode == s"Guardian Weekly", s"Product must be Guardian Weekly: ${pprint.apply(this)}")
  require(List("Quarter", "Annual").contains(billingPeriod), s"Guardian Weekly must be Quarterly or Annual: ${pprint.apply(this)}")
}

/**
  * Find new GuardianWeeklyProduct using billingPeriod and delivery country.
  */
object GuardianWeeklyProduct {
  def apply(
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue
  ): GuardianWeeklyProduct = {

    (Country.toFutureGuardianWeeklyProductId(currentGuardianWeeklySubscription.country) match {
      case Config.Zuora.guardianWeeklyDomesticProductId => newGuardianWeeklyProductCatalogue.domestic
      case Config.Zuora.guardianWeeklyRowProductId => newGuardianWeeklyProductCatalogue.restOfTheWorld
    })
      .find(_.billingPeriod == currentGuardianWeeklySubscription.billingPeriod)
      .getOrElse(throw new RuntimeException(s"${currentGuardianWeeklySubscription.subscriptionNumber} failed to determine new GuardianWeeklyProduct"))

  }
}

/**
  * This model represents the Guardian Weekly products to which subscriptions will be migrated to with a raised price.
  *
  * @param domestic product with name 'Guardian Weekly - Domestic'
  * @param restOfTheWorld product with name 'Guardian Weekly - ROW'
  */
case class NewGuardianWeeklyProductCatalogue(
  domestic: List[GuardianWeeklyProduct],
  restOfTheWorld: List[GuardianWeeklyProduct]
) {
  require(domestic.forall(_.productRatePlanName.contains("Domestic")))
  require(restOfTheWorld.forall(_.productRatePlanName.contains("ROW")))
  require(domestic.nonEmpty && restOfTheWorld.nonEmpty)

  def getAllProductRatePlanIds: List[String] =
    domestic.map(_.productRatePlanId) ++ restOfTheWorld.map(_.productRatePlanId)

  def getAllProductRatePlanChargeIds: List[String] =
    domestic.map(_.productRatePlanChargeId) ++ restOfTheWorld.map(_.productRatePlanChargeId)
}

/**
  * Maps a Zuora model to a custom made model representing Guardian Weekly product.
  *
  * Given a list of all Zuora Product Rate Plans associated with a Product returns list of
  * GuardianWeeklyProduct which is a custom model having productRatePlanId and productRatePlanChargeId
  * at the top level.
  */
object GuardianWeeklyProducts {
  def apply(productRatePlans: List[ProductRatePlan]): List[GuardianWeeklyProduct] = {
    require(productRatePlans.nonEmpty)
    require(productRatePlans.forall(_.productRatePlanCharges.size == 1), "Guardian Weekly should have one-to-one mapping between productRatePlanId and productRatePlanChargeId")
    productRatePlans
      .filter(productRatePlan => List("Quarter", "Annual").contains(productRatePlan.productRatePlanCharges.head.billingPeriod))
      .map(productRatePlan => GuardianWeeklyProduct(
        productRatePlan.name,
        productRatePlan.productRatePlanCharges.head.billingPeriod,
        productRatePlan.id,
        productRatePlan.productRatePlanCharges.head.id,
        productRatePlan.productRatePlanCharges.head.pricing,
        productRatePlan.productRatePlanCharges.head.taxCode
      ))
  }
}

