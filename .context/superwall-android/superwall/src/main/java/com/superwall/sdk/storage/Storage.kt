package com.superwall.sdk.storage

interface Storage {
    fun <T> read(storable: Storable<T>): T?

    fun <T : Any> write(
        storable: Storable<T>,
        data: T,
    )

    fun <T : Any> delete(storable: Storable<T>) {
    }

    fun clean()
}
