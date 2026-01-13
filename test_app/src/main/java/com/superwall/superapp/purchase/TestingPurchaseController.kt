package com.superwall.superapp.purchase

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.superwall.sdk.Superwall
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.dismiss
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class TestingPurchaseController : PurchaseController {
    var purchasesEnabled = false
    var restoreEnabled = true

    override suspend fun purchase(
        activity: Activity,
        productDetails: ProductDetails,
        basePlanId: String?,
        offerId: String?,
    ): PurchaseResult =
        if (purchasesEnabled) {
            Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Active(setOf(Entitlement("pro"))))
            // Since there is no actual purchase, we dismiss manually here
            IOScope().launch {
                delay(1.seconds)
                Superwall.instance.dismiss()
            }
            PurchaseResult.Purchased()
        } else {
            PurchaseResult.Failed("An error occurred")
        }

    override suspend fun restorePurchases(): RestorationResult =
        if (restoreEnabled) {
            RestorationResult.Restored()
        } else {
            RestorationResult.Failed(Exception("Restore failed"))
        }
}
