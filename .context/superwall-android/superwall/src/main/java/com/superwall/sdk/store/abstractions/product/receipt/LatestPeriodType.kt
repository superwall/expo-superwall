package com.superwall.sdk.store.abstractions.product.receipt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LatestPeriodType {
    @SerialName("trial")
    TRIAL,

    @SerialName("code")
    CODE,

    @SerialName("subscription")
    SUBSCRIPTION,

    @SerialName("promotional")
    PROMOTIONAL,

    @SerialName("winback")
    WINBACK,

    @SerialName("revoked")
    REVOKED,
}
