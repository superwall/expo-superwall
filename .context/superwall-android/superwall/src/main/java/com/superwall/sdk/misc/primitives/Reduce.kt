package com.superwall.sdk.misc.primitives

/**
 * A pure state transform — no side effects, no dispatch.
 *
 * Reducers are `(S) -> S`. They describe HOW state changes.
 * All side effects (storage, network, tracking) belong in [TypedAction]s.
 */
interface Reducer<S> {
    val reduce: (S) -> S
}
