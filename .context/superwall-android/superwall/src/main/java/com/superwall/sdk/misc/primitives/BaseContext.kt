package com.superwall.sdk.misc.primitives

import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.storage.Storable
import com.superwall.sdk.storage.Storage
import kotlinx.coroutines.launch

/**
 * SDK-level actor context — extends [StoreContext] with storage helpers.
 *
 * All Superwall domain contexts (IdentityContext, ConfigContext) extend this.
 */
interface BaseContext<S, Self : BaseContext<S, Self>> : StoreContext<S, Self> {
    val storage: Storage

    val tracker: suspend (TrackableSuperwallEvent) -> Unit

    /** Persist a value to storage. */
    fun <T : Any> persist(
        storable: Storable<T>,
        value: T,
    ) {
        storage.write(storable, value)
    }

    fun <T : Any> read(storable: Storable<T>): Result<T> =
        storage.read(storable)?.let {
            Result.success(it)
        } ?: Result.failure(IllegalArgumentException("Not found"))

    /** Delete a value from storage. */
    fun delete(storable: Storable<*>) {
        @Suppress("UNCHECKED_CAST")
        storage.delete(storable as Storable<Any>)
    }

    fun track(event: TrackableSuperwallEvent) {
        scope.launch { tracker(event) }
    }
}
