package com.superwall.sdk.paywall.presentation.get_presentation_result

import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.TrackingLogic
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.analytics.internal.trackable.UserInitiatedEvent
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.result.PresentationResult
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.HashMap

/**
 * Preemptively gets the result of registering an event.
 *
 * This helps you determine whether a particular event will present a paywall
 * in the future.
 *
 * Note that this method does not present a paywall. To do that, use
 * `register(placement:params:handler:feature:)`.
 *
 * @param placement The name of the event you want to register.
 * @param params Optional parameters you'd like to pass with your event.
 * @return A [PresentationResult] that indicates the result of registering an event.
 */
suspend fun Superwall.getPresentationResult(
    placement: String,
    params: Map<String, Any>? = null,
): Result<PresentationResult> =
    withErrorTracking {
        val event =
            UserInitiatedEvent.Track(
                rawName = placement,
                canImplicitlyTriggerPaywall = false,
                customParameters = HashMap(params ?: emptyMap()),
                isFeatureGatable = false,
            )

        return@withErrorTracking internallyGetPresentationResult(
            event,
            isImplicit = false,
        )
    }.toResult()

/**
 * Synchronously preemptively gets the result of registering an event.
 *
 * This helps you determine whether a particular event will present a paywall
 * in the future.
 *
 * Note that this method does not present a paywall. To do that, use
 * `register(placement:params:handler:feature:)`.
 *
 * Warning: This blocks the calling thread.
 *
 * @param placement The name of the event you want to register.
 * @param params Optional parameters you'd like to pass with your event.
 * @return A [PresentationResult] that indicates the result of registering an event.
 */
fun Superwall.getPresentationResultSync(
    placement: String,
    params: Map<String, Any>? = null,
): Result<PresentationResult> =
    runBlocking {
        getPresentationResult(
            placement,
            params,
        )
    }

internal suspend fun Superwall.internallyGetPresentationResult(
    event: Trackable,
    isImplicit: Boolean,
): PresentationResult {
    val eventCreatedAt = Date()
    val parameters =
        TrackingLogic.processParameters(
            trackableEvent = event,
            appSessionId = dependencyContainer.appSessionManager.appSession.id,
        )

    val eventData =
        EventData(
            name = event.rawName,
            parameters = parameters.audienceFilterParams,
            createdAt = eventCreatedAt,
        )

    val presentationRequest =
        dependencyContainer.makePresentationRequest(
            PresentationInfo.ExplicitTrigger(eventData), // Assuming a similar structure in Kotlin
            isDebuggerLaunched = false,
            isPaywallPresented = false,
            type =
                if (isImplicit) {
                    PresentationRequestType.GetImplicitPresentationResult
                } else {
                    PresentationRequestType.GetPresentationResult
                },
        )

    return getPresentationResult(presentationRequest)
}
