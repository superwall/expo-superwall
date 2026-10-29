package expo.modules.superwallexpo.bridges

import android.app.Activity
import com.superwall.sdk.delegate.PurchaseResult
import com.android.billingclient.api.ProductDetails
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.store.ProductReplacement
import com.superwall.sdk.store.abstractions.product.StoreProduct
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import expo.modules.superwallexpo.SuperwallExpoModule

class PurchaseControllerBridge(): PurchaseController {

    var purchasePromise: CompletableFuture<PurchaseResult>? = null
    var restorePromise: CompletableFuture<RestorationResult>? = null

    companion object {
        val instance = PurchaseControllerBridge()
    }

    fun completePurchase(result: PurchaseResult) {
        purchasePromise?.complete(result)
    }

    fun completeRestore(result: RestorationResult) {
        restorePromise?.complete(result)
    }

    override suspend fun purchase(
        activity: Activity,
        productDetails: ProductDetails,
        basePlanId: String?,
        offerId: String?
    ): PurchaseResult {
        return emitPurchase(productDetails.productId, basePlanId, offerId, null)
    }

    override suspend fun purchase(
        activity: Activity,
        product: StoreProduct,
        basePlanId: String?,
        offerId: String?,
        replacement: ProductReplacement?
    ): PurchaseResult {
        // Custom products aren't bridged, so let the default fail them as before
        val rawStoreProduct = product.rawStoreProduct
            ?: return purchase(activity, product, basePlanId, offerId)
        return emitPurchase(
            rawStoreProduct.underlyingProductDetails.productId,
            basePlanId,
            offerId,
            replacement
        )
    }

    private suspend fun emitPurchase(
        productId: String,
        basePlanId: String?,
        offerId: String?,
        replacement: ProductReplacement?
    ): PurchaseResult {
        purchasePromise = CompletableFuture()

        val productData = mutableMapOf<String, Any?>(
            "platform" to "android",
            "productId" to productId,
            "basePlanId" to basePlanId,
            "offerId" to offerId
        )
        replacement?.let {
            productData["replacement"] = mapOf(
                "mode" to it.mode.rawName,
                "oldProductId" to it.oldProductId,
                "oldPurchaseToken" to it.oldPurchaseToken
            )
        }

        SuperwallExpoModule.emitEvent(
            "onPurchase",
            productData
        )

        return purchasePromise!!.await()
    }

    override suspend fun restorePurchases(): RestorationResult {
        restorePromise = CompletableFuture()

        SuperwallExpoModule.emitEvent("onPurchaseRestore", null)

        return restorePromise!!.await()
    }
}