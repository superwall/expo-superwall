package com.superwall.sdk.billing

import com.superwall.sdk.store.abstractions.product.StoreProduct

interface GetStoreProductsCallback {
    /**
     * Will be called after products have been fetched successfully
     *
     * @param [storeProducts] The list of [StoreProduct] that have been able to be successfully fetched from the store.
     * Not found products will be ignored.
     */
    @JvmSuppressWildcards
    fun onReceived(storeProducts: Set<StoreProduct>)

    /**
     * Will be called after the purchase has completed with error
     *
     * @param error A [Error] containing the reason for the failure when fetching the [StoreProduct]
     */
    fun onError(error: BillingError)
}
