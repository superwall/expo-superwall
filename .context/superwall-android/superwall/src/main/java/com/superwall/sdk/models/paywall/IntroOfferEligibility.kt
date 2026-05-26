package com.superwall.sdk.models.paywall

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class IntroOfferEligibility {
    @SerialName("eligible")
    ELIGIBLE,

    @SerialName("ineligible")
    INELIGIBLE,

    @SerialName("automatic")
    AUTOMATIC,
}
