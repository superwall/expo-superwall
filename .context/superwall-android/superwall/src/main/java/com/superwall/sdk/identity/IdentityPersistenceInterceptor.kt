package com.superwall.sdk.identity

import com.superwall.sdk.misc.primitives.StateActor
import com.superwall.sdk.storage.AliasId
import com.superwall.sdk.storage.AppUserId
import com.superwall.sdk.storage.Seed
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.UserAttributes

/**
 * Auto-persists identity fields to storage whenever state changes.
 *
 * Only writes fields that actually changed, so reducers that only
 * touch `pending`/`isReady` (e.g. Configure, AssignmentsCompleted)
 * produce zero storage writes.
 */
internal object IdentityPersistenceInterceptor {
    fun install(
        actor: StateActor<IdentityContext, IdentityState>,
        storage: Storage,
    ) {
        actor.onUpdate { reducer, next ->
            val before = actor.state.value
            next(reducer)
            val after = actor.state.value

            if (after.aliasId != before.aliasId) storage.write(AliasId, after.aliasId)
            if (after.seed != before.seed) storage.write(Seed, after.seed)
            if (after.userAttributes != before.userAttributes) storage.write(UserAttributes, after.userAttributes)
            if (after.appUserId != before.appUserId) {
                if (after.appUserId != null) {
                    storage.write(AppUserId, after.appUserId)
                } else {
                    storage.delete(AppUserId)
                }
            }
        }
    }
}
