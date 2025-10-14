package com.superwall.sdk.paywall.request

import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.store.abstractions.product.StoreProduct

/**
 * A request to get a paywall.
 */
data class PaywallRequest(
    /**
     * The event data.
     */
    var eventData: EventData?,
    /**
     * The identifiers for the paywall and experiment.
     */
    val responseIdentifiers: ResponseIdentifiers,
    /**
     * Overrides within the paywall.
     */
    val overrides: Overrides,
    /**
     * If the debugger is launched when the request was created.
     */
    val isDebuggerLaunched: Boolean,
    /**
     * The source function type that created the presentation request.
     * e.g. implicit/register/getPaywall/nil
     */
    val presentationSourceType: String?,
    /**
     * The number of times to retry the request.
     */
    val retryCount: Int,
) {
    /**
     * Overrides within the paywall.
     */
    data class Overrides(
        /**
         * The products to substitute into the response.
         */
        val products: Map<String, StoreProduct>?,
        /**
         * Whether to override the displaying of a free trial.
         */
        val isFreeTrial: Boolean?,
    )
}
