package com.superwall.sdk.store.abstractions.transactions

import com.android.billingclient.api.Purchase
import com.superwall.sdk.models.serialization.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class GoogleBillingPurchaseTransaction(
    // Omit from serialization
    @Transient
    var underlyingSK2Transaction: Purchase? = null,
    @Serializable(with = DateSerializer::class)
    @SerialName("transaction_date")
    override val transactionDate: Date?,
    @SerialName("original_transaction_identifier")
    override val originalTransactionIdentifier: String?,
    @SerialName("state")
    override val state: StoreTransactionState,
    @SerialName("store_transaction_id")
    override val storeTransactionId: String?,
    @Serializable(with = DateSerializer::class)
    @SerialName("original_transaction_date")
    override val originalTransactionDate: Date?,
    @SerialName("web_order_line_item_id")
    override val webOrderLineItemID: String?,
    @SerialName("app_bundle_id")
    override val appBundleId: String?,
    @SerialName("subscription_group_id")
    override val subscriptionGroupId: String?,
    @SerialName("is_upgraded")
    override val isUpgraded: Boolean?,
    @Serializable(with = DateSerializer::class)
    @SerialName("expiration_date")
    override val expirationDate: Date?,
    @SerialName("offer_id")
    override val offerId: String?,
    @Serializable(with = DateSerializer::class)
    @SerialName("revocation_date")
    override val revocationDate: Date?,
    @SerialName("app_account_token")
    override val appAccountToken: String?,
    @SerialName("purchase_token")
    override val purchaseToken: String,
    override var payment: StorePayment,
    override val signature: String?,
) : StoreTransactionType {
    constructor(transaction: Purchase) : this(
        underlyingSK2Transaction = transaction,
        transactionDate = Date(transaction.purchaseTime),
        originalTransactionIdentifier = transaction.orderId,
        state = StoreTransactionState.Purchased,
        storeTransactionId = transaction.orderId,
        originalTransactionDate = Date(transaction.purchaseTime),
        webOrderLineItemID = null, // Not available in transaction, kept as iOS complement.
        appBundleId = null, // Not available in transaction, kept as iOS complement.
        subscriptionGroupId = null, // Base plan ID, filled after purchase from product.
        isUpgraded = null, // Not available in transaction, kept as iOS complement.
        expirationDate = null, // Expiration date, filled after purchase from product.
        offerId = null, // Offer ID, filled after purchase from product
        revocationDate = null, // Not available in transaction, kept as iOS complement
        appAccountToken = null, // iOS property only, kept as complement
        payment = StorePayment(transaction),
        purchaseToken = transaction.purchaseToken,
        signature = transaction.signature,
    )
}
