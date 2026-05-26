package com.superwall.sdk.misc.primitives

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun <Context, State> stateActor(initial: State, scope: CoroutineScope) = StateActor<Context,State>(initial, scope)

/**
 * Holds state and provides synchronous updates + async action dispatching.
 *
 * [update] uses [MutableStateFlow.update] internally (CAS retry) —
 * concurrent updates from multiple actions are safe.
 *
 * Dispatch modes:
 * - [effect]: fire-and-forget — launches in the actor's scope.
 * - [immediateUntil]: dispatch + suspend until state matches a condition.
 *
 * ## Interceptors
 *
 * ```kotlin
 * actor.onUpdate { reducer, next ->
 *     next(reducer)  // call to proceed, skip to suppress
 * }
 *
 * actor.onAction { action, next ->
 *     next()  // call to proceed, skip to suppress
 * }
 * ```
 */

open class StateActor<Context, S>(
    initial: S,
    internal val scope: CoroutineScope,
) : StateStore<S>,
    Actor<Context, S> {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<S> = _state.asStateFlow()

    // -- Interceptor chains --------------------------------------------------

    private var updateChain: (Reducer<S>) -> Unit = { reducer ->
        _state.update { reducer.reduce(it) }
    }

    private var actionInterceptors: List<(action: Any, next: () -> Unit) -> Unit> = emptyList()

    /**
     * Async interceptors that wrap the suspend execution of each action.
     * Unlike [onAction] (which wraps the dispatch/launch), these run
     * _inside_ the coroutine and can measure wall-clock execution time.
     */
    private var asyncActionInterceptors: List<suspend (action: Any, next: suspend () -> Unit) -> Unit> = emptyList()

    /**
     * Add an update interceptor. Call `next(reducer)` to proceed,
     * or skip to suppress the update.
     */
    fun onUpdate(interceptor: (reducer: Reducer<S>, next: (Reducer<S>) -> Unit) -> Unit) {
        val previous = updateChain
        updateChain = { reducer -> interceptor(reducer, previous) }
    }

    /**
     * Add an action interceptor. Call `next()` to proceed,
     * or skip to suppress the action. Action is [Any] — cast to inspect.
     *
     * Note: `next()` launches a coroutine and returns immediately.
     * To measure action execution time, use [onActionExecution] instead.
     */
    fun onAction(interceptor: (action: Any, next: () -> Unit) -> Unit) {
        actionInterceptors = actionInterceptors + interceptor
    }

    /**
     * Add an async interceptor that wraps the action's suspend execution.
     * Runs inside the coroutine — `next()` suspends until the action completes.
     *
     * ```kotlin
     * actor.onActionExecution { action, next ->
     *     val start = System.nanoTime()
     *     next()  // suspends until the action finishes
     *     val ms = (System.nanoTime() - start) / 1_000_000
     *     println("${action::class.simpleName} took ${ms}ms")
     * }
     * ```
     */
    fun onActionExecution(interceptor: suspend (action: Any, next: suspend () -> Unit) -> Unit) {
        asyncActionInterceptors = asyncActionInterceptors + interceptor
    }

    /** Atomic state mutation using CAS retry, routed through update interceptors. */
    override fun update(reducer: Reducer<S>) {
        updateChain(reducer)
    }

    /** Fire-and-forget: launch action in actor's scope, routed through interceptors. */
    override fun <Ctx> effect(
        ctx: Ctx,
        action: TypedAction<Ctx>,
    ) {
        val execute = {
            scope.launch { executeAction(ctx, action) }
            Unit
        }
        runInterceptorChain(action, execute)
    }

    /**
     * Dispatch action and suspend until state matches [until].
     *
     * Actor-native awaiting: fire the action, observe the state transition.
     */
    override suspend fun <Ctx> immediateUntil(
        ctx: Ctx,
        action: TypedAction<Ctx>,
        until: (S) -> Boolean,
    ): S {
        effect(ctx, action)
        return state.first { until(it) }
    }

    /**
     * Dispatch action inline and suspend until it completes.
     * Goes through action interceptors. Use for cross-slice coordination
     * where the caller needs to await the action finishing.
     */
    override suspend fun <Ctx> immediate(
        ctx: Ctx,
        action: TypedAction<Ctx>,
    ) {
        var shouldExecute = true
        if (actionInterceptors.isNotEmpty()) {
            shouldExecute = false
            var chain: () -> Unit = { shouldExecute = true }
            for (i in actionInterceptors.indices.reversed()) {
                val interceptor = actionInterceptors[i]
                val next = chain
                chain = { interceptor(action, next) }
            }
            chain()
        }
        if (shouldExecute) {
            executeAction(ctx, action)
        }
    }

    /** Runs the action through async interceptors. Override to add serialization. */
    protected open suspend fun <Ctx> executeAction(
        ctx: Ctx,
        action: TypedAction<Ctx>,
    ) {
        runAsyncInterceptorChain(action) { action.execute.invoke(ctx) }
    }

    protected suspend fun runAsyncInterceptorChain(
        action: Any,
        terminal: suspend () -> Unit,
    ) {
        if (asyncActionInterceptors.isEmpty()) {
            terminal()
        } else {
            var chain: suspend () -> Unit = terminal
            for (i in asyncActionInterceptors.indices.reversed()) {
                val interceptor = asyncActionInterceptors[i]
                val next = chain
                chain = { interceptor(action, next) }
            }
            chain()
        }
    }

    protected fun runInterceptorChain(
        action: Any,
        terminal: () -> Unit,
    ) {
        if (actionInterceptors.isEmpty()) {
            terminal()
        } else {
            var chain: () -> Unit = terminal
            for (i in actionInterceptors.indices.reversed()) {
                val interceptor = actionInterceptors[i]
                val next = chain
                chain = { interceptor(action, next) }
            }
            chain()
        }
    }
}

