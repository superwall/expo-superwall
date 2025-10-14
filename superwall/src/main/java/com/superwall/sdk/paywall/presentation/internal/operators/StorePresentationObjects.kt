package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Superwall
import com.superwall.sdk.paywall.presentation.LastPresentationItems
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import kotlinx.coroutines.flow.MutableSharedFlow

// / Stores the presentation request for future use.
internal suspend fun Superwall.storePresentationObjects(
    request: PresentationRequest?,
    paywallStatePublisher: MutableSharedFlow<PaywallState>,
) {
    val request = request ?: return

    val lastPaywallPresentation =
        LastPresentationItems(
            request,
            paywallStatePublisher,
        )
    presentationItems.last = lastPaywallPresentation
}
