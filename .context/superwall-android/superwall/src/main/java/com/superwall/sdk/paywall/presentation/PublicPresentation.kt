package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.TrackingLogic
import com.superwall.sdk.analytics.internal.TrackingResult
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.UserInitiatedEvent
import com.superwall.sdk.misc.SerialTaskManager
import com.superwall.sdk.misc.fold
import com.superwall.sdk.misc.onError
import com.superwall.sdk.models.config.FeatureGatingBehavior
import com.superwall.sdk.paywall.presentation.internal.InternalPresentationLogic
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
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
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal data class DismissParams(
    val dispatcher: CoroutineDispatcher,
    val paywallView: PaywallView?,
    val result: PaywallResult,
    val closeReason: PaywallCloseReason,
    val dismissAction: suspend (PaywallView, PaywallResult, PaywallCloseReason, (() -> Unit)?) -> Unit,
)

internal suspend fun dismissPaywall(params: DismissParams) {
    withContext(params.dispatcher) {
        val completionSignal = CompletableDeferred<Unit>()

        withErrorTracking {
            params.paywallView?.let { view ->
                params.dismissAction(view, params.result, params.closeReason) {
                    completionSignal.complete(Unit)
                }
            } ?: run {
                completionSignal.complete(Unit)
            }
        }.onError {
            it.printStackTrace()
        }

        completionSignal.await()
    }
}

internal data class TrackAndPresentContext(
    val track: suspend (UserInitiatedEvent.Track) -> Result<TrackingResult>,
    val makePresentationRequest: (PresentationInfo, PaywallOverrides?, Boolean, PresentationRequestType) -> PresentationRequest,
    val isPaywallPresented: () -> Boolean,
    val present: suspend (PresentationRequest, MutableSharedFlow<PaywallState>) -> Unit,
)

internal data class TrackAndPresentRequest(
    val placement: String,
    val params: Map<String, Any>?,
    val paywallOverrides: PaywallOverrides?,
    val isFeatureGatable: Boolean,
    val publisher: MutableSharedFlow<PaywallState>,
)

internal suspend fun performTrackAndPresent(
    context: TrackAndPresentContext,
    request: TrackAndPresentRequest,
) {
    try {
        TrackingLogic.checkNotSuperwallEvent(request.placement)
    } catch (e: Throwable) {
        return
    }

    val trackableEvent =
        UserInitiatedEvent.Track(
            rawName = request.placement,
            canImplicitlyTriggerPaywall = false,
            customParameters = request.params ?: emptyMap(),
            isFeatureGatable = request.isFeatureGatable,
        )

    withErrorTracking {
        val trackResult = context.track(trackableEvent)
        if (trackResult.isFailure) {
            throw trackResult.exceptionOrNull() ?: Exception("Unknown error")
        }

        val presentationRequest =
            context.makePresentationRequest(
                PresentationInfo.ExplicitTrigger(trackResult.getOrThrow().data),
                request.paywallOverrides,
                context.isPaywallPresented(),
                PresentationRequestType.Presentation,
            )

        context.present(presentationRequest, request.publisher)
    }.fold({}, onFailure = {
        request.publisher.emit(PaywallState.PresentationError(it))
    })
}

internal data class RegisterContext(
    val dispatcher: CoroutineDispatcher,
    val collectionScope: CoroutineScope,
    val serialTaskManager: SerialTaskManager,
    val trackAndPresentContext: TrackAndPresentContext,
)

internal data class RegisterRequest(
    val placement: String,
    val params: Map<String, Any>?,
    val handler: PaywallPresentationHandler?,
    val completion: (() -> Unit)?,
    val paywallOverrides: PaywallOverrides?,
)

internal fun registerPaywall(
    context: RegisterContext,
    request: RegisterRequest,
) {
    val publisher = MutableSharedFlow<PaywallState>()
    val collectionWillStart = CompletableDeferred<Unit>()

    context.collectionScope.launch {
        collectionWillStart.complete(Unit)

        publisher.collect { state ->
            withErrorTracking {
                when (state) {
                    is PaywallState.Presented -> {
                        request.handler?.onPresentHandler?.invoke(state.paywallInfo)
                    }

                    is PaywallState.Dismissed -> {
                        val (paywallInfo, paywallResult) = state
                        request.handler?.onDismissHandler?.invoke(paywallInfo, paywallResult)
                        when (paywallResult) {
                            is Purchased, is Restored -> {
                                request.completion?.invoke()
                            }

                            is Declined -> {
                                val closeReason = paywallInfo.closeReason
                                val featureGating = paywallInfo.featureGatingBehavior
                                if (closeReason.stateShouldComplete && featureGating == FeatureGatingBehavior.NonGated) {
                                    request.completion?.invoke()
                                }
                                if (closeReason == PaywallCloseReason.WebViewFailedToLoad && featureGating == FeatureGatingBehavior.Gated) {
                                    val error =
                                        InternalPresentationLogic.presentationError(
                                            domain = "SWKPresentationError",
                                            code = 106,
                                            title = "Webview Failed",
                                            value = "Trying to present gated paywall but the webview could not load.",
                                        )
                                    request.handler?.onErrorHandler?.invoke(error)
                                }
                            }
                        }
                    }

                    is PaywallState.Skipped -> {
                        val (reason) = state
                        request.handler?.onSkipHandler?.invoke(reason)
                        request.completion?.invoke()
                    }

                    is PaywallState.PresentationError -> {
                        request.handler?.onErrorHandler?.invoke(state.error)
                    }
                }
            }
        }
    }

    context.serialTaskManager.addTask {
        withErrorTracking {
            collectionWillStart.await()
            performTrackAndPresent(
                context = context.trackAndPresentContext,
                request =
                    TrackAndPresentRequest(
                        placement = request.placement,
                        params = request.params,
                        paywallOverrides = request.paywallOverrides,
                        isFeatureGatable = request.completion != null,
                        publisher = publisher,
                    ),
            )
        }
    }
}

/**
 * Dismisses the presented paywall, if one exists.
 */
suspend fun Superwall.dismiss() =
    dismissPaywall(
        DismissParams(
            dispatcher = Dispatchers.Main,
            paywallView = paywallView,
            result = PaywallResult.Declined(),
            closeReason = PaywallCloseReason.SystemLogic,
            dismissAction = { view, result, reason, completion ->
                dismiss(paywallView = view, result = result, closeReason = reason, completion = completion)
            },
        ),
    )

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
    dismissPaywall(
        DismissParams(
            dispatcher = Dispatchers.Main,
            paywallView = paywallView,
            result = PaywallResult.Declined(),
            closeReason = PaywallCloseReason.ForNextPaywall,
            dismissAction = { view, result, reason, completion ->
                dismiss(paywallView = view, result = result, closeReason = reason, completion = completion)
            },
        ),
    )

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
    val registerContext =
        RegisterContext(
            dispatcher = Dispatchers.Main,
            collectionScope = CoroutineScope(Dispatchers.Main),
            serialTaskManager = serialTaskManager,
            trackAndPresentContext =
                TrackAndPresentContext(
                    track = { event -> track(event) },
                    makePresentationRequest = { info, overrides, isPresented, type ->
                        dependencyContainer.makePresentationRequest(
                            info,
                            overrides,
                            isPaywallPresented = isPresented,
                            type = type,
                        )
                    },
                    isPaywallPresented = { isPaywallPresented },
                    present = { request, publisher -> internallyPresent(request, publisher) },
                ),
        )

    val registerRequest =
        RegisterRequest(
            placement = placement,
            params = params,
            handler = handler,
            completion = completion,
            paywallOverrides = null,
        )

    registerPaywall(registerContext, registerRequest)
}
