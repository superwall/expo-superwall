package com.superwall.superapp.handlers

import com.superwall.sdk.paywall.presentation.PaywallPresentationHandler

sealed class HandlerEvent {
    object OnPresent : HandlerEvent()

    object OnDismiss : HandlerEvent()

    object OnError : HandlerEvent()

    object OnSkip : HandlerEvent()
}

class TestHandler(
    private val presentationHandler: PaywallPresentationHandler,
) {
    private val _events = mutableListOf<HandlerEvent>()
    val events: List<HandlerEvent> = _events

    fun clearEvents() {
        _events.clear()
    }

    // Configure handlers on the presentation handler
    fun setupHandlers() {
        presentationHandler.onPresent { paywallInfo ->
            _events.add(HandlerEvent.OnPresent)
        }

        presentationHandler.onDismiss { paywallInfo, result ->
            _events.add(HandlerEvent.OnDismiss)
        }

        presentationHandler.onError { throwable ->
            _events.add(HandlerEvent.OnError)
        }

        presentationHandler.onSkip { reason ->
            _events.add(HandlerEvent.OnSkip)
        }
    }
}
