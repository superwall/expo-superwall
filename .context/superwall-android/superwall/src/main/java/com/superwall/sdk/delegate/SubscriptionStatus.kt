package com.superwall.sdk.delegate

import kotlinx.serialization.Serializable

// / An enum representing the subscription status of the user.
@Serializable
enum class SubscriptionStatus {
    // / The user has an active subscription.
    ACTIVE,

    // / The user doesn't have an active subscription.
    INACTIVE,

    // / The subscription status is unknown.
    UNKNOWN,

    ;

    override fun toString(): String =
        when (this) {
            ACTIVE -> "ACTIVE"
            INACTIVE -> "INACTIVE"
            UNKNOWN -> "UNKNOWN"
        }
}
