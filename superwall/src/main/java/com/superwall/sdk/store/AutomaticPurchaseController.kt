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
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.store.abstractions.product.OfferType
import com.superwall.sdk.store.abstractions.product.RawStoreProduct
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AutomaticPurchaseController(
    var context: Context,
    val scope: IOScope,
    val entitlementsInfo: Entitlements,
) : PurchaseController,
    PurchasesUpdatedListener {
    private var billingClient: BillingClient =
        BillingClient
            .newBuilder(context)
            .setListener(this)
            .enableAutoServiceReconnection()
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

    private val isConnected = MutableStateFlow(false)
    private val purchaseResults = MutableStateFlow<PurchaseResult?>(null)

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
                                "autoretrying.",
                        )
                    }
                },
            )
        } catch (e: IllegalStateException) {
            Logger.debug(
                LogLevel.error,
                LogScope.nativePurchaseController,
                "IllegalStateException when connecting to billing client for " +
                    "ExternalNativePurchaseController: ${e.message}",
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
                basePlanId = basePlanId ?: "",
                offerType = offerId?.let { OfferType.Offer(id = it) },
            )

        val offerToken = rawStoreProduct.selectedOffer?.offerToken

        val isOneTime =
            productDetails.productType == BillingClient.ProductType.INAPP && offerToken.isNullOrEmpty()

        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(productDetails)
                .also {
                    // Do not set empty offer token for one time products
                    // as Google play is not supporting it since June 12th 2024
                    if (!isOneTime && offerToken != null) {
                        it.setOfferToken(offerToken)
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
                    PurchaseResult.Failed(billingResult.responseCode.toString())
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

    private suspend fun syncSubscriptionStatusAndWait() {
        // We await for configuration to be set so our entitlements are available
        Superwall.instance.configurationStateListener.first { it is ConfigurationStatus.Configured }
        val subscriptionPurchases = queryPurchasesOfType(BillingClient.ProductType.SUBS)
        val inAppPurchases = queryPurchasesOfType(BillingClient.ProductType.INAPP)
        val allPurchases = subscriptionPurchases + inAppPurchases

        val hasActivePurchaseOrSubscription =
            allPurchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val status: SubscriptionStatus =
            if (hasActivePurchaseOrSubscription) {
                subscriptionPurchases
                    .flatMap {
                        it.products
                    }.toSet()
                    .flatMap {
                        val res = entitlementsInfo.byProductId(it)
                        res
                    }.toSet()
                    .let { entitlements ->
                        entitlementsInfo.activeDeviceEntitlements = entitlements
                        if (entitlements.isNotEmpty()) {
                            SubscriptionStatus.Active(entitlements)
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
    }

    private suspend fun queryPurchasesOfType(productType: String): List<Purchase> {
        val deferred = CompletableDeferred<List<Purchase>>()

        val params =
            QueryPurchasesParams
                .newBuilder()
                .setProductType(productType)
                .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.nativePurchaseController,
                    message = "Unable to query for purchases.",
                )
                return@queryPurchasesAsync
            }

            deferred.complete(purchasesList)
        }

        return deferred.await()
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
