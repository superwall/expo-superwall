package expo.modules.superwallexpo.bridges

import android.app.Activity
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.store.ProductReplacement
import com.superwall.sdk.store.abstractions.product.StoreProduct
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import expo.modules.superwallexpo.SuperwallExpoModule
import expo.modules.superwallexpo.json.toJson

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

    // The StoreProduct overloads also receive custom products, which have no ProductDetails;
    // the native default fails those before they reach JS.
    override suspend fun purchase(
        activity: Activity,
        product: StoreProduct,
        basePlanId: String?,
        offerId: String?
    ): PurchaseResult = purchase(activity, product, basePlanId, offerId, null)

    // The SDK calls this overload; `replacement` is set when the paywall's purchase replaces an
    // existing subscription.
    override suspend fun purchase(
        activity: Activity,
        product: StoreProduct,
        basePlanId: String?,
        offerId: String?,
        replacement: ProductReplacement?
    ): PurchaseResult {
        purchasePromise = CompletableFuture()

        val productData = mutableMapOf<String, Any?>(
            "platform" to "android",
            "productId" to product.productIdentifier,
            "basePlanId" to basePlanId,
            "offerId" to offerId,
            "store" to if (product.isCustomProduct) "CUSTOM" else "PLAY_STORE"
        )
        replacement?.let { productData["replacement"] = it.toJson() }

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