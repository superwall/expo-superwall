package com.superwall.sdk.paywall.view.webview.templating.models

import com.superwall.sdk.storage.core_data.toNullableTypedMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class DeviceTemplate(
    val publicApiKey: String,
    val platform: String,
    val appUserId: String,
    val aliases: List<String>,
    val vendorId: String,
    val deviceId: String,
    val appVersion: String,
    val appVersionPadded: String,
    val osVersion: String,
    val deviceModel: String,
    val deviceLocale: String,
    val preferredLocale: String,
    val deviceLanguageCode: String,
    val preferredLanguageCode: String,
    val regionCode: String,
    val preferredRegionCode: String,
    val deviceCurrencyCode: String,
    val deviceCurrencySymbol: String,
    val timezoneOffset: Int,
    val radioType: String,
    val interfaceStyle: String,
    val isLowPowerModeEnabled: Boolean,
    val bundleId: String,
    val appInstallDate: String,
    val isMac: Boolean,
    val daysSinceInstall: Int,
    val minutesSinceInstall: Int,
    val daysSinceLastPaywallView: Int?,
    val minutesSinceLastPaywallView: Int?,
    val totalPaywallViews: Int,
    val utcDate: String,
    val localDate: String,
    val utcTime: String,
    val localTime: String,
    val utcDateTime: String,
    val localDateTime: String,
    val isSandbox: String,
    val activeEntitlements: List<String>,
    val activeEntitlementsObject: List<Map<String, String>>,
    val subscriptionStatus: String?,
    val activeProducts: List<String>,
    val isFirstAppOpen: Boolean,
    val sdkVersion: String,
    val sdkVersionPadded: String,
    val appBuildString: String,
    val appBuildStringNumber: Int?,
    val interfaceStyleMode: String,
    @SerialName("capabilities")
    val capabilities: List<String>,
    @SerialName("capabilities_config")
    val capabilitiesConfig: JsonElement,
    @SerialName("platform_wrapper")
    val platformWrapper: String,
    @SerialName("platform_wrapper_version")
    val platformWrapperVersion: String,
    val deviceTier: String,
    val reviewRequestCount: Int,
    val kotlinVersion: String,
) {
    fun toDictionary(json: Json): Map<String, Any> {
        val jsonString = json.encodeToString(serializer(), this)
        return json.toNullableTypedMap(jsonString)
    }
}
