package com.superwall.sdk.config.models

import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.config.ConfigContext
import com.superwall.sdk.config.ConfigLogic
import com.superwall.sdk.config.options.computedShouldPreload
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.awaitFirstValidConfig
import com.superwall.sdk.misc.fold
import com.superwall.sdk.misc.into
import com.superwall.sdk.misc.onError
import com.superwall.sdk.misc.primitives.Reducer
import com.superwall.sdk.misc.primitives.TypedAction
import com.superwall.sdk.misc.then
import com.superwall.sdk.misc.thenIf
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.storage.DisableVerboseEvents
import com.superwall.sdk.storage.LatestConfig
import com.superwall.sdk.storage.LatestEnrichment
import com.superwall.sdk.web.WebPaywallRedeemer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed class ConfigState {
    object None : ConfigState()

    object Retrieving : ConfigState()

    object Retrying : ConfigState()

    data class Retrieved(
        val config: Config,
    ) : ConfigState()

    data class Failed(
        val throwable: Throwable,
        val retryCount: Int = 0,
    ) : ConfigState()

    internal sealed class Updates(
        override val reduce: (ConfigState) -> ConfigState,
    ) : Reducer<ConfigState> {
        object SetRetrieving : Updates({ Retrieving })

        object SetRetrying : Updates({ Retrying })

        data class SetRetrieved(val config: Config) : Updates({ Retrieved(config) })

        data class SetFailed(
            val error: Throwable,
            val retryCount: Int = 0,
        ) : Updates({ Failed(error, retryCount) })

        data class Set(val state: ConfigState) : Updates({ state })
    }

    internal sealed class Actions(
        override val execute: suspend ConfigContext.() -> Unit,
    ) : TypedAction<ConfigContext> {
        object FetchConfig : Actions(exec@{
            val current = state.value
            if (current is Retrieving || current is Retrying) return@exec

            // Capture before transitioning out of Failed; Retrieved resets the lineage.
            val priorRetries = (current as? Failed)?.retryCount ?: 0

            update(Updates.SetRetrieving)

            val oldConfig = storage.read(LatestConfig)
            val status = entitlements.status.value
            val cacheLimit =
                if (status is SubscriptionStatus.Active) 500.milliseconds else 1.seconds

            var isConfigFromCache = false
            var isEnrichmentFromCache = false
            val configRetryCount = AtomicInteger(0)
            var configDuration = 0L

            val configDeferred =
                scope.async {
                    val start = System.currentTimeMillis()
                    (
                        if (oldConfig?.featureFlags?.enableConfigRefresh == true) {
                            try {
                                withTimeout(cacheLimit) {
                                    network
                                        .getConfig {
                                            update(Updates.SetRetrying)
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
                                oldConfig.let {
                                    isConfigFromCache = true
                                    Either.Success(it)
                                }
                            }
                        } else {
                            network.getConfig {
                                update(Updates.SetRetrying)
                                configRetryCount.incrementAndGet()
                                awaitUtilNetwork()
                            }
                        }
                    ).also {
                        configDuration = System.currentTimeMillis() - start
                    }
                }

            val enrichmentDeferred =
                scope.async {
                    val cached = storage.read(LatestEnrichment)
                    if (oldConfig?.featureFlags?.enableConfigRefresh == true) {
                        val res =
                            deviceHelper
                                .getEnrichment(0, cacheLimit)
                                .then { storage.write(LatestEnrichment, it) }
                        if (res.getSuccess() == null) {
                            cached?.let {
                                deviceHelper.setEnrichment(cached)
                                isEnrichmentFromCache = true
                                Either.Success(it)
                            } ?: res
                        } else {
                            res
                        }
                    } else {
                        deviceHelper.getEnrichment(0, 1.seconds)
                    }
                }

            val attributesDeferred = scope.async { factory.makeSessionDeviceAttributes() }

            val (configResultAny, enrichmentResultAny) =
                listOf(configDeferred, enrichmentDeferred).awaitAll()
            val attributes = attributesDeferred.await()
            scope.launch {
                @Suppress("UNCHECKED_CAST")
                track(InternalSuperwallEvent.DeviceAttributes(attributes as HashMap<String, Any>))
            }

            @Suppress("UNCHECKED_CAST")
            val configResult = configResultAny as Either<Config, Throwable>

            @Suppress("UNCHECKED_CAST")
            val enrichmentResult = enrichmentResultAny as Either<Enrichment, Throwable>

            configResult
                .then { config ->
                        track(
                            InternalSuperwallEvent.ConfigRefresh(
                                isCached = isConfigFromCache,
                                buildId = config.buildId,
                                fetchDuration = configDuration,
                                retryCount = configRetryCount.get(),
                            ),
                        )
                }.then { config -> immediate(ApplyConfig(config)) }
                .thenIf(testMode?.isTestMode != true) {
                        sideEffect {
                            webPaywallRedeemer().redeem(WebPaywallRedeemer.RedeemType.Existing)
                        }
                }.then { config ->
                    if (testMode?.isTestMode != true &&
                        options.computedShouldPreload(deviceHelper.deviceTier)
                    ) {
                        val productIds = config.paywalls.flatMap { it.productIds }.toSet()
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
                    config
                }.then { config ->
                    update(Updates.SetRetrieved(config))
                }.then {
                    if (isEnrichmentFromCache || enrichmentResult.getThrowable() != null) {
                        scope.launch { deviceHelper.getEnrichment(6, 1.seconds) }
                    }
                }.fold(
                    onSuccess = {
                        // Preload before refresh — cached boot serves cached paywalls fast.
                        effect(PreloadIfEnabled)
                        if (isConfigFromCache) {
                            effect(RefreshConfig())
                        }
                    },
                    onFailure = { e ->
                        immediate(HandleFetchFailure(e, priorRetries, isConfigFromCache))
                    },
                )
        })

        data class HandleFetchFailure(
            val error: Throwable,
            val priorRetries: Int,
            val isConfigFromCache: Boolean,
        ) : Actions({
            error.printStackTrace()
            val newRetries = priorRetries + 1
            update(Updates.SetFailed(error, retryCount = newRetries))
            if (!isConfigFromCache && newRetries <= 1) {
                effect(FetchConfig)
            }
            track(InternalSuperwallEvent.ConfigFail(error.message ?: "Unknown error"))
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.superwallCore,
                message = "Failed to Fetch Configuration",
                error = error,
            )
        })

        data class RefreshConfig(val force: Boolean = false) : Actions(exec@{
            val oldConfig = state.value.getConfig() ?: return@exec
            if (!force && !oldConfig.featureFlags.enableConfigRefresh) return@exec

            scope.launch { deviceHelper.getEnrichment(0, 1.seconds) }

            val retryCount = AtomicInteger(0)
            val startTime = System.currentTimeMillis()
            val result =
                network.getConfig {
                    retryCount.incrementAndGet()
                    awaitUtilNetwork()
                }

            result
                .then { newConfig ->
                    paywallManager.resetPaywallRequestCache()
                    val previous = state.value.getConfig()
                    if (previous != null) {
                        paywallPreload.removeUnusedPaywallVCsFromCache(previous, newConfig)
                    }
                    newConfig
                }.then { newConfig ->
                    immediate(ApplyConfig(newConfig))
                    update(Updates.SetRetrieved(newConfig))
                    track(
                        InternalSuperwallEvent.ConfigRefresh(
                            isCached = false,
                            buildId = newConfig.buildId,
                            fetchDuration = System.currentTimeMillis() - startTime,
                            retryCount = retryCount.get(),
                        ),
                    )
                    newConfig
                }.fold(
                    onSuccess = { effect(PreloadIfEnabled) },
                    onFailure = { e ->
                        Logger.debug(
                            logLevel = LogLevel.warn,
                            scope = LogScope.superwallCore,
                            message = "Failed to refresh configuration.",
                            info = null,
                            error = e,
                        )
                    },
                )
        })

        data class ApplyConfig(val config: Config) : Actions({
            storage.write(DisableVerboseEvents, config.featureFlags.disableVerboseEvents)
            if (config.featureFlags.enableConfigRefresh) {
                storage.write(LatestConfig, config)
            }
            setTriggers(ConfigLogic.getTriggersByEventName(config.triggers))
            assignments.choosePaywallVariants(config.triggers)

            ConfigLogic.extractEntitlementsByProductId(config.products).let {
                entitlements.addEntitlementsByProductId(it)
            }
            config.productsV3?.let { productsV3 ->
                ConfigLogic.extractEntitlementsByProductIdFromCrossplatform(productsV3).let {
                    entitlements.addEntitlementsByProductId(it)
                }
            }

            val manager = testMode
            val wasTestMode = manager?.isTestMode == true
            manager?.evaluateTestMode(
                config = config,
                bundleId = deviceHelper.bundleId,
                appUserId = identityManager?.invoke()?.appUserId,
                aliasId = identityManager?.invoke()?.aliasId,
                testModeBehavior = options.testModeBehavior,
            )
            val testModeJustActivated = !wasTestMode && manager?.isTestMode == true

            if (manager?.isTestMode == true) {
                if (testModeJustActivated) {
                    val defaultStatus = manager.buildSubscriptionStatus()
                    manager.setOverriddenSubscriptionStatus(defaultStatus)
                    entitlements.setSubscriptionStatus(defaultStatus)
                }
                scope.launch { activateTestMode(config, testModeJustActivated) }
            } else {
                if (wasTestMode) {
                    manager?.clearTestModeState()
                    setSubscriptionStatus?.invoke(SubscriptionStatus.Inactive)
                }
                scope.launch {
                    storeManager.loadPurchasedProducts(entitlements.entitlementsByProductId)
                }
            }
        })

        data class ReevaluateTestMode(
            val config: Config,
            val appUserId: String?,
            val aliasId: String?,
        ) : Actions(exec@{
            val manager = testMode ?: return@exec
            val wasTestMode = manager.isTestMode
            manager.evaluateTestMode(
                config = config,
                bundleId = deviceHelper.bundleId,
                appUserId = appUserId ?: identityManager?.invoke()?.appUserId,
                aliasId = aliasId ?: identityManager?.invoke()?.aliasId,
                testModeBehavior = options.testModeBehavior,
            )
            val isNowTestMode = manager.isTestMode
            if (wasTestMode && !isNowTestMode) {
                manager.clearTestModeState()
                setSubscriptionStatus?.invoke(SubscriptionStatus.Inactive)
            } else if (!wasTestMode && isNowTestMode) {
                scope.launch { activateTestMode(config, true) }
            }
        })

        object PreloadIfEnabled : Actions(exec@{
            if (!options.computedShouldPreload(deviceHelper.deviceTier)) return@exec
            val config = state.value.getConfig() ?: return@exec
            paywallPreload.preloadAllPaywalls(
                config,
                context,
                fingerprint = deviceHelper.preloadFingerprint(),
            )
        })

        object PreloadAll : Actions(exec@{
            val config = state.value.getConfig() ?: return@exec
            paywallPreload.preloadAllPaywalls(config, context)
        })

        data class PreloadByNames(
            val eventNames: Set<String>,
        ) : Actions(exec@{
            val config = state.value.getConfig() ?: return@exec
            paywallPreload.preloadPaywallsByNames(config, eventNames)
        })

        object GetAssignments : Actions(exec@{
            val config = state.value.getConfig() ?: return@exec
            config.triggers.takeUnless { it.isEmpty() }?.let { triggers ->
                try {
                    assignments
                        .getAssignments(triggers)
                        .then { effect(PreloadIfEnabled) }
                        .onError { err ->
                            Logger.debug(
                                logLevel = LogLevel.error,
                                scope = LogScope.configManager,
                                message = "Error retrieving assignments.",
                                error = err,
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
        })
    }
}

internal fun ConfigState.getConfig(): Config? =
    when (this) {
        is ConfigState.Retrieved -> config
        else -> null
    }
