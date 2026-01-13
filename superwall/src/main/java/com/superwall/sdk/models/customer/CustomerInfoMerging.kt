package com.superwall.sdk.models.customer

import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.store.abstractions.product.receipt.LatestSubscriptionState

/**
 * Merges this CustomerInfo (device) with web CustomerInfo, deduplicating transactions by transaction ID.
 *
 * This method combines transaction history and entitlements from both on-device purchases
 * and web-based purchases/redemptions. It ensures:
 * - No duplicate transactions (keyed by `transactionId`)
 * - Web entitlements take precedence over device entitlements when IDs match (via priority rules)
 * - All transactions are sorted by purchase date
 *
 * When duplicate transaction IDs are found for subscriptions, priority is determined by:
 * - Active status (active > inactive)
 * - Revocation status (non-revoked > revoked)
 * - Auto-renewal (will renew > won't renew)
 * - Grace period (in grace period > not in grace period)
 * - Most recent purchase date (latest > earliest)
 *
 * For entitlements, priority rules (applied via `EntitlementPriorityComparator`):
 * - Active status (active > inactive)
 * - Transaction history presence (has history > no history)
 * - Lifetime status (lifetime > time-limited)
 * - Latest expiration date
 * - Auto-renewal (will renew > won't renew)
 * - Subscription state quality (SUBSCRIBED > GRACE_PERIOD > BILLING_RETRY > EXPIRED)
 *
 * @param other The CustomerInfo from web2app endpoints containing web purchases/redemptions
 * @return A new CustomerInfo with merged data from both sources
 */
fun CustomerInfo.merge(other: CustomerInfo): CustomerInfo {
    // Merge subscription transactions
    // Group by transaction ID, then select highest priority transaction for each ID
    // This prevents showing duplicate subscription history when a user has both native and
    // web purchases with the same transaction ID
    val mergedSubscriptions =
        (this.subscriptions + other.subscriptions)
            .groupBy { it.transactionId }
            .map { (_, transactions) ->
                // Apply priority rules to select the "best" transaction
                transactions.maxWithOrNull(SubscriptionTransactionComparator)
                    ?: transactions.first()
            }.sortedBy { it.purchaseDate }

    // Merge non-subscription transactions (consumables, non-consumables)
    // Simply deduplicate by transaction ID - first occurrence wins
    val mergedNonSubscriptions =
        (this.nonSubscriptions + other.nonSubscriptions)
            .distinctBy { it.transactionId }
            .sortedBy { it.purchaseDate }

    // Merge entitlements using priority-based merging
    // This intelligently selects the highest priority entitlement for each ID based on
    // the criteria defined in `EntitlementPriorityComparator`
    val mergedEntitlements =
        mergeEntitlements(
            this.entitlements,
            other.entitlements,
        )

    // Prefer device userId over web userId
    // Device user ID takes precedence when both are non-blank
    val mergedUserId =
        when {
            this.userId.isNotBlank() -> this.userId
            other.userId.isNotBlank() -> other.userId
            else -> ""
        }

    val isBlank =
        mergedSubscriptions.isEmpty() &&
            mergedNonSubscriptions.isEmpty() &&
            mergedEntitlements.isEmpty()

    return CustomerInfo(
        subscriptions = mergedSubscriptions,
        nonSubscriptions = mergedNonSubscriptions,
        userId = mergedUserId,
        entitlements = mergedEntitlements,
        isPlaceholder = isBlank,
    )
}

fun mergeEntitlements(
    first: Collection<Entitlement>,
    second: Collection<Entitlement>,
): List<Entitlement> =
    mergeEntitlementsPrioritized(
        buildList {
            addAll(first)
            addAll(second)
        },
    )

fun List<Entitlement>.toSet() =
    setOf(
        *mergeEntitlementsPrioritized(this).toTypedArray(),
    )

operator fun Collection<Entitlement>.plus(second: Collection<Entitlement>) = mergeEntitlements(this, second)

/**
 * Merges a list of entitlements, keeping the highest priority entitlement for each ID.
 * Uses EntitlementPriorityComparator to determine priority.
 * When merging, productIds from all entitlements with the same ID are combined.
 */
fun mergeEntitlementsPrioritized(entitlements: List<Entitlement>): List<Entitlement> =
    entitlements
        .groupBy { it.id }
        .map { (_, group) ->
            val winner = group.maxWithOrNull(EntitlementPriorityComparator) ?: group.first()
            // Merge productIds from all entitlements in the group
            val mergedProductIds = group.flatMap { it.productIds }.toSet()
            if (mergedProductIds != winner.productIds) {
                winner.copy(productIds = mergedProductIds)
            } else {
                winner
            }
        }

/**
 * Comparator implementing iOS priority rules for subscriptions.
 */
private object SubscriptionTransactionComparator : Comparator<SubscriptionTransaction> {
    override fun compare(
        a: SubscriptionTransaction,
        b: SubscriptionTransaction,
    ): Int {
        // Active > inactive
        if (a.isActive != b.isActive) return if (a.isActive) 1 else -1

        // Non-revoked > revoked
        if (a.isRevoked != b.isRevoked) return if (a.isRevoked) -1 else 1

        // Will renew > won't renew
        if (a.willRenew != b.willRenew) return if (a.willRenew) 1 else -1

        // In grace period is better than billing retry
        if (a.isInGracePeriod != b.isInGracePeriod) {
            return if (a.isInGracePeriod) 1 else -1
        }

        // Latest purchase date wins
        return a.purchaseDate.compareTo(b.purchaseDate)
    }
}

/**
 * Comparator implementing iOS priority rules for entitlements.
 *
 * Priority order (highest to lowest):
 * 1. Active entitlements (isActive = true)
 * 2. Has transaction history (startsAt != null)
 * 3. Lifetime entitlements (isLifetime = true)
 * 4. Latest expiry time (furthest future expiresAt)
 * 5. Will renew vs won't renew (willRenew = true)
 * 6. Subscription state quality (SUBSCRIBED > GRACE_PERIOD > BILLING_RETRY > EXPIRED)
 */
internal object EntitlementPriorityComparator : Comparator<Entitlement> {
    override fun compare(
        a: Entitlement,
        b: Entitlement,
    ): Int {
        // Active > inactive
        if (a.isActive != b.isActive) return if (a.isActive) 1 else -1

        // Has transaction history > no history
        val aHasHistory = a.startsAt != null
        val bHasHistory = b.startsAt != null
        if (aHasHistory != bHasHistory) return if (aHasHistory) 1 else -1

        // Lifetime > time-limited
        when {
            a.isLifetime == true && b.isLifetime != true -> return 1
            b.isLifetime == true && a.isLifetime != true -> return -1
        }

        // Latest expiration wins (only if both have expiration)
        if (a.expiresAt != null && b.expiresAt != null) {
            val expComp = a.expiresAt.compareTo(b.expiresAt)
            if (expComp != 0) return expComp
        }

        // Will renew > won't renew
        when {
            a.willRenew == true && b.willRenew != true -> return 1
            b.willRenew == true && a.willRenew != true -> return -1
        }

        // Non-revoked state wins
        val aState = a.state
        val bState = b.state
        if (aState != bState) {
            // Prioritize based on latest sub state
            val aScore = getStateScore(aState)
            val bScore = getStateScore(bState)
            if (aScore != bScore) return aScore.compareTo(bScore)
        }

        // Default: preserve order
        return 0
    }

    private fun getStateScore(state: LatestSubscriptionState?): Int =
        when (state) {
            LatestSubscriptionState.SUBSCRIBED -> 4
            LatestSubscriptionState.GRACE_PERIOD -> 3
            LatestSubscriptionState.BILLING_RETRY -> 2
            LatestSubscriptionState.EXPIRED -> 1
            else -> 0
        }
}
