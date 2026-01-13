package com.superwall.sdk.models.customer

import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.store.abstractions.product.receipt.LatestSubscriptionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class CustomerInfoMergingTest {
    @Test
    fun `merge combines subscriptions from both sources`() {
        val deviceInfo =
            CustomerInfo(
                subscriptions =
                    listOf(
                        SubscriptionTransaction(
                            transactionId = "tx1",
                            productId = "product1",
                            purchaseDate = Date(1000),
                            isActive = true,
                            expirationDate = Date(2000),
                            willRenew = true,
                            isInGracePeriod = false,
                            isRevoked = false,
                            isInBillingRetryPeriod = false,
                        ),
                    ),
                nonSubscriptions = emptyList(),
                userId = "user123",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions =
                    listOf(
                        SubscriptionTransaction(
                            transactionId = "tx2",
                            productId = "product2",
                            purchaseDate = Date(1500),
                            isActive = true,
                            expirationDate = Date(2500),
                            willRenew = true,
                            isInGracePeriod = false,
                            isRevoked = false,
                            isInBillingRetryPeriod = false,
                        ),
                    ),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(2, merged.subscriptions.size)
        assertTrue(merged.subscriptions.any { it.transactionId == "tx1" })
        assertTrue(merged.subscriptions.any { it.transactionId == "tx2" })
    }

    @Test
    fun `merge prioritizes active subscriptions over inactive`() {
        val inactiveTransaction =
            SubscriptionTransaction(
                transactionId = "tx1",
                productId = "product1",
                purchaseDate = Date(1000),
                isActive = false,
                expirationDate = Date(2000),
                willRenew = false,
                isInGracePeriod = false,
                isRevoked = false,
                isInBillingRetryPeriod = false,
            )

        val activeTransaction =
            SubscriptionTransaction(
                transactionId = "tx1",
                productId = "product1",
                purchaseDate = Date(1000),
                isActive = true,
                expirationDate = Date(2000),
                willRenew = true,
                isInGracePeriod = false,
                isRevoked = false,
                isInBillingRetryPeriod = false,
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = listOf(inactiveTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = listOf(activeTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.subscriptions.size)
        assertTrue(merged.subscriptions.first().isActive)
    }

    @Test
    fun `merge prioritizes will-renew over won't-renew`() {
        val wontRenewTransaction =
            SubscriptionTransaction(
                transactionId = "tx1",
                productId = "product1",
                purchaseDate = Date(1000),
                isActive = true,
                expirationDate = Date(2000),
                willRenew = false,
                isInGracePeriod = false,
                isRevoked = false,
                isInBillingRetryPeriod = false,
            )

        val willRenewTransaction =
            SubscriptionTransaction(
                transactionId = "tx1",
                productId = "product1",
                purchaseDate = Date(1000),
                isActive = true,
                expirationDate = Date(2000),
                willRenew = true,
                isInGracePeriod = false,
                isRevoked = false,
                isInBillingRetryPeriod = false,
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = listOf(wontRenewTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = listOf(willRenewTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.subscriptions.size)
        assertTrue(merged.subscriptions.first().willRenew)
    }

    @Test
    fun `merge combines non-subscriptions and removes duplicates`() {
        val deviceInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions =
                    listOf(
                        NonSubscriptionTransaction(
                            transactionId = "tx1",
                            productId = "product1",
                            purchaseDate = Date(1000),
                            isConsumable = false,
                            isRevoked = false,
                        ),
                    ),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions =
                    listOf(
                        NonSubscriptionTransaction(
                            transactionId = "tx1", // Same transaction ID
                            productId = "product1",
                            purchaseDate = Date(1000),
                            isConsumable = false,
                            isRevoked = false,
                        ),
                        NonSubscriptionTransaction(
                            transactionId = "tx2",
                            productId = "product2",
                            purchaseDate = Date(1500),
                            isConsumable = false,
                            isRevoked = false,
                        ),
                    ),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(2, merged.nonSubscriptions.size)
        assertTrue(merged.nonSubscriptions.any { it.transactionId == "tx1" })
        assertTrue(merged.nonSubscriptions.any { it.transactionId == "tx2" })
    }

    @Test
    fun `merge prioritizes device userId over empty web userId`() {
        val deviceInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "device_user",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals("device_user", merged.userId)
    }

    @Test
    fun `merge prioritizes active entitlements over inactive`() {
        val inactiveEntitlement =
            Entitlement(
                id = "ent1",
                isActive = false,
            )

        val activeEntitlement =
            Entitlement(
                id = "ent1",
                isActive = true,
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(inactiveEntitlement),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(activeEntitlement),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.entitlements.size)
        assertTrue(merged.entitlements.first().isActive)
    }

    @Test
    fun `merge prioritizes lifetime entitlements`() {
        val lifetimeEntitlement =
            Entitlement(
                id = "ent1",
                isActive = true,
                isLifetime = true,
            )

        val regularEntitlement =
            Entitlement(
                id = "ent1",
                isActive = true,
                isLifetime = false,
                expiresAt = Date(3000),
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(regularEntitlement),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(lifetimeEntitlement),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.entitlements.size)
        assertEquals(true, merged.entitlements.first().isLifetime)
    }

    @Test
    fun `merge returns empty CustomerInfo when both are blank`() {
        val deviceInfo = CustomerInfo.empty()
        val webInfo = CustomerInfo.empty()

        val merged = deviceInfo.merge(webInfo)

        assertTrue(merged.isPlaceholder)
        assertEquals(0, merged.subscriptions.size)
        assertEquals(0, merged.nonSubscriptions.size)
        assertEquals(0, merged.entitlements.size)
    }

    @Test
    fun `merge prioritizes non-revoked subscriptions over revoked`() {
        val revokedTransaction =
            SubscriptionTransaction(
                transactionId = "tx1",
                productId = "product1",
                purchaseDate = Date(1000),
                willRenew = true,
                isRevoked = true,
                isInGracePeriod = false,
                isInBillingRetryPeriod = false,
                isActive = false,
                expirationDate = Date(2000),
            )

        val nonRevokedTransaction =
            SubscriptionTransaction(
                transactionId = "tx1",
                productId = "product1",
                purchaseDate = Date(1000),
                willRenew = true,
                isRevoked = false,
                isInGracePeriod = false,
                isInBillingRetryPeriod = false,
                isActive = true,
                expirationDate = Date(2000),
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = listOf(revokedTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = listOf(nonRevokedTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.subscriptions.size)
        assertEquals(false, merged.subscriptions.first().isRevoked)
    }

    @Test
    fun `merge prioritizes subscriptions in grace period`() {
        val normalTransaction =
            SubscriptionTransaction(
                transactionId = "tx1",
                productId = "product1",
                purchaseDate = Date(1000),
                willRenew = true,
                isRevoked = false,
                isInGracePeriod = false,
                isInBillingRetryPeriod = false,
                isActive = true,
                expirationDate = Date(2000),
            )

        val gracePeriodTransaction =
            SubscriptionTransaction(
                transactionId = "tx1",
                productId = "product1",
                purchaseDate = Date(1000),
                willRenew = true,
                isRevoked = false,
                isInGracePeriod = true,
                isInBillingRetryPeriod = false,
                isActive = true,
                expirationDate = Date(2000),
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = listOf(normalTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = listOf(gracePeriodTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.subscriptions.size)
        assertEquals(true, merged.subscriptions.first().isInGracePeriod)
    }

    @Test
    fun `merge sorts subscriptions by purchase date`() {
        val olderTransaction =
            SubscriptionTransaction(
                transactionId = "tx1",
                productId = "product1",
                purchaseDate = Date(1000),
                willRenew = true,
                isRevoked = false,
                isInGracePeriod = false,
                isInBillingRetryPeriod = false,
                isActive = true,
                expirationDate = Date(2000),
            )

        val newerTransaction =
            SubscriptionTransaction(
                transactionId = "tx2",
                productId = "product2",
                purchaseDate = Date(3000),
                willRenew = true,
                isRevoked = false,
                isInGracePeriod = false,
                isInBillingRetryPeriod = false,
                isActive = true,
                expirationDate = Date(4000),
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = listOf(newerTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = listOf(olderTransaction),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = emptyList(),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(2, merged.subscriptions.size)
        assertEquals(Date(1000), merged.subscriptions[0].purchaseDate)
        assertEquals(Date(3000), merged.subscriptions[1].purchaseDate)
    }

    @Test
    fun `merge handles entitlements with different subscription states`() {
        val subscribedEntitlement =
            Entitlement(
                id = "ent1",
                isActive = true,
                state = LatestSubscriptionState.SUBSCRIBED,
            )

        val gracePeriodEntitlement =
            Entitlement(
                id = "ent1",
                isActive = true,
                state = LatestSubscriptionState.GRACE_PERIOD,
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(gracePeriodEntitlement),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(subscribedEntitlement),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.entitlements.size)
        assertEquals(LatestSubscriptionState.SUBSCRIBED, merged.entitlements.first().state)
    }

    @Test
    fun `merge prioritizes entitlements with transaction history`() {
        val entitlementWithHistory =
            Entitlement(
                id = "ent1",
                isActive = true,
                startsAt = Date(1000),
            )

        val entitlementWithoutHistory =
            Entitlement(
                id = "ent1",
                isActive = true,
                startsAt = null,
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(entitlementWithoutHistory),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(entitlementWithHistory),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.entitlements.size)
        assertEquals(Date(1000), merged.entitlements.first().startsAt)
    }

    @Test
    fun `merge keeps later expiration date for entitlements`() {
        val earlierExpiration =
            Entitlement(
                id = "ent1",
                isActive = true,
                expiresAt = Date(2000),
            )

        val laterExpiration =
            Entitlement(
                id = "ent1",
                isActive = true,
                expiresAt = Date(5000),
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(earlierExpiration),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(laterExpiration),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.entitlements.size)
        assertEquals(Date(5000), merged.entitlements.first().expiresAt)
    }

    @Test
    fun `merge handles complex multi-source scenario`() {
        // Device has: active subscription + inactive entitlement
        val deviceSub =
            SubscriptionTransaction(
                transactionId = "device_sub",
                productId = "premium",
                purchaseDate = Date(1000),
                willRenew = true,
                isRevoked = false,
                isInGracePeriod = false,
                isInBillingRetryPeriod = false,
                isActive = true,
                expirationDate = Date(3000),
            )
        val deviceEnt = Entitlement(id = "premium_ent", isActive = false)

        // Web has: different subscription + active entitlement
        val webSub =
            SubscriptionTransaction(
                transactionId = "web_sub",
                productId = "basic",
                purchaseDate = Date(500),
                willRenew = false,
                isRevoked = false,
                isInGracePeriod = false,
                isInBillingRetryPeriod = false,
                isActive = false,
                expirationDate = Date(2000),
            )
        val webEnt = Entitlement(id = "premium_ent", isActive = true)

        val deviceInfo =
            CustomerInfo(
                subscriptions = listOf(deviceSub),
                nonSubscriptions = emptyList(),
                userId = "device_user",
                entitlements = listOf(deviceEnt),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = listOf(webSub),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(webEnt),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        // Should have both subscriptions (different IDs)
        assertEquals(2, merged.subscriptions.size)
        // Web subscription should be first (earlier purchase date)
        assertEquals("web_sub", merged.subscriptions[0].transactionId)
        assertEquals("device_sub", merged.subscriptions[1].transactionId)

        // Should have active entitlement from web
        assertEquals(1, merged.entitlements.size)
        assertTrue(merged.entitlements.first().isActive)

        // Should prefer device userId
        assertEquals("device_user", merged.userId)
    }

    @Test
    fun `merge handles null expiration dates in entitlements`() {
        val withExpiration =
            Entitlement(
                id = "ent1",
                isActive = true,
                expiresAt = Date(5000),
            )

        val withoutExpiration =
            Entitlement(
                id = "ent1",
                isActive = true,
                expiresAt = null,
            )

        val deviceInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(withoutExpiration),
                isPlaceholder = false,
            )

        val webInfo =
            CustomerInfo(
                subscriptions = emptyList(),
                nonSubscriptions = emptyList(),
                userId = "",
                entitlements = listOf(withExpiration),
                isPlaceholder = false,
            )

        val merged = deviceInfo.merge(webInfo)

        assertEquals(1, merged.entitlements.size)
        // Without expiration (lifetime) is kept when comparing
        assertEquals(null, merged.entitlements.first().expiresAt)
    }
}
