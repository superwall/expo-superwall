package com.superwall.sdk.store.testmode

import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.ActivityProvider
import com.superwall.sdk.misc.CurrentActivityTracker
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.store.abstractions.product.StoreProductType
import com.superwall.sdk.store.testmode.models.SuperwallProduct
import com.superwall.sdk.store.testmode.ui.PurchaseSimulationResult
import com.superwall.sdk.store.testmode.ui.RestoreSimulationResult
import com.superwall.sdk.store.testmode.ui.TestModePurchaseDrawer
import com.superwall.sdk.store.testmode.ui.TestModeRestoreDrawer
import com.superwall.sdk.store.transactions.TransactionManager.PurchaseSource

class TestModeTransactionHandler(
    private val testMode: TestMode,
    private val activityProvider: ActivityProvider,
    private val activityTracker: CurrentActivityTracker? = null,
) {
    /**
     * Returns the actual foreground activity. Prefers the lifecycle-tracked activity
     * (which sees SuperwallPaywallActivity) over the user-provided ActivityProvider
     * (which may always return the root activity, e.g. in Expo/React Native).
     */
    private fun getForegroundActivity() = activityTracker?.getCurrentActivity() ?: activityProvider.getCurrentActivity()

    suspend fun handlePurchase(
        product: StoreProductType,
        purchaseSource: PurchaseSource,
    ): PurchaseResult {
        val activity =
            getForegroundActivity()
                ?: return PurchaseResult.Failed("Activity not found - required for test mode purchase drawer")

        val superwallProduct =
            testMode.products.find { it.identifier == product.fullIdentifier }

        val entitlements = superwallProduct?.entitlements ?: emptyList()
        val hasFreeTrial = testMode.shouldShowFreeTrial(product.hasFreeTrial)

        Logger.debug(
            LogLevel.debug,
            LogScope.paywallTransactions,
            "Test mode: showing purchase drawer for ${product.fullIdentifier}",
        )

        val result =
            TestModePurchaseDrawer.show(
                activity = activity,
                productIdentifier = product.fullIdentifier,
                localizedPrice = product.localizedPrice,
                period = product.period,
                hasFreeTrial = hasFreeTrial,
                trialPeriodText = product.trialPeriodText,
                entitlementNames = entitlements.map { it.identifier },
            )

        return when (result) {
            is PurchaseSimulationResult.Purchased -> {
                testMode.fakePurchase(entitlements)
                val status = testMode.buildSubscriptionStatus()
                testMode.setOverriddenSubscriptionStatus(status)
                PurchaseResult.Purchased()
            }
            is PurchaseSimulationResult.Abandoned -> {
                PurchaseResult.Cancelled()
            }
            is PurchaseSimulationResult.Failed -> {
                PurchaseResult.Failed("Simulated purchase failure (test mode)")
            }
        }
    }

    suspend fun handleRestore(): RestorationResult {
        val activity =
            getForegroundActivity()
                ?: return RestorationResult.Failed(Throwable("Activity not found"))

        val allEntitlements = testMode.allEntitlements()

        Logger.debug(
            LogLevel.debug,
            LogScope.paywallTransactions,
            "Test mode: showing restore drawer with ${allEntitlements.size} entitlements",
        )

        val result =
            TestModeRestoreDrawer.show(
                activity = activity,
                availableEntitlements = allEntitlements.toList(),
                currentSelections = testMode.testEntitlementSelections,
            )

        return when (result) {
            is RestoreSimulationResult.Restored -> {
                testMode.setEntitlements(result.selectedEntitlements)
                val status = testMode.buildSubscriptionStatus()
                testMode.setOverriddenSubscriptionStatus(status)
                RestorationResult.Restored()
            }
            is RestoreSimulationResult.Cancelled -> {
                RestorationResult.Failed(null)
            }
        }
    }

    fun findSuperwallProductForId(productId: String): SuperwallProduct? = testMode.products.find { it.identifier == productId }

    fun entitlementsForProduct(productId: String): Set<Entitlement> {
        val superwallProduct = findSuperwallProductForId(productId) ?: return emptySet()
        return superwallProduct.entitlements
            .map { Entitlement(it.identifier) }
            .toSet()
    }
}
