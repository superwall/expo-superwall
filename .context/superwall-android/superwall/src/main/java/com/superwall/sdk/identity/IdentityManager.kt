package com.superwall.sdk.identity

import com.superwall.sdk.SdkContext
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.primitives.StateActor
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.web.WebPaywallRedeemer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Facade over the identity state of the shared SDK actor.
 *
 * Implements [IdentityContext] directly — actions receive `this` as
 * their context, eliminating the intermediate object.
 */
class IdentityManager(
    override val storage: Storage,
    private val ioScope: IOScope,
    private val stringToSha: (String) -> String = { it },
    override val notifyUserChange: (change: Map<String, Any>) -> Unit,
    override val completeReset: () -> Unit = {
        Superwall.instance.reset(duringIdentify = true)
    },
    override val tracker: suspend (TrackableSuperwallEvent) -> Unit = {
        Superwall.instance.track(it)
    },
    private val options: () -> SuperwallOptions,
    override val webPaywallRedeemer: () -> WebPaywallRedeemer,
    override val actor: StateActor<IdentityContext, IdentityState>,
    @Suppress("EXPOSED_PARAMETER_TYPE")
    override val sdkContext: SdkContext,
) : IdentityContext {
    override val scope: CoroutineScope get() = ioScope

    private val identity get() = actor.state.value

    val appUserId: String? get() = identity.appUserId
    val aliasId: String get() = identity.aliasId
    val seed: Int get() = identity.seed
    val userId: String get() = identity.userId
    val userAttributes: Map<String, Any> get() = identity.enrichedAttributes
    val isLoggedIn: Boolean get() = identity.isLoggedIn

    val externalAccountId: String
        get() =
            if (options().passIdentifiersToPlayStore) {
                userId
            } else {
                stringToSha(userId)
            }

    val hasIdentity: Flow<Boolean>
        get() = actor.state.map { it.isReady }.filter { it }

    fun configure(neverCalledStaticConfig: Boolean) {
        effect(
            IdentityState.Actions.Configure(
                neverCalledStaticConfig = neverCalledStaticConfig,
            ),
        )
    }

    fun identify(
        userId: String,
        options: IdentityOptions? = null,
    ) {
        effect(IdentityState.Actions.Identify(userId, options))
    }

    fun reset() {
        effect(IdentityState.Actions.FullReset)
    }

    fun mergeUserAttributes(
        newUserAttributes: Map<String, Any?>,
        shouldTrackMerge: Boolean = true,
    ) {
        effect(
            IdentityState.Actions.MergeAttributes(
                attrs = newUserAttributes,
                shouldTrackMerge = shouldTrackMerge,
                shouldNotify = false,
            ),
        )
    }

    internal fun mergeAndNotify(
        newUserAttributes: Map<String, Any?>,
        shouldTrackMerge: Boolean = true,
    ) {
        effect(
            IdentityState.Actions.MergeAttributes(
                attrs = newUserAttributes,
                shouldTrackMerge = shouldTrackMerge,
                shouldNotify = true,
            ),
        )
    }

    suspend fun awaitLatestIdentity(): IdentityState {
        return actor.state.first { state -> !state.hasPendingIdentityResolution }
    }
}
