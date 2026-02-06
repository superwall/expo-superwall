package expo.modules.superwallexpo.json


import com.superwall.sdk.paywall.presentation.PaywallInfo

fun PaywallInfo.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()

  map["identifier"] = this.identifier
  map["name"] = this.name 
  map["url"] = this.url.toString()
  this.experiment?.let {
    map["experiment"] = Experiment.toJson(it)
  }

  val products = this.products.map { product ->
    mapOf(
      "type" to product.type.toString(),
      "id" to product.fullProductId
    )
  }
  map["products"] = products

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
      "type" to request.type.toString()
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
    mapOf(
      "id" to notification.id,
      "title" to notification.title,
      "body" to notification.body,
      "type" to notification.type.toJson(),
      "delay" to notification.delay.toDouble()
    )
  }
  map["localNotifications"] = localNotifications

  map["state"] = this.state

  return map
}
