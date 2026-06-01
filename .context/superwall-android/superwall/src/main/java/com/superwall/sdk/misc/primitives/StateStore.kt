package com.superwall.sdk.misc.primitives

import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for reading, updating, and dispatching on state.
 *
 * Both [StateActor] (root) and [ScopedState] (projection) implement this.
 * Contexts depend on [StateStore] — they never see the concrete type.
 */
interface StateStore<S> {
    val state: StateFlow<S>

    /** Atomic state mutation. */
    fun update(reducer: Reducer<S>)
}
