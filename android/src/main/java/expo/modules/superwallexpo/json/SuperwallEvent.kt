package expo.modules.superwallexpo.json


import com.superwall.sdk.analytics.superwall.AttributionMatchInfo
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.store.abstractions.transactions.StoreTransaction

class SuperwallEvent {
  companion object {
    fun toJson(superwallPlacement: SuperwallEvent): Map<String, Any> {
      val map = mutableMapOf<String, Any>()
      when (superwallPlacement) {
        is SuperwallEvent.FirstSeen -> map["event"] = "firstSeen"
        is SuperwallEvent.Reset -> map["event"] = "reset"
        is SuperwallEvent.Restore.Start -> map["event"] = "restoreStart"
        is SuperwallEvent.Restore.Complete -> map["event"] = "restoreComplete"
        is SuperwallEvent.ConfigRefresh -> map["event"] = "configRefresh"
        is SuperwallEvent.Restore.Fail -> {
          map["event"] = "restoreFail"
          map["message"] = superwallPlacement.reason
        }
        is SuperwallEvent.AppOpen -> map["event"] = "appOpen"
        is SuperwallEvent.AppLaunch -> map["event"] = "appLaunch"
        is SuperwallEvent.IdentityAlias -> map["event"] = "identityAlias"
        is SuperwallEvent.AppInstall -> map["event"] = "appInstall"
        is SuperwallEvent.SessionStart -> map["event"] = "sessionStart"
        is SuperwallEvent.DeviceAttributes -> {
          map["event"] = "deviceAttributes"
          map["attributes"] = superwallPlacement.attributes
        }
        is SuperwallEvent.SubscriptionStatusDidChange -> map["event"] = "subscriptionStatusDidChange"
        is SuperwallEvent.AppClose -> map["event"] = "appClose"
        is SuperwallEvent.DeepLink -> {
          map["event"] = "deepLink"
          map["url"] = superwallPlacement.uri.toString()
        }
        is SuperwallEvent.TriggerFire -> {
          map["event"] = "triggerFire"
          map["placementName"] = superwallPlacement.placementName
          map["result"] = TriggerResult.toJson(superwallPlacement.result)
        }
        is SuperwallEvent.PaywallOpen -> {
          map["event"] = "paywallOpen"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallClose -> {
          map["event"] = "paywallClose"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallPageView -> {
          map["event"] = "paywallPageView"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
          map["data"] = superwallPlacement.data.toJson()
        }
        is SuperwallEvent.PaywallDecline -> {
          map["event"] = "paywallDecline"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.TransactionStart -> {
          map["event"] = "transactionStart"
          map["product"] = superwallPlacement.product.toJson()
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.TransactionFail -> {
          map["event"] = "transactionFail"
          map["error"] = superwallPlacement.error.localizedMessage ?: "Error message unavailable"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.TransactionAbandon -> {
          map["event"] = "transactionAbandon"
          map["product"] = superwallPlacement.product.toJson()
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.TransactionComplete -> {
          map["event"] = "transactionComplete"
          map["product"] = superwallPlacement.product.toJson()
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()

          val transaction = superwallPlacement.transaction as? StoreTransaction
          transaction?.toJson()?.let {
            map["transaction"] = it
          }
        }
        is SuperwallEvent.SubscriptionStart -> {
          map["event"] = "subscriptionStart"
          map["product"] = superwallPlacement.product.toJson()
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.FreeTrialStart -> {
          map["event"] = "freeTrialStart"
          map["product"] = superwallPlacement.product.toJson()
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.TransactionRestore -> {
          map["event"] = "transactionRestore"
          map["restoreType"] = superwallPlacement.restoreType.toJson()
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.TransactionTimeout -> {
          map["event"] = "transactionTimeout"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.UserAttributes -> {
          map["event"] = "userAttributes"
          map["attributes"] = superwallPlacement.attributes
        }
        is SuperwallEvent.AttributionMatch -> {
          map["event"] = "attributionMatch"
          map["info"] = superwallPlacement.info.toJson()
        }
        is SuperwallEvent.IntegrationProps -> {
          map["event"] = "integrationAttributes"
          map["attributes"] = superwallPlacement.audienceFilterParams
        }
        is SuperwallEvent.IntegrationAttributes -> {
          map["event"] = "integrationAttributes"
          map["attributes"] = superwallPlacement.audienceFilterParams
        }
        is SuperwallEvent.NonRecurringProductPurchase -> {
          map["event"] = "nonRecurringProductPurchase"
          map["product"] = superwallPlacement.product.toJson()
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallResponseLoadStart -> {
          map["event"] = "paywallResponseLoadStart"
          putTriggeredPlacementName(map, superwallPlacement.triggeredPlacementName)
        }
        is SuperwallEvent.PaywallResponseLoadNotFound -> {
          map["event"] = "paywallResponseLoadNotFound"
          putTriggeredPlacementName(map, superwallPlacement.triggeredPlacementName)
        }
        is SuperwallEvent.PaywallResponseLoadFail -> {
          map["event"] = "paywallResponseLoadFail"
          putTriggeredPlacementName(map, superwallPlacement.triggeredPlacementName)
        }
        is SuperwallEvent.PaywallResponseLoadComplete -> {
          map["event"] = "paywallResponseLoadComplete"
          putTriggeredPlacementName(map, superwallPlacement.triggeredPlacementName)
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallWebviewLoadStart -> {
          map["event"] = "paywallWebviewLoadStart"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallWebviewLoadFail -> {
          map["event"] = "paywallWebviewLoadFail"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
          superwallPlacement.errorMessage?.let {
            map["errorMessage"] = it.toString()
          }
        }
        is SuperwallEvent.PaywallWebviewLoadComplete -> {
          map["event"] = "paywallWebviewLoadComplete"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallWebviewLoadTimeout -> {
          map["event"] = "paywallWebviewLoadTimeout"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallProductsLoadStart -> {
          map["event"] = "paywallProductsLoadStart"
          putTriggeredPlacementName(map, superwallPlacement.triggeredPlacementName)
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallProductsLoadFail -> {
          map["event"] = "paywallProductsLoadFail"
          putTriggeredPlacementName(map, superwallPlacement.triggeredPlacementName)
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
          superwallPlacement.errorMessage?.let {
            map["errorMessage"] = it
          }
        }
        is SuperwallEvent.PaywallProductsLoadComplete -> {
          map["event"] = "paywallProductsLoadComplete"
          putTriggeredPlacementName(map, superwallPlacement.triggeredPlacementName)
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.SurveyResponse -> {
          map["event"] = "surveyResponse"
          map["survey"] = superwallPlacement.survey.toJson()
          map["selectedOption"] = superwallPlacement.selectedOption.toJson()
          map["customResponse"] = superwallPlacement.customResponse ?: ""
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallPresentationRequest -> {
          map["event"] = "paywallPresentationRequest"
          map["status"] = superwallPlacement.status.toJson()
          superwallPlacement.reason?.toJson()?.let {
            map["reason"] = it
          }
        }
        is SuperwallEvent.SurveyClose -> {
          map["event"] = "surveyClose"
        }
        is SuperwallEvent.ConfigAttributes -> {
          map["event"] = "configAttributes"
        }
        is SuperwallEvent.CustomPlacement -> {
          map["event"] = "customPlacement"
          map["name"] = superwallPlacement.placementName
          map["params"] = superwallPlacement.params
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.PaywallWebviewLoadFallback -> {
          map["event"] = "paywallWebviewLoadFallback"
          map["paywallInfo"] = superwallPlacement.paywallInfo.toJson()
        }
        is SuperwallEvent.ConfigFail -> {
          map["event"] = "configFail"
        }
        is SuperwallEvent.ConfirmAllAssignments -> {
          map["event"] = "confirmAllAssignments"
        }
        is SuperwallEvent.PaywallResourceLoadFail -> {
          map["event"] = "paywallResourceLoadFail"
          map["url"] = superwallPlacement.url.toString()
          map["error"] = superwallPlacement.error
        }
        is SuperwallEvent.ShimmerViewComplete -> {
          map["event"] = "shimmerViewComplete"
          map["duration"] = superwallPlacement.duration
        }
        is SuperwallEvent.ShimmerViewStart -> {
          map["event"] = "shimmerViewStart"
        }
        is SuperwallEvent.RedemptionComplete -> {
          map["event"] = "redemptionComplete"
        }
        is SuperwallEvent.RedemptionFail -> {
          map["event"] = "redemptionFail"
        }
        is SuperwallEvent.RedemptionStart -> {
          map["event"] = "redemptionStart"
        }
        is SuperwallEvent.EnrichmentStart -> {
          map["event"] = "enrichmentStart"
        }
        is SuperwallEvent.EnrichmentFail -> {
          map["event"] = "enrichmentFail"
        }
        is SuperwallEvent.EnrichmentComplete -> {
          map["event"] = "enrichmentComplete"
          map["userEnrichment"] = superwallPlacement.userEnrichment
          map["deviceEnrichment"] = superwallPlacement.deviceEnrichment
        }
        is SuperwallEvent.ReviewRequested -> {
          map["event"] = "reviewRequested"
          map["count"] = superwallPlacement.count
        }
        is SuperwallEvent.ReviewGranted -> {
          map["event"] = "reviewGranted"
          map["count"] = superwallPlacement.count
        }
        is SuperwallEvent.ReviewDenied -> {
          map["event"] = "reviewDenied"
          map["count"] = superwallPlacement.count
        }
        is SuperwallEvent.CustomerInfoDidChange -> {
          map["event"] = "customerInfoDidChange"
        }
        is SuperwallEvent.PermissionRequested -> {
          map["event"] = "permissionRequested"
          map["permissionName"] = superwallPlacement.permissionName
          map["paywallIdentifier"] = superwallPlacement.paywallIdentifier
        }
        is SuperwallEvent.PermissionGranted -> {
          map["event"] = "permissionGranted"
          map["permissionName"] = superwallPlacement.permissionName
          map["paywallIdentifier"] = superwallPlacement.paywallIdentifier
        }
        is SuperwallEvent.PermissionDenied -> {
          map["event"] = "permissionDenied"
          map["permissionName"] = superwallPlacement.permissionName
          map["paywallIdentifier"] = superwallPlacement.paywallIdentifier
        }
        is SuperwallEvent.PaywallPreloadStart -> {
          map["event"] = "paywallPreloadStart"
          map["paywallCount"] = superwallPlacement.paywallCount
        }
        is SuperwallEvent.PaywallPreloadComplete -> {
          map["event"] = "paywallPreloadComplete"
          map["paywallCount"] = superwallPlacement.paywallCount
        }
        is SuperwallEvent.TestModeModalOpen -> {
          map["event"] = "testModeModalOpen"
        }
        is SuperwallEvent.TestModeModalClose -> {
          map["event"] = "testModeModalClose"
        }
        is SuperwallEvent.CustomerCenterOpen -> {
          map["event"] = "customerCenterOpen"
          map["screen"] = superwallPlacement.screen.toJson()
        }
        is SuperwallEvent.CustomerCenterClose -> {
          map["event"] = "customerCenterClose"
        }
        is SuperwallEvent.CustomerCenterAction -> {
          map["event"] = "customerCenterAction"
          map["action"] = superwallPlacement.action.toJson()
          map["pathId"] = superwallPlacement.pathId
          superwallPlacement.productId?.let { map["productId"] = it }
        }
        is SuperwallEvent.CustomerCenterSurveyResponse -> {
          map["event"] = "customerCenterSurveyResponse"
          map["surveyId"] = superwallPlacement.surveyId
          map["optionId"] = superwallPlacement.optionId
          map["action"] = superwallPlacement.action.toJson()
          map["pathId"] = superwallPlacement.pathId
          superwallPlacement.productId?.let { map["productId"] = it }
        }
        is SuperwallEvent.CustomerCenterRefundRequest -> {
          map["event"] = "customerCenterRefundRequest"
          map["productId"] = superwallPlacement.productId
          map["status"] = superwallPlacement.status.toJson()
        }
        // Same as iOS, so JS always gets an `event` name for cases added by a native bump.
        else -> map["event"] = "unknown"
      }
      return map
    }

    // iOS names this key `triggeredEventName`. `triggeredPlacementName` is kept for
    // anyone who read the old Android-only key.
    private fun putTriggeredPlacementName(map: MutableMap<String, Any>, name: String?) {
      map["triggeredEventName"] = name ?: ""
      map["triggeredPlacementName"] = name ?: ""
    }
  }
}

fun AttributionMatchInfo.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>(
    "provider" to provider.rawName,
    "matched" to matched,
  )
  source?.let { map["source"] = it }
  confidence?.let { map["confidence"] = it.rawName }
  matchScore?.let { map["matchScore"] = it }
  reason?.let { map["reason"] = it }
  return map
}
