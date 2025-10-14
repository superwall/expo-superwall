package com.superwall.sdk.store

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.delegate.subscription_controller.PurchaseControllerJava
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class InternalPurchaseController(
    private val kotlinPurchaseController: PurchaseController?,
    private val javaPurchaseController: PurchaseControllerJava?,
    val context: Context,
) : PurchaseController {
    val hasExternalPurchaseController: Boolean
        get() = !hasInternalPurchaseController

    val hasInternalPurchaseController: Boolean
        get() = kotlinPurchaseController is AutomaticPurchaseController

    override suspend fun purchase(
        activity: Activity,
        productDetails: ProductDetails,
        basePlanId: String?,
        offerId: String?,
    ): PurchaseResult {
        // TODO: Await beginPurchase with purchasing coordinator: https://linear.app/superwall/issue/SW-2415/[android]-implement-purchasingcoordinator

        if (kotlinPurchaseController != null) {
            return kotlinPurchaseController.purchase(activity, productDetails, basePlanId, offerId)
        } else if (javaPurchaseController != null) {
            return suspendCoroutine { continuation ->
                javaPurchaseController.purchase(productDetails, basePlanId, offerId) { result ->
                    continuation.resume(result)
                }
            }
        } else {
            // Here is where we would implement our own product purchaser.
            return PurchaseResult.Cancelled()
        }

//
//        kotlinPurchaseController?.let {
//            return it.purchase(currentActivity as Activity, sk1Product.skuDetails)
//            }
//
//            // There used to be a failure if raw store product wasn't present but there isn't anymore...
//            // not sure why
//        }
//
//        // SW-2217
//        // https://linear.app/superwall/issue/SW-2217/%5Bandroid%5D-%5Bv1%5D-add-back-support-for-javanon-kotlinxcoroutines-purchase
// //        javaPurchaseController?.let {
// //            product.sk1Product?.let { sk1Product ->
// //                return it.purchase(sk1Product)
// //            } ?: return PurchaseResult.failed(PurchaseError.productUnavailable)
// //        }
//        return PurchaseResult.Cancelled()
    }

    override suspend fun restorePurchases(): RestorationResult {
        if (kotlinPurchaseController != null) {
            return kotlinPurchaseController.restorePurchases()
        } else if (javaPurchaseController != null) {
            return suspendCoroutine { continuation ->
                javaPurchaseController.restorePurchases { result, error ->
                    if (error == null) {
                        continuation.resume(result)
                    } else {
                        continuation.resume(RestorationResult.Failed(error))
                    }
                }
            }
        } else {
            // Here is where we would implement our own restoration
            return RestorationResult.Failed(null)
        }
    }
}
