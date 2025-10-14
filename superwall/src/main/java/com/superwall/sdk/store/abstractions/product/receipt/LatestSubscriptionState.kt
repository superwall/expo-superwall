package com.superwall.sdk.store.abstractions.product.receipt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LatestSubscriptionState {
    @SerialName("GRACE_PERIOD")
    GRACE_PERIOD,

    @SerialName("EXPIRED")
    EXPIRED,

    @SerialName("SUBSCRIBED")
    SUBSCRIBED,

    @SerialName("UNKNOWN")
    UNKNOWN,
}
