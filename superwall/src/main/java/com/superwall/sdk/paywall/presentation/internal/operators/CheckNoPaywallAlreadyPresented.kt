package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Superwall
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.paywall.presentation.internal.InternalPresentationLogic
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import kotlinx.coroutines.flow.MutableSharedFlow

internal suspend fun Superwall.checkNoPaywallAlreadyPresented(
    request: PresentationRequest,
    paywallStatePublisher: MutableSharedFlow<PaywallState>,
) {
    if (request.flags.isPaywallPresented) {
        Logger.debug(
            logLevel = LogLevel.error,
            scope = LogScope.paywallPresentation,
            message = "Paywall Already Presented",
            info = mapOf("message" to "Superwall.instance.isPaywallPresented is true"),
        )
        val error =
            InternalPresentationLogic.presentationError(
                domain = "SWPresentationError",
                code = 102,
                title = "Paywall Already Presented",
                value = "You can only present one paywall at a time.",
            )
        val state: PaywallState = PaywallState.PresentationError(error)
        paywallStatePublisher.emit(state)
        throw PaywallPresentationRequestStatusReason.PaywallAlreadyPresented()
    }
}
