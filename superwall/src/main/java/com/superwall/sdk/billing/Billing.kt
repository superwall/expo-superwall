package com.superwall.sdk.billing

import com.android.billingclient.api.Purchase
import com.superwall.sdk.delegate.InternalPurchaseResult
import com.superwall.sdk.dependencies.StoreTransactionFactory
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.abstractions.transactions.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow

interface Billing {
    val purchaseResults: MutableStateFlow<InternalPurchaseResult?>

    suspend fun awaitGetProducts(identifiers: Set<String>): Set<StoreProduct>

    suspend fun getLatestTransaction(factory: StoreTransactionFactory): StoreTransaction?

    suspend fun queryAllPurchases(): List<Purchase>
}
