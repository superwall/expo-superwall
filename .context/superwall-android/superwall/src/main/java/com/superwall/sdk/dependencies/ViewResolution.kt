package com.superwall.sdk.dependencies

import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.ViewStorage

/**
 * Resolves a PaywallView by cache key, falling back to the currently active paywall view.
 */
internal fun resolvePaywallViewForKey(
    viewStore: ViewStorage,
    activeView: PaywallView?,
    key: String,
): PaywallView? {
    val byKey = viewStore.retrieveView(key) as? PaywallView?
    return byKey ?: activeView
}
