package com.superwall.sdk.store.abstractions.product.receipt

import com.superwall.sdk.storage.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

/**
 * A serializable representation of an EntitlementTransaction that can be persisted to storage.
 * This allows us to maintain transaction history even after purchases expire or are no longer
 * returned by Google Play.
 */
@Serializable
data class StoredEntitlementTransaction(
    @SerialName("product_id")
    override val productId: String,
    @SerialName("transaction_id")
    override val transactionId: String,
    @SerialName("purchase_date")
    @Serializable(with = DateSerializer::class)
    override val purchaseDate: Date,
    @SerialName("original_purchase_date")
    @Serializable(with = DateSerializer::class)
    override val originalPurchaseDate: Date,
    @SerialName("expiration_date")
    @Serializable(with = DateSerializer::class)
    override val expirationDate: Date?,
    @SerialName("is_revoked")
    override val isRevoked: Boolean,
    @SerialName("product_type")
    override val productType: EntitlementTransactionType,
    @SerialName("will_renew")
    override val willRenew: Boolean,
    @SerialName("renewed_at")
    @Serializable(with = DateSerializer::class)
    override val renewedAt: Date?,
    @SerialName("is_in_grace_period")
    override val isInGracePeriod: Boolean,
    @SerialName("is_in_billing_retry_period")
    override val isInBillingRetryPeriod: Boolean,
    @SerialName("is_active")
    override val isActive: Boolean,
) : EntitlementTransaction {
    companion object {
        /**
         * Creates a StoredEntitlementTransaction from any EntitlementTransaction.
         */
        fun from(transaction: EntitlementTransaction): StoredEntitlementTransaction =
            StoredEntitlementTransaction(
                productId = transaction.productId,
                transactionId = transaction.transactionId,
                purchaseDate = transaction.purchaseDate,
                originalPurchaseDate = transaction.originalPurchaseDate,
                expirationDate = transaction.expirationDate,
                isRevoked = transaction.isRevoked,
                productType = transaction.productType,
                willRenew = transaction.willRenew,
                renewedAt = transaction.renewedAt,
                isInGracePeriod = transaction.isInGracePeriod,
                isInBillingRetryPeriod = transaction.isInBillingRetryPeriod,
                isActive = transaction.isActive,
            )

        /**
         * Updates a stored transaction's active status based on current state.
         * If the transaction is no longer returned by Google Play but was previously active,
         * mark it as inactive.
         */
        fun withUpdatedActiveStatus(
            stored: StoredEntitlementTransaction,
            isCurrentlyActive: Boolean,
        ): StoredEntitlementTransaction =
            stored.copy(
                isActive = isCurrentlyActive,
                // If subscription was auto-renewing but is now inactive, it won't renew
                willRenew = if (!isCurrentlyActive) false else stored.willRenew,
            )
    }
}

/**
 * Container for storing transaction history.
 * Maps transaction ID to the stored transaction for efficient lookup and deduplication.
 */
@Serializable
data class UserTransactionHistory(
    @SerialName("transactions")
    val transactions: Map<String, StoredEntitlementTransaction> = emptyMap(),
) {
    /**
     * Merges new transactions with existing history.
     * - Updates existing transactions with fresh data from Google Play
     * - Marks transactions not in current purchases as inactive
     * - Adds new transactions
     *
     * @param currentTransactions Transactions currently returned by Google Play
     * @return Updated history with merged transactions
     */
    fun mergeWith(currentTransactions: List<EntitlementTransaction>): UserTransactionHistory {
        val currentByTxnId = currentTransactions.associateBy { it.transactionId }
        val updatedTransactions = transactions.toMutableMap()

        // Update existing transactions - mark as inactive if not in current purchases
        transactions.forEach { (txnId, stored) ->
            val current = currentByTxnId[txnId]
            if (current != null) {
                // Transaction still exists in Google Play - update with fresh data
                updatedTransactions[txnId] = StoredEntitlementTransaction.from(current)
            } else {
                // Transaction no longer returned by Google Play - mark as inactive
                updatedTransactions[txnId] =
                    StoredEntitlementTransaction.withUpdatedActiveStatus(
                        stored = stored,
                        isCurrentlyActive = false,
                    )
            }
        }

        // Add new transactions not in history
        currentTransactions.forEach { transaction ->
            if (!updatedTransactions.containsKey(transaction.transactionId)) {
                updatedTransactions[transaction.transactionId] =
                    StoredEntitlementTransaction.from(transaction)
            }
        }

        return UserTransactionHistory(transactions = updatedTransactions)
    }

    /**
     * Returns all transactions as a list, sorted by purchase date (newest first).
     */
    fun allTransactions(): List<StoredEntitlementTransaction> = transactions.values.sortedByDescending { it.purchaseDate }

    /**
     * Returns only active transactions.
     */
    fun activeTransactions(): List<StoredEntitlementTransaction> =
        transactions.values.filter { it.isActive }.sortedByDescending { it.purchaseDate }
}
