package com.superwall.sdk.paywall.view

data class WebCheckoutSession(
    val checkoutId: String,
    val productIdentifier: String,
    val paywallIdentifier: String,
    val experimentVariantId: Int,
    val presentedByEventName: String,
    val store: String,
)
