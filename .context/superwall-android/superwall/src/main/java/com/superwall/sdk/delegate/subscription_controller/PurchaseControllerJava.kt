package com.superwall.sdk.delegate.subscription_controller

import com.android.billingclient.api.ProductDetails
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult

/**
 * The Kotlin-only interface that handles Superwall's subscription-related logic.
 *
 * By default, the Superwall SDK handles all subscription-related logic. However, if you'd like
 * more control, you can return a `PurchaseControllerJava` when configuring the SDK via
 * `Superwall.instance.configure(apiKey, purchaseController, options, completion)`.
 *
 * When implementing this, you also need to set the subscription status using
 * `Superwall.instance.subscriptionStatus`.
 *
 * To learn how to implement the `PurchaseControllerJava` in your app
 * and best practices, see [Purchases and Subscription Status](https://docs.superwall.com/docs/advanced-configuration).
 */
interface PurchaseControllerJava {
    /**
     * Called when the user initiates purchasing of a product.
     *
     * Add your purchase logic here and call the completion lambda with the result. You can use Google's Play Billing APIs,
     * or if you use RevenueCat, you can call `Purchases.instance.purchase(product)`.
     * - Parameters:
     *   @param productDetails The `ProductDetails` the user would like to purchase.
     *   @param basePlanId An optional base plan identifier of the product that's being purchased.
     *   @param offerId An optional offer identifier of the product that's being purchased.
     *   Call this with the result of your purchase logic. When you pass a `.failed` result, make sure you also pass
     *   the error.
     *    **Note:** Make sure you handle all cases of `PurchaseResultJava`.
     */
    fun purchase(
        productDetails: ProductDetails,
        basePlanId: String?,
        offerId: String?,
        completion: (PurchaseResult) -> Unit,
    )

    /**
     * Called when the user initiates a restore.
     *
     * Add your restore logic here, making sure that the user's subscription status is updated after restore,
     * and call the completion lambda.
     *
     * - Parameters:
     *   - completion: A lambda that accepts two objects. 1. A `RestorationResultJava` that's `.restored` if the user's purchases were restored or `.failed` if they weren't. 2. An optional error that you can return when the restore failed.
     * **Note**: `restored` does not imply the user has an active subscription, it just mean the restore had no errors.
     */
    fun restorePurchases(completion: (RestorationResult, Throwable?) -> Unit)
}
