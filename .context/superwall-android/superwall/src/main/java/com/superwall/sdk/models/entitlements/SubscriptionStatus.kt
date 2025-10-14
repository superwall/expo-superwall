package com.superwall.sdk.models.entitlements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the status of user's subscriptions and entitlements.
 *
 * This sealed class has three possible states:
 * - [Unknown]: The initial state before any subuscription status is determined
 * - [Inactive]: When the user has no active entitlements
 * - [Active]: When the user has one or more active entitlements
 */
@Serializable
sealed class SubscriptionStatus {
    val isActive: Boolean
        get() = this is Active

    /**
     * Represents an unknown entitlement status.
     * This is the initial state before any entitlement status is determined.
     */
    @Serializable
    object Unknown : SubscriptionStatus()

    /**
     * Represents an inactive entitlement status.
     * This state indicates the user has no active entitlements.
     */
    @Serializable
    object Inactive : SubscriptionStatus()

    /**
     * Represents an active entitlement status.
     * This state indicates the user has one or more active entitlements.
     *
     * @property entitlements A Set of active [Entitlement] objects belonging to the user
     */
    @Serializable
    data class Active(
        @SerialName("entitlements")
        val entitlements: Set<Entitlement>,
    ) : SubscriptionStatus()
}
