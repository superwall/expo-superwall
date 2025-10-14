package com.superwall.sdk.paywall.request

import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.dependencies.ConfigManagerFactory
import com.superwall.sdk.dependencies.DeviceInfoFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.map
import com.superwall.sdk.misc.mapError
import com.superwall.sdk.misc.onError
import com.superwall.sdk.misc.then
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.network.Network
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.paywall.presentation.internal.request.ProductOverride
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface PaywallRequestManagerDepFactory :
    DeviceInfoFactory,
    ConfigManagerFactory {
    fun activePaywallId(): String?
}

class PaywallRequestManager(
    private val storeManager: StoreManager,
    private val network: Network,
    private val factory: PaywallRequestManagerDepFactory,
    private val ioScope: IOScope,
    private val track: suspend (TrackableSuperwallEvent) -> Unit = {
        Superwall.instance.track(it)
    },
    private val getGlobalOverrides: () -> Map<String, String> = {
        Superwall.instance.overrideProductsByName
    },
) {
    // Single thread context to make this class similar to an actor. All functions in this class
    // must execute with this context.

    private val activeTasks: MutableMap<String, Deferred<Paywall>> = mutableMapOf()
    private val paywallsByHash: MutableMap<String, Paywall> = mutableMapOf()

    suspend fun getPaywall(
        request: PaywallRequest,
        isPreloading: Boolean = false,
    ): Either<Paywall, Throwable> =
        withErrorTracking {
            withContext(ioScope.coroutineContext) {
                val deviceInfo = factory.makeDeviceInfo()
                val joinedSubstituteProductIds =
                    request.overrides.products
                        ?.values
                        ?.sortedBy { it.productIdentifier }
                        ?.joinToString(separator = "") { it.productIdentifier }
                val requestHash =
                    PaywallLogic.requestHash(
                        identifier = request.responseIdentifiers.paywallId,
                        event = request.eventData,
                        locale = deviceInfo.locale,
                        joinedSubstituteProductIds = joinedSubstituteProductIds,
                    )

                var paywall: Paywall? = paywallsByHash[requestHash]
                if (paywall != null &&
                    !request.isDebuggerLaunched
                ) {
                    if (!(isPreloading && paywall.identifier == factory.activePaywallId())) {
                        return@withContext updatePaywall(paywall, request)
                    } else {
                        return@withContext paywall
                    }
                }

                val existingTask = activeTasks[requestHash]
                if (existingTask != null) {
                    try {
                        paywall = existingTask.await()
                        if (!(isPreloading && paywall.identifier == factory.activePaywallId())) {
                            paywall = updatePaywall(paywall, request)
                        }
                        return@withContext paywall
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // Clean up cancelled task and continue with new request
                        activeTasks.remove(requestHash)
                        // Don't rethrow, let it continue to create a new task
                    }
                }

                // Use suspendCancellableCoroutine to ensure proper cleanup
                paywall =
                    suspendCancellableCoroutine { continuation ->
                        val deferredTask = CompletableDeferred<Paywall>()
                        activeTasks[requestHash] = deferredTask

                        // Set up cancellation handler to clean up activeTasks
                        continuation.invokeOnCancellation {
                            activeTasks.remove(requestHash)
                            deferredTask.cancel()
                        }

                        // Launch coroutine to handle async operations
                        ioScope.launch {
                            try {
                                val rawPaywallResult = getRawPaywall(request, isPreloading)
                                rawPaywallResult
                                    .then {
                                        val finalPaywall = addProducts(it, request)
                                        saveRequestHash(requestHash, finalPaywall, request.isDebuggerLaunched)

                                        // Complete both the deferred task and the continuation
                                        deferredTask.complete(finalPaywall)
                                        continuation.resume(finalPaywall)
                                    }.onError { error ->
                                        activeTasks.remove(requestHash)
                                        deferredTask.completeExceptionally(error)
                                        continuation.resumeWithException(error)
                                    }
                            } catch (error: Throwable) {
                                activeTasks.remove(requestHash)
                                deferredTask.completeExceptionally(error)
                                continuation.resumeWithException(error)
                            }
                        }
                    }

                // At this point paywall should not be null, but let's handle it safely
                val finalPaywall = paywall ?: throw IllegalStateException("Paywall should not be null")

                return@withContext if (!(isPreloading && finalPaywall.identifier == factory.activePaywallId())) {
                    updatePaywall(finalPaywall, request)
                } else {
                    finalPaywall
                }
            }
        }

    private suspend fun updatePaywall(
        paywall: Paywall,
        request: PaywallRequest,
    ): Paywall =
        withContext(ioScope.coroutineContext) {
            val paywall = paywall
            paywall.experiment = request.responseIdentifiers.experiment
            paywall.presentationSourceType = request.presentationSourceType
            return@withContext paywall
        }

    private suspend fun saveRequestHash(
        requestHash: String,
        paywall: Paywall,
        isDebuggerLaunched: Boolean,
    ) = withContext(ioScope.coroutineContext) {
        activeTasks.remove(requestHash)
        if (!isDebuggerLaunched) {
            paywallsByHash[requestHash] = paywall
        }
    }

    suspend fun getRawPaywall(
        request: PaywallRequest,
        isPreloading: Boolean = false,
    ): Either<Paywall, *> =

        withContext(ioScope.coroutineContext) {
            Logger.debug(
                LogLevel.debug,
                LogScope.all,
                "!!getRawPaywall - ${request.responseIdentifiers.paywallId}",
            )
            trackResponseStarted(event = request.eventData)
            return@withContext getPaywallResponse(request, isPreloading)
                .then {
                    val paywallInfo =
                        it.getInfo(
                            fromEvent = request.eventData,
                        )
                    trackResponseLoaded(
                        paywallInfo,
                        event = request.eventData,
                    )
                }
        }

    private suspend fun getPaywallResponse(
        request: PaywallRequest,
        isPreloading: Boolean = false,
    ): Either<Paywall, *> =
        withContext(ioScope.coroutineContext) {
            val responseLoadStartTime = Date()
            val paywallId = request.responseIdentifiers.paywallId
            val event = request.eventData

            return@withContext (
                factory
                    .makeStaticPaywall(
                        paywallId = paywallId,
                        isDebuggerLaunched = request.isDebuggerLaunched,
                    )?.let {
                        Either.Success<Paywall, Throwable>(it)
                    } ?: network.getPaywall(
                    identifier = paywallId,
                    event = event,
                )
            ).then {
                Logger.debug(
                    LogLevel.debug,
                    LogScope.all,
                    "!!getPaywallResponse - $paywallId - $it",
                )
            }.map {
                if (!(isPreloading && it.identifier == factory.activePaywallId())) {
                    it.experiment = request.responseIdentifiers.experiment
                    it.responseLoadingInfo.startAt = responseLoadStartTime
                    it.responseLoadingInfo.endAt = Date()
                }
                it
            }.then {
                Logger.debug(
                    LogLevel.debug,
                    LogScope.all,
                    "!!getPaywallResponse - $paywallId - $it - ${it.experiment}",
                )
            }.mapError {
                PaywallLogic.handlePaywallError(
                    it,
                    event,
                )
            }
        }

    // MARK: - Analytics
    private suspend fun trackResponseStarted(event: EventData?) =
        withContext(ioScope.coroutineContext) {
            val trackedEvent =
                InternalSuperwallEvent.PaywallLoad(
                    state = InternalSuperwallEvent.PaywallLoad.State.Start(),
                    eventData = event,
                )
            track(trackedEvent)
        }

    private suspend fun trackResponseLoaded(
        paywallInfo: PaywallInfo,
        event: EventData?,
    ) = withContext(ioScope.coroutineContext) {
        val responseLoadEvent =
            InternalSuperwallEvent.PaywallLoad(
                InternalSuperwallEvent.PaywallLoad.State.Complete(paywallInfo = paywallInfo),
                eventData = event,
            )
        track(responseLoadEvent)
    }

    suspend fun addProducts(
        paywall: Paywall,
        request: PaywallRequest,
    ): Paywall =
        withContext(ioScope.coroutineContext) {
            var paywall = paywall

            paywall = trackProductsLoadStart(paywall, request)
            paywall =
                try {
                    getProducts(paywall, request)
                } catch (error: Throwable) {
                    throw error
                }
            paywall = trackProductsLoadFinish(paywall, request.eventData)

            return@withContext paywall
        }

    private suspend fun getProducts(
        paywall: Paywall,
        request: PaywallRequest,
    ): Paywall =
        withContext(ioScope.coroutineContext) {
            var paywall = paywall

            // Use local overrides if available, otherwise use global overrides
            val substituteProducts =
                request.overrides.products
                    ?: run {
                        val globalOverrides = getGlobalOverrides()
                        if (globalOverrides.isNotEmpty()) {
                            val productOverrides =
                                globalOverrides.mapValues { ProductOverride.ById(it.value) }
                            convertProductOverrides(productOverrides)
                        } else {
                            null
                        }
                    }

            val result =
                storeManager.getProducts(
                    substituteProducts = substituteProducts,
                    paywall = paywall,
                    request = request,
                )
            if (result.paywall != null) {
                paywall = result.paywall
            }
            paywall.productItems = result.productItems

            val outcome =
                PaywallLogic.getVariablesAndFreeTrial(
                    productItems = result.productItems,
                    productsByFullId = result.productsByFullId,
                    isFreeTrialAvailableOverride = request.overrides.isFreeTrial,
                )
            paywall.productVariables = outcome.productVariables
            paywall.isFreeTrialAvailable = outcome.isFreeTrialAvailable

            return@withContext paywall
        }

    // Analytics
    private suspend fun trackProductsLoadStart(
        paywall: Paywall,
        request: PaywallRequest,
    ): Paywall =
        withContext(ioScope.coroutineContext) {
            var paywall = paywall
            paywall.productsLoadingInfo.startAt = Date()
            val paywallInfo = paywall.getInfo(request.eventData)
            val productLoadEvent =
                InternalSuperwallEvent.PaywallProductsLoad(
                    state = InternalSuperwallEvent.PaywallProductsLoad.State.Start(),
                    paywallInfo,
                    request.eventData,
                )
            track(productLoadEvent)
            return@withContext paywall
        }

    private suspend fun trackProductsLoadFinish(
        paywall: Paywall,
        event: EventData?,
    ): Paywall =
        withContext(ioScope.coroutineContext) {
            var paywall = paywall
            paywall.productsLoadingInfo.endAt = Date()
            val paywallInfo = paywall.getInfo(event)
            val productLoadEvent =
                InternalSuperwallEvent.PaywallProductsLoad(
                    state = InternalSuperwallEvent.PaywallProductsLoad.State.Complete(),
                    paywallInfo,
                    event,
                )
            track(productLoadEvent)

            return@withContext paywall
        }

    internal fun resetCache() {
        paywallsByHash.clear()
    }

    /**
     * Converts productOverrides to the format expected by StoreManager.getProducts.
     * This function handles ProductOverride.ByProduct objects by extracting the StoreProduct,
     * and ignores ProductOverride.ById objects since they need to be resolved by the StoreManager.
     */
    private suspend fun convertProductOverrides(productOverrides: Map<String, ProductOverride>?): Map<String, StoreProduct>? {
        if (productOverrides.isNullOrEmpty()) return null
        val convertedProducts = mutableMapOf<String, StoreProduct?>()
        val products =
            storeManager.getProductsWithoutPaywall(
                productOverrides.values
                    .map {
                        when (it) {
                            is ProductOverride.ById -> it.productId
                            is ProductOverride.ByProduct -> it.product.productIdentifier
                        }
                    }.toList(),
            )
        for ((name, override) in productOverrides) {
            when (override) {
                is ProductOverride.ByProduct -> {
                    convertedProducts[name] = override.product
                }

                is ProductOverride.ById -> {
                    val product = products[override.productId]
                    convertedProducts[name] = product
                }
            }
        }

        return (convertedProducts.filterNot { it.value == null } as Map<String, StoreProduct>).ifEmpty { null }
    }
}
