package expo.modules.superwallexpo.json


import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.paywall.presentation.PaywallInfo

fun PaywallInfo.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()

  map["identifier"] = this.identifier
  map["name"] = this.name 
  map["url"] = this.url.toString()
  this.presentationId?.let { map["presentationId"] = it }
  this.experiment?.let {
    map["experiment"] = Experiment.toJson(it)
  }

  map["products"] = this.products.map { it.toJson() }

  map["productIds"] = this.productIds.toList()

  this.presentedByEventWithName?.let { map["presentedByEventWithName"] = it }
  this.presentedByEventWithId?.let { map["presentedByEventWithId"] = it }
  this.presentedByEventAt?.let { map["presentedByEventAt"] = it }
  map["presentedBy"] = this.presentedBy
  this.presentationSourceType?.let { map["presentationSourceType"] = it }
  this.responseLoadStartTime?.let { map["responseLoadStartTime"] = it }
  this.responseLoadCompleteTime?.let { map["responseLoadCompleteTime"] = it }
  this.responseLoadFailTime?.let { map["responseLoadFailTime"] = it }
  this.responseLoadDuration?.let { map["responseLoadDuration"] = it.toDouble() }

  map["isFreeTrialAvailable"] = this.isFreeTrialAvailable
  map["featureGatingBehavior"] = this.featureGatingBehavior.rawValue()
  map["closeReason"] = this.closeReason.rawValue()

  this.webViewLoadStartTime?.let { map["webViewLoadStartTime"] = it }
  this.webViewLoadCompleteTime?.let { map["webViewLoadCompleteTime"] = it }
  this.webViewLoadFailTime?.let { map["webViewLoadFailTime"] = it }
  this.webViewLoadDuration?.let { map["webViewLoadDuration"] = it.toDouble() }

  this.productsLoadStartTime?.let { map["productsLoadStartTime"] = it }
  this.productsLoadCompleteTime?.let { map["productsLoadCompleteTime"] = it }
  this.productsLoadFailTime?.let { map["productsLoadFailTime"] = it }
  this.productsLoadDuration?.let { map["productsLoadDuration"] = it.toDouble() }

  this.paywalljsVersion?.let { map["paywalljsVersion"] = it }

  val computedPropertyRequests = this.computedPropertyRequests.map { request ->
    mapOf(
      "placementName" to request.eventName,
      // Same camelCase names as iOS, e.g. `minutesSince`.
      "type" to request.type.prefix.removeSuffix("_")
    )
  }
  map["computedPropertyRequests"] = computedPropertyRequests

  val surveys = this.surveys.map { survey ->
    val options = survey.options.map { option ->
      mapOf(
        "id" to option.id,
        "title" to option.title
      )
    }
    
    mapOf(
      "id" to survey.id,
      "message" to survey.message,
      "title" to survey.title,
      "assignmentKey" to survey.assignmentKey,
      "includeCloseOption" to survey.includeCloseOption,
      "includeOtherOption" to survey.includeOtherOption,
      "presentationProbability" to survey.presentationProbability,
      "presentationCondition" to survey.presentationCondition.rawValue,
      "options" to options
    )
  }
  map["surveys"] = surveys

  val localNotifications = this.localNotifications.map { notification ->
    val notificationMap = mutableMapOf<String, Any>(
      "id" to notification.id,
      "title" to notification.title,
      "body" to notification.body,
      "type" to notification.type.toJson(),
      "delay" to notification.delay.toDouble()
    )
    notification.subtitle?.let { notificationMap["subtitle"] = it }
    notificationMap
  }
  map["localNotifications"] = localNotifications

  map["state"] = this.state

  map["customerInfo"] = this.customerInfo.toJson()

  return map
}

// Same shape as iOS's `Product.toJson()`. `type` is the old Android-only key, kept for anyone
// reading it.
fun ProductItem.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>(
    "id" to fullProductId,
    "name" to name,
    "entitlements" to entitlements.toJson(),
    "type" to type.toString(),
  )
  when (val storeType = type) {
    is ProductItem.StoreProductType.PlayStore -> map["store"] = "PLAY_STORE"
    is ProductItem.StoreProductType.AppStore -> {
      map["store"] = "APP_STORE"
      map["appStoreProduct"] = mapOf("id" to storeType.product.productIdentifier)
    }
    is ProductItem.StoreProductType.Stripe -> {
      map["store"] = "STRIPE"
      map["stripeProduct"] = mutableMapOf<String, Any>("id" to storeType.product.productIdentifier).apply {
        storeType.product.trialDays?.let { put("trialDays", it) }
      }
    }
    is ProductItem.StoreProductType.Paddle -> {
      map["store"] = "PADDLE"
      map["paddleProduct"] = mapOf("id" to storeType.product.productIdentifier)
    }
    is ProductItem.StoreProductType.Custom -> {
      map["store"] = "CUSTOM"
      map["customProduct"] = mapOf("id" to storeType.product.productIdentifier)
    }
    is ProductItem.StoreProductType.Other -> map["store"] = storeType.product.store.name
  }
  return map
}
