@file:Suppress("ktlint:standard:no-blank-line-in-list")

package com.superwall.sdk.models.entitlements

import android.annotation.SuppressLint
import com.superwall.sdk.models.product.Store
import com.superwall.sdk.store.abstractions.product.receipt.LatestPeriodType
import com.superwall.sdk.store.abstractions.product.receipt.LatestSubscriptionState
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

/**
 * An entitlement that represents a subscription tier in your app.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Entitlement(
    /**
     * The identifier for the entitlement.
     */
    @SerialName("identifier")
    val id: String,
    /**
     * The type of entitlement.
     */
    @SerialName("type")
    val type: Type = Type.SERVICE_LEVEL,
    // region: Device-enriched properties (added on device after retrieving from server)

    /**
     * Indicates whether there is any active, non-revoked, transaction for this entitlement.
     */
    @SerialName("isActive")
    val isActive: Boolean = false,
    /**
     * All product identifiers that map to the entitlement.
     */
    @SerialName("productIds")
    val productIds: Set<String> = emptySet(),
    /**
     * The product identifier of the latest transaction to unlock this entitlement.
     *
     * If one or more lifetime products unlock this entitlement, the `latestProductId` will always be the product identifier of the first lifetime product.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement or if it was manually granted from Superwall.
     */
    @SerialName("latestProductId")
    val latestProductId: String? = null,
    /**
     * The purchase date of the first transaction that unlocked this entitlement.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement or if it was manually granted from Superwall.
     */
    @SerialName("startsAt")
    @Contextual
    val startsAt: Date? = null,
    /**
     * The date that the entitlement was last renewed.
     *
     * This could be `null` if:
     *   - There aren't any transactions that unlock this entitlement.
     *   - It was the first purchase.
     *   - If the entitlement belongs to a non-renewing subscription or non-consumable product.
     */
    @SerialName("renewedAt")
    @Contextual
    val renewedAt: Date? = null,
    /**
     * The expiry date of the last transaction that unlocked this entitlement.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement, if it was manually granted from Superwall or
     * if a lifetime product unlocked this entitlement.
     */
    @SerialName("expiresAt")
    @Contextual
    val expiresAt: Date? = null,
    /**
     * Indicates whether the entitlement is active for a lifetime due to the purchase of a non-consumable.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement.
     */
    @SerialName("isLifetime")
    val isLifetime: Boolean? = null,
    /**
     * Indicates whether the last subscription transaction associated with this entitlement will auto renew.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement.
     */
    @SerialName("willRenew")
    val willRenew: Boolean? = null,
    /**
     * The state of the last subscription transaction associated with the
     * entitlement.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement or if it was manually granted from Superwall.
     */
    @SerialName("state")
    val state: LatestSubscriptionState? = null,
    /**
     * The type of offer that applies to the last subscription transaction that
     * unlocks this entitlement.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement or if it was manually granted from Superwall.
     *
     * Note: This is only non-`null` on Android API 21+.
     */
    @SerialName("offerType")
    val offerType: LatestPeriodType? = null,
    /**
     * The store where the product that unlocked this entitlement was purchased.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement or if it was manually granted from Superwall.
     */
    @SerialName("store")
    val store: Store? = null,
) {
    @Serializable
    enum class Type(
        val raw: String,
    ) {
        /**
         *  A Superwall service level entitlement.
         */
        @SerialName("SERVICE_LEVEL")
        SERVICE_LEVEL("SERVICE_LEVEL"),
    }

    /**
     * Indicates whether the last subscription transaction associated with this
     * entitlement was revoked.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement.
     *
     * Note: In Android, revocation is tracked at the transaction level rather than
     * as a subscription state, so this property returns `null` for now.
     */
    val isRevoked: Boolean?
        get() = null // Android doesn't have a direct revoked state in LatestSubscriptionState

    /**
     * The `Date` at which the subscription renews, if at all.
     *
     * This is `null` if it won't renew or isn't active.
     */
    val renewsAt: Date?
        get() {
            if (!isActive) return null
            if (willRenew != true) return null
            return expiresAt
        }

    /**
     * Indicates whether the last subscription transaction associated with this
     * entitlement is in a billing grace period state.
     *
     * This is `null` if there aren't any transactions that unlock this entitlement or if it was manually granted from Superwall.
     */
    val isInGracePeriod: Boolean?
        get() = state?.let { it == LatestSubscriptionState.GRACE_PERIOD }

    /**
     * Convenience constructor for creating an entitlement with just an ID.
     */
    constructor(id: String) : this(id = id, type = Type.SERVICE_LEVEL, isActive = true)
    constructor(id: String, type: Type) : this(id = id, type = type, isActive = true)

    companion object {
        /**
         * Creates a stub entitlement for testing purposes.
         */
        fun stub(): Entitlement =
            Entitlement(
                id = "test",
                type = Type.SERVICE_LEVEL,
            )
    }
}
