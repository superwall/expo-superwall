package com.superwall.sdk.store.abstractions.product.receipt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LatestPeriodType {
    @SerialName("TRIAL")
    TRIAL,

    @SerialName("SUBSCRIPTION")
    SUBSCRIPTION,

    @SerialName("PROMOTIONAL")
    PROMOTIONAL,

    @SerialName("REVOKED")
    REVOKED,
}
