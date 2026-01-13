package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.dependencies.DependencyContainer
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import com.superwall.sdk.paywall.request.PaywallRequest
import com.superwall.sdk.paywall.request.ResponseIdentifiers
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.webview.webViewExists
import kotlinx.coroutines.flow.MutableSharedFlow

internal suspend fun getPaywallView(
    request: PresentationRequest,
    rulesOutcome: RuleEvaluationOutcome,
    debugInfo: Map<String, Any>,
    paywallStatePublisher: MutableSharedFlow<PaywallState>? = null,
    dependencyContainer: DependencyContainer,
): Result<PaywallView> {
    val experiment =
        getExperiment(
            request = request,
            rulesOutcome = rulesOutcome,
            debugInfo = debugInfo,
            paywallStatePublisher = paywallStatePublisher,
            storage = dependencyContainer.storage,
        )

    val responseIdentifiers =
        ResponseIdentifiers(
            paywallId = experiment.variant.paywallId,
            experiment = experiment,
        )

    val paywallRequest =
        dependencyContainer.makePaywallRequest(
            eventData = request.presentationInfo.eventData,
            responseIdentifiers = responseIdentifiers,
            overrides =
                PaywallRequest.Overrides(
                    products = request.paywallOverrides?.productsByName,
                    isFreeTrial = request.presentationInfo.freeTrialOverride,
                ),
            isDebuggerLaunched = request.flags.isDebuggerLaunched,
            presentationSourceType = request.presentationSourceType,
        )
    return try {
        val isForPresentation =
            request.flags.type != PresentationRequestType.GetImplicitPresentationResult &&
                request.flags.type != PresentationRequestType.GetPresentationResult
        val delegate = request.flags.type.paywallViewDelegateAdapter

        val webviewExists = webViewExists()
        if (webviewExists) {
            val res =
                dependencyContainer.paywallManager
                    .getPaywallView(
                        request = paywallRequest,
                        isForPresentation = isForPresentation,
                        isPreloading = false,
                        delegate = delegate,
                    ).toResult()
            if (res.isSuccess) {
                return res
            } else {
                throw res.exceptionOrNull()!!
            }
        } else {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.paywallPresentation,
                message =
                    "Paywalls cannot be presented because the Android System WebView has been disabled" +
                        " by the user.",
            )
            Result.failure(PaywallPresentationRequestStatusReason.NoPaywallView())
        }
    } catch (e: Throwable) {
        Result.failure(presentationFailure(e, debugInfo, paywallStatePublisher))
    }
}

private suspend fun presentationFailure(
    error: Throwable,
    debugInfo: Map<String, Any>,
    paywallStatePublisher: MutableSharedFlow<PaywallState>?,
): Throwable {
    Logger.debug(
        logLevel = LogLevel.error,
        scope = LogScope.paywallPresentation,
        message = "Error Getting Paywall View",
        info = debugInfo,
        error = error,
    )
    paywallStatePublisher?.emit(PaywallState.PresentationError(error))
    return PaywallPresentationRequestStatusReason.NoPaywallView()
}
