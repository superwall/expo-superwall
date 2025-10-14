package com.superwall.sdk.paywall.view.webview.templating.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class DeviceTemplateTest {
    val json =
        Json {
            encodeDefaults = true
            explicitNulls = true
        }

    private fun createSampleDeviceTemplate(): DeviceTemplate =
        DeviceTemplate(
            publicApiKey = "test_key",
            platform = "android",
            appUserId = "user123",
            aliases = listOf("alias1", "alias2"),
            vendorId = "vendor123",
            deviceId = "\$SuperwallDevice:${UUID.nameUUIDFromBytes("vendor123".encodeToByteArray())}",
            appVersion = "1.0.0",
            osVersion = "13",
            deviceModel = "Pixel 6",
            deviceLocale = "en_US",
            preferredLocale = "en_US",
            deviceLanguageCode = "en",
            preferredLanguageCode = "en",
            regionCode = "US",
            preferredRegionCode = "US",
            deviceCurrencyCode = "USD",
            deviceCurrencySymbol = "$",
            timezoneOffset = -480,
            radioType = "5G",
            interfaceStyle = "light",
            isLowPowerModeEnabled = false,
            bundleId = "com.example.app",
            appInstallDate = "2024-03-20",
            isMac = false,
            daysSinceInstall = 5,
            minutesSinceInstall = 7200,
            daysSinceLastPaywallView = 1,
            minutesSinceLastPaywallView = 1440,
            totalPaywallViews = 10,
            utcDate = "2024-03-20",
            localDate = "2024-03-20",
            utcTime = "10:00:00",
            localTime = "02:00:00",
            utcDateTime = "2024-03-20T10:00:00",
            localDateTime = "2024-03-20T02:00:00",
            isSandbox = "true",
            activeEntitlements = listOf("active"),
            activeEntitlementsObject = listOf(mapOf("identifier" to "active", "type" to "SERVICE_LEVEL")),
            isFirstAppOpen = false,
            sdkVersion = "1.0.0",
            sdkVersionPadded = "001.000.000",
            appBuildString = "100",
            appBuildStringNumber = 100,
            interfaceStyleMode = "light",
            capabilities = listOf("feature1", "feature2"),
            capabilitiesConfig = JsonObject(mapOf("key" to JsonPrimitive("value"))),
            platformWrapper = "native",
            platformWrapperVersion = "1.0.0",
            appVersionPadded = "001.000.000",
            activeProducts = listOf("product1", "product2"),
            subscriptionStatus = "ACTIVE",
            deviceTier = "LOW",
            reviewRequestCount = 0,
            kotlinVersion = KotlinVersion.CURRENT.toString(),
        )

    @Test
    fun `test DeviceTemplate serialization`() {
        val template = createSampleDeviceTemplate()

        // Test toDictionary()
        val dictionary = template.toDictionary(json)

        // Verify some key fields
        assertEquals("test_key", dictionary["publicApiKey"])
        assertEquals("android", dictionary["platform"])
        assertEquals("user123", dictionary["appUserId"])
        assertEquals(listOf("alias1", "alias2"), dictionary["aliases"])
        assertEquals("Pixel 6", dictionary["deviceModel"])
        assertEquals(-480.0, dictionary["timezoneOffset"])
        assertEquals(false, dictionary["isLowPowerModeEnabled"])
        assertEquals(listOf("feature1", "feature2"), dictionary["capabilities"])
    }

    @Test
    fun `test DeviceTemplate serialization with null values`() {
        val template =
            DeviceTemplate(
                publicApiKey = "test_key",
                platform = "android",
                appUserId = "user123",
                aliases = listOf(),
                vendorId = "vendor123",
                deviceId = "\$SuperwallDevice:${UUID.nameUUIDFromBytes("vendor123".encodeToByteArray())}",
                appVersion = "1.0.0",
                osVersion = "13",
                deviceModel = "Pixel 6",
                deviceLocale = "en_US",
                preferredLocale = "en_US",
                deviceLanguageCode = "en",
                preferredLanguageCode = "en",
                regionCode = "US",
                preferredRegionCode = "US",
                deviceCurrencyCode = "USD",
                deviceCurrencySymbol = "$",
                timezoneOffset = -480,
                radioType = "5G",
                interfaceStyle = "light",
                isLowPowerModeEnabled = false,
                bundleId = "com.example.app",
                appInstallDate = "2024-03-20",
                isMac = false,
                daysSinceInstall = 5,
                minutesSinceInstall = 7200,
                daysSinceLastPaywallView = null,
                minutesSinceLastPaywallView = null,
                totalPaywallViews = 0,
                utcDate = "2024-03-20",
                localDate = "2024-03-20",
                utcTime = "10:00:00",
                localTime = "02:00:00",
                utcDateTime = "2024-03-20T10:00:00",
                localDateTime = "2024-03-20T02:00:00",
                isSandbox = "true",
                activeEntitlements = listOf(),
                activeEntitlementsObject = listOf(),
                isFirstAppOpen = true,
                sdkVersion = "1.0.0",
                sdkVersionPadded = "001.000.000",
                appBuildString = "100",
                appBuildStringNumber = null,
                interfaceStyleMode = "light",
                capabilities = listOf(),
                capabilitiesConfig = JsonObject(emptyMap()),
                platformWrapper = "native",
                platformWrapperVersion = "1.0.0",
                appVersionPadded = "001.000.000",
                activeProducts = listOf(),
                subscriptionStatus = "INACTIVE",
                deviceTier = "HIGH",
                reviewRequestCount = 0,
                kotlinVersion = KotlinVersion.CURRENT.toString(),
            )

        val dictionary1 = template.toDictionary(json)

        assertNull(dictionary1["daysSinceLastPaywallView"])
        assertNull(dictionary1["minutesSinceLastPaywallView"])
        assertNull(dictionary1["appBuildStringNumber"])
        assertNull(dictionary1["ipRegion"])
    }
}
