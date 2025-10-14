package com.superwall.sdk.store.abstractions.transactions

import com.android.billingclient.api.BillingClient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

interface StoreTransactionType {
    val transactionDate: Date?
    val originalTransactionIdentifier: String?
    val state: StoreTransactionState
    val storeTransactionId: String?
    val payment: StorePayment?

    // MARK: iOS 15 only properties
    val originalTransactionDate: Date?
    val webOrderLineItemID: String?
    val appBundleId: String?
    val subscriptionGroupId: String?
    val isUpgraded: Boolean?
    val expirationDate: Date?
    val offerId: String?
    val revocationDate: Date?
    val appAccountToken: String?
    val purchaseToken: String

    val signature: String?
}

// Custom serializer
@Serializer(forClass = StoreTransactionState::class)
object StoreTransactionStateSerializer : KSerializer<StoreTransactionState> {
    override fun serialize(
        encoder: Encoder,
        value: StoreTransactionState,
    ) {
        // Transform the object to a lowercase string representation
        val str =
            when (value) {
                is StoreTransactionState.Purchasing -> "purchasing"
                is StoreTransactionState.Purchased -> "purchased"
                is StoreTransactionState.Failed -> "failed"
                is StoreTransactionState.Restored -> "restored"
                is StoreTransactionState.Deferred -> "deferred"
            }
        encoder.encodeString(str)
    }

    override fun deserialize(decoder: Decoder): StoreTransactionState {
        // Convert back from string to object
        return when (val str = decoder.decodeString()) {
            "purchasing" -> StoreTransactionState.Purchasing
            "purchased" -> StoreTransactionState.Purchased
            "failed" -> StoreTransactionState.Failed
            "restored" -> StoreTransactionState.Restored
            "deferred" -> StoreTransactionState.Deferred
            else -> throw SerializationException("Unknown string value: $str")
        }
    }
}

// Annotated sealed class
@Serializable(with = StoreTransactionStateSerializer::class)
sealed class StoreTransactionState {
    @Serializable
    object Purchasing : StoreTransactionState() // When purchase is initiated

    @Serializable
    object Purchased : StoreTransactionState() // When purchase is successful

    @Serializable
    object Failed : StoreTransactionState() // When purchase has failed

    @Serializable
    object Restored : StoreTransactionState() // When a previous purchase has been restored

    @Serializable
    object Deferred : StoreTransactionState() // Optional: When a purchase is pending some external action

    companion object {
        fun from(purchaseResult: Int): StoreTransactionState =
            when (purchaseResult) {
                BillingClient.BillingResponseCode.OK -> Purchased
                BillingClient.BillingResponseCode.USER_CANCELED -> Purchasing
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> Restored
                // Add other cases based on the specific responses you need to handle
                else -> Failed
            }
    }
}
