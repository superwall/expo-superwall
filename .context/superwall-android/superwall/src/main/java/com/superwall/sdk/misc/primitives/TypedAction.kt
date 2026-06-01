package com.superwall.sdk.misc.primitives

/**
 * An async operation scoped to a [Ctx] that provides all dependencies.
 *
 * Actions do the real work: network calls, storage writes, tracking.
 * They call [StateActor.update] with pure [Reducer]s to mutate state.
 *
 * Actions are launched via [StateActor.action] and run concurrently.
 */
interface TypedAction<Ctx> {
    val execute: suspend Ctx.() -> Unit
}
