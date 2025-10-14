package com.superwall.sdk.models.paywall

import android.graphics.Color
import com.superwall.sdk.config.models.OnDeviceCaching
import com.superwall.sdk.config.models.Survey
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.SerializableEntity
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.config.FeatureGatingBehavior
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.models.product.ProductItemsDeserializer
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.models.serialization.DateSerializer
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.paywall.presentation.PaywallCloseReason
import com.superwall.sdk.paywall.presentation.PaywallInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@JvmInline
value class PaywallURL(
    val value: String,
) {
    override fun toString(): String = value
}

@Serializable
data class Paywalls(
    val paywalls: List<Paywall>,
) : SerializableEntity

@Serializable
data class Paywall(
    @SerialName("id")
    val databaseId: String,
    @SerialName("identifier")
    var identifier: PaywallIdentifier,
    @SerialName("name")
    val name: String,
    @SerialName("url")
    val url: PaywallURL,
    @SerialName("paywalljs_event")
    val htmlSubstitutions: String,
    @SerialName("presentation_style_v2")
    private val presentationStyle: String?,
    @SerialName("presentation_style_v3")
    private val expandedPresentationStyle: PaywallPresentationStyle?,
    @SerialName("presentation_delay")
    private val presentationDelay: Long,
    @SerialName("presentation_condition")
    private val presentationCondition: String,
    @kotlinx.serialization.Transient()
    var presentation: PaywallPresentationInfo =
        PaywallPresentationInfo(
            style =
                expandedPresentationStyle
                    ?: PaywallPresentationStyle.None.also {
                        Logger.debug(
                            LogLevel.warn,
                            LogScope.paywallPresentation,
                            "Unknown or unsupported presentation style: $presentationStyle",
                        )
                    },
            delay = presentationDelay,
        ),
    @SerialName("background_color_hex")
    val backgroundColorHex: String,
    @SerialName("dark_background_color_hex")
    val darkBackgroundColorHex: String? = null,
    // Declared as private to prevent direct access
    @kotlinx.serialization.Transient()
    private var _products: List<ProductItem> = emptyList(),
    @Serializable(with = ProductItemsDeserializer::class)
    @SerialName("products_v2")
    private var _productItems: List<ProductItem>,
    @kotlinx.serialization.Transient()
    var productIds: List<String> = arrayListOf(),
    @kotlinx.serialization.Transient()
    var responseLoadingInfo: LoadingInfo = LoadingInfo(),
    @kotlinx.serialization.Transient()
    var webviewLoadingInfo: LoadingInfo = LoadingInfo(),
    @kotlinx.serialization.Transient()
    var productsLoadingInfo: LoadingInfo = LoadingInfo(),
    @kotlinx.serialization.Transient()
    var shimmerLoadingInfo: LoadingInfo = LoadingInfo(),
    var productVariables: List<ProductVariable>? = null,
    var swProductVariablesTemplate: List<ProductVariable>? = null,
    var paywalljsVersion: String? = null,
    var isFreeTrialAvailable: Boolean = false,
    // / The source of the presentation request. Either 'implicit', 'getPaywall', 'register'.
    var presentationSourceType: String? = null,
    @SerialName("feature_gating")
    var featureGating: FeatureGatingBehavior = FeatureGatingBehavior.NonGated,
    @SerialName("computed_properties")
    var computedPropertyRequests: List<ComputedPropertyRequest> = emptyList(),
    @SerialName("local_notifications")
    var localNotifications: List<LocalNotification> = emptyList(),
    /**
     * Indicates whether the caching of the paywall is enabled or not.
     */
    @SerialName("on_device_cache")
    var onDeviceCache: OnDeviceCaching = OnDeviceCaching.Disabled,
    @kotlinx.serialization.Transient()
    var experiment: Experiment? = null,
    @kotlinx.serialization.Transient()
    var closeReason: PaywallCloseReason = PaywallCloseReason.None,
    @SerialName("url_config")
    val urlConfig: PaywallWebviewUrl.Config? = null,
    @Serializable
    @SerialName("cache_key")
    val cacheKey: CacheKey,
    @Serializable
    @SerialName("build_id")
    val buildId: String,
    /**
     Surveys to potentially show when an action happens in the paywall.
     */
    @SerialName("surveys")
    var surveys: List<Survey> = emptyList(),
    @SerialName("is_scroll_enabled")
    val isScrollEnabled: Boolean? = true,
    @SerialName("reroute_back_button")
    val rerouteBackButton: ToggleMode? = null,
) : SerializableEntity {
    // Public getter for productItems
    var productItems: List<ProductItem>
        get() = _productItems
        set(value) {
            _productItems = value
            // Automatically update related properties when productItems is set
            productIds = value.map { it.fullProductId }
            _products = value // Assuming makeProducts is a function that generates products based on product items
        }

    // Public getter for products to allow access but not direct modification
    val products: List<ProductItem>
        get() = _products

    val backgroundColor: Int by lazy {
        try {
            Color.parseColor(this.backgroundColorHex)
        } catch (e: Throwable) {
            Logger.debug(
                logLevel = LogLevel.warn,
                scope = LogScope.paywallView,
                message =
                    "Invalid paywall background color: ${this.backgroundColorHex}. " +
                        "Defaulting to white.",
            )
            Color.WHITE
        }
    }

    val darkBackgroundColor: Int? by lazy {
        try {
            Color.parseColor(this.darkBackgroundColorHex)
        } catch (e: Throwable) {
            Logger.debug(
                logLevel = LogLevel.warn,
                scope = LogScope.paywallView,
                message =
                    "Invalid paywall background color: ${this.darkBackgroundColorHex}. " +
                        "Defaulting to white.",
            )
            null
        }
    }

    init {
        productItems = _productItems
    }

    @Serializable
    data class LoadingInfo(
        @Serializable(with = DateSerializer::class)
        var startAt: Date? = null,
        @Serializable(with = DateSerializer::class)
        var endAt: Date? = null,
        @Serializable(with = DateSerializer::class)
        var failAt: Date? = null,
    )

    fun update(paywall: Paywall) {
        productItems = paywall.productItems
        productVariables = paywall.productVariables
        swProductVariablesTemplate = paywall.swProductVariablesTemplate
        isFreeTrialAvailable = paywall.isFreeTrialAvailable
        productsLoadingInfo = paywall.productsLoadingInfo
        presentationSourceType = paywall.presentationSourceType
        experiment = paywall.experiment
    }

    fun getInfo(fromEvent: EventData?): PaywallInfo =
        PaywallInfo(
            databaseId = databaseId,
            identifier = identifier,
            name = name,
            url = url,
            products = products,
            productIds = productIds,
            eventData = fromEvent,
            responseLoadStartTime = responseLoadingInfo.startAt,
            responseLoadCompleteTime = responseLoadingInfo.endAt,
            responseLoadFailTime = responseLoadingInfo.failAt,
            webViewLoadStartTime = webviewLoadingInfo.startAt,
            webViewLoadCompleteTime = webviewLoadingInfo.endAt,
            webViewLoadFailTime = webviewLoadingInfo.failAt,
            productsLoadStartTime = productsLoadingInfo.startAt,
            productsLoadFailTime = productsLoadingInfo.failAt,
            productsLoadCompleteTime = productsLoadingInfo.endAt,
            shimmerLoadStartTime = shimmerLoadingInfo.startAt,
            shimmerLoadCompleteTime = shimmerLoadingInfo.endAt,
            experiment = experiment,
            paywalljsVersion = paywalljsVersion,
            isFreeTrialAvailable = isFreeTrialAvailable,
            presentationSourceType = presentationSourceType,
            featureGatingBehavior = featureGating,
            closeReason = closeReason,
            localNotifications = localNotifications,
            computedPropertyRequests = computedPropertyRequests,
            surveys = surveys,
            presentation = presentation,
            cacheKey = cacheKey,
            buildId = buildId,
            isScrollEnabled = isScrollEnabled ?: true,
        )

    companion object {
        fun stub(): Paywall =
            Paywall(
                databaseId = "id",
                identifier = "identifier",
                name = "abac",
                url = PaywallURL("https://google.com"),
                htmlSubstitutions = "",
                presentation =
                    PaywallPresentationInfo(
                        PaywallPresentationStyle.Modal,
                        300,
                    ),
                expandedPresentationStyle = PaywallPresentationStyle.Modal,
                presentationStyle = "MODAL",
                presentationCondition = "CHECK_USER_SUBSCRIPTION",
                backgroundColorHex = "000000",
                darkBackgroundColorHex = null,
                productIds = arrayListOf(),
                _productItems = emptyList(),
                _products = emptyList(),
                responseLoadingInfo = LoadingInfo(),
                webviewLoadingInfo = LoadingInfo(),
                productsLoadingInfo = LoadingInfo(),
                productVariables = arrayListOf(),
                swProductVariablesTemplate = arrayListOf(),
                paywalljsVersion = "",
                isFreeTrialAvailable = false,
                featureGating = FeatureGatingBehavior.NonGated,
                localNotifications = arrayListOf(),
                presentationDelay = 300,
                urlConfig =
                    PaywallWebviewUrl.Config(
                        3,
                        listOf(
                            PaywallWebviewUrl("https://google.com", 1000L, 1),
                        ),
                    ),
                cacheKey = "123",
                buildId = "test",
                isScrollEnabled = true,
                rerouteBackButton = ToggleMode.DISABLED,
            )
    }

    @Serializable
    enum class ToggleMode {
        @SerialName("ENABLED")
        ENABLED,

        @SerialName("DISABLED")
        DISABLED,
    }
}
