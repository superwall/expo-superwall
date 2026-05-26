package com.superwall.sdk.misc.primitives

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger

/**
 * Installs debug interceptors on an [StateActor] that log every action dispatch
 * and state update, building a traceable timeline of what happened and why.
 *
 * Usage:
 * ```kotlin
 * val actor = Actor(initialState, scope)
 * DebugInterceptor.install(actor, name = "Identity")
 * ```
 *
 * Output example:
 * ```
 * [Identity] action  → Identify(userId=user_123)
 * [Identity] update  → Identify | 2ms
 * [Identity] update  → AttributesMerged | 0ms
 * [Identity] action  → ResolveSeed(userId=user_123)
 * [Identity] update  → SeedResolved | 1ms
 * ```
 */
object DebugInterceptor {
    /**
     * Install debug logging on an [StateActor].
     *
     * @param actor The actor to instrument.
     * @param name A human-readable label for log output (e.g. "Identity", "Config").
     * @param scope The [LogScope] to log under. Defaults to [LogScope.superwallCore].
     * @param level The [LogLevel] to log at. Defaults to [LogLevel.debug].
     */
    fun <Ctx, S> install(
        actor: StateActor<Ctx, S>,
        name: String,
        scope: LogScope = LogScope.superwallCore,
        level: LogLevel = LogLevel.debug,
    ) {
        actor.onUpdate { reducer, next ->
            val reducerName = reducer.labelOf()
            val start = System.nanoTime()
            next(reducer)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            Logger.debug(
                logLevel = level,
                scope = scope,
                message = "Interceptor: [$name] update → $reducerName | ${elapsedMs}ms",
            )
        }

        actor.onAction { action, next ->
            val actionName = action.labelOf()
            Logger.debug(
                logLevel = level,
                scope = scope,
                message = "Interceptor: [$name] action → $actionName",
            )
            next()
        }

        actor.onActionExecution { action, next ->
            val actionName = action.labelOf()
            val start = System.nanoTime()
            next()
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            Logger.debug(
                logLevel = level,
                scope = scope,
                message = "Interceptor: [$name] action ✓ $actionName | ${elapsedMs}ms",
            )
        }
    }

    /**
     * Derive a readable label from an action or reducer instance.
     *
     * For sealed-class members like `IdentityState.Updates.Identify(userId=foo)`,
     * this returns `"Identify(userId=foo)"` — the simple class name plus toString
     * for data classes, or just the simple name for objects.
     */
    private fun Any.labelOf(): String {
        val cls = this::class
        val simple = cls.simpleName ?: cls.qualifiedName ?: "anonymous"
        // Data classes have a useful toString; objects don't — just use the name.
        val str = toString()
        return if (str.startsWith(simple)) str else simple
    }
}
