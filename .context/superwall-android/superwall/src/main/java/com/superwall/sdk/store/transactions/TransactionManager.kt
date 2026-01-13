package com.superwall.sdk.store.transactions

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent.Transaction.TransactionSource
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEvents
import com.superwall.sdk.delegate.InternalPurchaseResult
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.dependencies.AttributesFactory
import com.superwall.sdk.dependencies.CacheFactory
import com.superwall.sdk.dependencies.DeviceHelperFactory
import com.superwall.sdk.dependencies.EnrichmentFactory
import com.superwall.sdk.dependencies.HasExternalPurchaseControllerFactory
import com.superwall.sdk.dependencies.HasInternalPurchaseControllerFactory
import com.superwall.sdk.dependencies.OptionsFactory
import com.superwall.sdk.dependencies.StoreTransactionFactory
import com.superwall.sdk.dependencies.TransactionVerifierFactory
import com.superwall.sdk.dependencies.TriggerFactory
import com.superwall.sdk.dependencies.WebToAppFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.ActivityProvider
import com.superwall.sdk.misc.AlertControllerFactory.AlertProps
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.launchWithTracking
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.PaywallViewState
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import com.superwall.sdk.storage.EventsQueue
import com.superwall.sdk.storage.PurchasingProductdIds
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.store.PurchasingObserverState
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.store.abstractions.product.RawStoreProduct
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.abstractions.transactions.StoreTransaction
import com.superwall.sdk.web.openRestoreOnWeb
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class TransactionManager(
    private val storeManager: StoreManager,
    private val purchaseController: PurchaseController,
    private val eventsQueue: EventsQueue,
    private val storage: Storage,
    private val activityProvider: ActivityProvider,
    private val factory: Factory,
    private val ioScope: IOScope,
    private val track: suspend (TrackableSuperwallEvent) -> Unit = {
        Superwall.instance.track(it)
    },
    private val dismiss: suspend (paywallId: String, result: PaywallResult) -> Unit,
    private val showAlert: (AlertProps) -> Unit,
    private val subscriptionStatus: () -> SubscriptionStatus = {
        Superwall.instance.entitlements.status.value
    },
    private val entitlementsById: (String) -> Set<Entitlement>,
    private val allEntitlementsByProductId: () -> Map<String, Set<Entitlement>>,
    private val showRestoreDialogForWeb: suspend () -> Unit,
    private val refreshReceipt: () -> Unit,
    private val updateState: (cacheKey: String, update: PaywallViewState.Updates) -> Unit,
    private val notifyOfTransactionComplete: suspend (paywallCacheKey: String, trialEndDate: Long?, productId: String) -> Unit,
) {
    sealed class PurchaseSource {
        data class Internal(
            val productId: String,
            val state: PaywallViewState,
        ) : PurchaseSource() {
            val paywallInfo: PaywallInfo
                get() = state.info
        }

        data class ExternalPurchase(
            val product: StoreProduct,
        ) : PurchaseSource()

        data class ObserverMode(
            val product: StoreProduct,
        ) : PurchaseSource()
    }

    interface Factory :
        OptionsFactory,
        TriggerFactory,
        TransactionVerifierFactory,
        StoreTransactionFactory,
        DeviceHelperFactory,
        CacheFactory,
        HasExternalPurchaseControllerFactory,
        HasInternalPurchaseControllerFactory,
        WebToAppFactory,
        AttributesFactory,
        EnrichmentFactory

    private var transactionsInProgress: ConcurrentHashMap<String, ProductDetails> =
        ConcurrentHashMap()

    private val shouldObserveTransactionFinishingAutomatically: Boolean
        get() = factory.makeSuperwallOptions().shouldObservePurchases

    init {
        if (shouldObserveTransactionFinishingAutomatically
        ) {
            ioScope.launch {
                storeManager.billing.purchaseResults
                    .asSharedFlow()
                    .dropWhile {
                        transactionsInProgress.isEmpty()
                    }.filterNotNull()
                    .collectLatest { it: InternalPurchaseResult ->
                        val state =
                            when (it) {
                                is InternalPurchaseResult.Purchased -> {
                                    it.purchase.products.forEach {
                                        transactionsInProgress.remove(it)
                                    }
                                    PurchasingObserverState.PurchaseResult(
                                        BillingResult
                                            .newBuilder()
                                            .setResponseCode(BillingClient.BillingResponseCode.OK)
                                            .build(),
                                        listOf(it.purchase),
                                    )
                                }

                                is InternalPurchaseResult.Cancelled -> {
                                    val last = transactionsInProgress.entries.last()
                                    transactionsInProgress.remove(last.key)
                                    PurchasingObserverState.PurchaseResult(
                                        BillingResult
                                            .newBuilder()
                                            .setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
                                            .build(),
                                        emptyList(),
                                    )
                                }

                                else -> {
                                    val last = transactionsInProgress.entries.last()
                                    transactionsInProgress.remove(last.key)
                                    PurchasingObserverState.PurchaseError(
                                        error =
                                            (it as? InternalPurchaseResult.Failed)?.error
                                                ?: Throwable("Unknown error"),
                                        product = last.value,
                                    )
                                }
                            }
                        handle(it, state)
                    }
            }
        }
    }

    internal suspend fun handle(
        result: InternalPurchaseResult,
        state: PurchasingObserverState,
    ) {
        if (transactionsInProgress.isEmpty()) {
            Logger.debug(
                LogLevel.error,
                LogScope.paywallTransactions,
                "Transactions in progress is empty but a transaction was attempted to be handled." +
                    " This is likely a bug. Have you tracked the purchase start?",
            )
            return
        }

        val lastProduct = transactionsInProgress.entries.last()
        when (result) {
            is InternalPurchaseResult.Purchased -> {
                refreshReceipt()
                transactionsInProgress.remove(lastProduct.key)
                val state = state as PurchasingObserverState.PurchaseResult
                state.purchases?.forEach { purchase ->
                    purchase.products.map {
                        storeManager.getProductFromCache(it)?.let { product ->
                            didPurchase(
                                product,
                                PurchaseSource.ObserverMode(product),
                                product.hasFreeTrial,
                                purchase,
                            )
                        }
                    }
                }
            }

            is InternalPurchaseResult.Cancelled -> {
                val product = StoreProduct(RawStoreProduct.from(lastProduct.value))
                trackCancelled(
                    product = product,
                    purchaseSource = PurchaseSource.ObserverMode(product),
                )
                transactionsInProgress.remove(lastProduct.key)
            }

            is InternalPurchaseResult.Failed -> {
                transactionsInProgress.remove(lastProduct.key)
                val state = state as PurchasingObserverState.PurchaseError
                val product = StoreProduct(RawStoreProduct.from(state.product))
                trackFailure(
                    state.error.localizedMessage
                        ?.let {
                            try {
                                PlayBillingErrors.fromCode(it)
                            } catch (e: Throwable) {
                                Logger.debug(
                                    LogLevel.error,
                                    LogScope.nativePurchaseController,
                                    message = "Play store issue occured, code: $it",
                                )
                                null
                            }
                        }?.message ?: "Unknown error",
                    product,
                    PurchaseSource.ObserverMode(product),
                )
            }

            InternalPurchaseResult.Pending -> {
                val result = state as PurchasingObserverState.PurchaseResult
                transactionsInProgress.remove(lastProduct.key)
                result.purchases?.forEach { purchase ->
                    purchase.products.map {
                        storeManager.getProductFromCache(it)?.let { product ->
                            handlePendingTransaction(PurchaseSource.ObserverMode(product))
                        }
                    }
                }
            }

            InternalPurchaseResult.Restored -> {
                val state = state as PurchasingObserverState.PurchaseResult
                transactionsInProgress.remove(lastProduct.key)
                state.purchases?.forEach { purchase ->
                    purchase.products.map {
                        storeManager.getProductFromCache(it)?.let { product ->
                            didRestore(product, PurchaseSource.ObserverMode(product))
                        }
                    }
                }
            }
        }
    }

    fun updatePaymentQueue(removedTransactions: List<String>) {
        var stored = storage.read(PurchasingProductdIds)
        val remainingTransactions =
            stored?.filter { transaction ->
                !removedTransactions.any { it == transaction }
            } ?: emptyList()
        storage.write(PurchasingProductdIds, remainingTransactions.toSet())
    }

    suspend fun purchase(purchaseSource: PurchaseSource): PurchaseResult {
        val product =
            when (purchaseSource) {
                is PurchaseSource.Internal ->
                    storeManager.getProductFromCache(purchaseSource.productId) ?: run {
                        log(
                            LogLevel.error,
                            "Trying to purchase (${purchaseSource.productId}) but the product has failed to load. Visit https://superwall.com/l/missing-products to diagnose.",
                        )
                        return PurchaseResult.Failed("Product not found")
                    }

                is PurchaseSource.ExternalPurchase -> {
                    purchaseSource.product
                }

                is PurchaseSource.ObserverMode -> purchaseSource.product
            }
        val rawStoreProduct = product.rawStoreProduct
        log(
            message =
                "!!! Purchasing product ${rawStoreProduct.hasFreeTrial}",
        )
        val productDetails = rawStoreProduct.underlyingProductDetails
        val activity =
            activityProvider.getCurrentActivity()
                ?: return PurchaseResult.Failed("Activity not found - required for starting the billing flow")

        prepareToPurchase(product, purchaseSource)
        val result =
            storeManager.purchaseController.purchase(
                activity = activity,
                productDetails = productDetails,
                offerId = rawStoreProduct.offerId,
                basePlanId = rawStoreProduct.basePlanId,
            )

        if (purchaseSource is PurchaseSource.ExternalPurchase &&
            factory.makeHasExternalPurchaseController() &&
            !factory.makeHasInternalPurchaseController()
        ) {
            return result
        }

        val isEligibleForTrial = rawStoreProduct.selectedOffer != null

        when (result) {
            is PurchaseResult.Purchased -> {
                didPurchase(product, purchaseSource, isEligibleForTrial && product.hasFreeTrial)
            }

            is PurchaseResult.Failed -> {
                val superwallOptions = factory.makeSuperwallOptions()
                val shouldShowPurchaseFailureAlert =
                    superwallOptions.paywalls.shouldShowPurchaseFailureAlert
                val triggers = factory.makeTriggers()
                val transactionFailExists =
                    triggers.contains(SuperwallEvents.TransactionFail.rawName)
                if (shouldShowPurchaseFailureAlert && !transactionFailExists) {
                    trackFailure(
                        result.errorMessage,
                        product,
                        purchaseSource,
                    )
                    if (purchaseSource is PurchaseSource.Internal) {
                        presentAlert(
                            Error(result.errorMessage),
                            product,
                            purchaseSource.state,
                        )
                    }
                } else {
                    trackFailure(
                        result.errorMessage,
                        product,
                        purchaseSource,
                    )
                    if (purchaseSource is PurchaseSource.Internal) {
                        updateState(
                            purchaseSource.paywallInfo.cacheKey,
                            PaywallViewState.Updates.ToggleSpinner(hidden = true),
                        )
                    }
                }
            }

            is PurchaseResult.Pending -> {
                handlePendingTransaction(purchaseSource)
            }

            is PurchaseResult.Cancelled -> {
                trackCancelled(product, purchaseSource)
            }
        }
        return result
    }

    private suspend fun didRestore(
        product: StoreProduct? = null,
        purchaseSource: PurchaseSource,
    ) {
        val purchasingCoordinator = factory.makeTransactionVerifier()
        var transaction: StoreTransaction?
        val restoreType: RestoreType

        if (product != null) {
            // Product exists so much have been via a purchase of a specific product.
            transaction =
                purchasingCoordinator.getLatestTransaction(
                    factory = factory,
                )
            restoreType = RestoreType.ViaPurchase(transaction)
        } else {
            // Otherwise it was a generic restore.
            restoreType = RestoreType.ViaRestore
        }

        val trackedEvent =
            InternalSuperwallEvent.Transaction(
                state = InternalSuperwallEvent.Transaction.State.Restore(restoreType),
                paywallInfo = if (purchaseSource is PurchaseSource.Internal) purchaseSource.paywallInfo else PaywallInfo.empty(),
                product = product,
                model = null,
                isObserved = factory.makeSuperwallOptions().shouldObservePurchases,
                source =
                    when (purchaseSource) {
                        is PurchaseSource.ExternalPurchase -> TransactionSource.EXTERNAL
                        is PurchaseSource.Internal -> TransactionSource.INTERNAL
                        is PurchaseSource.ObserverMode -> TransactionSource.OBSERVER
                    },
                demandScore = factory.demandScore(),
                demandTier = factory.demandTier(),
            )
        track(trackedEvent)

        val superwallOptions = factory.makeSuperwallOptions()
        if (superwallOptions.paywalls.automaticallyDismiss && purchaseSource is PurchaseSource.Internal) {
            dismiss(
                purchaseSource.paywallInfo.cacheKey,
                PaywallResult.Restored(),
            )
        }
    }

    private fun trackFailure(
        errorMessage: String,
        product: StoreProduct,
        purchaseSource: PurchaseSource,
    ) {
        when (purchaseSource) {
            is PurchaseSource.Internal -> {
                log(
                    message =
                        "Transaction Error: $errorMessage",
                    info =
                        mapOf(
                            "product_id" to product.fullIdentifier,
                            "paywall_state" to purchaseSource.state,
                        ),
                )

                ioScope.launchWithTracking {
                    val paywallInfo = purchaseSource.paywallInfo
                    val trackedEvent =
                        InternalSuperwallEvent.Transaction(
                            state =
                                InternalSuperwallEvent.Transaction.State.Fail(
                                    TransactionError.Failure(
                                        errorMessage,
                                        product,
                                    ),
                                ),
                            paywallInfo = paywallInfo,
                            product = product,
                            model = null,
                            isObserved = factory.makeSuperwallOptions().shouldObservePurchases,
                            source = TransactionSource.INTERNAL,
                            demandScore = factory.demandScore(),
                            demandTier = factory.demandTier(),
                        )
                    val fail = (trackedEvent.state as InternalSuperwallEvent.Transaction.State.Fail)
                    track(trackedEvent)
                }
            }

            is PurchaseSource.ExternalPurchase, is PurchaseSource.ObserverMode -> {
                log(
                    message = "Transaction Error: $errorMessage",
                    info = mapOf("product_id" to product.fullIdentifier),
                    error = Error(errorMessage),
                )
                ioScope.launch {
                    val trackedEvent =
                        InternalSuperwallEvent.Transaction(
                            state =
                                InternalSuperwallEvent.Transaction.State.Fail(
                                    TransactionError.Failure(
                                        errorMessage,
                                        product,
                                    ),
                                ),
                            paywallInfo = PaywallInfo.empty(),
                            product = product,
                            model = null,
                            isObserved = purchaseSource is PurchaseSource.ObserverMode,
                            source = TransactionSource.EXTERNAL,
                            demandScore = factory.demandScore(),
                            demandTier = factory.demandTier(),
                        )
                    track(trackedEvent)
                }
            }
        }
    }

    internal suspend fun prepareToPurchase(
        product: StoreProduct,
        source: PurchaseSource,
    ) {
        val isObserved =
            source is PurchaseSource.ObserverMode

        when (source) {
            is PurchaseSource.Internal -> {
                ioScope.launch {
                    log(
                        message =

                            "Transaction Purchasing",
                        info = mapOf("paywall_vc" to source),
                    )
                }

                val paywallInfo = source.paywallInfo
                val trackedEvent =
                    InternalSuperwallEvent.Transaction(
                        InternalSuperwallEvent.Transaction.State.Start(product),
                        paywallInfo,
                        product,
                        null,
                        isObserved = isObserved,
                        source = TransactionSource.INTERNAL,
                        demandScore = factory.demandScore(),
                        demandTier = factory.demandTier(),
                    )
                track(trackedEvent)
            }

            is PurchaseSource.ExternalPurchase, is PurchaseSource.ObserverMode -> {
                if (isObserved) {
                    transactionsInProgress.put(
                        product.fullIdentifier,
                        product.rawStoreProduct.underlyingProductDetails,
                    )
                }
                if (!storeManager.hasCached(product.fullIdentifier)) {
                    storeManager.cacheProduct(product.fullIdentifier, product)
                }

                if (factory.makeHasExternalPurchaseController() && !factory.makeHasInternalPurchaseController()) {
                    return
                }
                // If an external purchase controller is being used, skip because this will
                // get called by the purchase function of the purchase controller.
                val options = factory.makeSuperwallOptions()
                if (!options.shouldObservePurchases && factory.makeHasExternalPurchaseController()) {
                    return
                }

                ioScope.launch {
                    log(
                        message =

                            "External Transaction Purchasing",
                    )
                }

                val trackedEvent =
                    InternalSuperwallEvent.Transaction(
                        InternalSuperwallEvent.Transaction.State.Start(product),
                        PaywallInfo.empty(),
                        product,
                        null,
                        isObserved = isObserved,
                        source = TransactionSource.EXTERNAL,
                        demandScore = factory.demandScore(),
                        demandTier = factory.demandTier(),
                    )
                track(trackedEvent)
            }
        }
    }

    private suspend fun didPurchase(
        product: StoreProduct,
        purchaseSource: PurchaseSource,
        didStartFreeTrial: Boolean,
        purchase: Purchase? = null,
    ) {
        when (purchaseSource) {
            is PurchaseSource.Internal -> {
                ioScope.launch {
                    log(
                        message = "Transaction Succeeded",
                        info =
                            mapOf(
                                "product_id" to product.fullIdentifier,
                                "paywall_state" to purchaseSource.state,
                            ),
                    )
                }

                val transactionVerifier = factory.makeTransactionVerifier()
                val transaction =
                    transactionVerifier.getLatestTransaction(
                        factory = factory,
                    )

                storeManager.loadPurchasedProducts(allEntitlementsByProductId())

                trackTransactionDidSucceed(transaction, product, purchaseSource, didStartFreeTrial)

                if (factory.makeSuperwallOptions().paywalls.automaticallyDismiss) {
                    dismiss(
                        purchaseSource.paywallInfo.cacheKey,
                        PaywallResult.Purchased(product.fullIdentifier),
                    )
                }
            }

            is PurchaseSource.ExternalPurchase, is PurchaseSource.ObserverMode -> {
                log(
                    message = "Transaction Succeeded",
                    info = mapOf("product_id" to product.fullIdentifier),
                )
                val transactionVerifier = factory.makeTransactionVerifier()
                val transaction =
                    if (purchaseSource is PurchaseSource.ExternalPurchase) {
                        transactionVerifier.getLatestTransaction(
                            factory = factory,
                        )
                        if (purchase != null) {
                            factory.makeStoreTransaction(purchase)
                        } else {
                            transactionVerifier.getLatestTransaction(
                                factory = factory,
                            )
                        }
                    } else {
                        null
                    }
                storeManager.loadPurchasedProducts(allEntitlementsByProductId())

                trackTransactionDidSucceed(transaction, product, purchaseSource, didStartFreeTrial)
            }
        }
    }

    private suspend fun trackCancelled(
        product: StoreProduct,
        purchaseSource: PurchaseSource,
    ) {
        val isObserved =
            purchaseSource is PurchaseSource.ObserverMode

        when (purchaseSource) {
            is PurchaseSource.Internal -> {
                ioScope.launch {
                    log(
                        message = "Transaction Abandoned",
                        info =
                            mapOf(
                                "product_id" to product.fullIdentifier,
                                "paywall_state" to purchaseSource.state,
                            ),
                    )
                }

                val paywallInfo = purchaseSource.paywallInfo
                val trackedEvent =
                    InternalSuperwallEvent.Transaction(
                        InternalSuperwallEvent.Transaction.State.Abandon(product),
                        paywallInfo,
                        product,
                        null,
                        isObserved = isObserved,
                        source = TransactionSource.INTERNAL,
                        demandScore = factory.demandScore(),
                        demandTier = factory.demandTier(),
                    )
                track(trackedEvent)

                updateState(
                    paywallInfo.cacheKey,
                    PaywallViewState.Updates.SetLoadingState(
                        PaywallLoadingState.Ready,
                    ),
                )
            }

            is PurchaseSource.ExternalPurchase, is PurchaseSource.ObserverMode -> {
                ioScope.launch {
                    log(
                        message = "Transaction Abandoned",
                        info = mapOf("product_id" to product.fullIdentifier),
                    )
                }

                val trackedEvent =
                    InternalSuperwallEvent.Transaction(
                        InternalSuperwallEvent.Transaction.State.Abandon(product),
                        PaywallInfo.empty(),
                        product,
                        null,
                        isObserved = isObserved,
                        source = TransactionSource.EXTERNAL,
                        demandScore = factory.demandScore(),
                        demandTier = factory.demandTier(),
                    )
                track(trackedEvent)
            }
        }
    }

    private suspend fun handlePendingTransaction(purchaseSource: PurchaseSource) {
        val isObserved =
            purchaseSource is PurchaseSource.ObserverMode

        when (purchaseSource) {
            is PurchaseSource.Internal -> {
                ioScope.launch {
                    log(
                        message = "Transaction Pending",
                        info = mapOf("paywall" to purchaseSource.paywallInfo),
                    )
                }

                val paywallInfo = purchaseSource.paywallInfo

                val trackedEvent =
                    InternalSuperwallEvent.Transaction(
                        InternalSuperwallEvent.Transaction.State.Fail(TransactionError.Pending("Needs parental approval")),
                        paywallInfo,
                        null,
                        null,
                        isObserved = factory.makeSuperwallOptions().shouldObservePurchases,
                        source = TransactionSource.INTERNAL,
                        demandScore = factory.demandScore(),
                        demandTier = factory.demandTier(),
                    )
                track(trackedEvent)

                showAlert(
                    AlertProps(
                        "Waiting for Approval",
                        "Thank you! This purchase is pending approval from your parent. Please try again once it is approved.",
                    ),
                )
            }

            is PurchaseSource.ExternalPurchase,
            is PurchaseSource.ObserverMode,
            -> {
                ioScope.launch {
                    log(message = "Transaction Pending")
                }

                val trackedEvent =
                    InternalSuperwallEvent.Transaction(
                        InternalSuperwallEvent.Transaction.State.Fail(TransactionError.Pending("Needs parental approval")),
                        PaywallInfo.empty(),
                        null,
                        null,
                        isObserved = isObserved,
                        source = TransactionSource.EXTERNAL,
                        demandScore = factory.demandScore(),
                        demandTier = factory.demandTier(),
                    )
                track(trackedEvent)
            }
        }
    }

    /**
     * Attempt to restore purchases.
     *
     * @param paywallView The paywall view that initiated the restore or null if initiated externally.
     * @return A [RestorationResult] indicating the result of the restoration.
     */
    suspend fun tryToRestorePurchases(paywallView: PaywallView?): RestorationResult {
        log(message = "Attempting Restore")
        val paywallInfo = paywallView?.state?.info ?: PaywallInfo.empty()
        paywallView?.updateState(PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.LoadingPurchase))

        track(
            InternalSuperwallEvent.Restore(
                state = InternalSuperwallEvent.Restore.State.Start,
                paywallInfo = paywallInfo,
            ),
        )
        val restorationResult = purchaseController.restorePurchases()

        val hasRestored = restorationResult is RestorationResult.Restored
        val status = subscriptionStatus()
        val hasEntitlements =
            status is SubscriptionStatus.Active
        storeManager.loadPurchasedProducts(allEntitlementsByProductId())

        val webToAppEnabled = factory.isWebToAppEnabled()
        val allPaywallEntitlements =
            paywallView
                ?.state
                ?.paywall
                ?.productIds
                ?.map {
                    entitlementsById(it)
                }?.flatten() ?: emptyList()
        val existingEntitlements =
            (status as? SubscriptionStatus.Active)?.entitlements ?: emptySet()
        if (hasRestored && hasEntitlements) {
            if (existingEntitlements.containsAll(allPaywallEntitlements) || !webToAppEnabled) {
                log(message = "Transactions Restored")
                track(
                    InternalSuperwallEvent.Restore(
                        state = InternalSuperwallEvent.Restore.State.Complete,
                        paywallInfo = paywallView?.state?.info ?: PaywallInfo.empty(),
                    ),
                )
                if (paywallView != null) {
                    didRestore(null, PurchaseSource.Internal("", paywallView.state))
                }
            } else {
                paywallView?.updateState(
                    PaywallViewState.Updates.SetLoadingState(
                        PaywallLoadingState.Ready,
                    ),
                )
                askToRestoreFromWeb()
            }
        } else {
            val msg = "Transactions Failed to Restore.${
                if (hasRestored && !hasEntitlements) {
                    " The user's subscription status is \"inactive\", but the restoration result is \"restored\"." +
                        " Ensure the subscription status is active before confirming successful restoration."
                } else {
                    " Original restoration error message: ${
                        when (restorationResult) {
                            is RestorationResult.Failed -> restorationResult.error?.localizedMessage
                            else -> null
                        }
                    }"
                }
            }"
            log(message = msg)

            track(
                InternalSuperwallEvent.Restore(
                    state = InternalSuperwallEvent.Restore.State.Failure(msg),
                    paywallInfo = paywallView?.state?.info ?: PaywallInfo.empty(),
                ),
            )
            if (webToAppEnabled) {
                askToRestoreFromWeb()
            } else {
                paywallView?.showAlert(
                    title =
                        factory
                            .makeSuperwallOptions()
                            .paywalls.restoreFailed.title,
                    message =
                        factory
                            .makeSuperwallOptions()
                            .paywalls.restoreFailed.message,
                    closeActionTitle =
                        factory
                            .makeSuperwallOptions()
                            .paywalls.restoreFailed.closeButtonTitle,
                )
            }
        }
        return restorationResult
    }

    private fun askToRestoreFromWeb() {
        val hasEntitlements =
            Superwall.instance.entitlements.activeDeviceEntitlements
                .isNotEmpty()
        val hasSubsText =
            "Your Play Store subscriptions were restored. Would you like to check for more on the web?"
        val noSubsText = "No Play Store subscription found, would you like to check on the web?"

        showAlert(
            AlertProps(
                title =
                    if (hasEntitlements) "Restore via the web?" else "No Subscription Found",
                message =
                    if (hasEntitlements) hasSubsText else noSubsText,
                closeActionTitle =
                    "Close",
                actionTitle = "Yes",
                action = {
                    Superwall.instance.openRestoreOnWeb()
                },
            ),
        )
    }

    private suspend fun presentAlert(
        error: Error,
        product: StoreProduct,
        state: PaywallViewState,
    ) {
        ioScope.launch {
            log(
                message = "Transaction Error",
                info =
                    mapOf(
                        "product_id" to product.fullIdentifier,
                        "paywall_state" to state,
                    ),
                error = error,
            )
        }

        showAlert(
            AlertProps(
                "An error occurred",
                error.message ?: "Unknown error",
            ),
        )
    }

    private suspend fun trackTransactionDidSucceed(
        transaction: StoreTransaction?,
        product: StoreProduct,
        purchaseSource: PurchaseSource,
        didStartFreeTrial: Boolean,
    ) {
        val isObserved =
            purchaseSource is PurchaseSource.ObserverMode

        when (purchaseSource) {
            is PurchaseSource.Internal -> {
                val trialEnd = product.trialPeriodEndDate?.time
                val paywallInfo = purchaseSource.paywallInfo

                val trackedEvent =
                    InternalSuperwallEvent.Transaction(
                        InternalSuperwallEvent.Transaction.State.Complete(product, transaction),
                        paywallInfo,
                        product,
                        transaction,
                        source = TransactionSource.INTERNAL,
                        isObserved = false,
                        demandScore = factory.demandScore(),
                        demandTier = factory.demandTier(),
                        userAttributes = factory.getCurrentUserAttributes(),
                    )
                track(trackedEvent)
                eventsQueue.flushInternal()
                if (product.subscriptionPeriod == null) {
                    val nonRecurringEvent =
                        InternalSuperwallEvent.NonRecurringProductPurchase(
                            paywallInfo,
                            product,
                        )
                    track(nonRecurringEvent)
                } else {
                    notifyOfTransactionComplete(purchaseSource.paywallInfo.cacheKey, trialEnd, product.fullIdentifier)
                    if (didStartFreeTrial) {
                        val freeTrialEvent =
                            InternalSuperwallEvent.FreeTrialStart(paywallInfo, product)
                        track(freeTrialEvent)
                    } else {
                        val subscriptionEvent =
                            InternalSuperwallEvent.SubscriptionStart(paywallInfo, product)
                        track(subscriptionEvent)
                    }
                }
            }

            is PurchaseSource.ExternalPurchase, is PurchaseSource.ObserverMode -> {
                val trackedEvent =
                    InternalSuperwallEvent.Transaction(
                        InternalSuperwallEvent.Transaction.State.Complete(product, transaction),
                        PaywallInfo.empty(),
                        product,
                        transaction,
                        source = TransactionSource.EXTERNAL,
                        isObserved = isObserved,
                        userAttributes = factory.getCurrentUserAttributes(),
                        demandScore = factory.demandScore(),
                        demandTier = factory.demandTier(),
                    )
                track(trackedEvent)
                eventsQueue.flushInternal()

                if (product.subscriptionPeriod == null) {
                    val nonRecurringEvent =
                        InternalSuperwallEvent.NonRecurringProductPurchase(
                            PaywallInfo.empty(),
                            product,
                        )
                    track(nonRecurringEvent)
                } else {
                    if (didStartFreeTrial) {
                        val freeTrialEvent =
                            InternalSuperwallEvent.FreeTrialStart(PaywallInfo.empty(), product)
                        track(freeTrialEvent)
                    } else {
                        val subscriptionEvent =
                            InternalSuperwallEvent.SubscriptionStart(
                                PaywallInfo.empty(),
                                product,
                            )
                        track(subscriptionEvent)
                    }
                }
            }
        }
    }

    private fun log(
        logLevel: LogLevel = LogLevel.debug,
        message: String,
        info: Map<String, Any>? = null,
        error: Throwable? = null,
    ) = Logger.debug(
        logLevel = logLevel,
        scope = LogScope.paywallTransactions,
        message = message,
        info = info,
        error = error,
    )
}
