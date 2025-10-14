package com.superwall.sdk.store.transactions

import com.superwall.sdk.store.abstractions.product.StoreProduct

sealed class TransactionError : Throwable() {
    data class Pending(
        override val message: String,
    ) : TransactionError()

    data class Failure(
        override val message: String,
        val product: StoreProduct,
    ) : TransactionError()
}
