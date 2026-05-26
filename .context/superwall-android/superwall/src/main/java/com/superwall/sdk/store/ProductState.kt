package com.superwall.sdk.store

import com.superwall.sdk.store.abstractions.product.StoreProduct
import kotlinx.coroutines.CompletableDeferred

sealed class ProductState {
    class Loading(
        val deferred: CompletableDeferred<StoreProduct> = CompletableDeferred(),
    ) : ProductState()

    data class Loaded(
        val product: StoreProduct,
    ) : ProductState()

    data class Error(
        val error: Throwable,
    ) : ProductState()
}
