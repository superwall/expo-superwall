package com.superwall.sdk.misc.primitives

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * A [StateActor] that serializes all action execution via a FIFO [Channel].
 *
 * Actions dispatched with [effect] or [immediate] execute in dispatch order
 * and never run concurrently — matching the behavior of a single-threaded
 * queue dispatcher.
 *
 * Re-entrant: actions that call [immediate] on the same actor execute
 * inline (they're already inside the consumer loop).
 */
class SequentialActor<Context, S>(
    initial: S,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : StateActor<Context, S>(initial, scope) {

    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    private class OwnerElement(val actor: Any) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<OwnerElement>
    }

    init {
        // Single consumer — FIFO ordering guaranteed.
        scope.launch(OwnerElement(this@SequentialActor)) {
            for (work in queue) {
                work()
            }
        }
    }

    override suspend fun <Ctx> executeAction(
        ctx: Ctx,
        action: TypedAction<Ctx>,
    ) {
        if (coroutineContext[OwnerElement]?.actor === this) {
            // Re-entrant: already inside the consumer loop — run inline.
            super.executeAction(ctx, action)
        } else {
            // External call (immediate from outside): enqueue and wait.
            val done = CompletableDeferred<Unit>()
            queue.trySend {
                try {
                    super.executeAction(ctx, action)
                    done.complete(Unit)
                } catch (e: Throwable) {
                    done.completeExceptionally(e)
                }
            }
            done.await()
        }
    }

    /** Fire-and-forget: enqueue action in FIFO order, return immediately. */
    override fun <Ctx> effect(
        ctx: Ctx,
        action: TypedAction<Ctx>,
    ) {
        runInterceptorChain(action) {
            queue.trySend {
                runAsyncInterceptorChain(action) {
                    action.execute.invoke(ctx)
                }
            }
        }
    }

    /** Closes the queue. The consumer loop exits after draining remaining items. */
    fun close() {
        queue.close()
    }
}
