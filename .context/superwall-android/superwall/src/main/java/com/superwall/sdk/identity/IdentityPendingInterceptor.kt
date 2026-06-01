package com.superwall.sdk.identity

import com.superwall.sdk.misc.primitives.StateActor

internal object IdentityPendingInterceptor {
    fun install(actor: StateActor<IdentityContext, IdentityState>) {
        actor.onAction { action, next ->
            when (action) {
                is IdentityState.Actions.Identify -> actor.update(IdentityState.Updates.BeginIdentify(action.userId))
                is IdentityState.Actions.MergeAttributes -> actor.update(IdentityState.Updates.BeginAttributes)
                is IdentityState.Actions.FullReset -> actor.update(IdentityState.Updates.BeginReset)
            }
            next()
        }

        actor.onActionExecution { action, next ->
            try {
                next()
            } finally {
                when (action) {
                    is IdentityState.Actions.Identify -> actor.update(IdentityState.Updates.EndIdentify(action.userId))
                    is IdentityState.Actions.MergeAttributes -> actor.update(IdentityState.Updates.EndAttributes)
                    is IdentityState.Actions.FullReset -> actor.update(IdentityState.Updates.EndReset)
                }
            }
        }
    }
}
