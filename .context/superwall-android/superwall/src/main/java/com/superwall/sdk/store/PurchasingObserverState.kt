package com.superwall.sdk.store

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

sealed class PurchasingObserverState {
    /**
     * Tracks a beginning of a purchase flow for a product, equalt to Transaction Start event.
     * @param product The product that is being purchased.
     * */
    class PurchaseWillBegin(
        val product: ProductDetails,
    ) : PurchasingObserverState()

    /**
     * Tracks a successful purchase flow for a product, equal to Transaction Success event.
     * @param result The result of the purchase flow.
     * @param purchases The list of purchases that were made.
     */
    class PurchaseResult(
        val result: BillingResult,
        val purchases: List<Purchase>?,
    ) : PurchasingObserverState()

    /**
     * Tracks a failed purchase flow for a product, equal to Transaction Fail event.
     * @param product The product that was being purchased.
     * @param error The error that caused the purchase to fail.
     */
    class PurchaseError(
        val product: ProductDetails,
        val error: Throwable,
    ) : PurchasingObserverState()
}
