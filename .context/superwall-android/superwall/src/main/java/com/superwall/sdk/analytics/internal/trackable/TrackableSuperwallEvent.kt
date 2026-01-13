package com.superwall.sdk.analytics.internal.trackable

import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.analytics.superwall.TransactionProduct
import com.superwall.sdk.config.models.Survey
import com.superwall.sdk.config.models.SurveyOption
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.config.options.toMap
import com.superwall.sdk.dependencies.ComputedPropertyRequestsFactory
import com.superwall.sdk.dependencies.FeatureFlagsFactory
import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.network.JsonFactory
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatus
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.view.survey.SurveyPresentationResult
import com.superwall.sdk.paywall.view.webview.WebviewError
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.abstractions.product.StoreProductType
import com.superwall.sdk.store.abstractions.transactions.StoreTransaction
import com.superwall.sdk.store.abstractions.transactions.StoreTransactionType
import com.superwall.sdk.store.transactions.RestoreType
import com.superwall.sdk.store.transactions.TransactionError
import com.superwall.sdk.web.WebPaywallRedeemer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.net.URI

interface TrackableSuperwallEvent : Trackable {
    val superwallPlacement: SuperwallEvent
}

sealed class InternalSuperwallEvent(
    override val superwallPlacement: SuperwallEvent,
) : TrackableSuperwallEvent {
    override val rawName: String
        get() = this.superwallPlacement.rawName

    override val canImplicitlyTriggerPaywall: Boolean
        get() = this.superwallPlacement.canImplicitlyTriggerPaywall

    class AppOpen(
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.AppOpen()) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap()
    }

    class AppInstall(
        val appInstalledAtString: String,
        val hasExternalPurchaseController: Boolean,
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.AppInstall()) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> =
            hashMapOf(
                "application_installed_at" to appInstalledAtString,
                "using_purchase_controller" to hasExternalPurchaseController,
            )
    }

    // TODO: Implement the rest

    class SurveyClose(
        override val audienceFilterParams: MutableMap<String, Any> = mutableMapOf(),
    ) : InternalSuperwallEvent(SuperwallEvent.SurveyClose()) {
        override suspend fun getSuperwallParameters(): Map<String, Any> = emptyMap()
    }

    data class SurveyResponse(
        val survey: Survey,
        val selectedOption: SurveyOption,
        val customResponse: String?,
        val paywallInfo: PaywallInfo,
    ) : InternalSuperwallEvent(
            SuperwallEvent.SurveyResponse(
                survey,
                selectedOption,
                customResponse,
                paywallInfo,
            ),
        ) {
        override val superwallPlacement: SuperwallEvent
            get() =
                SuperwallEvent.SurveyResponse(
                    survey,
                    selectedOption,
                    customResponse,
                    paywallInfo,
                )

        override val audienceFilterParams: Map<String, Any>
            get() {
                val output = paywallInfo.audienceFilterParams()
                return (
                    output +
                        mapOf(
                            "survey_selected_option_title" to selectedOption.title,
                            "survey_custom_response" to customResponse,
                        ).map { (key, value) -> if (value != null) key to value else null }
                            .filterNotNull()
                            .toMap()
                )
            }

        override suspend fun getSuperwallParameters(): Map<String, Any> {
            val params: Map<String, Any> =
                mapOf(
                    "survey_id" to survey.id,
                    "survey_assignment_key" to survey.assignmentKey,
                    "survey_selected_option_id" to selectedOption.id,
                )

            return paywallInfo.eventParams(otherParams = params)
        }
    }

    class AppLaunch(
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.AppLaunch()) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap()
    }

    class Attributes(
        val appInstalledAtString: String,
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.UserAttributes(audienceFilterParams)) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> =
            hashMapOf(
                "application_installed_at" to appInstalledAtString,
            )
    }

    class IdentityAlias(
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.IdentityAlias()) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap()
    }

    class DeepLink(
        val uri: URI,
    ) : InternalSuperwallEvent(SuperwallEvent.DeepLink(uri)) {
        private fun extractedParams(): HashMap<String, Any> =
            hashMapOf(
                "url" to uri.toString(),
                "path" to (uri.path ?: ""),
                "pathExtension" to (
                    uri.path?.substringAfterLast("/")?.substringAfterLast('.')
                        ?: ""
                ),
                "lastPathComponent" to (uri.path?.substringAfterLast("/") ?: ""),
                "host" to (uri.host ?: ""),
                "query" to (uri.query ?: ""),
                "fragment" to (uri.fragment ?: ""),
            )

        override suspend fun getSuperwallParameters(): HashMap<String, Any> = extractedParams()

        override var audienceFilterParams: HashMap<String, Any> =
            HashMap(extractQueryParameters(uri).plus(extractedParams()))

        companion object {
            private fun extractQueryParameters(uri: URI): HashMap<String, Any> =
                HashMap(
                    (uri.query ?: "")
                        .split("&")
                        .associate {
                            it.split("=").let {
                                it[0] to it.getOrNull(1)
                            }
                        }.map { (paramName, paramValue) ->
                            when {
                                paramValue == null -> null
                                paramValue.equals("true", ignoreCase = true) ->
                                    paramName to true

                                paramValue.equals("false", ignoreCase = true) ->
                                    paramName to false

                                paramValue.toIntOrNull() != null ->
                                    paramName to
                                        paramValue.toInt()

                                paramValue.toDoubleOrNull() != null ->
                                    paramName to
                                        paramValue.toDouble()

                                else -> paramName to paramValue
                            }
                        }.filterNotNull()
                        .toMap<String, Any>(),
                )
        }
    }

    class FirstSeen(
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.FirstSeen()) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap()
    }

    class AppClose(
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.AppClose()) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap()
    }

    class PaywallLoad(
        val state: State,
        val eventData: EventData?,
    ) : InternalSuperwallEvent(SuperwallEvent.PaywallResponseLoadStart(eventData?.name)) {
        sealed class State {
            class Start : State()

            class NotFound : State()

            class Fail : State()

            class Complete(
                val paywallInfo: PaywallInfo,
            ) : State()
        }

        override val superwallPlacement: SuperwallEvent
            get() =
                when (state) {
                    is State.Start ->
                        SuperwallEvent.PaywallResponseLoadStart(
                            eventData?.name,
                        )

                    is State.Complete ->
                        SuperwallEvent.PaywallResponseLoadComplete(
                            eventData?.name,
                            state.paywallInfo,
                        )

                    is State.Fail ->
                        SuperwallEvent.PaywallResponseLoadFail(
                            eventData?.name,
                        )

                    is State.NotFound ->
                        SuperwallEvent.PaywallResponseLoadNotFound(
                            eventData?.name,
                        )
                }

        override val audienceFilterParams: Map<String, Any>
            get() =
                when (state) {
                    is State.Complete -> state.paywallInfo.audienceFilterParams()
                    else -> emptyMap()
                }

        override suspend fun getSuperwallParameters(): HashMap<String, Any> {
            val fromEvent = eventData != null
            val params: HashMap<String, Any> =
                hashMapOf(
                    "is_triggered_from_event" to fromEvent,
                )

            when (state) {
                is State.Start -> return params
                is State.NotFound -> return params
                is State.Fail -> return params
                is State.Complete -> return HashMap(
                    state.paywallInfo
                        .eventParams(otherParams = params),
                )
            }
        }
    }

    class SubscriptionStatusDidChange(
        val subscriptionStatus: SubscriptionStatus,
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.SubscriptionStatusDidChange()) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> =
            hashMapOf(
                "subscription_status" to
                    when (subscriptionStatus) {
                        is SubscriptionStatus.Active -> "active"
                        is SubscriptionStatus.Inactive -> "inactive"
                        is SubscriptionStatus.Unknown -> "unknown"
                    },
            )
    }

    class SessionStart(
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.SessionStart()) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap()
    }

    class ConfigAttributes(
        val options: SuperwallOptions,
        val hasExternalPurchaseController: Boolean,
        val hasDelegate: Boolean,
    ) : InternalSuperwallEvent(SuperwallEvent.ConfigAttributes) {
        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): HashMap<String, Any> =
            hashMapOf(
                *options
                    .toMap()
                    .plus(
                        mapOf(
                            "using_purchase_controller" to hasExternalPurchaseController,
                            "has_delegate" to hasDelegate,
                        ),
                    ).toList()
                    .toTypedArray(),
            )
    }

    class DeviceAttributes(
        val deviceAttributes: HashMap<String, Any>,
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(SuperwallEvent.DeviceAttributes(attributes = deviceAttributes)) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> = deviceAttributes
    }

    class IntegrationAttributes(
        var params: Map<String, Any>,
    ) : InternalSuperwallEvent(SuperwallEvent.IntegrationAttributes(audienceFilterParams = params)) {
        override var audienceFilterParams: HashMap<String, Any> = HashMap(params)

        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap()
    }

    class TriggerFire(
        val triggerResult: InternalTriggerResult,
        val triggerName: String,
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(
            SuperwallEvent.TriggerFire(
                placementName = triggerName,
                result = triggerResult.toPublicType(),
            ),
        ) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> {
            val params: HashMap<String, Any> =
                hashMapOf(
                    "trigger_name" to triggerName,
                )

            return when (triggerResult) {
                is InternalTriggerResult.NoAudienceMatch -> {
                    params.apply {
                        this["result"] = "no_rule_match"
                    }
                }

                is InternalTriggerResult.Holdout -> {
                    params.apply {
                        this["variant_id"] = triggerResult.experiment.variant.id
                        this["experiment_id"] = triggerResult.experiment.id
                        this["result"] = "holdout"
                    }
                }

                is InternalTriggerResult.Paywall -> {
                    params.apply {
                        this["variant_id"] = triggerResult.experiment.variant.id
                        this["experiment_id"] = triggerResult.experiment.id
                        this["paywall_identifier"] =
                            triggerResult.experiment.variant.paywallId ?: ""
                        this["result"] = "present"
                    }
                }

                is InternalTriggerResult.PlacementNotFound -> {
                    params.apply {
                        this["result"] = "eventNotFound"
                    }
                }

                is InternalTriggerResult.Error -> {
                    params.apply {
                        this["result"] = "error"
                    }
                }
            }
        }
    }

    class PresentationRequest(
        val eventData: EventData?,
        val type: PresentationRequestType,
        val status: PaywallPresentationRequestStatus,
        val statusReason: PaywallPresentationRequestStatusReason?,
        val factory: PresentationRequest.Factory,
        override var audienceFilterParams: HashMap<String, Any> = HashMap(),
    ) : InternalSuperwallEvent(
            SuperwallEvent.PaywallPresentationRequest(
                status = status,
                reason = statusReason,
            ),
        ) {
        interface Factory :
            RuleAttributesFactory,
            FeatureFlagsFactory,
            ComputedPropertyRequestsFactory

        override suspend fun getSuperwallParameters(): HashMap<String, Any> =
            hashMapOf(
                "source_event_name" to (eventData?.name ?: ""),
                "pipeline_type" to type.description,
                "status" to status.status,
                "status_reason" to (statusReason?.description ?: ""),
            )
    }

    class PaywallOpen(
        val paywallInfo: PaywallInfo,
        val userAttributes: Map<String, Any>,
        val demandTier: String?,
        val demandScore: Int?,
    ) : InternalSuperwallEvent(SuperwallEvent.PaywallOpen(paywallInfo = paywallInfo)) {
        override val audienceFilterParams: Map<String, Any>
            get() {
                return paywallInfo.audienceFilterParams()
            }

        override suspend fun getSuperwallParameters(): HashMap<String, Any> =
            HashMap(
                paywallInfo.eventParams() +
                    mutableMapOf<String, Any>(
                        "user_attributes" to userAttributes,
                    ).apply {
                        if (demandTier != null) {
                            put("attr_demandTier", demandTier)
                        }
                        if (demandScore != null) {
                            put("attr_demandScore", demandScore)
                        }
                    },
            )
    }

    class PaywallClose(
        val paywallInfo: PaywallInfo,
        val surveyPresentationResult: SurveyPresentationResult,
    ) : InternalSuperwallEvent(SuperwallEvent.PaywallClose(paywallInfo)) {
        override val audienceFilterParams: Map<String, Any>
            get() {
                return paywallInfo.audienceFilterParams()
            }

        override suspend fun getSuperwallParameters(): HashMap<String, Any> {
            var params: MutableMap<String, Any> =
                mutableMapOf(
                    "survey_attached" to paywallInfo.surveys.isNotEmpty(),
                )

            if (surveyPresentationResult != SurveyPresentationResult.NOSHOW) {
                params["survey_presentation"] = surveyPresentationResult.rawValue
            }

            val eventParams = paywallInfo.eventParams()
            params.putAll(eventParams)
            return HashMap(params)
        }
    }

    class PaywallDecline(
        val paywallInfo: PaywallInfo,
    ) : InternalSuperwallEvent(SuperwallEvent.PaywallDecline(paywallInfo = paywallInfo)) {
        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap(paywallInfo.eventParams())

        override val audienceFilterParams: Map<String, Any>
            get() {
                return paywallInfo.audienceFilterParams()
            }
    }

    class Transaction(
        val state: State,
        val paywallInfo: PaywallInfo,
        val product: StoreProductType?,
        val model: StoreTransaction?,
        val source: TransactionSource,
        val isObserved: Boolean,
        var demandScore: Int?,
        var demandTier: String?,
        var userAttributes: Map<String, Any>? = null,
        var store: String = "PLAY_STORE",
    ) : TrackableSuperwallEvent {
        enum class TransactionSource(
            val raw: String,
        ) {
            INTERNAL("SUPERWALL"),
            OBSERVER("OBSERVER"),
            EXTERNAL("APP"),
        }

        sealed class State {
            class Start(
                val product: StoreProductType,
            ) : State()

            class Fail(
                val error: TransactionError,
            ) : State()

            class Abandon(
                val product: StoreProductType,
            ) : State()

            class Complete(
                val product: StoreProductType,
                val transaction: StoreTransactionType?,
            ) : State()

            class Restore(
                val restoreType: RestoreType,
            ) : State()

            class Timeout : State()
        }

        override val audienceFilterParams: Map<String, Any>
            get() {
                return paywallInfo.audienceFilterParams().let {
                    if (superwallPlacement is SuperwallEvent.TransactionAbandon) {
                        it.plus("abandoned_product_id" to (product?.productIdentifier ?: ""))
                    } else {
                        it
                    }
                }
            }

        override val superwallPlacement: SuperwallEvent
            get() =
                when (state) {
                    is State.Start ->
                        SuperwallEvent.TransactionStart(
                            product = state.product,
                            paywallInfo = paywallInfo,
                        )

                    is State.Fail ->
                        SuperwallEvent.TransactionFail(
                            error = state.error,
                            paywallInfo = paywallInfo,
                        )

                    is State.Abandon ->
                        SuperwallEvent.TransactionAbandon(
                            product = state.product,
                            paywallInfo = paywallInfo,
                        )

                    is State.Complete ->
                        SuperwallEvent.TransactionComplete(
                            transaction = state.transaction,
                            product = state.product,
                            paywallInfo = paywallInfo,
                        )

                    is State.Restore ->
                        SuperwallEvent.TransactionRestore(
                            restoreType = state.restoreType,
                            paywallInfo = paywallInfo,
                        )

                    is State.Timeout -> SuperwallEvent.TransactionTimeout(paywallInfo = paywallInfo)
                }
        override val rawName: String
            get() = superwallPlacement.rawName

        override val canImplicitlyTriggerPaywall: Boolean
            get() = if (isObserved) false else superwallPlacement.canImplicitlyTriggerPaywall

        override suspend fun getSuperwallParameters(): HashMap<String, Any> =
            when (state) {
                is State.Restore -> {
                    var eventParams = HashMap(paywallInfo.eventParams(product))
                    model?.toDictionary()?.let { transactionDict ->
                        eventParams.putAll(transactionDict)
                    }
                    eventParams["store"] = store
                    eventParams["restore_via_purchase_attempt"] = model != null
                    eventParams
                }

                is State.Start,
                is State.Abandon,
                is State.Complete,
                is State.Timeout,
                -> {
                    var eventParams = HashMap(paywallInfo.eventParams(product))
                    model?.toDictionary()?.let { transactionDict ->
                        eventParams.putAll(transactionDict)
                    }
                    if (demandScore != null) {
                        eventParams["attr_demandScore"] = demandScore
                    }
                    if (demandTier != null) {
                        eventParams["attr_demandTier"] = demandTier
                    }
                    eventParams["store"] = store
                    eventParams["source"] = source.raw
                    if (state is State.Complete) {
                        eventParams["user_attributes"] = userAttributes
                    }

                    eventParams
                }

                is State.Fail -> {
                    when (state.error) {
                        is TransactionError.Failure,
                        is TransactionError.Pending,
                        -> {
                            val message = state.error.message
                            var eventParams =
                                HashMap(
                                    paywallInfo.eventParams(
                                        product,
                                        otherParams = mapOf("message" to message),
                                    ),
                                )
                            if (demandScore != null) {
                                eventParams["attr_demandScore"] = demandScore
                            }
                            if (demandTier != null) {
                                eventParams["attr_demandTier"] = demandTier
                            }
                            eventParams["store"] = store
                            eventParams
                        }
                    }
                }
            }
    }

    class SubscriptionStart(
        val paywallInfo: PaywallInfo,
        val product: StoreProduct,
    ) : InternalSuperwallEvent(
            SuperwallEvent.SubscriptionStart(
                product = product,
                paywallInfo = paywallInfo,
            ),
        ) {
        override val audienceFilterParams: Map<String, Any>
            get() {
                return paywallInfo.audienceFilterParams()
            }

        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap(paywallInfo.eventParams(product))
    }

    class FreeTrialStart(
        val paywallInfo: PaywallInfo,
        val product: StoreProduct,
    ) : InternalSuperwallEvent(
            SuperwallEvent.FreeTrialStart(
                product = product,
                paywallInfo = paywallInfo,
            ),
        ) {
        override val audienceFilterParams: Map<String, Any>
            get() {
                return paywallInfo.audienceFilterParams()
            }

        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap(paywallInfo.eventParams(product))
    }

    class NonRecurringProductPurchase(
        val paywallInfo: PaywallInfo,
        val product: StoreProduct,
    ) : InternalSuperwallEvent(
            SuperwallEvent.NonRecurringProductPurchase(
                product =
                    TransactionProduct(
                        product = product,
                    ),
                paywallInfo = paywallInfo,
            ),
        ) {
        override val audienceFilterParams: Map<String, Any>
            get() {
                return paywallInfo.audienceFilterParams()
            }

        override suspend fun getSuperwallParameters(): HashMap<String, Any> = HashMap(paywallInfo.eventParams(product))
    }

    class PaywallWebviewLoad(
        val state: State,
        val paywallInfo: PaywallInfo,
    ) : InternalSuperwallEvent(SuperwallEvent.PaywallWebviewLoadStart(paywallInfo)) {
        sealed class State {
            class Start : State()

            data class Fail(
                val error: WebviewError,
                val urls: List<String>,
            ) : State()

            object Fallback : State()

            class Timeout(
                val msg: String,
            ) : State()

            class Complete : State()
        }

        override val superwallPlacement: SuperwallEvent
            get() =
                when (state) {
                    is PaywallWebviewLoad.State.Start ->
                        SuperwallEvent.PaywallWebviewLoadStart(
                            paywallInfo,
                        )

                    is PaywallWebviewLoad.State.Timeout ->
                        SuperwallEvent.PaywallWebviewLoadTimeout(
                            paywallInfo,
                        )

                    is PaywallWebviewLoad.State.Fail ->
                        SuperwallEvent.PaywallWebviewLoadFail(
                            paywallInfo,
                            state.error,
                        )

                    is PaywallWebviewLoad.State.Complete ->
                        SuperwallEvent.PaywallWebviewLoadComplete(
                            paywallInfo,
                        )

                    is State.Fallback -> {
                        SuperwallEvent.PaywallWebviewLoadFallback(paywallInfo)
                    }
                }

        override val audienceFilterParams: Map<String, Any>
            get() {
                return paywallInfo.audienceFilterParams()
            }

        override suspend fun getSuperwallParameters(): HashMap<String, Any> {
            val extras =
                when (state) {
                    is State.Fail ->
                        mapOf(
                            "error_message" to state.error.toString(),
                            *state.urls
                                .mapIndexed { i, it ->
                                    "url_$i" to it
                                }.toTypedArray(),
                        )

                    is State.Timeout ->
                        mapOf(
                            "error_message" to state.msg,
                        )

                    else -> mapOf()
                }
            val params = paywallInfo.eventParams() + extras
            return HashMap(params)
        }
    }

    class PaywallResourceLoadFail(
        val url: String,
        val error: String,
    ) : InternalSuperwallEvent(SuperwallEvent.PaywallResourceLoadFail(url, error)) {
        override val audienceFilterParams: Map<String, Any>
            get() = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> = mapOf("url" to url, "error" to error)
    }

    class PaywallProductsLoad(
        val state: State,
        val paywallInfo: PaywallInfo,
        val eventData: EventData?,
    ) : InternalSuperwallEvent(
            SuperwallEvent.PaywallProductsLoadStart(
                eventData?.name,
                paywallInfo,
            ),
        ) {
        sealed class State {
            class Start : State()

            class Fail(
                val errorMessage: String?,
            ) : State()

            class Complete : State()
        }

        override val superwallPlacement: SuperwallEvent
            get() =
                when (state) {
                    is State.Start ->
                        SuperwallEvent.PaywallProductsLoadStart(
                            eventData?.name,
                            paywallInfo,
                        )

                    is State.Fail ->
                        SuperwallEvent.PaywallProductsLoadFail(
                            state.errorMessage,
                            eventData?.name,
                            paywallInfo,
                        )

                    is State.Complete ->
                        SuperwallEvent.PaywallProductsLoadComplete(
                            eventData?.name,
                            paywallInfo,
                        )
                }

        override val audienceFilterParams: Map<String, Any>
            get() {
                return paywallInfo.audienceFilterParams()
            }

        override suspend fun getSuperwallParameters(): HashMap<String, Any> {
            val fromEvent = eventData != null
            var params: HashMap<String, Any> =
                hashMapOf(
                    "is_triggered_from_event" to fromEvent,
                )

            when (state) {
                is State.Fail -> {
                    if (state.errorMessage != null) {
                        params["error_message"] = state.errorMessage
                    }
                }

                else -> Unit
            }

            params.putAll(paywallInfo.eventParams())
            return params
        }
    }

    data class ConfigRefresh(
        val isCached: Boolean,
        val buildId: String,
        val retryCount: Int,
        val fetchDuration: Long,
    ) : InternalSuperwallEvent(SuperwallEvent.ConfigRefresh) {
        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> =
            mapOf(
                "cache_status" to if (isCached) "CACHED" else "NOT_CACHED",
                "config_build_id" to buildId,
                "retry_count" to retryCount,
                "fetch_duration" to fetchDuration,
            )
    }

    data class ConfigFail(
        val errorMessage: String,
    ) : InternalSuperwallEvent(SuperwallEvent.ConfigFail) {
        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> = mapOf("error_message" to errorMessage)
    }

    object Reset : InternalSuperwallEvent(SuperwallEvent.Reset) {
        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> = emptyMap()
    }

    data class Restore(
        val state: State,
        val paywallInfo: PaywallInfo,
    ) : InternalSuperwallEvent(
            when (state) {
                is State.Complete -> SuperwallEvent.Restore.Complete
                is State.Failure -> SuperwallEvent.Restore.Fail(state.reason)
                is State.Start -> SuperwallEvent.Restore.Start
            },
        ) {
        sealed class State {
            object Start : State()

            data class Failure(
                val reason: String,
            ) : State()

            object Complete : State()
        }

        override suspend fun getSuperwallParameters(): Map<String, Any> =
            paywallInfo.eventParams().let {
                when (state) {
                    is State.Failure -> it + mapOf("error_message" to state.reason)
                    else -> it
                }
            }

        override val audienceFilterParams: Map<String, Any>
            get() = paywallInfo.audienceFilterParams()
    }

    data class CustomPlacement(
        val placementName: String,
        val paywallInfo: PaywallInfo,
        val params: Map<String, Any>,
    ) : InternalSuperwallEvent(
            SuperwallEvent.CustomPlacement(
                placementName,
                paywallInfo,
                params,
            ),
        ) {
        override val audienceFilterParams: Map<String, Any>
            get() = paywallInfo.audienceFilterParams() + params

        override val rawName: String
            get() = placementName

        override suspend fun getSuperwallParameters(): Map<String, Any> =
            paywallInfo.eventParams() + params + mapOf("name" to placementName)

        override val canImplicitlyTriggerPaywall: Boolean = true
    }

    data class ShimmerLoad(
        val state: State,
        val paywallId: String,
        val visibleDuration: Double?,
        val delay: Double,
        val preloadingEnabled: Boolean,
    ) : InternalSuperwallEvent(
            if (state ==
                State.Started
            ) {
                SuperwallEvent.ShimmerViewStart
            } else {
                SuperwallEvent.ShimmerViewComplete(visibleDuration ?: 0.0)
            },
        ) {
        enum class State {
            Started,
            Complete,
        }

        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override val rawName: String
            get() = superwallPlacement.rawName

        override suspend fun getSuperwallParameters(): Map<String, Any> =
            mapOf(
                "paywall_id" to paywallId,
                "preloading_enabled" to preloadingEnabled,
                "visible_duration" to visibleDuration,
            ).map { (key, value) -> if (value != null) key to value else null }
                .filterNotNull()
                .toMap()

        override val canImplicitlyTriggerPaywall: Boolean = false
    }

    object ConfirmAllAssignments :
        InternalSuperwallEvent(SuperwallEvent.ConfirmAllAssignments) {
        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> = emptyMap()
    }

    internal data class ErrorThrown(
        val message: String,
        val stacktrace: String,
        val occuredAt: Long,
        val type: String,
        val isFatal: Boolean,
    ) : InternalSuperwallEvent(SuperwallEvent.ErrorThrown) {
        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters() =
            mapOf(
                "exception_type" to type,
                "error_message" to message,
                "error_stack_trace" to stacktrace,
                "occured_at" to occuredAt,
                "is_fatal" to isFatal,
            )
    }

    internal data class ExpressionResult(
        val celExpression: String? = null,
        val liquidExpression: String? = null,
        val jsExpression: String? = null,
        val celExpressionResult: Boolean? = null,
        val liquidExpressionResult: Boolean? = null,
        val jsExpressionResult: Boolean? = null,
        val errorMessage: String? = null,
    ) : InternalSuperwallEvent(SuperwallEvent.ExpressionResult) {
        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> =
            mapOf(
                "cel_expression" to (celExpression ?: ""),
                "liquid_expression" to (liquidExpression ?: ""),
                "js_expression" to (jsExpression ?: ""),
                "cel_expression_result" to (celExpressionResult ?: ""),
                "liquid_expression_result" to (liquidExpressionResult ?: ""),
                "js_expression_result" to (jsExpressionResult ?: ""),
                "error_message" to (errorMessage ?: ""),
            )
    }

    class Redemptions(
        val state: RedemptionState,
        val type: WebPaywallRedeemer.RedeemType,
    ) : InternalSuperwallEvent(
            when (state) {
                RedemptionState.Start -> SuperwallEvent.RedemptionStart
                is RedemptionState.Complete -> SuperwallEvent.RedemptionComplete
                is RedemptionState.Fail -> SuperwallEvent.RedemptionFail
            },
        ) {
        sealed class RedemptionState {
            object Start : RedemptionState()

            object Complete : RedemptionState()

            object Fail : RedemptionState()
        }

        override val audienceFilterParams: Map<String, Any>
            get() = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> {
            val map =
                mutableMapOf(
                    "type" to type.description,
                )
            if (type.code != null) map["code"] = type.code!!
            return map.toMap()
        }
    }

    data class EnrichmentLoad(
        val state: State,
    ) : InternalSuperwallEvent(
            when (state) {
                is State.Complete ->
                    SuperwallEvent.EnrichmentComplete(
                        state.enrichment.user,
                        state.enrichment.device,
                    )

                State.Fail -> SuperwallEvent.EnrichmentFail
                State.Start -> SuperwallEvent.EnrichmentStart
            },
        ) {
        sealed class State {
            object Start : State()

            data class Complete(
                val enrichment: Enrichment,
            ) : State()

            object Fail : State()
        }

        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> =
            if (state is EnrichmentLoad.State.Complete) {
                (
                    state.enrichment.user.map {
                        "user_${it.key}" to it.value
                    } +
                        state.enrichment.device.map {
                            "device_${it.key}" to it.value
                        }
                ).toMap()
            } else {
                emptyMap<String, Any>()
            }
    }

    class ReviewRequested(
        val count: Int,
        val type: String,
    ) : InternalSuperwallEvent(SuperwallEvent.ReviewRequested(count)) {
        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): HashMap<String, Any> =
            hashMapOf(
                "count" to count,
                "type" to type,
            )
    }

    data class CustomerInfoDidChange(
        val fromCustomerInfo: CustomerInfo,
        val toCustomerInfo: CustomerInfo,
        override val audienceFilterParams: Map<String, Any> = emptyMap(),
    ) : TrackableSuperwallEvent {
        override val superwallPlacement: SuperwallEvent =
            SuperwallEvent.CustomerInfoDidChange(fromCustomerInfo, toCustomerInfo)
        override val rawName: String =
            SuperwallEvent.CustomerInfoDidChange(fromCustomerInfo, toCustomerInfo).rawName

        override val canImplicitlyTriggerPaywall: Boolean = false

        @Serializable
        private data class EntitlementsSnapshot(
            val entitlements: List<Entitlement>,
            val isPlaceholder: Boolean,
        )

        override suspend fun getSuperwallParameters(): Map<String, Any> {
            val fromSnapshot =
                EntitlementsSnapshot(
                    entitlements = fromCustomerInfo.entitlements,
                    isPlaceholder = fromCustomerInfo.isPlaceholder,
                )
            val toSnapshot =
                EntitlementsSnapshot(
                    entitlements = toCustomerInfo.entitlements,
                    isPlaceholder = toCustomerInfo.isPlaceholder,
                )

            val fromJson =
                try {
                    JsonFactory.JSON.encodeToString(fromSnapshot)
                } catch (e: Exception) {
                    Logger.debug(
                        LogLevel.error,
                        LogScope.customerInfo,
                        "Unable to serialize customer info \"from\" - ${e.message}",
                    )
                    "{}"
                }

            val toJson =
                try {
                    JsonFactory.JSON.encodeToString(toSnapshot)
                } catch (e: Exception) {
                    Logger.debug(
                        LogLevel.error,
                        LogScope.customerInfo,
                        "Unable to serialize customer info \"to\" - ${e.message}",
                    )
                    "{}"
                }

            return mapOf(
                "from" to fromJson,
                "to" to toJson,
            )
        }
    }

    data class Permission(
        val state: State,
        val permissionName: String,
        val paywallIdentifier: String,
    ) : InternalSuperwallEvent(
            SuperwallEvent.PermissionRequested(permissionName, paywallIdentifier),
        ) {
        enum class State {
            Requested,
            Granted,
            Denied,
        }

        override val superwallPlacement: SuperwallEvent
            get() =
                when (state) {
                    State.Requested ->
                        SuperwallEvent.PermissionRequested(
                            permissionName = permissionName,
                            paywallIdentifier = paywallIdentifier,
                        )
                    State.Granted ->
                        SuperwallEvent.PermissionGranted(
                            permissionName = permissionName,
                            paywallIdentifier = paywallIdentifier,
                        )
                    State.Denied ->
                        SuperwallEvent.PermissionDenied(
                            permissionName = permissionName,
                            paywallIdentifier = paywallIdentifier,
                        )
                }

        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> =
            mapOf(
                "permission_name" to permissionName,
                "paywall_identifier" to paywallIdentifier,
            )
    }

    data class PaywallPreload(
        val state: State,
        val paywallCount: Int,
    ) : InternalSuperwallEvent(
            SuperwallEvent.PaywallPreloadStart(paywallCount),
        ) {
        enum class State {
            Start,
            Complete,
        }

        override val superwallPlacement: SuperwallEvent
            get() =
                when (state) {
                    State.Start -> SuperwallEvent.PaywallPreloadStart(paywallCount)
                    State.Complete -> SuperwallEvent.PaywallPreloadComplete(paywallCount)
                }

        override val audienceFilterParams: Map<String, Any> = emptyMap()

        override suspend fun getSuperwallParameters(): Map<String, Any> =
            mapOf(
                "paywall_count" to paywallCount,
            )
    }
}
