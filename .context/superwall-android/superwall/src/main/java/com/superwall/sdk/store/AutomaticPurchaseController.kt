package com.superwall.sdk.store

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.superwall.sdk.Superwall
import com.superwall.sdk.billing.RECONNECT_TIMER_MAX_TIME_MILLISECONDS
import com.superwall.sdk.billing.RECONNECT_TIMER_START_MILLISECONDS
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.retryOrNull
import com.superwall.sdk.models.customer.toSet
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.store.abstractions.product.BasePlanType
import com.superwall.sdk.store.abstractions.product.OfferType
import com.superwall.sdk.store.abstractions.product.RawStoreProduct
import com.superwall.sdk.store.transactions.PlayBillingErrors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

private val BILLING_INSANTIATION_ERROR =
    """Cannot create Google Play Billing Client. This can be caused by:
    - Play store client not existing on this device
    - User not being signed in into the play store
    - Mismatching Google Play Billing versions"""

class AutomaticPurchaseController(
    var context: Context,
    val scope: IOScope,
    val entitlementsInfo: Entitlements,
    val getBilling: (Context, PurchasesUpdatedListener) -> BillingClient = { ctx, listener ->
        try {
            BillingClient
                .newBuilder(ctx)
                .setListener(listener)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
                ).build()
        } catch (e: Throwable) {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.nativePurchaseController,
                message = BILLING_INSANTIATION_ERROR,
                info = mapOf("error_message" to (e.message ?: "Unknown message")),
                error = e,
            )
            throw e
        }
    },
) : PurchaseController,
    PurchasesUpdatedListener {
    companion object {
        private const val QUERY_TIMEOUT_MS = 10_000L
        private const val MAX_RETRIES = 3
    }

    private var billingClient: BillingClient = getBilling(context, this)

    private val isConnected = MutableStateFlow(false)
    private val purchaseResults = MutableStateFlow<PurchaseResult?>(null)

    // how long before the data source tries to reconnect to Google play
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    //region Initialization

    init {
        scope.launch {
            startConnection()
        }
    }

    private fun startConnection() {
        try {
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        isConnected.value =
                            billingResult.responseCode == BillingClient.BillingResponseCode.OK
                        syncSubscriptionStatus()
                    }

                    override fun onBillingServiceDisconnected() {
                        isConnected.value = false

                        Logger.debug(
                            LogLevel.error,
                            LogScope.nativePurchaseController,
                            "ExternalNativePurchaseController billing client disconnected, " +
                                "retrying in $reconnectMilliseconds milliseconds",
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            delay(reconnectMilliseconds)
                            startConnection()
                        }

                        reconnectMilliseconds =
                            min(
                                reconnectMilliseconds * 2,
                                RECONNECT_TIMER_MAX_TIME_MILLISECONDS,
                            )
                    }
                },
            )
        } catch (e: IllegalStateException) {
            Logger.debug(
                LogLevel.error,
                LogScope.nativePurchaseController,
                "IllegalStateException when connecting to billing client for " + "ExternalNativePurchaseController: ${e.message}",
            )
        }
    }

    //endregion

    //region Public

    private fun syncSubscriptionStatus() {
        scope.launch {
            Superwall.hasInitialized.first { it }
            syncSubscriptionStatusAndWait()
        }
    }

    //endregion

    //region PurchaseController

    private fun buildFullId(
        subscriptionId: String,
        basePlanId: String?,
        offerId: String?,
    ): String =
        buildString {
            append(subscriptionId)
            basePlanId?.let { append(":$it") }
            offerId?.let { append(":$it") }
        }

    override suspend fun purchase(
        activity: Activity,
        productDetails: ProductDetails,
        basePlanId: String?,
        offerId: String?,
    ): PurchaseResult {
        // Clear previous purchase results to avoid emitting old results
        purchaseResults.value = null

        val fullId =
            buildFullId(
                subscriptionId = productDetails.productId,
                basePlanId = basePlanId,
                offerId = offerId,
            )

        val rawStoreProduct =
            RawStoreProduct(
                underlyingProductDetails = productDetails,
                fullIdentifier = fullId,
                basePlanType = BasePlanType.from(basePlanId),
                offerType = OfferType.from(offerId),
            )

        val offerToken =
            when (val offer = rawStoreProduct.selectedOffer) {
                is RawStoreProduct.SelectedOfferDetails.Subscription -> offer.underlying.offerToken
                is RawStoreProduct.SelectedOfferDetails.OneTime -> {
                    // For OTP with purchase options, we need the offerToken to specify which
                    // purchase option to use, even when there's no discount offer (offerId=null).
                    // Only skip offerToken for legacy OTPs without purchase options.
                    if (offer.purchaseOptionId != null || offerId != null) {
                        offer.underlying.offerToken
                    } else {
                        null
                    }
                }

                null -> null
            }

        val hasOfferToken = !offerToken.isNullOrEmpty()

        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(productDetails)
                .also {
                    // Set offer token if we have one (for both subscriptions and OTP with purchase options)
                    // Don't set empty token as Google Play doesn't support it
                    if (hasOfferToken) {
                        it.setOfferToken(offerToken!!)
                    }
                }.build()

        val shouldPassIdToPlayStore =
            try {
                Superwall.instance.options.passIdentifiersToPlayStore
            } catch (e: Throwable) {
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.nativePurchaseController,
                    message = "Error getting Superwall options",
                )
                false
            }

        val id =
            try {
                Superwall.instance.userId
            } catch (e: Throwable) {
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.nativePurchaseController,
                    message = "Error getting userId",
                )
                null
            }
        val flowParams =
            BillingFlowParams
                .newBuilder()
                .apply {
                    setObfuscatedAccountId(Superwall.instance.externalAccountId)
                }.setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

        Logger.debug(
            logLevel = LogLevel.info,
            scope = LogScope.nativePurchaseController,
            message = "Waiting for billing client to be connected",
        )

        // Wait until the billing client becomes connected
        isConnected.first { it }

        Logger.debug(
            logLevel = LogLevel.info,
            scope = LogScope.nativePurchaseController,
            message = "Billing client is connected",
        )

        billingClient.launchBillingFlow(activity, flowParams)

        // Wait until a purchase result is emitted before returning the result
        val value = purchaseResults.first { it != null } ?: PurchaseResult.Failed("Purchase failed")

        return value
    }

    override suspend fun restorePurchases(): RestorationResult {
        syncSubscriptionStatusAndWait()
        return RestorationResult.Restored()
    }

//endregion

//region PurchasesUpdatedListener

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        // Determine the result based on the billing response code
        val result =
            when (billingResult.responseCode) {
                // If the purchase was successful, acknowledge any necessary purchases and create a Purchased result
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.let { acknowledgePurchasesIfNecessary(it) }
                    PurchaseResult.Purchased()
                }

                // If the user canceled the purchase, create a Cancelled result
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    PurchaseResult.Cancelled()
                }

                // For all other response codes, create a Failed result with an exception
                else -> {
                    PurchaseResult.Failed(
                        PlayBillingErrors.fromCode(billingResult.responseCode)?.message
                            ?: "Unknown error ${billingResult.responseCode}",
                    )
                }
            }

        scope.launch {
            // Emit the purchase result to any observers
            purchaseResults.emit(result)

            // Sync the subscription status with the server
            syncSubscriptionStatus()
        }
    }

//endregion

//region Private

    private suspend fun syncSubscriptionStatusAndWait(count: Int = 0) {
        // We await for configuration to be set so our entitlements are available
        Superwall.instance.configurationStateListener.first { it is ConfigurationStatus.Configured }
        val subscriptionPurchases =
            retryOrNull(MAX_RETRIES) { queryPurchasesOfType(BillingClient.ProductType.SUBS).getOrThrow() }
        val inAppPurchases =
            retryOrNull(MAX_RETRIES) { queryPurchasesOfType(BillingClient.ProductType.INAPP).getOrThrow() }
        val failed = subscriptionPurchases == null || inAppPurchases == null
        val allPurchases = (subscriptionPurchases ?: emptyList()) + (inAppPurchases ?: emptyList())
        val hasActivePurchaseOrSubscription =
            allPurchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.nativePurchaseController,
            message = "Found purchases: ${
                allPurchases.mapIndexed { i, it ->
                    val items = it.products.joinToString(",")
                    "<$i. Products: $items id: ${it.orderId} time: ${it.purchaseTime} state: ${it.purchaseState} >"
                }
            }",
        )

        val status: SubscriptionStatus =
            if (hasActivePurchaseOrSubscription) {
                allPurchases
                    .flatMap {
                        it.products
                    }.toSet()
                    .flatMap {
                        val res = entitlementsInfo.byProductId(it)
                        res
                    }.toSet()
                    .let { entitlements ->
                        Logger.debug(
                            logLevel = LogLevel.debug,
                            scope = LogScope.nativePurchaseController,
                            message = "Found entitlements: ${entitlements.joinToString { it.id }}",
                        )

                        entitlementsInfo.activeDeviceEntitlements = entitlements
                        if (entitlements.isNotEmpty()) {
                            SubscriptionStatus.Active(
                                entitlements.map { it.copy(isActive = true) }.toSet(),
                            )
                        } else {
                            SubscriptionStatus.Inactive
                        }
                    }
            } else {
                SubscriptionStatus.Inactive
            }
        if (!Superwall.initialized) {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.nativePurchaseController,
                message = "Attempting to sync subscription status before Superwall has been initialized.",
            )
            return
        }

        Superwall.instance.internallySetSubscriptionStatus(status)

        if (failed && count < MAX_RETRIES) {
            scope.launch {
                delay(count.seconds)
                syncSubscriptionStatusAndWait(count + 1)
            }
        }
    }

    private suspend fun queryPurchasesOfType(productType: String): Result<List<Purchase>> {
        val deferred = CompletableDeferred<Result<List<Purchase>>>()

        val params = QueryPurchasesParams.newBuilder().setProductType(productType).build()

        if (!billingClient.isReady) {
            return Result.failure(IllegalStateException("Billing client not ready"))
        }

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.nativePurchaseController,
                    message = "Unable to query for purchases.",
                )
                deferred.complete(Result.failure(Throwable("Billing query failed with code ${billingResult.responseCode}")))
                return@queryPurchasesAsync
            }
            deferred.complete(Result.success(purchasesList))
        }

        return withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            deferred.await()
        } ?: Result.failure(IllegalStateException("Query purchases timed out"))
    }

    private fun acknowledgePurchasesIfNecessary(purchases: List<Purchase>) {
        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.isAcknowledged == false }
            .forEach { purchase ->
                val acknowledgePurchaseParams =
                    AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        Logger.debug(
                            logLevel = LogLevel.error,
                            scope = LogScope.nativePurchaseController,
                            message = "Unable to acknowledge purchase.",
                        )
                    }
                }
            }
    }

//endregion
}
