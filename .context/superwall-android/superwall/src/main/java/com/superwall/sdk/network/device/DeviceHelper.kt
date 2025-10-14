package com.superwall.sdk.network.device

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.superwall.sdk.BuildConfig
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.DefaultClassifierDataFactory
import com.superwall.sdk.analytics.DeviceClassifier
import com.superwall.sdk.dependencies.ExperimentalPropertiesFactory
import com.superwall.sdk.dependencies.IdentityInfoFactory
import com.superwall.sdk.dependencies.IdentityManagerFactory
import com.superwall.sdk.dependencies.LocaleIdentifierFactory
import com.superwall.sdk.dependencies.OptionsFactory
import com.superwall.sdk.dependencies.StoreTransactionFactory
import com.superwall.sdk.identity.setUserAttributes
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.then
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.enrichment.EnrichmentRequest
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.internal.DeviceVendorId
import com.superwall.sdk.models.internal.VendorId
import com.superwall.sdk.network.JsonFactory
import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.network.SuperwallAPI
import com.superwall.sdk.paywall.view.webview.templating.models.DeviceTemplate
import com.superwall.sdk.storage.LastPaywallView
import com.superwall.sdk.storage.LatestEnrichment
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.storage.ReviewCount
import com.superwall.sdk.storage.ReviewData
import com.superwall.sdk.storage.TotalPaywallViews
import com.superwall.sdk.storage.core_data.convertFromJsonElement
import com.superwall.sdk.storage.core_data.convertToJsonElement
import com.superwall.sdk.utilities.DateUtils
import com.superwall.sdk.utilities.dateFormat
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import org.threeten.bp.Instant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration

enum class InterfaceStyle(
    val rawValue: String,
) {
    LIGHT("Light"),
    DARK("Dark"),
}

class DeviceHelper(
    private val context: Context,
    val storage: LocalStorage,
    val network: SuperwallAPI,
    val factory: Factory,
    private val classifier: DeviceClassifier = DeviceClassifier(DefaultClassifierDataFactory { context }),
) {
    interface Factory :
        IdentityInfoFactory,
        LocaleIdentifierFactory,
        JsonFactory,
        StoreTransactionFactory,
        IdentityManagerFactory,
        ExperimentalPropertiesFactory,
        OptionsFactory

    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = true
        }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val appInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    private val appInstallDate = Date(appInfo.firstInstallTime)

    fun daysSince(date: Date): Int {
        val fromDate = Instant.ofEpochMilli(date.time)
        val toDate = Instant.now()
        val duration =
            org.threeten.bp.Duration
                .between(fromDate, toDate)
        return duration.toDays().toInt()
    }

    fun minutesSince(date: Date): Int {
        val fromDate = Instant.ofEpochMilli(date.time)
        val toDate = Instant.now()
        val duration =
            org.threeten.bp.Duration
                .between(fromDate, toDate)
        return duration.toMinutes().toInt()
    }

    fun hoursSince(date: Date): Int {
        val fromDate = Instant.ofEpochMilli(date.time)
        val toDate = Instant.now()
        val duration =
            org.threeten.bp.Duration
                .between(fromDate, toDate)
        return duration.toHours().toInt()
    }

    fun monthsSince(date: Date): Int {
        val fromDate = Instant.ofEpochMilli(date.time)
        val toDate = Instant.now()
        val duration =
            org.threeten.bp.Duration
                .between(fromDate, toDate)
        return duration.toDays().toInt() / 30
    }

    private val daysSinceInstall: Int
        get() {
            val fromDate = Instant.ofEpochMilli(appInstallDate.time)
            val toDate = Instant.now()
            val duration =
                org.threeten.bp.Duration
                    .between(fromDate, toDate)
            return duration.toDays().toInt()
        }

    private val minutesSinceInstall: Int
        get() {
            val fromDate = Instant.ofEpochMilli(appInstallDate.time)
            val toDate = Instant.now()
            val duration =
                org.threeten.bp.Duration
                    .between(fromDate, toDate)
            return duration.toMinutes().toInt()
        }

    private val daysSinceLastPaywallView: Int?
        get() {
            val fromDate =
                storage.read(LastPaywallView)?.let {
                    Instant
                        .ofEpochMilli(it.time)
                }
                    ?: return null
            val toDate = Instant.now()
            val duration =
                org.threeten.bp.Duration
                    .between(fromDate, toDate)
            return duration.toDays().toInt()
        }

    private val minutesSinceLastPaywallView: Int?
        get() {
            val fromDate =
                storage.read(LastPaywallView)?.let {
                    Instant
                        .ofEpochMilli(it.time)
                }
                    ?: return null
            val toDate = Instant.now()
            val duration =
                org.threeten.bp.Duration
                    .between(fromDate, toDate)
            return duration.toMinutes().toInt()
        }

    private val totalPaywallViews: Int
        get() {
            return storage.read(TotalPaywallViews) ?: 0
        }

    private val reviewData: ReviewCount
        get() {
            return storage.read(ReviewData) ?: ReviewCount()
        }

    val reviewRequestCount: Int
        get() = reviewData.timesQueried

    suspend fun reviewRequestsTotal(): Int {
        // Use a very old date as the start date to get all records
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, 2000)
        val startDate = calendar.time
        val endDate = Date()
        return storage.coreDataManager.countEventsByNameInPeriod(
            name = "review_requested",
            startDate = startDate,
            endDate = endDate,
        )
    }

    private val lastEnrichment: MutableStateFlow<Enrichment?> =
        MutableStateFlow(storage.read(LatestEnrichment))

    internal val demandTier: String?
        get() =
            lastEnrichment
                ?.value
                ?.device
                ?.get("demandTier")
                ?.convertFromJsonElement()
                ?.toString()

    internal val demandScore: Int?
        get() =
            lastEnrichment?.value?.device?.get("demandScore")?.convertFromJsonElement()?.let {
                when (it) {
                    is Double -> it.toInt()
                    is Float -> it.toInt()
                    else -> it.toString().toIntOrNull()
                }
            }

    val locale: String
        get() {
            val localeIdentifier = factory.makeLocaleIdentifier()
            return localeIdentifier ?: Locale.getDefault().toString()
        }

    val appVersion: String
        get() =
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "Unknown"
            } catch (e: Throwable) {
                Logger.debug(
                    LogLevel.error,
                    LogScope.device,
                    "DeviceHelper: Failed to load version info - $e",
                )
                ""
            }

    private val appVersionPadded: String
        get() = appVersion.asPadded()

    private val enrichment: Enrichment? get() = lastEnrichment.value
    val osVersion: String
        get() = Build.VERSION.RELEASE ?: ""

    val isEmulator: Boolean
        get() = Build.DEVICE.contains("generic") || Build.DEVICE.contains("emulator")

    val model: String
        get() = Build.MODEL

    val vendorId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    val deviceId: String
        get() = DeviceVendorId(VendorId(vendorId)).value

    var platformWrapper: String = ""
    var platformWrapperVersion: String = ""

    private val _locale: Locale = Locale.getDefault()

    private val _currency: Currency?
        get() {
            return try {
                Currency.getInstance(_locale)
            } catch (e: Throwable) {
                null
            }
        }

    val languageCode: String
        get() = _locale.language

    private val regionCode: String
        get() = _locale.country

    val currencyCode: String
        get() = _currency?.currencyCode ?: ""

    val currencySymbol: String
        get() = _currency?.symbol ?: ""

    val secondsFromGMT: String
        get() = (TimeZone.getDefault().rawOffset / 1000).toString()

    val isFirstAppOpen: Boolean
        get() = !storage.didTrackFirstSession

    val radioType: String
        @SuppressLint("MissingPermission")
        get() {

            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_NETWORK_STATE,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return ""
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                return when {
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wifi"
                    else -> ""
                }
            } else {
                when (connectivityManager.activeNetworkInfo?.type) {
                    ConnectivityManager.TYPE_MOBILE -> return "Cellular"
                    ConnectivityManager.TYPE_WIFI -> return "Wifi"
                    else -> return ""
                }
            }
        }

    val bundleId: String
        get() = context.packageName

    val isSandbox: Boolean
        get() {
            // Not exactly the same as iOS, but similar
            val isDebuggable: Boolean =
                (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            return isDebuggable
        }

    val urlScheme: String
        get() = context.packageName

    val appInstalledAtString: String
        get() {
            val date =
                withErrorTracking<Date> {

                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    return@withErrorTracking Date(packageInfo.firstInstallTime)
                }
            val formatter = dateFormat(DateUtils.SIMPLE)
            return formatter.format(date.getSuccess() ?: Date())
        }

    var interfaceStyleOverride: InterfaceStyle? = null

    val interfaceStyle: String
        get() {
            return interfaceStyleOverride?.rawValue ?: run {
                val style =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    } else {
                        Configuration.UI_MODE_NIGHT_UNDEFINED
                    }
                return when (style) {
                    Configuration.UI_MODE_NIGHT_NO -> "Light"
                    Configuration.UI_MODE_NIGHT_YES -> "Dark"
                    else -> "Unspecified"
                }
            }
        }

    val isLowPowerModeEnabled: String
        get() {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (powerManager.isPowerSaveMode) "true" else "false"
            } else {
                "false"
            }
        }

    private val localDateFormat: SimpleDateFormat
        get() {
            val formatter = dateFormat(DateUtils.yyyy_MM_dd)
            formatter.timeZone = TimeZone.getDefault()
            return formatter
        }

    private val utcDateFormat: SimpleDateFormat
        get() {
            val formatter = dateFormat(DateUtils.yyyy_MM_dd)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            return formatter
        }

    private val utcTimeFormat: SimpleDateFormat
        get() {
            val formatter = dateFormat(DateUtils.HH_mm_ss)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            return formatter
        }

    private val localDateTimeFormat: SimpleDateFormat
        get() {
            val formatter = dateFormat(DateUtils.ISO_SECONDS)
            formatter.timeZone = TimeZone.getDefault()
            return formatter
        }

    private val localTimeFormat: SimpleDateFormat
        get() {
            val formatter = dateFormat(DateUtils.HH_mm_ss)
            formatter.timeZone = TimeZone.getDefault()
            return formatter
        }

    private val utcDateTimeFormat: SimpleDateFormat
        get() {
            val formatter = dateFormat(DateUtils.ISO_SECONDS)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            return formatter
        }

    private val localDateString: String
        get() = localDateFormat.format(System.currentTimeMillis())

    private val localTimeString: String
        get() = localTimeFormat.format(System.currentTimeMillis())

    private val localDateTimeString: String
        get() = localDateTimeFormat.format(System.currentTimeMillis())

    private val utcDateString: String
        get() = utcDateFormat.format(System.currentTimeMillis())

    private val utcTimeString: String
        get() = utcTimeFormat.format(System.currentTimeMillis())

    private val utcDateTimeString: String
        get() = utcDateTimeFormat.format(System.currentTimeMillis())

    private val sdkVersionPadded: String
        get() = sdkVersion.asPadded()

    val appBuildString: String
        get() {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.versionCode.toString()
        }

    val sdkVersion: String
        get() = BuildConfig.SDK_VERSION

    val buildTime: String
        get() = BuildConfig.BUILD_TIME

    val gitSha: String
        get() = BuildConfig.GIT_SHA

    val kotlinVersion: String
        get() =
            try {
                KotlinVersion.CURRENT.toString()
            } catch (e: Throwable) {
                "UNKNOWN"
            }

    suspend fun getDeviceAttributes(
        sinceEvent: EventData?,
        computedPropertyRequests: List<ComputedPropertyRequest>,
    ): Map<String, Any> {
        val dictionary = getTemplateDevice()

        val computedProperties =
            getComputedDevicePropertiesSinceEvent(
                sinceEvent,
                computedPropertyRequests,
            )
        return dictionary + computedProperties
    }

    private suspend fun getComputedDevicePropertiesSinceEvent(
        event: EventData?,
        computedPropertyRequests: List<ComputedPropertyRequest>,
    ): Map<String, Any> {
        val output = mutableMapOf<String, Any>()

        for (computedPropertyRequest in computedPropertyRequests) {
            val value =
                storage.coreDataManager.getComputedPropertySinceEvent(
                    event,
                    request = computedPropertyRequest,
                )
            value?.let {
                output[computedPropertyRequest.type.prefix + computedPropertyRequest.eventName] = it
            }
        }

        return output
    }

    suspend fun getTemplateDevice(): Map<String, Any> {
        return withErrorTracking {
            val identityInfo = factory.makeIdentityInfo()
            val aliases = listOf(identityInfo.aliasId)
            val capabilities: List<Capability> =
                listOf(
                    Capability.PaywallEventReceiver(),
                    Capability.MultiplePaywallUrls,
                    Capability.ConfigCaching,
                )

            DeviceTemplate(
                publicApiKey = storage.apiKey,
                platform = "Android",
                appUserId = identityInfo.appUserId ?: "",
                aliases = aliases,
                vendorId = vendorId,
                deviceId = deviceId,
                appVersion = appVersion,
                osVersion = osVersion,
                deviceModel = model,
                deviceLocale = locale,
                preferredLocale = locale,
                deviceLanguageCode = languageCode,
                preferredLanguageCode = languageCode,
                regionCode = regionCode,
                preferredRegionCode = regionCode,
                deviceCurrencyCode = currencyCode,
                deviceCurrencySymbol = currencySymbol,
                timezoneOffset = (TimeZone.getDefault().rawOffset) / 1000,
                radioType = radioType,
                interfaceStyle = interfaceStyle,
                isLowPowerModeEnabled = isLowPowerModeEnabled.toBoolean(),
                bundleId = bundleId,
                appInstallDate = appInstalledAtString,
                isMac = false,
                daysSinceInstall = daysSinceInstall,
                minutesSinceInstall = minutesSinceInstall,
                daysSinceLastPaywallView = daysSinceLastPaywallView,
                minutesSinceLastPaywallView = minutesSinceLastPaywallView,
                totalPaywallViews = totalPaywallViews,
                utcDate = utcDateString,
                localDate = localDateString,
                utcTime = utcTimeString,
                localTime = localTimeString,
                utcDateTime = utcDateTimeString,
                localDateTime = localDateTimeString,
                isSandbox = isSandbox.toString(),
                activeEntitlements =
                    Superwall.instance.entitlements.active
                        .map { it.id },
                activeEntitlementsObject =
                    Superwall.instance.entitlements.active
                        .map { mapOf("identifier" to it.id, "type" to it.type.raw) },
                subscriptionStatus =
                    Superwall.instance.subscriptionStatus.value?.let {
                        when (it) {
                            is SubscriptionStatus.Active -> "ACTIVE"
                            is SubscriptionStatus.Inactive -> "INACTIVE"
                            is SubscriptionStatus.Unknown -> "UNKNOWN"
                        }
                    },
                activeProducts = factory.activeProductIds(),
                isFirstAppOpen = isFirstAppOpen,
                sdkVersion = sdkVersion,
                sdkVersionPadded = sdkVersionPadded,
                appBuildString = appBuildString,
                appBuildStringNumber = appBuildString.toInt(),
                interfaceStyleMode = if (interfaceStyleOverride == null) "automatic" else "manual",
                capabilities = capabilities.map { it.name },
                capabilitiesConfig =
                    capabilities.toJson(factory.json()),
                platformWrapper = platformWrapper,
                platformWrapperVersion = platformWrapperVersion,
                appVersionPadded = appVersionPadded,
                deviceTier = classifier.deviceTier().raw,
                reviewRequestCount = reviewRequestCount,
                kotlinVersion = kotlinVersion,
            )
        }.toResult()
            .map {
                it.toDictionary(json)
            }.map {
                val enriched =
                    (
                        enrichment
                            ?.device
                            ?.filterValues { it != null }
                            ?.mapValues { it.value.convertFromJsonElement() }
                            as Map<String, Any>?
                    )
                        ?: emptyMap()
                enriched
                    .plus(it)
                    .let {
                        if (factory.makeSuperwallOptions().enableExperimentalDeviceVariables) {
                            it.plus(latestExperimentalDeviceProperties())
                        } else {
                            it
                        }
                    }
            }.fold(
                onSuccess = { deviceTemplate ->
                    return@fold deviceTemplate
                },
                onFailure = {
                    Logger.debug(
                        logLevel = LogLevel.error,
                        scope = LogScope.device,
                        message = "Failed to get device template",
                        error = it,
                    )
                    return@fold emptyMap()
                },
            )
    }

    internal fun setEnrichment(enrichment: Enrichment) {
        this.lastEnrichment.value = enrichment
    }

    fun latestExperimentalDeviceProperties(): Map<String, Any> = factory.experimentalProperties()

    suspend fun getEnrichment(
        maxRetry: Int,
        timeout: Duration,
    ): Either<Enrichment, NetworkError> {
        val userAttributes =
            factory.makeIdentityManager().userAttributes.mapValues {
                it.value.convertToJsonElement()
            }

        val deviceAttributes =
            getTemplateDevice().mapValues {
                it.value.convertToJsonElement()
            }
        return network
            .getEnrichment(EnrichmentRequest(userAttributes, deviceAttributes), maxRetry, timeout)
            .then {
                lastEnrichment.value = it
            }.then {
                storage.write(LatestEnrichment, it)
                it.user.let {
                    Superwall.instance.setUserAttributes(it)
                }
            }
    }
}

private fun String.asPadded(): String {
    val components = split("-")
    if (components.isEmpty()) {
        return ""
    }
    val versionNumber = components[0]

    var appendix = ""

    // If there is a "beta" part...
    if (components.size > 1) {
        // Separate out the number from the name, e.g. beta.1 -> [beta, 1]
        val appendixComponents = components[1].split(".")
        appendix = "-" + appendixComponents[0]

        var appendixVersion = ""

        // Pad beta number and add to appendix
        if (appendixComponents.size > 1) {
            appendixVersion =
                String.format("%03d", appendixComponents[1].toIntOrNull() ?: 0)
            appendix += ".$appendixVersion"
        }
    }

    // Separate out the version numbers.
    val versionComponents = versionNumber.split(".")
    var newVersion = ""
    if (versionComponents.isNotEmpty()) {
        val major = String.format("%03d", versionComponents[0].toIntOrNull() ?: 0)
        newVersion += major
    }
    if (versionComponents.size > 1) {
        val minor = String.format("%03d", versionComponents[1].toIntOrNull() ?: 0)
        newVersion += ".$minor"
    }
    if (versionComponents.size > 2) {
        val patch = String.format("%03d", versionComponents[2].toIntOrNull() ?: 0)
        newVersion += ".$patch"
    }

    newVersion += appendix

    return newVersion
}
