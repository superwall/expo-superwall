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
        transactionDate = Date(transaction.purchaseTime), // Replace with correct mapping
        originalTransactionIdentifier = transaction.orderId, // Replace with correct mapping
        state = StoreTransactionState.Purchased, // Replace with correct mapping
        storeTransactionId = transaction.orderId, // Replace with correct mapping
        originalTransactionDate = Date(transaction.purchaseTime), // Replace with correct mapping
        webOrderLineItemID = null, // Replace with correct mapping
        appBundleId = null, // Replace with correct mapping
        subscriptionGroupId = null, // Replace with correct mapping
        isUpgraded = null, // Replace with correct mapping
        expirationDate = null, // Replace with correct mapping
        offerId = null, // Replace with correct mapping
        revocationDate = null, // Replace with correct mapping
        appAccountToken = null, // Replace with correct mapping
        payment = StorePayment(transaction), // Replace with correct mapping
        purchaseToken = transaction.purchaseToken,
        signature = transaction.signature,
    )
}
