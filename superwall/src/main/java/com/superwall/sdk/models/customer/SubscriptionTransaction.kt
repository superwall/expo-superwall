package com.superwall.sdk.models.customer

import com.superwall.sdk.models.product.Store
import com.superwall.sdk.models.serialization.DateSerializer
import com.superwall.sdk.store.abstractions.product.receipt.LatestPeriodType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Represents a subscription transaction in the customer's purchase history.
 */
@Serializable
data class SubscriptionTransaction(
    /** The unique identifier for the transaction. */
    @SerialName("transactionId")
    val transactionId: String,
    /** The product identifier of the subscription. */
    @SerialName("productId")
    val productId: String,
    /** The date that the App Store charged the user's account. */
    @Serializable(with = DateSerializer::class)
    @SerialName("purchaseDate")
    val purchaseDate: Date,
    /** Indicates whether the subscription will renew. */
    @SerialName("willRenew")
    val willRenew: Boolean,
    /** Indicates whether the transaction has been revoked. */
    @SerialName("isRevoked")
    val isRevoked: Boolean,
    /** Indicates whether the subscription is in a billing grace period state. */
    @SerialName("isInGracePeriod")
    val isInGracePeriod: Boolean,
    /** Indicates whether the subscription is in a billing retry period state. */
    @SerialName("isInBillingRetryPeriod")
    val isInBillingRetryPeriod: Boolean,
    /** Indicates whether the subscription is active. */
    @SerialName("isActive")
    val isActive: Boolean,
    /** The date that the subscription expires.
     *
     * This is `null` if it's a non-renewing subscription. */
    @Serializable(with = DateSerializer::class)
    @SerialName("expirationDate")
    val expirationDate: Date?,
    /** The store from which this transaction originated. */
    @SerialName("store")
    val store: Store = Store.PLAY_STORE,
    /** The type of offer that applied to this transaction.
     *
     * This is `null` if no offer was applied. */
    @SerialName("offerType")
    val offerType: LatestPeriodType? = null,
    // Note: iOS Only - The subscription group identifier for this subscription.
    @SerialName("subscriptionGroupId")
    val subscriptionGroupId: String? = null,
) {
    companion object {
        /**
         * Creates a empty SubscriptionTransaction for testing or default states
         */
        fun empty(): SubscriptionTransaction =
            SubscriptionTransaction(
                transactionId = "",
                productId = "",
                purchaseDate = Date(),
                willRenew = false,
                isRevoked = false,
                isInGracePeriod = false,
                isInBillingRetryPeriod = false,
                isActive = false,
                expirationDate = null,
                store = Store.PLAY_STORE,
                offerType = null,
            )
    }
}
