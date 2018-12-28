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
    newGuardianWeeklyProduct: NewGuardianWeeklyProduct,
    currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription
  ): Float = {
    newGuardianWeeklyProduct
      .pricing
      .find(_.currency == currentGuardianWeeklySubscription.currency)
      .map(_.price)
      .getOrElse(throw new RuntimeException(s"Guardian Weekly product should have a default price: $newGuardianWeeklyProduct, $currentGuardianWeeklySubscription"))
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
case class NewGuardianWeeklyProduct(
  productRatePlanName: String,
  billingPeriod: String,
  productRatePlanId: String,
  productRatePlanChargeId: String,
  pricing: List[Price],
  taxCode: String
) {
  require(taxCode == s"Guardian Weekly", s"Product must be Guardian Weekly: ${pprint.apply(this)}")
  require(List("Quarter", "Annual").contains(billingPeriod), s"Guardian Weekly must be Quarterly or Annual: ${pprint.apply(this)}")
  require(Config.Zuora.New.guardianWeeklyProductRatePlanIds.contains(productRatePlanId), s"New Guardian Weekly product must have new product rate plan ID: ${pprint.apply(this)}")
}

/**
  * Find new GuardianWeeklyProduct on the basis of current billingPeriod, delivery country and currency.
  */
object NewGuardianWeeklyProduct {
  def apply(
      currentGuardianWeeklySubscription: CurrentGuardianWeeklySubscription,
      newGuardianWeeklyProductCatalogue: NewGuardianWeeklyProductCatalogue
  ): NewGuardianWeeklyProduct = {

    (Country.toFutureGuardianWeeklyProductId(currentGuardianWeeklySubscription.country, currentGuardianWeeklySubscription.currency) match {
      case Config.Zuora.New.guardianWeeklyDomesticProductId => newGuardianWeeklyProductCatalogue.domestic
      case Config.Zuora.New.guardianWeeklyRowProductId => newGuardianWeeklyProductCatalogue.restOfTheWorld
    })
      .find(_.billingPeriod == currentGuardianWeeklySubscription.billingPeriod)
      .find(_.pricing.map(_.currency).contains(currentGuardianWeeklySubscription.currency)) // make sure currency exists (could be disabled in Zuora)
      .getOrElse(throw new RuntimeException(s"${currentGuardianWeeklySubscription.subscriptionNumber} failed to determine NewGuardianWeeklyProduct"))

  }
}

/**
  * This model represents the Guardian Weekly products to which subscriptions will be migrated to with a raised price.
  *
  * @param domestic product with name 'Guardian Weekly - Domestic'
  * @param restOfTheWorld product with name 'Guardian Weekly - ROW'
  */
case class NewGuardianWeeklyProductCatalogue(
  domestic: List[NewGuardianWeeklyProduct],
  restOfTheWorld: List[NewGuardianWeeklyProduct]
) {
  require(domestic.forall(_.productRatePlanName.contains("Domestic")))
  require(restOfTheWorld.forall(_.productRatePlanName.contains("ROW")))
  require(domestic.nonEmpty && restOfTheWorld.nonEmpty)
  require((domestic ++ restOfTheWorld).map(_.billingPeriod).forall(List("Quarter", "Annual").contains))

  def getAllProductRatePlanIds: List[String] =
    domestic.map(_.productRatePlanId) ++ restOfTheWorld.map(_.productRatePlanId)

  def getAllProductRatePlanChargeIds: List[String] =
    domestic.map(_.productRatePlanChargeId) ++ restOfTheWorld.map(_.productRatePlanChargeId)
}

/**
  * Maps a Zuora model to a custom made model representing new after-price-rise Guardian Weekly product.
  *
  * Given a list of all Zuora Product Rate Plans associated with a Product returns list of
  * GuardianWeeklyProduct which is a custom model having productRatePlanId and productRatePlanChargeId
  * at the top level.
  */
object NewGuardianWeeklyProducts {
  def apply(productRatePlans: List[ProductRatePlan]): List[NewGuardianWeeklyProduct] = {
    require(productRatePlans.nonEmpty)
    require(productRatePlans.forall(_.productRatePlanCharges.size == 1), "Guardian Weekly should have one-to-one mapping between productRatePlanId and productRatePlanChargeId")
    productRatePlans
      .filter(productRatePlan => List("Quarter", "Annual").contains(productRatePlan.productRatePlanCharges.head.billingPeriod))
      .filter(productRatePlan => Config.Zuora.New.guardianWeeklyProductRatePlanIds.contains(productRatePlan.id))
      .map(productRatePlan => NewGuardianWeeklyProduct(
        productRatePlan.name,
        productRatePlan.productRatePlanCharges.head.billingPeriod,
        productRatePlan.id,
        productRatePlan.productRatePlanCharges.head.id,
        productRatePlan.productRatePlanCharges.head.pricing,
        productRatePlan.productRatePlanCharges.head.taxCode
      ))
  }
}

