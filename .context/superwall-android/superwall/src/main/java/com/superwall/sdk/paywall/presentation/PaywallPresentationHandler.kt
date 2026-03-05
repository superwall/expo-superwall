package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason

class PaywallPresentationHandler() {
    constructor(definition: PaywallPresentationHandler.() -> Unit) : this() {
        definition()
    }

    // A block called when the paywall did present
    internal var onPresentHandler: ((PaywallInfo) -> Unit)? = null

    // A block called when the paywall did dismiss
    internal var onDismissHandler: ((PaywallInfo, PaywallResult) -> Unit)? = null

    // A block called when an error occurred while trying to present a paywall
    internal var onErrorHandler: ((Throwable) -> Unit)? = null

    // A block called when a paywall is skipped, but no error has occurred
    internal var onSkipHandler: ((PaywallSkippedReason) -> Unit)? = null

    // A block called when the paywall requests a custom callback
    internal var onCustomCallbackHandler: (suspend (CustomCallback) -> CustomCallbackResult)? = null

    // Sets the handler that will be called when the paywall did present
    fun onPresent(handler: (PaywallInfo) -> Unit) {
        onPresentHandler = handler
    }

    // Sets the handler that will be called when the paywall did dismiss
    fun onDismiss(handler: (PaywallInfo, PaywallResult) -> Unit) {
        onDismissHandler = handler
    }

    // Sets the handler that will be called when an error occurred while trying to present a paywall
    fun onError(handler: (Throwable) -> Unit) {
        onErrorHandler = handler
    }

    // Sets the handler that will be called when a paywall is skipped, but no error has occurred
    fun onSkip(handler: (PaywallSkippedReason) -> Unit) {
        onSkipHandler = handler
    }

    /**
     * Sets the handler that will be called when the paywall requests a custom callback.
     *
     * Custom callbacks allow paywalls to request arbitrary actions from the app and
     * receive results that determine which branch (onSuccess/onFailure) executes.
     *
     * @param handler A function that receives a [CustomCallback] containing the callback
     *                name and optional variables, and returns a [CustomCallbackResult]
     *                indicating success/failure with optional data.
     *
     * Example:
     * ```
     * handler.onCustomCallback { callback ->
     *     when (callback.name) {
     *         "validate_email" -> {
     *             val email = callback.variables?.get("email") as? String
     *             if (isValidEmail(email)) {
     *                 CustomCallbackResult.success(mapOf("validated" to true))
     *             } else {
     *                 CustomCallbackResult.failure(mapOf("error" to "Invalid email"))
     *             }
     *         }
     *         else -> CustomCallbackResult.failure()
     *     }
     * }
     * ```
     */
    fun onCustomCallback(handler: suspend (CustomCallback) -> CustomCallbackResult) {
        onCustomCallbackHandler = handler
    }
}
