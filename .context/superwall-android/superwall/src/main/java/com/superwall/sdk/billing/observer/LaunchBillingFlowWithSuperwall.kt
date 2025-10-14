package com.superwall.sdk.billing.observer

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.superwall.sdk.Superwall
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.store.PurchasingObserverState
import com.superwall.sdk.store.abstractions.product.RawStoreProduct

/**
 * Extension function for BillingClient that launches the Google Play billing flow while allowing Superwall to observe the purchase.
 *
 * This method acts as a proxy between your app's purchase flow and Google Play Billing, enabling Superwall to track the
 * purchase lifecycle when observer mode is enabled. It wraps the standard [BillingClient.launchBillingFlow] method and adds
 * purchase observation capabilities.
 *
 * The method will:
 * 1. Check if Superwall SDK is initialized
 * 2. Verify if purchase observation is enabled via [SuperwallOptions.shouldObservePurchases]
 * 3. Notify Superwall before each product purchase begins
 * 4. Launch the actual billing flow
 *
 * Purchase events can then be observed through [Superwall.delegate] or [Superwall.placements], which will emit events like:
 * - [SuperwallEvent.TransactionStart] when purchase begins
 * - [SuperwallEvent.TransactionComplete] on successful purchase
 * - [SuperwallEvent.TransactionFail] on purchase failure
 *
 * @param activity The activity that will host the billing flow
 * @param params Wrapper around BillingFlowParams containing product details for the purchase
 * @return BillingResult containing the response from launching the billing flow
 * @throws IllegalStateException if Superwall SDK is not initialized
 *
 * @see SuperwallBillingFlowParams
 * @see Superwall.observe
 */
fun BillingClient.launchBillingFlowWithSuperwall(
    activity: Activity,
    params: SuperwallBillingFlowParams,
): BillingResult {
    if (Superwall.initialized.not()) {
        throw IllegalStateException("Superwall SDK is not initialized")
    }
    if (Superwall.instance.options.shouldObservePurchases
            .not()
    ) {
        Logger.debug(
            LogLevel.error,
            LogScope.superwallCore,
            "Observer mode is not enabled. In order to observe purchases, please enable it in the SuperwallOptions by setting `shouldObservePurchases` to true.",
            mapOf(
                "method" to "launchBillingFlowWithSuperwall",
                "products" to
                    params.productDetailsParams
                        .map { it.details.productId }
                        .joinToString(", "),
            ),
        )
        return launchBillingFlow(activity, params.toOriginal())
    }

    params.productDetailsParams.forEach {
        val product = RawStoreProduct.from(it.details)
        Superwall.instance.observe(
            PurchasingObserverState.PurchaseWillBegin(product.underlyingProductDetails),
        )
    }
    return launchBillingFlow(activity, params.toOriginal())
}
