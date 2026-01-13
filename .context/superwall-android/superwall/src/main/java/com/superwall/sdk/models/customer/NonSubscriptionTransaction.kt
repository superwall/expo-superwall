package com.superwall.sdk.models.customer

import com.superwall.sdk.models.product.Store
import com.superwall.sdk.models.serialization.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Represents a non-subscription transaction (e.g., one-time purchases) in the customer's purchase history.
 */
@Serializable
data class NonSubscriptionTransaction(
    /** The unique identifier for the transaction. */
    @SerialName("transactionId")
    val transactionId: String,
    /** The product identifier of the in-app purchase. */
    @SerialName("productId")
    val productId: String,
    /** The date that the App Store charged the user's account. */
    @Serializable(with = DateSerializer::class)
    @SerialName("purchaseDate")
    val purchaseDate: Date,
    /** Indicates whether it's a consumable in-app purchase. */
    @SerialName("isConsumable")
    val isConsumable: Boolean,
    /** Indicates whether the transaction has been revoked. */
    @SerialName("isRevoked")
    val isRevoked: Boolean,
    /** The store from which this transaction originated. */
    @SerialName("store")
    val store: Store = Store.PLAY_STORE,
) {
    companion object {
        /**
         * Creates a blank NonSubscriptionTransaction for testing or default states
         */
        fun empty(): NonSubscriptionTransaction =
            NonSubscriptionTransaction(
                transactionId = "",
                productId = "",
                purchaseDate = Date(),
                isConsumable = false,
                isRevoked = false,
                store = Store.PLAY_STORE,
            )
    }
}
