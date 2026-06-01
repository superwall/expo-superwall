package com.superwall.sdk.config

import android.content.Context
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.config.models.getConfig
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.dependencies.DeviceHelperFactory
import com.superwall.sdk.dependencies.DeviceInfoFactory
import com.superwall.sdk.dependencies.HasExternalPurchaseControllerFactory
import com.superwall.sdk.dependencies.RequestFactory
import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.dependencies.StoreTransactionFactory
import com.superwall.sdk.identity.IdentityManager
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.awaitFirstValidConfig
import com.superwall.sdk.misc.primitives.StateActor
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.ExperimentID
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.models.triggers.TriggerPreloadBehavior
import com.superwall.sdk.network.SuperwallAPI
import com.superwall.sdk.network.awaitUntilNetworkExists
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.store.Entitlements
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.store.testmode.TestMode
import com.superwall.sdk.web.WebPaywallRedeemer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

open class ConfigManager(
    override val context: Context,
    override val storeManager: StoreManager,
    override val entitlements: Entitlements,
    override val storage: Storage,
    override val network: SuperwallAPI,
    override val deviceHelper: DeviceHelper,
    override var options: SuperwallOptions,
    override val paywallManager: PaywallManager,
    override val webPaywallRedeemer: () -> WebPaywallRedeemer,
    override val factory: Factory,
    override val assignments: Assignments,
    override val paywallPreload: PaywallPreload,
    private val ioScope: IOScope,
    override val tracker: suspend (TrackableSuperwallEvent) -> Unit,
    override val testMode: TestMode? = null,
    override val identityManager: (() -> IdentityManager)? = null,
    override val setSubscriptionStatus: ((SubscriptionStatus) -> Unit)? = null,
    override val awaitUtilNetwork: suspend () -> Unit = {
        context.awaitUntilNetworkExists()
    },
    override val activateTestMode: suspend (Config, Boolean) -> Unit = { _, _ -> },
    override val actor: StateActor<ConfigContext, ConfigState>,
) : ConfigContext {
    interface Factory :
        RequestFactory,
        DeviceInfoFactory,
        RuleAttributesFactory,
        DeviceHelperFactory,
        StoreTransactionFactory,
        HasExternalPurchaseControllerFactory

    override val scope: CoroutineScope get() = ioScope

    internal val configState: StateFlow<ConfigState> get() = actor.state

    val config: Config?
        get() {
            val current = actor.state.value
            if (current is ConfigState.Failed) {
                effect(ConfigState.Actions.FetchConfig)
            }
            return current.getConfig()
        }

    val hasConfig: Flow<Config> =
        actor.state
            .mapNotNull { it.getConfig() }
            .take(1)

    private var _triggersByEventName = mutableMapOf<String, Trigger>()
    var triggersByEventName: Map<String, Trigger>
        get() = _triggersByEventName
        set(value) {
            _triggersByEventName = value.toMutableMap()
        }

    override fun setTriggers(triggers: Map<String, Trigger>) {
        triggersByEventName = triggers
    }

    val unconfirmedAssignments: Map<ExperimentID, Experiment.Variant>
        get() = assignments.unconfirmedAssignments

    suspend fun fetchConfiguration() {
        val current = actor.state.value
        if (current is ConfigState.Retrieving || current is ConfigState.Retrying) return
        immediate(ConfigState.Actions.FetchConfig)
    }

    // Sync on caller for the mutation; preload follow-up goes through the actor.
    fun reset() {
        val config = actor.state.value.getConfig() ?: return
        assignments.reset()
        assignments.choosePaywallVariants(config.triggers)
        effect(ConfigState.Actions.PreloadIfEnabled)
    }

    suspend fun reevaluateTestMode(
        config: Config? = null,
        appUserId: String? = null,
        aliasId: String? = null,
    ) {
        // Resolved in body, not as default param — actor reads in defaults trip MockK.
        val resolvedConfig = config ?: actor.state.value.getConfig() ?: return
        if (testMode == null) return
        immediate(
            ConfigState.Actions.ReevaluateTestMode(
                config = resolvedConfig,
                appUserId = appUserId,
                aliasId = aliasId,
            ),
        )
    }

    suspend fun getAssignments() {
        actor.state.awaitFirstValidConfig()
        immediate(ConfigState.Actions.GetAssignments)
    }

    suspend fun preloadAllPaywalls() {
        actor.state.awaitFirstValidConfig()
        immediate(ConfigState.Actions.PreloadAll)
    }

    /**
     * Re-runs preload if any DeviceHelper field referenced by IF_TRUE rules has
     * changed since the last preload. Cheap no-op when (a) config has no IF_TRUE
     * rule or (b) the fingerprint matches the last dispatched preload.
     *
     * Call this from change-emitting signals (subscription status, configuration
     * change, interface style override, store readiness, review-request increment).
     */
    suspend fun recheckPreloadIfNeeded() {
        val config = actor.state.value.getConfig() ?: return
        val hasIfTrue =
            config.triggers.any { trigger ->
                trigger.rules.any { it.preload.behavior == TriggerPreloadBehavior.IF_TRUE }
            }
        if (!hasIfTrue) return
        // Cheap pre-check; the authoritative dedup + atomic claim lives inside
        // paywallPreload.preloadAllPaywalls so it commits only when a preload
        // actually starts (and rolls back if the run is dropped).
        val fingerprint = deviceHelper.preloadFingerprint()
        if (paywallPreload.lastFingerprint.get() == fingerprint) return
        immediate(ConfigState.Actions.PreloadIfEnabled)
    }

    suspend fun preloadPaywallsByNames(eventNames: Set<String>) {
        actor.state.awaitFirstValidConfig()
        immediate(ConfigState.Actions.PreloadByNames(eventNames))
    }

    internal suspend fun refreshConfiguration(force: Boolean = false) {
        if (actor.state.value.getConfig() == null) return
        immediate(ConfigState.Actions.RefreshConfig(force = force))
    }

    internal fun applyRetrievedConfigForTesting(config: Config) {
        actor.update(ConfigState.Updates.SetRetrieved(config))
    }

    internal fun setConfigStateForTesting(state: ConfigState) {
        actor.update(ConfigState.Updates.Set(state))
    }
}
