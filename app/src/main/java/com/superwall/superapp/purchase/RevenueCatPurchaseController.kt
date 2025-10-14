package com.superwall.superapp.purchase

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases.purchaseWith
import com.superwall.sdk.Superwall
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import kotlinx.coroutines.CompletableDeferred

// Extension function to convert callback to suspend function
suspend fun Purchases.awaitProducts(productIds: List<String>): List<StoreProduct> {
    val deferred = CompletableDeferred<List<StoreProduct>>()
    getProducts(
        productIds,
        object : GetStoreProductsCallback {
            override fun onReceived(storeProducts: List<StoreProduct>) {
                deferred.complete(storeProducts)
            }

            override fun onError(error: PurchasesError) {
                // Not sure about this cast...
                deferred.completeExceptionally(Exception(error.message))
            }
        },
    )
    return deferred.await()
}

interface PurchaseCompletion {
    var storeTransaction: StoreTransaction
    var customerInfo: CustomerInfo
}

// Create a custom exception class that wraps PurchasesError
private class PurchasesException(
    val purchasesError: PurchasesError,
) : Exception(purchasesError.toString())

suspend fun Purchases.awaitPurchase(
    activity: Activity,
    storeProduct: StoreProduct,
): PurchaseCompletion {
    val deferred = CompletableDeferred<PurchaseCompletion>()
    purchase(
        PurchaseParams.Builder(activity, storeProduct).build(),
        object : PurchaseCallback {
            override fun onCompleted(
                storeTransaction: StoreTransaction,
                customerInfo: CustomerInfo,
            ) {
                deferred.complete(
                    object : PurchaseCompletion {
                        override var storeTransaction: StoreTransaction = storeTransaction
                        override var customerInfo: CustomerInfo = customerInfo
                    },
                )
            }

            override fun onError(
                error: PurchasesError,
                p1: Boolean,
            ) {
                deferred.completeExceptionally(PurchasesException(error))
            }
        },
    )
    return deferred.await()
}

suspend fun Purchases.awaitRestoration(): CustomerInfo {
    val deferred = CompletableDeferred<CustomerInfo>()
    restorePurchases(
        object : ReceiveCustomerInfoCallback {
            override fun onReceived(purchaserInfo: CustomerInfo) {
                deferred.complete(purchaserInfo)
            }

            override fun onError(error: PurchasesError) {
                deferred.completeExceptionally(error as Throwable)
            }
        },
    )
    return deferred.await()
}

class RevenueCatPurchaseController(
    val context: Context,
) : PurchaseController,
    UpdatedCustomerInfoListener {
    init {
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration
                .Builder(
                    context,
                    "goog_DCSOujJzRNnPmxdgjOwdOOjwilC",
                ).build(),
        )

        // Make sure we get the updates
        Purchases.sharedInstance.updatedCustomerInfoListener = this
    }

    fun syncSubscriptionStatus() {
        // Refetch the customer info on load
        Purchases.sharedInstance.getCustomerInfoWith {
            if (hasAnyActiveEntitlements(it)) {
                setSubscriptionStatus(
                    SubscriptionStatus.Active(
                        it.entitlements.active
                            .map {
                                Entitlement(it.key, Entitlement.Type.SERVICE_LEVEL)
                            }.toSet(),
                    ),
                )
            } else {
                setSubscriptionStatus(SubscriptionStatus.Inactive)
            }
        }
    }

    /**
     * Callback for rc customer updated info
     */
    override fun onReceived(customerInfo: CustomerInfo) {
        if (hasAnyActiveEntitlements(customerInfo)) {
            setSubscriptionStatus(
                SubscriptionStatus.Active(
                    customerInfo.entitlements.active
                        .map {
                            Entitlement(it.key, Entitlement.Type.SERVICE_LEVEL)
                        }.toSet(),
                ),
            )
        } else {
            setSubscriptionStatus(SubscriptionStatus.Inactive)
        }
    }

    /**
     * Initiate a purchase
     */
    override suspend fun purchase(
        activity: Activity,
        productDetails: ProductDetails,
        basePlanId: String?,
        offerId: String?,
    ): PurchaseResult {
        // Find products matching productId from RevenueCat
        val products = Purchases.sharedInstance.awaitProducts(listOf(productDetails.productId))
        // Choose the product which matches the given base plan.
        // If no base plan set, select first product or fail.
        val product =
            products.firstOrNull { it.googleProduct?.basePlanId == basePlanId }
                ?: products.firstOrNull()
                ?: return PurchaseResult.Failed("Product not found")

        return when (product.type) {
            ProductType.SUBS, ProductType.UNKNOWN ->
                handleSubscription(
                    activity,
                    product,
                    basePlanId,
                    offerId,
                )

            ProductType.INAPP -> handleInAppPurchase(activity, product)
        }
    }

    private fun buildSubscriptionOptionId(
        basePlanId: String?,
        offerId: String?,
    ): String =
        buildString {
            basePlanId?.let { append("$it") }
            offerId?.let { append(":$it") }
        }

    private suspend fun handleSubscription(
        activity: Activity,
        storeProduct: StoreProduct,
        basePlanId: String?,
        offerId: String?,
    ): PurchaseResult {
        storeProduct.subscriptionOptions?.let { subscriptionOptions ->
            // If subscription option exists, concatenate base + offer ID.
            val subscriptionOptionId = buildSubscriptionOptionId(basePlanId, offerId)

            // Find first subscription option that matches the subscription option ID or default
            // to letting revenuecat choose.
            val subscriptionOption =
                subscriptionOptions.firstOrNull { it.id == subscriptionOptionId }
                    ?: subscriptionOptions.defaultOffer

            // Purchase subscription option, otherwise fail.
            if (subscriptionOption != null) {
                return purchaseSubscription(activity, subscriptionOption)
            }
        }
        return PurchaseResult.Failed("Valid subscription option not found for product.")
    }

    private suspend fun purchaseSubscription(
        activity: Activity,
        subscriptionOption: SubscriptionOption,
    ): PurchaseResult {
        val deferred = CompletableDeferred<PurchaseResult>()
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, subscriptionOption).build(),
            onError = { error, userCancelled ->
                deferred.complete(
                    if (userCancelled) {
                        PurchaseResult.Cancelled()
                    } else {
                        PurchaseResult.Failed(
                            error.message,
                        )
                    },
                )
            },
            onSuccess = { _, _ ->
                deferred.complete(PurchaseResult.Purchased())
            },
        )
        return deferred.await()
    }

    private suspend fun handleInAppPurchase(
        activity: Activity,
        storeProduct: StoreProduct,
    ): PurchaseResult =
        try {
            Purchases.sharedInstance.awaitPurchase(activity, storeProduct)
            PurchaseResult.Purchased()
        } catch (e: PurchasesException) {
            when (e.purchasesError.code) {
                PurchasesErrorCode.PurchaseCancelledError -> PurchaseResult.Cancelled()
                else ->
                    PurchaseResult.Failed(
                        e.message ?: "Purchase failed due to an unknown error",
                    )
            }
        }

    /**
     * Restore purchases
     */
    override suspend fun restorePurchases(): RestorationResult {
        try {
            if (hasAnyActiveEntitlements(Purchases.sharedInstance.awaitRestoration())) {
                return RestorationResult.Restored()
            } else {
                return RestorationResult.Failed(Exception("No active entitlements"))
            }
        } catch (e: Throwable) {
            return RestorationResult.Failed(e)
        }
    }

    /**
     * Check if the customer has any active entitlements
     */
    private fun hasAnyActiveEntitlements(customerInfo: CustomerInfo): Boolean {
        val entitlements =
            customerInfo.entitlements.active.values
                .map { it.identifier }
        return entitlements.isNotEmpty()
    }

    private fun setSubscriptionStatus(subscriptionStatus: SubscriptionStatus) {
        if (Superwall.initialized) {
            Superwall.instance.setSubscriptionStatus(subscriptionStatus)
        }
    }
}
