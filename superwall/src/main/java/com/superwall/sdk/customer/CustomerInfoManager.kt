package com.superwall.sdk.customer

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.customer.merge
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.storage.*
import kotlinx.coroutines.launch

/**
 * Manages merging of device and web CustomerInfo sources.
 *
 * This class is responsible for:
 * - Reading device CustomerInfo (built from Google Play receipts)
 * - Reading web CustomerInfo (fetched from Superwall backend)
 * - Merging both sources using priority-based rules
 * - Updating the public CustomerInfo flow that the SDK exposes
 * - Persisting the merged result to storage
 *
 * When an external purchase controller is present (e.g., RevenueCat, Qonversion):
 * - The subscription status is the source of truth for active entitlements
 * - Device receipts are only used for inactive entitlements (history)
 * - This preserves entitlements set by external controllers that device receipts don't know about
 *
 * The merge happens whenever:
 * - Device receipts are refreshed (via ReceiptManager)
 * - Web entitlements are fetched (via WebPaywallRedeemer)
 * - SDK initialization completes
 */
class CustomerInfoManager(
    private val storage: Storage,
    private val updateCustomerInfo: (CustomerInfo) -> Unit,
    private val ioScope: IOScope,
    private val hasExternalPurchaseController: () -> Boolean,
    private val getSubscriptionStatus: () -> SubscriptionStatus,
) {
    /**
     * Merges device and web CustomerInfo and updates the public CustomerInfo flow.
     *
     * This method:
     * 1. Reads the latest device CustomerInfo from storage (built from Google Play receipts)
     * 2. Reads the latest web CustomerInfo from storage (fetched from backend)
     * 3. Merges them using priority rules (see CustomerInfo.merge())
     * 4. Persists the merged result to storage
     * 5. Updates the public flow so listeners get the latest merged state
     *
     * When an external purchase controller is present:
     * - Uses CustomerInfo.forExternalPurchaseController() instead of standard merge
     * - This ensures external controller's entitlements are preserved as source of truth
     *
     * The merge is performed asynchronously on the IO scope to avoid blocking the caller.
     */
    fun updateMergedCustomerInfo() {
        ioScope.launch {
            val merged: CustomerInfo

            if (hasExternalPurchaseController()) {
                merged =
                    CustomerInfo.forExternalPurchaseController(
                        storage = storage,
                        subscriptionStatus = getSubscriptionStatus(),
                    )

                Logger.debug(
                    logLevel = LogLevel.debug,
                    scope = LogScope.superwallCore,
                    message =
                        "Built CustomerInfo for external controller - " +
                            "${merged.subscriptions.size} subs, " +
                            "${merged.entitlements.size} entitlements",
                )
            } else {
                val deviceInfo = storage.read(LatestDeviceCustomerInfo) ?: CustomerInfo.empty()
                val webInfo = storage.read(LatestWebCustomerInfo) ?: CustomerInfo.empty()

                Logger.debug(
                    logLevel = LogLevel.debug,
                    scope = LogScope.superwallCore,
                    message =
                        "Merging CustomerInfo - Device: ${deviceInfo.subscriptions.size} subs, " +
                            "Web: ${webInfo.subscriptions.size} subs",
                )

                // Merge with optimization: skip merge if one source is blank
                merged =
                    when {
                        deviceInfo.isPlaceholder && webInfo.isPlaceholder -> CustomerInfo.empty()
                        deviceInfo.isPlaceholder -> webInfo
                        webInfo.isPlaceholder -> deviceInfo
                        else -> deviceInfo.merge(webInfo) // Apply priority-based merging
                    }

                Logger.debug(
                    logLevel = LogLevel.debug,
                    scope = LogScope.superwallCore,
                    message =
                        "Merged CustomerInfo - Total: ${merged.subscriptions.size} subs, " +
                            "${merged.entitlements.size} entitlements",
                )
            }

            storage.write(LatestCustomerInfo, merged)

            updateCustomerInfo(merged)
        }
    }
}
