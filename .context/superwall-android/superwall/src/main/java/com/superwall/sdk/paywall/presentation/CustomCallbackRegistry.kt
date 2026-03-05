package com.superwall.sdk.paywall.presentation

import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for custom callback handlers associated with paywall presentations.
 *
 * Handlers are stored by paywall identifier and should be cleaned up when
 * the paywall is dismissed to prevent memory leaks.
 */
class CustomCallbackRegistry {
    private val handlers = ConcurrentHashMap<String, suspend (CustomCallback) -> CustomCallbackResult>()

    /**
     * Registers a custom callback handler for a paywall.
     *
     * @param paywallIdentifier The unique identifier of the paywall
     * @param handler The callback handler to register
     */
    fun register(
        paywallIdentifier: String,
        handler: suspend (CustomCallback) -> CustomCallbackResult,
    ) {
        handlers[paywallIdentifier] = handler
    }

    /**
     * Unregisters the custom callback handler for a paywall.
     * Should be called when the paywall is dismissed.
     *
     * @param paywallIdentifier The unique identifier of the paywall
     */
    fun unregister(paywallIdentifier: String) {
        handlers.remove(paywallIdentifier)
    }

    /**
     * Gets the custom callback handler for a paywall, if registered.
     *
     * @param paywallIdentifier The unique identifier of the paywall
     * @return The handler, or null if not registered
     */
    fun getHandler(paywallIdentifier: String): (suspend (CustomCallback) -> CustomCallbackResult)? = handlers[paywallIdentifier]

    /**
     * Clears all registered handlers.
     */
    fun clear() {
        handlers.clear()
    }
}
