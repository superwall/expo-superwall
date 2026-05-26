package com.superwall.sdk.store.abstractions.product.receipt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LatestSubscriptionState {
    @SerialName("grace_period")
    GRACE_PERIOD,

    @SerialName("expired")
    EXPIRED,

    @SerialName("subscribed")
    SUBSCRIBED,

    @SerialName("billing_retry")
    BILLING_RETRY,

    @SerialName("revoked")
    REVOKED,

    @SerialName("unknown")
    UNKNOWN,
}
