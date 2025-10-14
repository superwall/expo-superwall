package com.superwall.sdk.config

import android.content.Context
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.config.models.getConfig
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.dependencies.DeviceHelperFactory
import com.superwall.sdk.dependencies.DeviceInfoFactory
import com.superwall.sdk.dependencies.RequestFactory
import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.dependencies.StoreTransactionFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.awaitFirstValidConfig
import com.superwall.sdk.misc.fold
import com.superwall.sdk.misc.into
import com.superwall.sdk.misc.onError
import com.superwall.sdk.misc.then
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.ExperimentID
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.network.SuperwallAPI
import com.superwall.sdk.network.awaitUntilNetworkExists
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.storage.DisableVerboseEvents
import com.superwall.sdk.storage.LatestConfig
import com.superwall.sdk.storage.LatestEnrichment
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.store.Entitlements
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.web.WebPaywallRedeemer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

open class ConfigManager(
    private val context: Context,
    private val storeManager: StoreManager,
    private val entitlements: Entitlements,
    private val storage: Storage,
    private val network: SuperwallAPI,
    private val deviceHelper: DeviceHelper,
    var options: SuperwallOptions,
    private val paywallManager: PaywallManager,
    private val webPaywallRedeemer: () -> WebPaywallRedeemer,
    private val factory: Factory,
    private val assignments: Assignments,
    private val paywallPreload: PaywallPreload,
    private val ioScope: IOScope,
    private val track: suspend (InternalSuperwallEvent) -> Unit,
    private val awaitUtilNetwork: suspend () -> Unit = {
        context.awaitUntilNetworkExists()
    },
) {
    interface Factory :
        RequestFactory,
        DeviceInfoFactory,
        RuleAttributesFactory,
        DeviceHelperFactory,
        StoreTransactionFactory

    // The configuration of the Superwall dashboard
    internal val configState = MutableStateFlow<ConfigState>(ConfigState.None)

    // Convenience variable to access config
    val config: Config?
        get() =
            configState.value
                .also {
                    if (it is ConfigState.Failed) {
                        ioScope.launch {
                            fetchConfiguration()
                        }
                    }
                }.getConfig()

    // A flow that emits just once only when `config` is non-`nil`.
    val hasConfig: Flow<Config> =
        configState
            .mapNotNull { it.getConfig() }
            .take(1)

    // A dictionary of triggers by their event name.
    private var _triggersByEventName = mutableMapOf<String, Trigger>()
    var triggersByEventName: Map<String, Trigger>
        get() = _triggersByEventName
        set(value) {
            _triggersByEventName = value.toMutableMap()
        }

    // A memory store of assignments that are yet to be confirmed.

    val unconfirmedAssignments: Map<ExperimentID, Experiment.Variant>
        get() = assignments.unconfirmedAssignments

    suspend fun fetchConfiguration() {
        if (configState.value != ConfigState.Retrieving) {
            fetchConfig()
        }
    }

    private suspend fun fetchConfig() {
        configState.update { ConfigState.Retrieving }
        val oldConfig = storage.read(LatestConfig)
        val status = entitlements.status.value
        val CACHE_LIMIT = if (status is SubscriptionStatus.Active) 500.milliseconds else 1.seconds
        var isConfigFromCache = false
        var isEnrichmentFromCache = false

        // If config is cached, get config from the network but timeout after 300ms
        // and default to the cached version. Then, refresh in the background.
        val configRetryCount: AtomicInteger = AtomicInteger(0)
        var configDuration = 0L
        val configDeferred =
            ioScope.async {
                val start = System.currentTimeMillis()
                (
                    if (oldConfig?.featureFlags?.enableConfigRefresh == true) {
                        try {
                            // If config refresh is enabled, try loading with a timeout
                            withTimeout(CACHE_LIMIT) {
                                network
                                    .getConfig {
                                        // Emit retrying state
                                        configState.update { ConfigState.Retrying }
                                        configRetryCount.incrementAndGet()
                                        awaitUtilNetwork()
                                    }.into {
                                        if (it is Either.Failure) {
                                            isConfigFromCache = true
                                            Either.Success(oldConfig)
                                        } else {
                                            it
                                        }
                                    }
                            }
                        } catch (e: Throwable) {
                            // If fetching config fails, default to the cached version
                            // Note: Only a timeout exception is possible here
                            oldConfig?.let {
                                isConfigFromCache = true
                                Either.Success(it)
                            } ?: Either.Failure(e)
                        }
                    } else {
                        // If config refresh is disabled or there is no cache
                        // just fetch with a normal retry
                        network
                            .getConfig {
                                configState.update { ConfigState.Retrying }
                                configRetryCount.incrementAndGet()
                                context.awaitUntilNetworkExists()
                            }
                    }
                ).also {
                    configDuration = System.currentTimeMillis() - start
                }
            }

        val enrichmentDeferred =
            ioScope.async {
                val cached = storage.read(LatestEnrichment)
                if (oldConfig?.featureFlags?.enableConfigRefresh == true) {
                    // If we have a cached config and refresh was enabled, try loading with
                    // a timeout or load from cache
                    val res =
                        deviceHelper
                            .getEnrichment(0, CACHE_LIMIT)
                            .then {
                                storage.write(LatestEnrichment, it)
                            }
                    if (res.getSuccess() == null) {
                        // Loading timed out, we default to cached version
                        cached?.let {
                            deviceHelper.setEnrichment(cached)
                            isEnrichmentFromCache = true
                            Either.Success(it)
                        } ?: res
                    } else {
                        res
                    }
                } else {
                    // If there's no cached enrichment and config refresh is disabled,
                    // try to fetch with 1 sec timeout or fail.
                    deviceHelper.getEnrichment(0, 1.seconds)
                }
            }

        val attributesDeferred = ioScope.async { factory.makeSessionDeviceAttributes() }

        // Await results from both operations
        val (result, enriched) =
            listOf(
                configDeferred,
                enrichmentDeferred,
            ).awaitAll()
        val attributes = attributesDeferred.await()
        ioScope.launch {
            @Suppress("UNCHECKED_CAST")
            track(InternalSuperwallEvent.DeviceAttributes(attributes as HashMap<String, Any>))
        }
        val configResult = result as Either<Config, Throwable>
        val enrichmentResult = enriched as Either<Enrichment, Throwable>
        configResult
            .then {
                ioScope.launch {
                    track(
                        InternalSuperwallEvent.ConfigRefresh(
                            isCached = isConfigFromCache,
                            buildId = it.buildId,
                            fetchDuration = configDuration,
                            retryCount = configRetryCount.get(),
                        ),
                    )
                }
            }.then(::processConfig)
            .then {
                ioScope.launch {
                    checkForWebEntitlements()
                }
            }.then {
                if (options.paywalls.shouldPreload) {
                    val productIds = it.paywalls.flatMap { it.productIds }.toSet()
                    try {
                        storeManager.products(productIds)
                    } catch (e: Throwable) {
                        Logger.debug(
                            logLevel = LogLevel.error,
                            scope = LogScope.productsManager,
                            message = "Failed to preload products",
                            error = e,
                        )
                    }
                }
            }.then {
                configState.update { _ -> ConfigState.Retrieved(it) }
            }.then {
                if (isConfigFromCache) {
                    ioScope.launch { refreshConfiguration() }
                }
                if (isEnrichmentFromCache || enrichmentResult.getThrowable() != null) {
                    ioScope.launch { deviceHelper.getEnrichment(6, 1.seconds) }
                }
            }.fold(
                onSuccess =
                    {
                        ioScope.launch { preloadPaywalls() }
                    },
                onFailure =
                    { e ->
                        e.printStackTrace()
                        configState.update { ConfigState.Failed(e) }
                        if (!isConfigFromCache) {
                            refreshConfiguration()
                        }
                        track(InternalSuperwallEvent.ConfigFail(e.message ?: "Unknown error"))
                        Logger.debug(
                            logLevel = LogLevel.error,
                            scope = LogScope.superwallCore,
                            message = "Failed to Fetch Configuration",
                            error = e,
                        )
                    },
            )
    }

    fun reset() {
        val config = configState.value.getConfig() ?: return

        assignments.reset()
        assignments.choosePaywallVariants(config.triggers)
        ioScope.launch { preloadPaywalls() }
    }

    suspend fun getAssignments() {
        val config = configState.awaitFirstValidConfig() ?: return

        config.triggers.takeUnless { it.isEmpty() }?.let { triggers ->
            try {
                assignments
                    .getAssignments(triggers)
                    .then {
                        ioScope.launch { preloadPaywalls() }
                    }.onError {
                        Logger.debug(
                            logLevel = LogLevel.error,
                            scope = LogScope.configManager,
                            message = "Error retrieving assignments.",
                            error = it,
                        )
                    }
            } catch (e: Throwable) {
                e.printStackTrace()
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.configManager,
                    message = "Error retrieving assignments.",
                    error = e,
                )
            }
        }
    }

    private fun processConfig(config: Config) {
        storage.write(DisableVerboseEvents, config.featureFlags.disableVerboseEvents)
        if (config.featureFlags.enableConfigRefresh) {
            storage.write(LatestConfig, config)
        }
        triggersByEventName = ConfigLogic.getTriggersByEventName(config.triggers)
        assignments.choosePaywallVariants(config.triggers)
        ConfigLogic.extractEntitlementsByProductId(config.products).let {
            entitlements.addEntitlementsByProductId(it)
        }
        ioScope.launch {
            storeManager.loadPurchasedProducts()
        }
    }

// Preloading Paywalls

    // Preloads paywalls.
    private suspend fun preloadPaywalls() {
        if (!options.paywalls.shouldPreload) return
        preloadAllPaywalls()
    }

    // Preloads paywalls referenced by triggers.
    suspend fun preloadAllPaywalls() =
        paywallPreload.preloadAllPaywalls(
            configState.awaitFirstValidConfig(),
            context,
        )

    // Preloads paywalls referenced by the provided triggers.
    suspend fun preloadPaywallsByNames(eventNames: Set<String>) =
        paywallPreload.preloadPaywallsByNames(
            configState.awaitFirstValidConfig(),
            eventNames,
        )

    private suspend fun Either<Config, *>.handleConfigUpdate(
        fetchDuration: Long,
        retryCount: Int,
    ) = then {
        paywallManager.resetPaywallRequestCache()
        val oldConfig = config
        if (oldConfig != null) {
            paywallPreload.removeUnusedPaywallVCsFromCache(oldConfig, it)
        }
    }.then { config ->
        processConfig(config)
        configState.update { ConfigState.Retrieved(config) }
        track(
            InternalSuperwallEvent.ConfigRefresh(
                isCached = false,
                buildId = config.buildId,
                fetchDuration = fetchDuration,
                retryCount = retryCount,
            ),
        )
    }.fold(
        onSuccess = { newConfig ->
            ioScope.launch { preloadPaywalls() }
        },
        onFailure = {
            Logger.debug(
                logLevel = LogLevel.warn,
                scope = LogScope.superwallCore,
                message = "Failed to refresh configuration.",
                info = null,
                error = it,
            )
        },
    )

    internal suspend fun refreshConfiguration() {
        // Make sure config already exists
        val oldConfig = config ?: return

        // Ensure the config refresh feature flag is enabled
        if (!oldConfig.featureFlags.enableConfigRefresh) {
            return
        }

        ioScope.launch {
            deviceHelper.getEnrichment(0, 1.seconds)
        }

        val retryCount: AtomicInteger = AtomicInteger(0)
        val startTime = System.currentTimeMillis()
        network
            .getConfig {
                retryCount.incrementAndGet()
                context.awaitUntilNetworkExists()
            }.handleConfigUpdate(
                retryCount = retryCount.get(),
                fetchDuration = System.currentTimeMillis() - startTime,
            )
    }

    suspend fun checkForWebEntitlements() {
        if (config?.featureFlags?.web2App == true) {
            ioScope.launch {
                webPaywallRedeemer().redeem(WebPaywallRedeemer.RedeemType.Existing)
            }
        }
    }
}
