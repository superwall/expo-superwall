package com.superwall.sdk.config

import android.content.Context
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent.TestModeModal.*
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
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.ActivityProvider
import com.superwall.sdk.misc.CurrentActivityTracker
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
import com.superwall.sdk.network.Network
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
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.testmode.TestModeManager
import com.superwall.sdk.store.testmode.TestStoreProduct
import com.superwall.sdk.store.testmode.models.SuperwallProductPlatform
import com.superwall.sdk.store.testmode.ui.TestModeModal
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
    private val fullNetwork: Network? = null,
    private val deviceHelper: DeviceHelper,
    var options: SuperwallOptions,
    private val paywallManager: PaywallManager,
    private val webPaywallRedeemer: () -> WebPaywallRedeemer,
    private val factory: Factory,
    private val assignments: Assignments,
    private val paywallPreload: PaywallPreload,
    private val ioScope: IOScope,
    private val track: suspend (InternalSuperwallEvent) -> Unit,
    private val testModeManager: TestModeManager? = null,
    private val identityManager: (() -> IdentityManager)? = null,
    private val activityProvider: ActivityProvider? = null,
    private val activityTracker: CurrentActivityTracker? = null,
    private val setSubscriptionStatus: ((SubscriptionStatus) -> Unit)? = null,
    private val awaitUtilNetwork: suspend () -> Unit = {
        context.awaitUntilNetworkExists()
    },
) {
    interface Factory :
        RequestFactory,
        DeviceInfoFactory,
        RuleAttributesFactory,
        DeviceHelperFactory,
        StoreTransactionFactory,
        HasExternalPurchaseControllerFactory

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
                            e.printStackTrace()
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
                if (testModeManager?.isTestMode != true) {
                    ioScope.launch {
                        checkForWebEntitlements()
                    }
                }
            }.then {
                if (testModeManager?.isTestMode != true && options.paywalls.shouldPreload) {
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

        reevaluateTestMode(config)
        assignments.reset()
        assignments.choosePaywallVariants(config.triggers)

        ioScope.launch { preloadPaywalls() }
    }

    /**
     * Re-evaluates test mode with the current identity and config.
     * If test mode was active but the current user no longer qualifies, clears test mode
     * and resets subscription status. If a new user qualifies, activates test mode and
     * shows the modal.
     */
    fun reevaluateTestMode(
        config: Config? = configState.value.getConfig(),
        appUserId: String? = null,
        aliasId: String? = null,
    ) {
        config ?: return
        val wasTestMode = testModeManager?.isTestMode == true
        testModeManager?.evaluateTestMode(
            config = config,
            bundleId = deviceHelper.bundleId,
            appUserId = appUserId ?: identityManager?.invoke()?.appUserId,
            aliasId = aliasId ?: identityManager?.invoke()?.aliasId,
            testModeBehavior = options.testModeBehavior,
        )
        val isNowTestMode = testModeManager?.isTestMode == true
        if (wasTestMode && !isNowTestMode) {
            testModeManager?.clearTestModeState()
            setSubscriptionStatus?.invoke(SubscriptionStatus.Inactive)
        } else if (!wasTestMode && isNowTestMode) {
            ioScope.launch {
                fetchTestModeProducts()
                presentTestModeModal(config)
            }
        }
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
        // Extract entitlements from both products (ProductItem) and productsV3 (CrossplatformProduct)
        ConfigLogic.extractEntitlementsByProductId(config.products).let {
            entitlements.addEntitlementsByProductId(it)
        }
        config.productsV3?.let { productsV3 ->
            ConfigLogic.extractEntitlementsByProductIdFromCrossplatform(productsV3).let {
                entitlements.addEntitlementsByProductId(it)
            }
        }

        // Test mode evaluation
        val wasTestMode = testModeManager?.isTestMode == true
        testModeManager?.evaluateTestMode(
            config = config,
            bundleId = deviceHelper.bundleId,
            appUserId = identityManager?.invoke()?.appUserId,
            aliasId = identityManager?.invoke()?.aliasId,
            testModeBehavior = options.testModeBehavior,
        )
        val testModeJustActivated = !wasTestMode && testModeManager?.isTestMode == true

        if (testModeManager?.isTestMode == true) {
            // Set a default subscription status immediately so the paywall pipeline
            // doesn't timeout waiting for it while the test mode modal is shown.
            if (testModeJustActivated) {
                val defaultStatus = testModeManager.buildSubscriptionStatus()
                testModeManager.setOverriddenSubscriptionStatus(defaultStatus)
                entitlements.setSubscriptionStatus(defaultStatus)
            }
            ioScope.launch {
                fetchTestModeProducts()
                if (testModeJustActivated) {
                    presentTestModeModal(config)
                }
            }
        } else {
            if (wasTestMode) {
                testModeManager?.clearTestModeState()
                setSubscriptionStatus?.invoke(SubscriptionStatus.Inactive)
            }
            ioScope.launch {
                storeManager.loadPurchasedProducts(entitlements.entitlementsByProductId)
            }
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

    internal suspend fun refreshConfiguration(force: Boolean = false) {
        // Make sure config already exists
        val oldConfig = config ?: return

        // Ensure the config refresh feature flag is enabled
        if (!force && !oldConfig.featureFlags.enableConfigRefresh) {
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
        ioScope.launch {
            webPaywallRedeemer().redeem(WebPaywallRedeemer.RedeemType.Existing)
        }
    }

    private suspend fun fetchTestModeProducts() {
        val net = fullNetwork ?: return
        val manager = testModeManager ?: return

        net.getSuperwallProducts().fold(
            onSuccess = { response ->
                val androidProducts =
                    response.data.filter { it.platform == SuperwallProductPlatform.ANDROID && it.price != null }
                manager.setProducts(androidProducts)

                val productsByFullId =
                    androidProducts.associate { superwallProduct ->
                        val testProduct = TestStoreProduct(superwallProduct)
                        superwallProduct.identifier to StoreProduct(testProduct)
                    }
                manager.setTestProducts(productsByFullId)

                Logger.debug(
                    LogLevel.info,
                    LogScope.superwallCore,
                    "Test mode: loaded ${androidProducts.size} products",
                )
            },
            onFailure = { error ->
                Logger.debug(
                    LogLevel.error,
                    LogScope.superwallCore,
                    "Test mode: failed to fetch products - ${error.message}",
                )
            },
        )
    }

    private suspend fun presentTestModeModal(config: Config) {
        val manager = testModeManager ?: return
        // Prefer the lifecycle-tracked activity (sees the actual foreground activity,
        // e.g. SuperwallPaywallActivity) over the user-provided ActivityProvider
        // (which in Expo/RN may always return the root MainActivity).
        val activity =
            activityTracker?.getCurrentActivity()
                ?: activityProvider?.getCurrentActivity()
                ?: activityTracker?.awaitActivity(10.seconds)
        if (activity == null) {
            Logger.debug(
                LogLevel.warn,
                LogScope.superwallCore,
                "Test mode modal could not be presented: no activity available. Setting default subscription status.",
            )
            with(manager) {
                val status = buildSubscriptionStatus()
                setOverriddenSubscriptionStatus(status)
                entitlements.setSubscriptionStatus(status)
            }
            return
        }

        track(InternalSuperwallEvent.TestModeModal(State.Open))

        val reason = manager.testModeReason?.description ?: "Test mode activated"
        val allEntitlements =
            config.productsV3
                ?.flatMap { it.entitlements.map { e -> e.id } }
                ?.distinct()
                ?.sorted()
                ?: emptyList()

        val dashboardBaseUrl =
            when (options.networkEnvironment) {
                is SuperwallOptions.NetworkEnvironment.Developer -> "https://superwall.dev"
                else -> "https://superwall.com"
            }

        val apiKey = deviceHelper.storage.apiKey
        val savedSettings = manager.loadSettings()

        val result =
            TestModeModal.show(
                activity = activity,
                reason = reason,
                hasPurchaseController = factory.makeHasExternalPurchaseController(),
                availableEntitlements = allEntitlements,
                apiKey = apiKey,
                dashboardBaseUrl = dashboardBaseUrl,
                savedSettings = savedSettings,
            )

        with(manager) {
            setFreeTrialOverride(result.freeTrialOverride)
            setEntitlements(result.entitlements)
            saveSettings()
            val status = buildSubscriptionStatus()
            setOverriddenSubscriptionStatus(status)
            entitlements.setSubscriptionStatus(status)
        }

        track(InternalSuperwallEvent.TestModeModal(State.Close))
    }
}
