package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

internal class PresentationItems {
    private val queue = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(queue)

    var last: LastPresentationItems?
        get() = runBlocking(queue) { _last }
        set(newValue) {
            scope.launch {
                _last = newValue
            }
        }
    private var _last: LastPresentationItems? = null

    var paywallInfo: PaywallInfo?
        get() = runBlocking(queue) { _paywallInfo }
        set(newValue) {
            scope.launch {
                _paywallInfo = newValue
            }
        }
    private var _paywallInfo: PaywallInfo? = null

    fun reset() {
        scope.launch {
            _last = null
            _paywallInfo = null
        }
    }
}

// Items involved in the last successful paywall presentation request.
data class LastPresentationItems(
    // The last paywall presentation request.
    val request: PresentationRequest,
    // The last state publisher.
    val statePublisher: MutableSharedFlow<PaywallState>,
)
