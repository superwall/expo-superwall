package com.superwall.sdk.paywall.presentation.get_paywall

import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.UserInitiatedEvent
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.delegate.PaywallViewCallback
import com.superwall.sdk.paywall.view.delegate.PaywallViewDelegateAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Throws(Throwable::class)
@Deprecated("Will be removed in the upcoming versions, use Superwall.getPaywall instead")
suspend fun Superwall.getPaywallOrThrow(
    placement: String,
    params: Map<String, Any>? = null,
    paywallOverrides: PaywallOverrides? = null,
    delegate: PaywallViewCallback,
): PaywallView =
    withContext(Dispatchers.Main) {
        val view =
            internallyGetPaywall(
                placement = placement,
                params = params,
                paywallOverrides = paywallOverrides,
                delegate = PaywallViewDelegateAdapter(kotlinDelegate = delegate),
            ).toResult().getOrThrow()

        // Note: Deviation from iOS. Unique to Android. This is also done in `InternalPresentation.kt`.
        // Ideally `InternalPresentation` would call this function to get the paywall, and `InternalPresentation.kt`
        // would only handle presentation. This cannot be done is the shared `GetPaywallComponents.kt` because it's
        // also used for getting a presentation result, and we don't want that to have side effects. Opting to
        // do at the top-most point.
        view.prepareToDisplay()

        return@withContext view
    }

@Throws(Throwable::class)
@Deprecated("Will be removed in the upcoming versions, use `PaywallBuilder` instead")
suspend fun Superwall.getPaywall(
    placement: String,
    params: Map<String, Any>? = null,
    paywallOverrides: PaywallOverrides? = null,
    delegate: PaywallViewCallback,
): Result<PaywallView> =
    withContext(Dispatchers.Main) {
        val result =
            internallyGetPaywall(
                placement = placement,
                params = params,
                paywallOverrides = paywallOverrides,
                delegate = PaywallViewDelegateAdapter(kotlinDelegate = delegate),
            )

        // Note: Deviation from iOS. Unique to Android. This is also done in `InternalPresentation.kt`.
        // Ideally `InternalPresentation` would call this function to get the paywall, and `InternalPresentation.kt`
        // would only handle presentation. This cannot be done is the shared `GetPaywallComponents.kt` because it's
        // also used for getting a presentation result, and we don't want that to have side effects. Opting to
        // do at the top-most point.
        return@withContext when (result) {
            is Either.Success -> {
                val view = result.value
                view.prepareToDisplay()
                result.toResult()
            }

            is Either.Failure -> {
                result.toResult()
            }
        }
    }

@Throws(Throwable::class)
private suspend fun Superwall.internallyGetPaywall(
    placement: String,
    params: Map<String, Any>? = null,
    paywallOverrides: PaywallOverrides? = null,
    delegate: PaywallViewDelegateAdapter,
): Either<PaywallView, Throwable> =
    withContext(Dispatchers.Main) {
        val trackableEvent =
            UserInitiatedEvent.Track(
                rawName = placement,
                canImplicitlyTriggerPaywall = false,
                customParameters = params ?: emptyMap(),
                isFeatureGatable = false,
            )
        val trackResult = track(trackableEvent)

        if (trackResult.isFailure) {
            return@withContext Either.Failure(
                trackResult.exceptionOrNull() ?: IllegalStateException("Error in tracking event"),
            )
        }
        val presentationRequest =
            dependencyContainer.makePresentationRequest(
                PresentationInfo.ExplicitTrigger(trackResult.getOrThrow().data),
                paywallOverrides = paywallOverrides,
                isPaywallPresented = false,
                type = PresentationRequestType.GetPaywall(delegate),
            )

        return@withContext getPaywall(presentationRequest)
    }
