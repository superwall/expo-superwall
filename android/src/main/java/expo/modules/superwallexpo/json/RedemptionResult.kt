package expo.modules.superwallexpo.json

import com.superwall.sdk.models.internal.RedemptionResult
import com.superwall.sdk.models.internal.RedemptionInfo
import com.superwall.sdk.models.internal.PurchaserInfo
import com.superwall.sdk.models.internal.StoreIdentifiers
import com.superwall.sdk.models.internal.ErrorInfo
import com.superwall.sdk.models.internal.ExpiredInfo
import com.superwall.sdk.models.internal.RedemptionOwnership
import com.superwall.sdk.models.entitlements.Entitlement

fun RedemptionResult.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["code"] = this.code

    when (this) {
        is RedemptionResult.Success -> {
            map["status"] = "SUCCESS"
            map["redemptionInfo"] = this.redemptionInfo.toJson()
        }
        is RedemptionResult.Error -> {
            map["status"] = "ERROR"
            map["error"] = this.error.toJson()
        }
        is RedemptionResult.Expired -> {
            map["status"] = "CODE_EXPIRED"
            map["expired"] = this.expired.toJson()
        }
        is RedemptionResult.InvalidCode -> {
            map["status"] = "INVALID_CODE"
        }
        is RedemptionResult.ExpiredSubscription -> {
            map["status"] = "EXPIRED_SUBSCRIPTION"
            map["redemptionInfo"] = this.redemptionInfo.toJson()
        }
    }

    return map
}

private fun RedemptionInfo.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["ownership"] = this.ownership.toJson()
    map["purchaserInfo"] = this.purchaserInfo.toJson()
    this.paywallInfo?.let { map["paywallInfo"] = it.toJson() }

    val entitlementsArray = mutableListOf<Map<String, Any>>()
    this.entitlements.forEach { entitlement ->
      val entitlementMap = mutableMapOf<String, Any>()
      entitlementMap["id"] = entitlement.id
      entitlementsArray.add(entitlementMap)
    }
    map["entitlements"] = entitlementsArray

    return map
}

private fun PurchaserInfo.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["appUserId"] = this.appUserId
    this.email?.let { map["email"] = it }
    map["storeIdentifiers"] = this.storeIdentifiers.toJson()
    return map
}

private fun StoreIdentifiers.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    when (this) {
        is StoreIdentifiers.Stripe -> {
            map["store"] = "STRIPE"
            map["stripeCustomerId"] = this.stripeCustomerId
            val subscriptionIdsArray = mutableListOf<String>()
            this.subscriptionIds.forEach { id ->
                subscriptionIdsArray.add(id)
            }
            map["stripeSubscriptionIds"] = subscriptionIdsArray
        }
        is StoreIdentifiers.Paddle -> {
            map["store"] = "PADDLE"
            map["paddleCustomerId"] = this.paddleCustomerId
            val subscriptionIdsArray = mutableListOf<String>()
            this.paddleSubscriptionIds.forEach { id ->
                subscriptionIdsArray.add(id)
            }
            map["paddleSubscriptionIds"] = subscriptionIdsArray
        }
        is StoreIdentifiers.Unknown -> {
            map["store"] = "UNKNOWN"
        }
    }
    return map
}

private fun ErrorInfo.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["message"] = this.message
    return map
}

private fun ExpiredInfo.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["resent"] = this.resent
    this.obfuscatedEmail?.let { map["obfuscatedEmail"] = it }
    return map
}

private fun RedemptionOwnership.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    when (this) {
        is RedemptionOwnership.Device -> {
            map["type"] = "DEVICE"
            map["deviceId"] = this.deviceId
        }
        is RedemptionOwnership.AppUser -> {
            map["type"] = "APP_USER"
            map["appUserId"] = this.appUserId
        }
    }
    return map
}

private fun RedemptionResult.PaywallInfo.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["identifier"] = this.identifier
    map["placementName"] = this.placementName

    val placementParamsMap = mutableMapOf<String, Any>()
    this.placementParams.forEach { (key, value) ->
        placementParamsMap[key] = value.toString()
    }
    map["placementParams"] = placementParamsMap

    map["variantId"] = this.variantId
    map["experimentId"] = this.experimentId
    return map
}
