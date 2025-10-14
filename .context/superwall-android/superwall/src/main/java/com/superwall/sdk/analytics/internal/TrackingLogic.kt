package com.superwall.sdk.analytics.internal

import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEvents
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.paywall.PaywallURL
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.storage.core_data.convertFromJsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.net.URI
import java.util.*

sealed class TrackingLogic {
    sealed class ImplicitTriggerOutcome {
        object TriggerPaywall : ImplicitTriggerOutcome()

        object DeepLinkTrigger : ImplicitTriggerOutcome()

        object DisallowedEventAsTrigger : ImplicitTriggerOutcome()

        object DontTriggerPaywall : ImplicitTriggerOutcome()

        object ClosePaywallThenTriggerPaywall : ImplicitTriggerOutcome()
    }

    companion object {
        suspend fun processParameters(
            trackableEvent: Trackable,
            appSessionId: String,
        ): TrackingParameters =
            withContext(Dispatchers.IO) {
                val superwallParameters = trackableEvent.getSuperwallParameters().toMutableMap()
                superwallParameters["app_session_id"] = appSessionId

                val dirtyAudienceFilterParams = trackableEvent.audienceFilterParams
                val eventName = trackableEvent.rawName

                val delegateParams: MutableMap<String, Any> = mutableMapOf("is_superwall" to true)

                // Add a special property if it's a superwall event
                val isStandardEvent = trackableEvent is TrackableSuperwallEvent

                val audienceFilterParams: MutableMap<String, Any> =
                    mutableMapOf(
                        "\$is_standard_event" to isStandardEvent,
                        "\$event_name" to eventName,
                        "event_name" to eventName,
                    )

                // Filter then assign Superwall parameters
                superwallParameters.forEach { (key, value) ->
                    clean(value)?.let {
                        val keyWithDollar = "$$key"
                        audienceFilterParams[keyWithDollar] = it

                        // no $ for delegate methods
                        delegateParams[key] = it
                    }
                }

                // Filter then assign custom parameters
                dirtyAudienceFilterParams.forEach { (key, value) ->
                    clean(value)?.let {
                        if (key.startsWith("$")) {
                            // Log dropping key due to $ signs not allowed
                        } else {
                            delegateParams[key] = it
                            audienceFilterParams[key] = it
                        }
                    }
                }

                return@withContext TrackingParameters(delegateParams, audienceFilterParams)
            }

        fun isNotDisabledVerboseEvent(
            event: Trackable,
            disableVerboseEvents: Boolean?,
            isSandbox: Boolean,
        ): Boolean {
            val disableVerboseEvents = disableVerboseEvents ?: return true
            if (isSandbox) {
                return true
            }

            if (event is InternalSuperwallEvent.PresentationRequest) {
                return !disableVerboseEvents
            }

            (event as? InternalSuperwallEvent.PaywallLoad)?.let {
                return when (it.state) {
                    is InternalSuperwallEvent.PaywallLoad.State.Start,
                    is InternalSuperwallEvent.PaywallLoad.State.Complete,
                    -> !disableVerboseEvents

                    else -> true
                }
            }

            if (event is InternalSuperwallEvent.ShimmerLoad) {
                return !disableVerboseEvents
            }

            (event as? InternalSuperwallEvent.PaywallProductsLoad)?.let {
                return when (it.state) {
                    is InternalSuperwallEvent.PaywallProductsLoad.State.Start,
                    is InternalSuperwallEvent.PaywallProductsLoad.State.Complete,
                    -> !disableVerboseEvents

                    else -> true
                }
            }

            (event as? InternalSuperwallEvent.PaywallWebviewLoad)?.let {
                return when (it.state) {
                    is InternalSuperwallEvent.PaywallWebviewLoad.State.Start,
                    is InternalSuperwallEvent.PaywallWebviewLoad.State.Complete,
                    -> !disableVerboseEvents

                    else -> true
                }
            }

            return true
        }

        @OptIn(ExperimentalSerializationApi::class)
        private fun clean(input: Any?): Any? =
            input?.let { value ->
                when (value) {
                    is List<*> -> null
                    is LinkedHashMap<*, *> -> value.mapValues { clean(it.value) }.filterValues { it != null }.toMap()
                    is Map<*, *> -> value.mapValues { clean(it.value) }.filterValues { it != null }.toMap()
                    is String -> value
                    is Int, is Float, is Double, is Long, is Boolean -> value
                    is JsonElement -> value.convertFromJsonElement()
                    else -> {
                        try {
                            Json.encodeToString(value)
                            value
                        } catch (e: SerializationException) {
                            when (value) {
                                is LocalDateTime -> value.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
                                is URI -> value.toString()
                                is PaywallURL -> value.toString()
                                else -> null
                            }
                        }
                    }
                }
            }

        @Throws(Exception::class)
        fun checkNotSuperwallEvent(event: String) {
            // Try to create a SuperwallEvents event from the event string
            val superwallEvent =
                SuperwallEvents.values().find { superwallEvent ->
                    superwallEvent.rawName == event
                }
            if (superwallEvent != null) {
                // Log error saying do not track an event with the same name as a SuperwallEvent
                throw Exception("Do not track an event with the same name as a SuperwallEvent")
            }
        }

        fun canTriggerPaywall(
            event: Trackable,
            triggers: Set<String>,
            paywallView: PaywallView?,
        ): ImplicitTriggerOutcome {
            if (event is TrackableSuperwallEvent && event.superwallPlacement.rawName == SuperwallEvents.DeepLink.rawName) {
                return ImplicitTriggerOutcome.DeepLinkTrigger
            }

            if (!triggers.contains(event.rawName)) {
                Logger.debug(
                    LogLevel.debug,
                    LogScope.all,
                    "!! canTriggerPaywall: triggers.contains(event.rawName) ${event.rawName} $triggers",
                )
                return ImplicitTriggerOutcome.DontTriggerPaywall
            }

            val notAllowedReferringEventNames: Set<String> =
                setOf(
                    SuperwallEvents.TransactionAbandon.rawName,
                    SuperwallEvents.TransactionFail.rawName,
                    SuperwallEvents.PaywallDecline.rawName,
                    SuperwallEvents.CustomPlacement.rawName,
                )

            val referringEventName = paywallView?.info?.presentedByEventWithName
            if (referringEventName != null) {
                if (notAllowedReferringEventNames.contains(referringEventName)) {
                    Logger.debug(
                        LogLevel.debug,
                        LogScope.all,
                        "!! canTriggerPaywall: notAllowedReferringEventNames.contains(referringEventName) $referringEventName",
                    )
                    return ImplicitTriggerOutcome.DontTriggerPaywall
                }
            }

            if (event is TrackableSuperwallEvent) {
                return when (event.superwallPlacement.backingEvent) {
                    SuperwallEvents.TransactionAbandon,
                    SuperwallEvents.TransactionFail,
                    SuperwallEvents.SurveyResponse,
                    SuperwallEvents.PaywallDecline,
                    SuperwallEvents.CustomPlacement,
                    -> ImplicitTriggerOutcome.ClosePaywallThenTriggerPaywall

                    else -> ImplicitTriggerOutcome.TriggerPaywall
                }
            }

            if (paywallView != null) {
                Logger.debug(
                    LogLevel.debug,
                    LogScope.all,
                    "!! canTriggerPaywall: paywallViewController != null",
                )
                return ImplicitTriggerOutcome.DontTriggerPaywall
            }

            return ImplicitTriggerOutcome.TriggerPaywall
        }
    }
}
