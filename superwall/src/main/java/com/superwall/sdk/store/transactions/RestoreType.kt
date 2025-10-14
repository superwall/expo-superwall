package com.superwall.sdk.store.transactions

import com.superwall.sdk.store.abstractions.transactions.StoreTransaction

sealed class RestoreType {
    data class ViaPurchase(
        val transaction: StoreTransaction?,
    ) : RestoreType()

    object ViaRestore : RestoreType()
}
