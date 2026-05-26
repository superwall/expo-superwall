package com.superwall.sdk.misc.primitives

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Pure actor context — the minimal contract for action execution.
 *
 * Provides a [StateStore] for state reads/updates, a [CoroutineScope],
 * and a type-safe [effect] for fire-and-forget sub-action dispatch.
 *
 * SDK-specific concerns (storage, persistence) live in [BaseContext].
 */
interface StoreContext<S, Self : StoreContext<S, Self>> : StateStore<S> {
    val actor: StateActor<Self, S>
    val scope: CoroutineScope

    /** Delegate state reads to the actor. */
    override val state: StateFlow<S> get() = actor.state

    /** Apply a state reducer inline. */
    override fun update(reducer: Reducer<S>) {
        actor.update(reducer)
    }

    /**
     * Fire-and-forget dispatch of a sub-action on this context's actor.
     *
     * Type-safe: [Self] is the implementing context, matching the action's
     * receiver type. The cast is guaranteed correct by the F-bounded constraint.
     */
    @Suppress("UNCHECKED_CAST")
    fun effect(action: TypedAction<Self>) {
        actor.effect(this as Self, action)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun immediate(action: TypedAction<Self>) {
        actor.immediate(this as Self, action)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun immediateUntil(
        action: TypedAction<Self>,
        until: (S) -> Boolean,
    ) {
        actor.immediateUntil(this as Self, action, until)
    }

    fun sideEffect(what: suspend () -> Unit){
        scope.launch { what() }
    }
}
