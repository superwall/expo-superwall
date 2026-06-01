package com.superwall.sdk.identity

import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.primitives.Reducer
import com.superwall.sdk.misc.primitives.TypedAction
import com.superwall.sdk.misc.sha256MappedToRange
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.storage.AliasId
import com.superwall.sdk.storage.AppUserId
import com.superwall.sdk.storage.DidTrackFirstSeen
import com.superwall.sdk.storage.Seed
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.UserAttributes
import com.superwall.sdk.web.WebPaywallRedeemer

internal object Keys {
    const val APP_USER_ID = "appUserId"
    const val ALIAS_ID = "aliasId"
    const val SEED = "seed"
}

data class IdentityState(
    val appUserId: String? = null,
    val aliasId: String = IdentityLogic.generateAlias(),
    val seed: Int = IdentityLogic.generateSeed(),
    val userAttributes: Map<String, Any> = emptyMap(),
    val phase: Phase = Phase.Pending(setOf(Pending.Configuration)),
    val appInstalledAtString: String = "",
) {
    sealed class Pending {
        object Configuration : Pending()
        data class Identification(val id: String) : Pending()
        object Attributes : Pending()
        object Reset : Pending()
        object Seed : Pending()
        object Assignments : Pending()
    }

    sealed class Phase {
        data class Pending(val items: Set<IdentityState.Pending>) : Phase()
        object Ready : Phase()
    }

    val isReady: Boolean get() = phase is Phase.Ready

    val userId: String get() = appUserId ?: aliasId

    val isLoggedIn: Boolean get() = appUserId != null

    /**
     * User attributes enriched with the current identity fields.
     *
     * Matches iOS behavior: `appUserId` is only included when the user has
     * been explicitly identified via `identify()`. For anonymous users the
     * key is absent from the map — callers that need "some user identifier"
     * should use [userId] instead (which falls back to aliasId).
     *
     * `aliasId` is always present because every session has one.
     */
    val enrichedAttributes: Map<String, Any>
        get() =
            userAttributes.toMutableMap().apply {
                appUserId?.let { put(Keys.APP_USER_ID, it) }
                put(Keys.ALIAS_ID, aliasId)
            }

    val pending: Set<Pending>
        get() = (phase as? Phase.Pending)?.items ?: emptySet()

    val hasPendingIdentityResolution: Boolean
        get() =
            pending.any { pending ->
                pending is Pending.Identification ||
                        pending is Pending.Attributes ||
                        pending is Pending.Reset ||
                        pending is Pending.Seed ||
                        pending is Pending.Assignments
            }

    fun resolve(item: Pending): IdentityState {
        val current = (phase as? Phase.Pending)?.items ?: return this
        val next = current - item
        return copy(phase = if (next.isEmpty()) Phase.Ready else Phase.Pending(next))
    }

    internal sealed class Updates(
        override val reduce: (IdentityState) -> IdentityState,
    ) : Reducer<IdentityState> {
        data class Identify(
            val userId: String,
            val restoreAssignments: Boolean,
        ) : Updates({ state ->

            if (userId == state.appUserId) {
                state
            } else {
                val base =
                    if (state.appUserId != null) {
                        IdentityState(
                            appInstalledAtString = state.appInstalledAtString,
                            phase = Phase.Ready,
                        )
                    } else {
                        state
                    }

                val merged =
                    IdentityLogic.mergeAttributes(
                        newAttributes =
                            mapOf(
                                Keys.APP_USER_ID to userId,
                                Keys.ALIAS_ID to base.aliasId,
                                Keys.SEED to base.seed,
                            ),
                        oldAttributes = base.userAttributes,
                        appInstalledAtString = state.appInstalledAtString,
                    )

                val existing = (state.phase as? Phase.Pending)?.items.orEmpty()
                base.copy(
                    appUserId = userId,
                    userAttributes = merged,
                    phase = Phase.Pending(existing + buildSet {
                        add(Pending.Seed)
                        if (restoreAssignments) add(Pending.Assignments)
                    }),
                )
            }
        })

        data class SeedResolved(
            val seed: Int,
        ) : Updates({ state ->
            val merged =
                IdentityLogic.mergeAttributes(
                    newAttributes =
                        mapOf(
                            Keys.APP_USER_ID to state.userId,
                            Keys.ALIAS_ID to state.aliasId,
                            Keys.SEED to seed,
                        ),
                    oldAttributes = state.userAttributes,
                    appInstalledAtString = state.appInstalledAtString,
                )

            state
                .copy(seed = seed, userAttributes = merged)
                .resolve(Pending.Seed)
        })

        object SeedSkipped : Updates({ state ->
            state.resolve(Pending.Seed)
        })

        data class AttributesMerged(
            val attrs: Map<String, Any?>,
        ) : Updates({ state ->
            val merged =
                IdentityLogic.mergeAttributes(
                    newAttributes = attrs,
                    oldAttributes = state.userAttributes,
                    appInstalledAtString = state.appInstalledAtString,
                )
            state.copy(userAttributes = merged)
        })

        object AssignmentsCompleted : Updates({ state ->
            state.resolve(Pending.Assignments)
        })

        data class BeginIdentify(val id: String) : Updates({ state ->
            val existing = (state.phase as? Phase.Pending)?.items ?: emptySet()
            state.copy(phase = Phase.Pending(existing + Pending.Identification(id)))
        })

        data class EndIdentify(val id: String) : Updates({ state ->
            state.resolve(Pending.Identification(id))
        })

        object BeginAttributes : Updates({ state ->
            val existing = (state.phase as? Phase.Pending)?.items ?: emptySet()
            state.copy(phase = Phase.Pending(existing + Pending.Attributes))
        })

        object EndAttributes : Updates({ state ->
            state.resolve(Pending.Attributes)
        })

        object BeginReset : Updates({ state ->
            val existing = (state.phase as? Phase.Pending)?.items ?: emptySet()
            state.copy(phase = Phase.Pending(existing + Pending.Reset))
        })

        object EndReset : Updates({ state ->
            state.resolve(Pending.Reset)
        })

        data class Configure(
            val needsAssignments: Boolean,
        ) : Updates({ state ->
            // Resolve the Configuration item, optionally add Assignments,
            // but preserve any existing pending items (e.g. Seed from a
            // concurrent identify() that started before config was fetched).
            val existing = (state.phase as? Phase.Pending)?.items ?: emptySet()
            val next =
                (existing - Pending.Configuration) +
                        (if (needsAssignments) setOf(Pending.Assignments) else emptySet())
            if (next.isEmpty()) {
                state.copy(phase = Phase.Ready)
            } else {
                state.copy(phase = Phase.Pending(next))
            }
        })

        object Reset : Updates({ state ->
            val fresh = IdentityState(appInstalledAtString = state.appInstalledAtString)
            val merged =
                IdentityLogic.mergeAttributes(
                    newAttributes =
                        mapOf(
                            Keys.ALIAS_ID to fresh.aliasId,
                            Keys.SEED to fresh.seed,
                        ),
                    oldAttributes = emptyMap(),
                    appInstalledAtString = state.appInstalledAtString,
                )
            val existing = (state.phase as? Phase.Pending)?.items.orEmpty()
            val nextPhase =
                if (existing.isEmpty()) {
                    Phase.Ready
                } else {
                    Phase.Pending(existing)
                }
            fresh.copy(
                userAttributes = merged,
                phase = nextPhase,
            )
        })

    }

    internal sealed class Actions(
        override val execute: suspend IdentityContext.() -> Unit,
    ) : TypedAction<IdentityContext> {
        data class Configure(
            val neverCalledStaticConfig: Boolean,
        ) : Actions({
            val isFirstAppOpen = !(storage.read(DidTrackFirstSeen) ?: false)
            val needsAssignments =
                IdentityLogic.shouldGetAssignments(
                    isLoggedIn = actor.state.value.isLoggedIn,
                    neverCalledStaticConfig = neverCalledStaticConfig,
                    isFirstAppOpen = isFirstAppOpen,
                )
            update(Updates.Configure(needsAssignments = needsAssignments))
            if (needsAssignments) {
                effect(FetchAssignments)
            }
        })

        data class Identify(
            val userId: String,
            val options: IdentityOptions?,
        ) : Actions({
            val sanitized = IdentityLogic.sanitize(userId)
            if (sanitized.isNullOrEmpty()) {
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.identityManager,
                    message = "The provided userId was null or empty.",
                )
            } else if (sanitized != state.value.appUserId) {
                val wasLoggedIn = state.value.appUserId != null

                // Reset other managers BEFORE updating state so storage.reset() doesn't wipe the new IDs
                if (wasLoggedIn) {
                    completeReset()
                    immediate(Reset)
                }

                // Update state (pure) — persistence handled by interceptor
                update(Updates.Identify(sanitized, options?.restorePaywallAssignments == true))

                val newState = state.value
                immediate(
                    IdentityChanged(
                        sanitized,
                        newState.aliasId,
                        options?.restorePaywallAssignments,
                    ),
                )
            }
        })

        data class IdentityChanged(
            val id: String,
            val alias: String,
            val restoreAssignments: Boolean?,
        ) : Actions({
            track(InternalSuperwallEvent.IdentityAlias())

            // Track user_attributes after identity change.
            // Old code did this via saveIds() → _mergeUserAttributes(shouldTrackMerge=true).
            val current = state.value
            track(
                InternalSuperwallEvent.Attributes(
                    appInstalledAtString = current.appInstalledAtString,
                    audienceFilterParams = HashMap(current.userAttributes),
                ),
            )

            effect(ResolveSeed(id))
            effect(CheckWebEntitlements)
            sdkContext.reevaluateTestMode(id, alias)

            // Fetch assignments — inline if restoring, fire-and-forget otherwise
            if (restoreAssignments == true) {
                immediate(FetchAssignments)
            } else {
                effect(FetchAssignments)
            }
        })

        data class ResolveSeed(
            val userId: String,
        ) : Actions({
            try {
                val config = sdkContext.awaitConfig()
                if (config != null && config.featureFlags.enableUserIdSeed) {
                    userId.sha256MappedToRange()?.let { mapped ->
                        update(Updates.SeedResolved(mapped))
                        // Track user_attributes after seed change.
                        // Old code did this via saveIds() → _mergeUserAttributes(shouldTrackMerge=true).
                        val current = state.value
                        track(
                            InternalSuperwallEvent.Attributes(
                                appInstalledAtString = current.appInstalledAtString,
                                audienceFilterParams = HashMap(current.userAttributes),
                            ),
                        )
                    } ?: update(Updates.SeedSkipped)
                } else {
                    update(Updates.SeedSkipped)
                }
            } catch (_: Exception) {
                update(Updates.SeedSkipped)
            }
        })

        object FetchAssignments : Actions({
            try {
                sdkContext.fetchAssignments()
            } finally {
                update(Updates.AssignmentsCompleted)
            }
        })

        object CheckWebEntitlements : Actions({
            webPaywallRedeemer().redeem(WebPaywallRedeemer.RedeemType.Existing)
        })

        data class MergeAttributes(
            val attrs: Map<String, Any?>,
            val shouldTrackMerge: Boolean = true,
            val shouldNotify: Boolean = false,
        ) : Actions({
            update(Updates.AttributesMerged(attrs))
            if (shouldTrackMerge) {
                val current = state.value
                track(
                    InternalSuperwallEvent.Attributes(
                        appInstalledAtString = current.appInstalledAtString,
                        audienceFilterParams = HashMap(current.userAttributes),
                    ),
                )
            }
            if (shouldNotify) {
                effect(NotifyUserChange(state.value.userAttributes))
            }
        })

        data class NotifyUserChange(
            val attributes: Map<String, Any>,
        ) : Actions({
            notifyUserChange?.invoke(attributes)
        })

        object Reset : Actions({
            update(Updates.Reset)
            // Track user_attributes with the intermediate reset state during re-identify.
            // Old code did this via _reset() → saveIds() → _mergeUserAttributes(shouldTrackMerge=true).
            val current = state.value
            track(
                InternalSuperwallEvent.Attributes(
                    appInstalledAtString = current.appInstalledAtString,
                    audienceFilterParams = HashMap(current.userAttributes),
                ),
            )
        })

        /** Matches iOS behavior where identitySubject is set to false during the reset window. */
        object FullReset : Actions({
            update(Updates.Reset)         // identity not ready
            // Track user_attributes with the new (reset) identity.
            // Old code did this via _reset() → saveIds() → _mergeUserAttributes(shouldTrackMerge=true).
            val current = state.value
            track(
                InternalSuperwallEvent.Attributes(
                    appInstalledAtString = current.appInstalledAtString,
                    audienceFilterParams = HashMap(current.userAttributes),
                ),
            )
            completeReset()               // storage, config, paywall cache cleanup
        })
    }
}

/**
 * Builds initial IdentityState from storage BEFORE the actor starts.
 * This is synchronous — same as the old IdentityManager constructor.
 */
internal fun createInitialIdentityState(
    storage: Storage,
    appInstalledAtString: String,
): IdentityState {
    val storedAliasId = storage.read(AliasId)
    val storedSeed = storage.read(Seed)

    val aliasId =
        storedAliasId ?: IdentityLogic.generateAlias().also {
            storage.write(AliasId, it)
        }
    val seed =
        storedSeed ?: IdentityLogic.generateSeed().also {
            storage.write(Seed, it)
        }
    val appUserId = storage.read(AppUserId)
    val userAttributes = storage.read(UserAttributes) ?: emptyMap()

    // Merge when alias/seed are new OR when stored attributes are empty/missing
    // (e.g. deserialization failed but individual fields were intact).
    val needsMerge = storedAliasId == null || storedSeed == null || userAttributes.isEmpty()
    val finalAttributes =
        if (needsMerge) {
            val enriched =
                IdentityLogic.mergeAttributes(
                    newAttributes =
                        buildMap {
                            put(Keys.ALIAS_ID, aliasId)
                            put(Keys.SEED, seed)
                            appUserId?.let { put(Keys.APP_USER_ID, it) }
                        },
                    oldAttributes = userAttributes,
                    appInstalledAtString = appInstalledAtString,
                )
            if (enriched != userAttributes) {
                storage.write(UserAttributes, enriched)
            }
            enriched
        } else {
            userAttributes
        }

    return IdentityState(
        appUserId = appUserId,
        aliasId = aliasId,
        seed = seed,
        userAttributes = finalAttributes,
        appInstalledAtString = appInstalledAtString,
    )
}
