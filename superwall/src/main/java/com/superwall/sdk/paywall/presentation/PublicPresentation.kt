package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.TrackingLogic
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.UserInitiatedEvent
import com.superwall.sdk.misc.fold
import com.superwall.sdk.models.config.FeatureGatingBehavior
import com.superwall.sdk.paywall.presentation.internal.InternalPresentationLogic
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.dismiss
import com.superwall.sdk.paywall.presentation.internal.internallyPresent
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult.Declined
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult.Purchased
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult.Restored
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Dismisses the presented paywall, if one exists.
 */
suspend fun Superwall.dismiss() =
    withContext(Dispatchers.Main) {
        val completionSignal = CompletableDeferred<Unit>()

        withErrorTracking {
            paywallView?.let {
                dismiss(paywallView = it, result = PaywallResult.Declined()) {
                    completionSignal.complete(Unit)
                }
            } ?: completionSignal.complete(Unit)
        }

        completionSignal.await()
    }

/**
 * Dismisses the presented paywall synchronously, if one exists.
 * Warning: This blocks the calling thread.
 */
fun Superwall.dismissSync() {
    runBlocking {
        dismiss()
    }
}

/**
 * Dismisses the presented paywall, if it exists, in order to present a different one.
 */
suspend fun Superwall.dismissForNextPaywall() =
    withContext(Dispatchers.Main) {
        val completionSignal = CompletableDeferred<Unit>()

        withErrorTracking {
            paywallView?.let {
                dismiss(
                    paywallView = it,
                    result = PaywallResult.Declined(),
                    closeReason = PaywallCloseReason.ForNextPaywall,
                ) {
                    completionSignal.complete(Unit)
                }
            } ?: completionSignal.complete(Unit)
        }
        completionSignal.await()
    }

/**
 * Dismisses the presented paywall synchronously, if it exists, in order to present a different one.
 * Warning: This blocks the calling thread.
 */
fun Superwall.dismissSyncForNextPaywall() =
    runBlocking {
        dismissForNextPaywall()
    }

/**
 * Registers an event to access a feature. When the event is added to a campaign on the Superwall dashboard, it can show a paywall.
 *
 * This shows a paywall to the user when: An event you provide is added to a campaign on the [Superwall Dashboard](https://superwall.com/dashboard);
 * the user matches a rule in the campaign; and the user doesn't have an active subscription.
 *
 * Before using this method, you'll first need to create a campaign and add the event to the campaign on the [Superwall Dashboard](https://superwall.com/dashboard).
 *
 * The paywall shown to the user is determined by the rules defined in the campaign. When a user is assigned a paywall within a rule,
 * they will continue to see that paywall unless you remove the paywall from the rule or reset assignments to the paywall.
 *
 * @param placement The name of the event you wish to register.
 * @param params Optional parameters you'd like to pass with your event. These can be referenced within the rules of your campaign.
 *               Keys beginning with `$` are reserved for Superwall and will be dropped. Values can be any JSON encodable value, URLs or Dates.
 *               Arrays and dictionaries as values are not supported at this time, and will be dropped. Defaults to `null`.
 * @param handler An optional handler whose functions provide status updates for a paywall. Defaults to `null`.
 * @param feature A completion block containing a feature that you wish to paywall. Access to this block is remotely configurable via the
 *                [Superwall Dashboard](https://superwall.com/dashboard). If the paywall is set to _Non Gated_, this will be called when
 *                the paywall is dismissed or if the user is already paying. If the paywall is _Gated_, this will be called only if the user
 *                is already paying or if they begin paying. If no paywall is configured, this gets called immediately. This will not be called
 *                in the event of an error, which you can detect via the `handler`.
 */
@JvmOverloads
fun Superwall.register(
    placement: String,
    params: Map<String, Any>? = null,
    handler: PaywallPresentationHandler? = null,
    feature: (() -> Unit)? = null,
) {
    internallyRegister(placement, params, handler, feature)
}

private fun Superwall.internallyRegister(
    placement: String,
    params: Map<String, Any>? = null,
    handler: PaywallPresentationHandler? = null,
    completion: (() -> Unit)? = null,
) {
    val publisher = MutableSharedFlow<PaywallState>()
    val collectionWillStart = CompletableDeferred<Unit>()

    CoroutineScope(Dispatchers.Main).launch {
        collectionWillStart.complete(Unit)

        publisher.collect { state ->
            withErrorTracking {
                when (state) {
                    is PaywallState.Presented -> {
                        handler?.onPresentHandler?.invoke(state.paywallInfo)
                    }

                    is PaywallState.Dismissed -> {
                        val (paywallInfo, paywallResult) = state
                        handler?.onDismissHandler?.invoke(paywallInfo, paywallResult)
                        when (paywallResult) {
                            is Purchased, is Restored -> {
                                completion?.invoke()
                            }

                            is Declined -> {
                                val closeReason = paywallInfo.closeReason
                                val featureGating = paywallInfo.featureGatingBehavior
                                if (closeReason.stateShouldComplete && featureGating == FeatureGatingBehavior.NonGated) {
                                    completion?.invoke()
                                }
                                if (closeReason == PaywallCloseReason.WebViewFailedToLoad && featureGating == FeatureGatingBehavior.Gated) {
                                    val error =
                                        InternalPresentationLogic.presentationError(
                                            domain = "SWKPresentationError",
                                            code = 106,
                                            title = "Webview Failed",
                                            value = "Trying to present gated paywall but the webview could not load.",
                                        )
                                    handler?.onErrorHandler?.invoke(error)
                                } else {
                                }
                            }
                        }
                    }

                    is PaywallState.Skipped -> {
                        val (reason) = state
                        handler?.onSkipHandler?.invoke(reason)
                        completion?.invoke()
                    }

                    is PaywallState.PresentationError -> {
                        handler?.onErrorHandler?.invoke(state.error)
                    }
                }
            }
        }
    }

    serialTaskManager.addTask {
        withErrorTracking {
            collectionWillStart.await()
            trackAndPresentPaywall(
                placement = placement,
                params = params,
                paywallOverrides = null,
                isFeatureGatable = completion != null,
                publisher = publisher,
            )
        }
    }
}

private suspend fun Superwall.trackAndPresentPaywall(
    placement: String,
    params: Map<String, Any>? = null,
    paywallOverrides: PaywallOverrides? = null,
    isFeatureGatable: Boolean,
    publisher: MutableSharedFlow<PaywallState>,
) {
    try {
        TrackingLogic.checkNotSuperwallEvent(placement)
    } catch (e: Throwable) {
        return
    }

    val trackableEvent =
        UserInitiatedEvent.Track(
            rawName = placement,
            canImplicitlyTriggerPaywall = false,
            customParameters = params ?: emptyMap(),
            isFeatureGatable = isFeatureGatable,
        )

    withErrorTracking {
        val trackResult = track(trackableEvent)
        if (trackResult.isFailure) {
            throw trackResult.exceptionOrNull() ?: Exception("Unknown error")
        }

        val presentationRequest =
            dependencyContainer.makePresentationRequest(
                PresentationInfo.ExplicitTrigger(trackResult.getOrThrow().data),
                paywallOverrides,
                isPaywallPresented = isPaywallPresented,
                type = PresentationRequestType.Presentation,
            )

        internallyPresent(presentationRequest, publisher)
    }.fold({}, onFailure = {
        publisher.emit(PaywallState.PresentationError(it))
    })
}
