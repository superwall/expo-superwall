package expo.modules.superwallexpo.json

import com.superwall.sdk.models.internal.RedemptionResult
import com.superwall.sdk.models.internal.RedemptionInfo
import com.superwall.sdk.models.internal.PurchaserInfo
import com.superwall.sdk.models.internal.StoreIdentifiers
import com.superwall.sdk.models.internal.ErrorInfo
import com.superwall.sdk.models.internal.ExpiredInfo
import com.superwall.sdk.models.internal.RedemptionOwnership
import com.superwall.sdk.models.entitlements.Entitlement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

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

    // Only set for a successful redemption of a Stripe purchase, as on iOS.
    if (this is RedemptionResult.Success &&
        this.redemptionInfo.purchaserInfo?.storeIdentifiers is StoreIdentifiers.Stripe
    ) {
        map["stripeSubscriptionIds"] = this.stripeSubscriptionId.filterNotNull()
    }

    return map
}

private fun RedemptionInfo.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["ownership"] = this.ownership.toJson()
    map["purchaserInfo"] = this.purchaserInfo.toJson()
    this.paywallInfo?.let { map["paywallInfo"] = it.toJson() }

    map["entitlements"] = this.entitlements.map { it.toJson() }

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
            // As on iOS, the unknown store's details are sent alongside `store`, which they
            // replace when they name the store.
            this.properties.forEach { (key, value) ->
                value?.toNative()?.let { map[key] = it }
            }
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

    // Plain values, as iOS sends them, not JSON text.
    map["placementParams"] = this.placementParams.mapValues { (_, value) -> value.toNative() }

    this.variantId?.let { map["variantId"] = it }
    this.experimentId?.let { map["experimentId"] = it }
    this.productIdentifier?.let { map["productIdentifier"] = it }
    this.product?.let { map["product"] = it.toJson() }
    return map
}

private fun RedemptionResult.PaywallInfo.PaywallProduct.toJson(): Map<String, Any> =
    mapOf(
        "identifier" to identifier,
        "languageCode" to languageCode,
        "locale" to locale,
        "currencyCode" to currencyCode,
        "currencySymbol" to currencySymbol,
        "period" to period,
        "periodly" to periodly,
        "localizedPeriod" to localizedPeriod,
        "periodAlt" to periodAlt,
        "periodDays" to periodDays,
        "periodWeeks" to periodWeeks,
        "periodMonths" to periodMonths,
        "periodYears" to periodYears,
        "rawPrice" to rawPrice,
        "price" to price,
        "dailyPrice" to dailyPrice,
        "weeklyPrice" to weeklyPrice,
        "monthlyPrice" to monthlyPrice,
        "yearlyPrice" to yearlyPrice,
        "rawTrialPeriodPrice" to rawTrialPeriodPrice,
        "trialPeriodPrice" to trialPeriodPrice,
        "trialPeriodDailyPrice" to trialPeriodDailyPrice,
        "trialPeriodWeeklyPrice" to trialPeriodWeeklyPrice,
        "trialPeriodMonthlyPrice" to trialPeriodMonthlyPrice,
        "trialPeriodYearlyPrice" to trialPeriodYearlyPrice,
        "trialPeriodDays" to trialPeriodDays,
        "trialPeriodWeeks" to trialPeriodWeeks,
        "trialPeriodMonths" to trialPeriodMonths,
        "trialPeriodYears" to trialPeriodYears,
        "trialPeriodText" to trialPeriodText,
        "trialPeriodEndDate" to trialPeriodEndDate,
    )

private fun JsonElement.toNative(): Any? =
    when (this) {
        is JsonNull -> null
        is JsonPrimitive ->
            if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
        is JsonArray -> map { it.toNative() }
        is JsonObject -> mapValues { (_, value) -> value.toNative() }
    }
