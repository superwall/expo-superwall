package com.superwall.sdk.models.customer

import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.product.Store
import com.superwall.sdk.storage.LatestDeviceCustomerInfo
import com.superwall.sdk.storage.LatestRedemptionResponse
import com.superwall.sdk.storage.Storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A class that contains the latest subscription and entitlement info about the customer.
 * These objects are non-mutable and do not update automatically.
 */
@Serializable
data class CustomerInfo(
    /** The subscription transactions the user has made. The transactions are
     * ordered by purchase date in ascending order. */
    @SerialName("subscriptions")
    val subscriptions: List<SubscriptionTransaction>,
    /** The non-subscription transactions the user has made. The transactions are
     * ordered by purchase date in ascending order. */
    @SerialName("nonSubscriptions")
    val nonSubscriptions: List<NonSubscriptionTransaction>,
    /** The ID of the user. Equivalent to Superwall.userId. */
    @SerialName("userId")
    val userId: String = "",
    /** All entitlements available to the user. */
    @SerialName("entitlements")
    val entitlements: List<Entitlement>,
    /** Indicates whether this is a placeholder CustomerInfo that hasn't been populated with real data yet.
     *  true means this is the initial placeholder state before data has been loaded.
     *  false means real data has been loaded (even if that data is empty)
     * */
    @SerialName("isPlaceholder")
    internal val isPlaceholder: Boolean = false,
) {
    /** A Set of the product identifiers for the active subscriptions. */
    @Transient
    val activeSubscriptionProductIds: Set<String>
        get() =
            subscriptions
                .filter { it.isActive }
                .map { it.productId }
                .toSet()

    override fun toString(): String =
        buildString {
            appendLine("CustomerInfo(")
            appendLine("  userId=$userId,")
            appendLine("  isPlaceholder=$isPlaceholder,")
            appendLine("  subscriptions=[")
            subscriptions.forEachIndexed { index, subscription ->
                appendLine("    #$index $subscription")
            }
            appendLine("  ],")
            appendLine("  nonSubscriptions=[")
            nonSubscriptions.forEachIndexed { index, transaction ->
                appendLine("    #$index $transaction")
            }
            appendLine("  ],")
            appendLine("  entitlements=[")
            entitlements.forEachIndexed { index, entitlement ->
                appendLine("    #$index $entitlement")
            }
            appendLine("  ],")
            append(")")
        }

    companion object {
        /**
         * Creates a blank CustomerInfo instance for testing or default states.
         */
        fun empty(): CustomerInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = true,
            )

        /**
         * Creates a merged CustomerInfo from device, web, and external purchase controller sources.
         *
         * When using an external purchase controller, the
         * subscriptionStatus is the source of truth for active entitlements. This method:
         * 1. Merges device and web transactions (subscriptions and nonSubscriptions)
         * 2. Takes only inactive device entitlements (for history)
         * 3. Takes active Play Store entitlements from subscriptionStatus (source of truth)
         * 4. Keeps all web entitlements
         * 5. Merges using priority rules
         *
         * This ensures the external purchase controller's entitlements are preserved even when
         * device receipts don't have that information (e.g., RevenueCat granted entitlements
         * from cross-platform purchases).
         *
         * @param storage Storage to read device and web CustomerInfo from
         * @param subscriptionStatus The subscription status containing entitlements from external controller
         * @return A new CustomerInfo with all sources merged
         */
        fun forExternalPurchaseController(
            storage: Storage,
            subscriptionStatus: SubscriptionStatus,
        ): CustomerInfo {
            // Get web CustomerInfo
            val webCustomerInfo =
                storage.read(LatestRedemptionResponse)?.customerInfo ?: empty()

            // Get device CustomerInfo to preserve history
            // Use device-only CustomerInfo to avoid using stale cached web entitlements
            val deviceCustomerInfo = storage.read(LatestDeviceCustomerInfo) ?: empty()

            // Merge device and web transactions (subscriptions and nonSubscriptions)
            // This handles transaction deduplication by transaction ID
            val baseCustomerInfo = deviceCustomerInfo.merge(webCustomerInfo)

            // For entitlements: only take inactive device entitlements
            // Active entitlements come from the external purchase controller (source of truth)
            val inactiveDeviceEntitlements = deviceCustomerInfo.entitlements.filter { !it.isActive }

            // Get active Play Store entitlements from external controller (the source of truth)
            // Filter for PLAY_STORE ones only to avoid duplicating web-granted entitlements
            val externalEntitlements: List<Entitlement> =
                when (subscriptionStatus) {
                    is SubscriptionStatus.Active ->
                        subscriptionStatus.entitlements.filter { it.store == Store.PLAY_STORE }
                    SubscriptionStatus.Inactive,
                    SubscriptionStatus.Unknown,
                    -> emptyList()
                }

            // Merge: active from external controller + all web + inactive device
            // This gives us complete history while respecting external controller as source of truth
            val allEntitlements = externalEntitlements + webCustomerInfo.entitlements + inactiveDeviceEntitlements
            val finalEntitlements = mergeEntitlementsPrioritized(allEntitlements).sortedBy { it.id }

            return CustomerInfo(
                subscriptions = baseCustomerInfo.subscriptions,
                nonSubscriptions = baseCustomerInfo.nonSubscriptions,
                userId = baseCustomerInfo.userId,
                entitlements = finalEntitlements,
                isPlaceholder = false,
            )
        }
    }
}
