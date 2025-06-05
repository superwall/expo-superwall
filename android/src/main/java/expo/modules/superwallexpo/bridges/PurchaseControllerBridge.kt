package expo.modules.superwallexpo.bridges

import android.app.Activity
import com.superwall.sdk.delegate.PurchaseResult
import com.android.billingclient.api.ProductDetails
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
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
        purchasePromise = CompletableFuture()
        
        val productData = mapOf(
            "platform" to "android",
            "productId" to productDetails.productId,
            "basePlanId" to basePlanId,
            "offerId" to offerId
        )
        
        SuperwallExpoModule.instance?.emitEvent(
            "onPurchase",
            productData
        )

        return purchasePromise!!.await()
    }

    override suspend fun restorePurchases(): RestorationResult {
        restorePromise = CompletableFuture()

        SuperwallExpoModule.instance?.emitEvent("onPurchaseRestore", null)

        return restorePromise!!.await()
    }
}