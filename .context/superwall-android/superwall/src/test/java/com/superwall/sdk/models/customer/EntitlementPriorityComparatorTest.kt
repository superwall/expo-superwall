package com.superwall.sdk.models.customer

import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.store.abstractions.product.receipt.LatestSubscriptionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Comprehensive tests for EntitlementPriorityComparator.
 *
 * Priority order (highest to lowest):
 * 1. Active entitlements (isActive = true)
 * 2. Has transaction history (startsAt != null)
 * 3. Lifetime entitlements (isLifetime = true)
 * 4. Latest expiry time (furthest future expiresAt)
 * 5. Will renew vs won't renew (willRenew = true)
 * 6. Subscription state quality (SUBSCRIBED > GRACE_PERIOD > BILLING_RETRY > EXPIRED)
 */
class EntitlementPriorityComparatorTest {
    private val comparator = EntitlementPriorityComparator

    // Helper to check if a takes priority over b
    private fun takesPriorityOver(
        a: Entitlement,
        b: Entitlement,
    ): Boolean = comparator.compare(a, b) > 0

    // MARK: - Basic Comparison

    @Test
    fun `identical entitlements return zero`() {
        val date = Date()
        val entitlement1 =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
            )
        val entitlement2 =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
            )

        assertEquals(0, comparator.compare(entitlement1, entitlement2))
        assertEquals(0, comparator.compare(entitlement2, entitlement1))
    }

    // MARK: - Priority 1: Active Status

    @Test
    fun `active takes priority over inactive`() {
        val active = Entitlement(id = "premium", isActive = true, isLifetime = false)
        val inactive = Entitlement(id = "premium", isActive = false, isLifetime = false)

        assertTrue(takesPriorityOver(active, inactive))
        assertTrue(!takesPriorityOver(inactive, active))
    }

    @Test
    fun `active subscription takes priority over inactive lifetime`() {
        val activeSubscription = Entitlement(id = "premium", isActive = true, isLifetime = false)
        val inactiveLifetime = Entitlement(id = "premium", isActive = false, isLifetime = true)

        // Active status is checked first, so active subscription wins
        assertTrue(takesPriorityOver(activeSubscription, inactiveLifetime))
        assertTrue(!takesPriorityOver(inactiveLifetime, activeSubscription))
    }

    @Test
    fun `active with near expiry takes priority over inactive with far expiry`() {
        val activeNearExpiry =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 60_000), // Expires in 1 minute
            )
        val inactiveFarExpiry =
            Entitlement(
                id = "premium",
                isActive = false,
                expiresAt = Date(System.currentTimeMillis() - 3600_000), // Expired 1 hour ago
            )

        assertTrue(takesPriorityOver(activeNearExpiry, inactiveFarExpiry))
        assertTrue(!takesPriorityOver(inactiveFarExpiry, activeNearExpiry))
    }

    @Test
    fun `both active continue to next priority check - lifetime`() {
        val activeLifetime = Entitlement(id = "premium", isActive = true, isLifetime = true)
        val activeSubscription = Entitlement(id = "premium", isActive = true, isLifetime = false)

        // Should fall through to lifetime check
        assertTrue(takesPriorityOver(activeLifetime, activeSubscription))
        assertTrue(!takesPriorityOver(activeSubscription, activeLifetime))
    }

    @Test
    fun `both inactive continue to next priority check - lifetime`() {
        val inactiveLifetime = Entitlement(id = "premium", isActive = false, isLifetime = true)
        val inactiveSubscription = Entitlement(id = "premium", isActive = false, isLifetime = false)

        // Should fall through to lifetime check
        assertTrue(takesPriorityOver(inactiveLifetime, inactiveSubscription))
        assertTrue(!takesPriorityOver(inactiveSubscription, inactiveLifetime))
    }

    // MARK: - Priority 2: Has Transaction History

    @Test
    fun `has transaction history takes priority over no transaction history when both inactive`() {
        val withTransactionHistory =
            Entitlement(
                id = "premium",
                isActive = false,
                startsAt = Date(1000),
            )
        val noTransactionHistory =
            Entitlement(
                id = "premium",
                isActive = false,
                startsAt = null,
            )

        assertTrue(takesPriorityOver(withTransactionHistory, noTransactionHistory))
        assertTrue(!takesPriorityOver(noTransactionHistory, withTransactionHistory))
    }

    @Test
    fun `has transaction history takes priority when both active`() {
        val withTransactionHistory =
            Entitlement(
                id = "premium",
                isActive = true,
                startsAt = Date(1000),
            )
        val noTransactionHistory =
            Entitlement(
                id = "premium",
                isActive = true,
                startsAt = null,
            )

        assertTrue(takesPriorityOver(withTransactionHistory, noTransactionHistory))
        assertTrue(!takesPriorityOver(noTransactionHistory, withTransactionHistory))
    }

    @Test
    fun `both have transaction history continues to next priority check - lifetime`() {
        val lifetime =
            Entitlement(
                id = "premium",
                isActive = true,
                startsAt = Date(1000),
                isLifetime = true,
            )
        val subscription =
            Entitlement(
                id = "premium",
                isActive = true,
                startsAt = Date(2000),
                isLifetime = false,
            )

        // Should fall through to lifetime check
        assertTrue(takesPriorityOver(lifetime, subscription))
        assertTrue(!takesPriorityOver(subscription, lifetime))
    }

    @Test
    fun `both have no transaction history continues to next priority check - lifetime`() {
        val lifetime =
            Entitlement(
                id = "premium",
                isActive = true,
                startsAt = null,
                isLifetime = true,
            )
        val subscription =
            Entitlement(
                id = "premium",
                isActive = true,
                startsAt = null,
                isLifetime = false,
            )

        // Should fall through to lifetime check
        assertTrue(takesPriorityOver(lifetime, subscription))
        assertTrue(!takesPriorityOver(subscription, lifetime))
    }

    // MARK: - Priority 3: Lifetime

    @Test
    fun `lifetime takes priority over non-lifetime when both active`() {
        val lifetime = Entitlement(id = "premium", isActive = true, isLifetime = true)
        val subscription = Entitlement(id = "premium", isActive = true, isLifetime = false)

        assertTrue(takesPriorityOver(lifetime, subscription))
        assertTrue(!takesPriorityOver(subscription, lifetime))
    }

    @Test
    fun `lifetime with null isLifetime treated as false`() {
        val lifetimeNil = Entitlement(id = "premium", isActive = true, isLifetime = null)
        val lifetimeTrue = Entitlement(id = "premium", isActive = true, isLifetime = true)

        assertTrue(takesPriorityOver(lifetimeTrue, lifetimeNil))
        assertTrue(!takesPriorityOver(lifetimeNil, lifetimeTrue))
    }

    @Test
    fun `both active and lifetime with no other differences returns zero`() {
        val lifetime1 = Entitlement(id = "premium", isActive = true, isLifetime = true, state = null)
        val lifetime2 = Entitlement(id = "premium", isActive = true, isLifetime = true, state = null)

        // Both active, both lifetime, both have nil state
        // Should return 0 (no preference)
        assertEquals(0, comparator.compare(lifetime1, lifetime2))
    }

    // MARK: - Priority 4: Expiry Date

    @Test
    fun `later expiry takes priority when both have expiry dates`() {
        val later =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 7200_000),
            )
        val earlier =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 3600_000),
            )

        assertTrue(takesPriorityOver(later, earlier))
        assertTrue(!takesPriorityOver(earlier, later))
    }

    @Test
    fun `far future expiry takes priority over near future`() {
        val farFuture =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 86400_000L * 365), // 1 year
            )
        val nearFuture =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 86400_000), // 1 day
            )

        assertTrue(takesPriorityOver(farFuture, nearFuture))
        assertTrue(!takesPriorityOver(nearFuture, farFuture))
    }

    @Test
    fun `future expiry takes priority over past expiry`() {
        val future =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 3600_000),
            )
        val past =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() - 3600_000),
            )

        assertTrue(takesPriorityOver(future, past))
        assertTrue(!takesPriorityOver(past, future))
    }

    @Test
    fun `both nil expiry continue to next priority check`() {
        val willRenew =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = null,
                willRenew = true,
            )
        val wontRenew =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = null,
                willRenew = false,
            )

        // Should fall through to willRenew check
        assertTrue(takesPriorityOver(willRenew, wontRenew))
        assertTrue(!takesPriorityOver(wontRenew, willRenew))
    }

    // MARK: - Priority 5: Will Renew

    @Test
    fun `will renew takes priority over won't renew`() {
        val date = Date(System.currentTimeMillis() + 3600_000)
        val willRenew =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
            )
        val wontRenew =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = false,
            )

        assertTrue(takesPriorityOver(willRenew, wontRenew))
        assertTrue(!takesPriorityOver(wontRenew, willRenew))
    }

    @Test
    fun `willRenew true takes priority over nil`() {
        val date = Date(System.currentTimeMillis() + 3600_000)
        val willRenewTrue =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
            )
        val willRenewNil =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = null,
            )

        assertTrue(takesPriorityOver(willRenewTrue, willRenewNil))
        assertTrue(!takesPriorityOver(willRenewNil, willRenewTrue))
    }

    // MARK: - Priority 6: Subscription State

    @Test
    fun `subscribed takes priority over grace period`() {
        val date = Date(System.currentTimeMillis() + 3600_000)
        val subscribed =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
                state = LatestSubscriptionState.SUBSCRIBED,
            )
        val gracePeriod =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
                state = LatestSubscriptionState.GRACE_PERIOD,
            )

        assertTrue(takesPriorityOver(subscribed, gracePeriod))
        assertTrue(!takesPriorityOver(gracePeriod, subscribed))
    }

    @Test
    fun `grace period takes priority over billing retry`() {
        val date = Date(System.currentTimeMillis() + 3600_000)
        val gracePeriod =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
                state = LatestSubscriptionState.GRACE_PERIOD,
            )
        val billingRetry =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
                state = LatestSubscriptionState.BILLING_RETRY,
            )

        assertTrue(takesPriorityOver(gracePeriod, billingRetry))
        assertTrue(!takesPriorityOver(billingRetry, gracePeriod))
    }

    @Test
    fun `billing retry takes priority over expired`() {
        val date = Date(System.currentTimeMillis() + 3600_000)
        val billingRetry =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
                state = LatestSubscriptionState.BILLING_RETRY,
            )
        val expired =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
                state = LatestSubscriptionState.EXPIRED,
            )

        assertTrue(takesPriorityOver(billingRetry, expired))
        assertTrue(!takesPriorityOver(expired, billingRetry))
    }

    @Test
    fun `expired takes priority over null state`() {
        val date = Date(System.currentTimeMillis() + 3600_000)
        val expired =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
                state = LatestSubscriptionState.EXPIRED,
            )
        val nullState =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                willRenew = true,
                state = null,
            )

        assertTrue(takesPriorityOver(expired, nullState))
        assertTrue(!takesPriorityOver(nullState, expired))
    }

    @Test
    fun `subscribed takes priority over revoked`() {
        val subscribed =
            Entitlement(
                id = "premium",
                isActive = true,
                state = LatestSubscriptionState.SUBSCRIBED,
            )
        val revoked =
            Entitlement(
                id = "premium",
                isActive = true,
                state = LatestSubscriptionState.REVOKED,
            )

        assertTrue(takesPriorityOver(subscribed, revoked))
        assertTrue(!takesPriorityOver(revoked, subscribed))
    }

    // MARK: - Complex Scenarios

    @Test
    fun `active overrides all other factors`() {
        val active =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() - 86400_000), // Expired
                isLifetime = false,
                willRenew = false,
                state = LatestSubscriptionState.REVOKED,
            )
        val inactive =
            Entitlement(
                id = "premium",
                isActive = false,
                expiresAt = Date(System.currentTimeMillis() + 86400_000L * 365), // Far future
                isLifetime = true, // Lifetime
                willRenew = true,
                state = LatestSubscriptionState.SUBSCRIBED,
            )

        // Active wins even though inactive has better everything else
        assertTrue(takesPriorityOver(active, inactive))
        assertTrue(!takesPriorityOver(inactive, active))
    }

    @Test
    fun `lifetime overrides all factors except active status and history`() {
        val activeSubscription =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 3600_000), // Short expiry
                isLifetime = false,
                willRenew = false,
                state = LatestSubscriptionState.REVOKED,
            )
        val inactiveLifetime =
            Entitlement(
                id = "premium",
                isActive = false,
                expiresAt = null,
                isLifetime = true,
                willRenew = null,
                state = LatestSubscriptionState.SUBSCRIBED,
            )

        // Active wins even though lifetime has better other factors
        assertTrue(takesPriorityOver(activeSubscription, inactiveLifetime))
        assertTrue(!takesPriorityOver(inactiveLifetime, activeSubscription))
    }

    @Test
    fun `later expiry overrides renewal and state`() {
        val laterExpiry =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 7200_000),
                isLifetime = false,
                willRenew = false,
                state = LatestSubscriptionState.GRACE_PERIOD,
            )
        val earlierExpiry =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 3600_000),
                isLifetime = false,
                willRenew = true,
                state = LatestSubscriptionState.SUBSCRIBED,
            )

        assertTrue(takesPriorityOver(laterExpiry, earlierExpiry))
        assertTrue(!takesPriorityOver(earlierExpiry, laterExpiry))
    }

    @Test
    fun `will renew overrides state only`() {
        val date = Date(System.currentTimeMillis() + 3600_000)
        val willRenew =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                isLifetime = false,
                willRenew = true,
                state = LatestSubscriptionState.GRACE_PERIOD,
            )
        val wontRenew =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = date,
                isLifetime = false,
                willRenew = false,
                state = LatestSubscriptionState.SUBSCRIBED,
            )

        assertTrue(takesPriorityOver(willRenew, wontRenew))
        assertTrue(!takesPriorityOver(wontRenew, willRenew))
    }

    @Test
    fun `real-world device vs web scenario - lifetime web wins`() {
        // Device has active monthly subscription expiring soon
        val device =
            Entitlement(
                id = "premium",
                isActive = true,
                startsAt = Date(System.currentTimeMillis() - 86400_000L * 30),
                expiresAt = Date(System.currentTimeMillis() + 3600_000), // 1 hour
                isLifetime = false,
                willRenew = true,
                state = LatestSubscriptionState.SUBSCRIBED,
            )

        // Web has lifetime purchase
        val web =
            Entitlement(
                id = "premium",
                isActive = true,
                startsAt = Date(System.currentTimeMillis() - 86400_000L * 365),
                expiresAt = null,
                isLifetime = true,
                willRenew = null,
                state = null,
            )

        assertTrue(takesPriorityOver(web, device))
        assertTrue(!takesPriorityOver(device, web))
    }

    @Test
    fun `real-world expired vs grace period scenario`() {
        // Subscription expired but in grace period (payment issue)
        val gracePeriod =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() - 3600_000), // Expired 1 hour ago
                isLifetime = false,
                willRenew = true,
                state = LatestSubscriptionState.GRACE_PERIOD,
            )

        // Subscription that's still fully valid
        val subscribed =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 3600_000), // Valid for 1 hour
                isLifetime = false,
                willRenew = true,
                state = LatestSubscriptionState.SUBSCRIBED,
            )

        // Later expiry should win (still valid vs expired)
        assertTrue(takesPriorityOver(subscribed, gracePeriod))
        assertTrue(!takesPriorityOver(gracePeriod, subscribed))
    }

    @Test
    fun `real-world multiple expired subscriptions scenario`() {
        // Old expired subscription
        val oldExpired =
            Entitlement(
                id = "premium",
                isActive = false,
                expiresAt = Date(System.currentTimeMillis() - 86400_000L * 365), // 1 year ago
                isLifetime = false,
                willRenew = false,
                state = LatestSubscriptionState.EXPIRED,
            )

        // Recently expired subscription
        val recentExpired =
            Entitlement(
                id = "premium",
                isActive = false,
                expiresAt = Date(System.currentTimeMillis() - 3600_000), // 1 hour ago
                isLifetime = false,
                willRenew = false,
                state = LatestSubscriptionState.EXPIRED,
            )

        // Both inactive, so expiry date wins
        assertTrue(takesPriorityOver(recentExpired, oldExpired))
        assertTrue(!takesPriorityOver(oldExpired, recentExpired))
    }

    // MARK: - mergeEntitlementsPrioritized Tests

    @Test
    fun `mergeEntitlementsPrioritized selects lifetime over subscription`() {
        val lifetime = Entitlement(id = "premium", isActive = true, isLifetime = true)
        val subscription =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 3600_000),
                isLifetime = false,
                willRenew = true,
            )

        val merged = mergeEntitlementsPrioritized(listOf(lifetime, subscription))

        assertEquals(1, merged.size)
        assertEquals(true, merged.first().isLifetime)
    }

    @Test
    fun `mergeEntitlementsPrioritized preserves different entitlement IDs`() {
        val premium = Entitlement(id = "premium", isActive = true, isLifetime = true)
        val basic = Entitlement(id = "basic", isActive = true, expiresAt = Date(System.currentTimeMillis() + 3600_000))
        val pro = Entitlement(id = "pro", isActive = false)

        val merged = mergeEntitlementsPrioritized(listOf(premium, basic, pro))

        assertEquals(3, merged.size)
        val ids = merged.map { it.id }.toSet()
        assertTrue(ids.contains("premium"))
        assertTrue(ids.contains("basic"))
        assertTrue(ids.contains("pro"))
    }

    @Test
    fun `mergeEntitlementsPrioritized handles multiple same-ID entitlements`() {
        val device =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 1800_000), // 30 minutes
                willRenew = true,
            )

        val webLifetime = Entitlement(id = "premium", isActive = true, isLifetime = true)

        val webExpired =
            Entitlement(
                id = "premium",
                isActive = false,
                expiresAt = Date(System.currentTimeMillis() - 3600_000), // Expired 1 hour ago
                willRenew = false,
            )

        val merged = mergeEntitlementsPrioritized(listOf(device, webLifetime, webExpired))

        assertEquals(1, merged.size)
        val result = merged.first()
        assertEquals("premium", result.id)
        assertEquals(true, result.isLifetime)
        assertEquals(true, result.isActive)
    }

    @Test
    fun `mergeEntitlementsPrioritized returns empty list for empty input`() {
        val merged = mergeEntitlementsPrioritized(emptyList())
        assertTrue(merged.isEmpty())
    }

    @Test
    fun `mergeEntitlementsPrioritized returns single entitlement unchanged`() {
        val entitlement =
            Entitlement(
                id = "premium",
                isActive = true,
                startsAt = Date(1000),
            )

        val merged = mergeEntitlementsPrioritized(listOf(entitlement))

        assertEquals(1, merged.size)
        assertEquals("premium", merged.first().id)
        assertEquals(Date(1000), merged.first().startsAt)
    }

    @Test
    fun `mergeEntitlementsPrioritized selects active over inactive with all other factors equal`() {
        val active =
            Entitlement(
                id = "premium",
                isActive = true,
                expiresAt = Date(System.currentTimeMillis() + 3600_000),
                willRenew = true,
                state = LatestSubscriptionState.SUBSCRIBED,
            )
        val inactive =
            Entitlement(
                id = "premium",
                isActive = false,
                expiresAt = Date(System.currentTimeMillis() + 3600_000),
                willRenew = true,
                state = LatestSubscriptionState.SUBSCRIBED,
            )

        val merged = mergeEntitlementsPrioritized(listOf(inactive, active))

        assertEquals(1, merged.size)
        assertTrue(merged.first().isActive)
    }

    @Test
    fun `mergeEntitlementsPrioritized merges productIds from all entitlements with same ID`() {
        val device =
            Entitlement(
                id = "premium",
                isActive = true,
                productIds = setOf("monthly_premium"),
                latestProductId = "monthly_premium",
                expiresAt = Date(System.currentTimeMillis() + 3600_000),
                willRenew = true,
            )

        val web =
            Entitlement(
                id = "premium",
                isActive = true,
                productIds = setOf("lifetime_premium"),
                latestProductId = "lifetime_premium",
                isLifetime = true,
            )

        val merged = mergeEntitlementsPrioritized(listOf(device, web))

        assertEquals(1, merged.size)
        val result = merged.first()
        // Lifetime should win (both active, lifetime takes priority)
        assertEquals(true, result.isLifetime)
        assertEquals("lifetime_premium", result.latestProductId)
        // ProductIds should be merged from both entitlements
        assertEquals(setOf("monthly_premium", "lifetime_premium"), result.productIds)
    }

    @Test
    fun `mergeEntitlementsPrioritized merges productIds from multiple same-ID entitlements`() {
        val ent1 =
            Entitlement(
                id = "premium",
                isActive = true,
                productIds = setOf("product_a"),
                isLifetime = true,
            )

        val ent2 =
            Entitlement(
                id = "premium",
                isActive = false,
                productIds = setOf("product_b"),
            )

        val ent3 =
            Entitlement(
                id = "premium",
                isActive = true,
                productIds = setOf("product_c"),
                isLifetime = false,
            )

        val merged = mergeEntitlementsPrioritized(listOf(ent1, ent2, ent3))

        assertEquals(1, merged.size)
        val result = merged.first()
        // Lifetime should win
        assertEquals(true, result.isLifetime)
        // All productIds should be merged
        assertEquals(setOf("product_a", "product_b", "product_c"), result.productIds)
    }
}
