package com.superwall.sdk.misc.primitives

interface Actor<Ctx, S> {
    /** Fire-and-forget action dispatch. */
    fun <Ctx> effect(
        ctx: Ctx,
        action: TypedAction<Ctx>,
    )

    /** Dispatch action inline, suspending until it completes. */
    suspend fun <Ctx> immediate(
        ctx: Ctx,
        action: TypedAction<Ctx>,
    )

    /** Dispatch action, suspending until state matches [until]. */
    suspend fun <Ctx> immediateUntil(
        ctx: Ctx,
        action: TypedAction<Ctx>,
        until: (S) -> Boolean,
    ): S
}
