package com.superwall.sdk.customer

import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.customer.SubscriptionTransaction
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.internal.WebRedemptionResponse
import com.superwall.sdk.models.product.Store
import com.superwall.sdk.storage.LatestCustomerInfo
import com.superwall.sdk.storage.LatestDeviceCustomerInfo
import com.superwall.sdk.storage.LatestRedemptionResponse
import com.superwall.sdk.storage.LatestWebCustomerInfo
import com.superwall.sdk.storage.Storage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date

class CustomerInfoManagerTest {
    private lateinit var storage: Storage
    private lateinit var customerInfoFlow: MutableStateFlow<CustomerInfo>
    private lateinit var manager: CustomerInfoManager
    private val testDispatcher = StandardTestDispatcher()
    private val ioScope = IOScope(testDispatcher)

    @Before
    fun setup() {
        storage = mockk(relaxed = true)
        customerInfoFlow = MutableStateFlow(CustomerInfo.empty())
        manager =
            CustomerInfoManager(
                storage = storage,
                updateCustomerInfo = { customerInfoFlow.value = it },
                ioScope = ioScope,
                hasExternalPurchaseController = { false },
                getSubscriptionStatus = { SubscriptionStatus.Unknown },
            )
    }

    @Test
    fun `updateMergedCustomerInfo merges device and web info`() =
        runTest(testDispatcher) {
            val deviceInfo =
                CustomerInfo(
                    subscriptions =
                        listOf(
                            SubscriptionTransaction(
                                transactionId = "device_tx",
                                productId = "product1",
                                purchaseDate = Date(1000),
                                willRenew = true,
                                isRevoked = false,
                                isInGracePeriod = false,
                                isInBillingRetryPeriod = false,
                                isActive = true,
                                expirationDate = Date(2000),
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
                                transactionId = "web_tx",
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

            every { storage.read(LatestDeviceCustomerInfo) } returns deviceInfo
            every { storage.read(LatestWebCustomerInfo) } returns webInfo

            manager.updateMergedCustomerInfo()
            testDispatcher.scheduler.advanceUntilIdle()

            verify { storage.write(LatestCustomerInfo, any()) }
            assert(customerInfoFlow.value.subscriptions.size == 2)
            assert(customerInfoFlow.value.userId == "user123")
        }

    @Test
    fun `updateMergedCustomerInfo uses device info when web is blank`() =
        runTest(testDispatcher) {
            val deviceInfo =
                CustomerInfo(
                    subscriptions =
                        listOf(
                            SubscriptionTransaction(
                                transactionId = "device_tx",
                                productId = "product1",
                                purchaseDate = Date(1000),
                                willRenew = true,
                                isRevoked = false,
                                isInGracePeriod = false,
                                isInBillingRetryPeriod = false,
                                isActive = true,
                                expirationDate = Date(2000),
                            ),
                        ),
                    nonSubscriptions = emptyList(),
                    userId = "user123",
                    entitlements = emptyList(),
                    isPlaceholder = false,
                )

            every { storage.read(LatestDeviceCustomerInfo) } returns deviceInfo
            every { storage.read(LatestWebCustomerInfo) } returns null

            manager.updateMergedCustomerInfo()
            testDispatcher.scheduler.advanceUntilIdle()

            verify { storage.write(LatestCustomerInfo, deviceInfo) }
            assert(customerInfoFlow.value == deviceInfo)
        }

    @Test
    fun `updateMergedCustomerInfo uses web info when device is blank`() =
        runTest(testDispatcher) {
            val webInfo =
                CustomerInfo(
                    subscriptions =
                        listOf(
                            SubscriptionTransaction(
                                transactionId = "web_tx",
                                productId = "product1",
                                purchaseDate = Date(1000),
                                willRenew = true,
                                isRevoked = false,
                                isInGracePeriod = false,
                                isInBillingRetryPeriod = false,
                                isActive = true,
                                expirationDate = Date(2000),
                            ),
                        ),
                    nonSubscriptions = emptyList(),
                    userId = "webuser",
                    entitlements = emptyList(),
                    isPlaceholder = false,
                )

            every { storage.read(LatestDeviceCustomerInfo) } returns null
            every { storage.read(LatestWebCustomerInfo) } returns webInfo

            manager.updateMergedCustomerInfo()
            testDispatcher.scheduler.advanceUntilIdle()

            verify { storage.write(LatestCustomerInfo, webInfo) }
            assert(customerInfoFlow.value == webInfo)
        }

    @Test
    fun `updateMergedCustomerInfo returns empty when both are blank`() =
        runTest(testDispatcher) {
            every { storage.read(LatestDeviceCustomerInfo) } returns null
            every { storage.read(LatestWebCustomerInfo) } returns null

            manager.updateMergedCustomerInfo()
            testDispatcher.scheduler.advanceUntilIdle()

            verify { storage.write(LatestCustomerInfo, match { it.isPlaceholder }) }
            assert(customerInfoFlow.value.isPlaceholder)
        }

    // Tests for external purchase controller flow

    @Test
    fun `forExternalPurchaseController uses subscription status as source of truth`() =
        runTest(testDispatcher) {
            // Create entitlements with different sources
            val playStoreEntitlement =
                Entitlement(
                    id = "pro",
                    isActive = true,
                    store = Store.PLAY_STORE,
                )
            val webEntitlement =
                Entitlement(
                    id = "web_feature",
                    isActive = true,
                    store = Store.STRIPE,
                )
            val deviceEntitlement =
                Entitlement(
                    id = "device_feature",
                    isActive = false, // inactive
                    store = Store.PLAY_STORE,
                )

            val deviceInfo =
                CustomerInfo(
                    subscriptions = emptyList(),
                    nonSubscriptions = emptyList(),
                    userId = "user123",
                    entitlements = listOf(deviceEntitlement),
                    isPlaceholder = false,
                )

            val webCustomerInfo =
                CustomerInfo(
                    subscriptions = emptyList(),
                    nonSubscriptions = emptyList(),
                    userId = "",
                    entitlements = listOf(webEntitlement),
                    isPlaceholder = false,
                )

            val subscriptionStatus =
                SubscriptionStatus.Active(
                    entitlements = setOf(playStoreEntitlement),
                )

            every { storage.read(LatestDeviceCustomerInfo) } returns deviceInfo
            every { storage.read(LatestRedemptionResponse) } returns
                WebRedemptionResponse(
                    codes = emptyList(),
                    allCodes = emptyList(),
                    customerInfo = webCustomerInfo,
                )

            // Create manager with external controller
            val externalManager =
                CustomerInfoManager(
                    storage = storage,
                    updateCustomerInfo = { customerInfoFlow.value = it },
                    ioScope = ioScope,
                    hasExternalPurchaseController = { true },
                    getSubscriptionStatus = { subscriptionStatus },
                )

            externalManager.updateMergedCustomerInfo()
            testDispatcher.scheduler.advanceUntilIdle()

            val result = customerInfoFlow.value

            // Should include: pro (from subscription status), web_feature (from web), device_feature (inactive from device)
            assert(result.entitlements.size == 3) { "Expected 3 entitlements, got ${result.entitlements.size}" }
            assert(result.entitlements.any { it.id == "pro" && it.isActive }) { "pro should be active" }
            assert(result.entitlements.any { it.id == "web_feature" }) { "web_feature should be present" }
            assert(result.entitlements.any { it.id == "device_feature" && !it.isActive }) { "device_feature should be inactive" }
        }

    @Test
    fun `forExternalPurchaseController filters Play Store entitlements from subscription status`() =
        runTest(testDispatcher) {
            // External controller has both Play Store and web entitlements in subscription status
            val playStoreEntitlement =
                Entitlement(
                    id = "play_pro",
                    isActive = true,
                    store = Store.PLAY_STORE,
                )
            val stripeEntitlement =
                Entitlement(
                    id = "stripe_pro",
                    isActive = true,
                    store = Store.STRIPE, // Should be filtered out from external entitlements
                )

            val subscriptionStatus =
                SubscriptionStatus.Active(
                    entitlements = setOf(playStoreEntitlement, stripeEntitlement),
                )

            every { storage.read(LatestDeviceCustomerInfo) } returns null
            every { storage.read(LatestRedemptionResponse) } returns null

            val externalManager =
                CustomerInfoManager(
                    storage = storage,
                    updateCustomerInfo = { customerInfoFlow.value = it },
                    ioScope = ioScope,
                    hasExternalPurchaseController = { true },
                    getSubscriptionStatus = { subscriptionStatus },
                )

            externalManager.updateMergedCustomerInfo()
            testDispatcher.scheduler.advanceUntilIdle()

            val result = customerInfoFlow.value

            // Should only include play_pro (filtered by PLAY_STORE)
            assert(result.entitlements.size == 1) { "Expected 1 entitlement, got ${result.entitlements.size}" }
            assert(result.entitlements.first().id == "play_pro") { "Should be play_pro" }
        }

    @Test
    fun `forExternalPurchaseController merges transactions from device and web`() =
        runTest(testDispatcher) {
            val deviceTx =
                SubscriptionTransaction(
                    transactionId = "device_tx",
                    productId = "product1",
                    purchaseDate = Date(1000),
                    willRenew = true,
                    isRevoked = false,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                    expirationDate = Date(2000),
                )

            val webTx =
                SubscriptionTransaction(
                    transactionId = "web_tx",
                    productId = "product2",
                    purchaseDate = Date(1500),
                    willRenew = true,
                    isRevoked = false,
                    isInGracePeriod = false,
                    isInBillingRetryPeriod = false,
                    isActive = true,
                    expirationDate = Date(2500),
                )

            val deviceInfo =
                CustomerInfo(
                    subscriptions = listOf(deviceTx),
                    nonSubscriptions = emptyList(),
                    userId = "user123",
                    entitlements = emptyList(),
                    isPlaceholder = false,
                )

            val webCustomerInfo =
                CustomerInfo(
                    subscriptions = listOf(webTx),
                    nonSubscriptions = emptyList(),
                    userId = "",
                    entitlements = emptyList(),
                    isPlaceholder = false,
                )

            every { storage.read(LatestDeviceCustomerInfo) } returns deviceInfo
            every { storage.read(LatestRedemptionResponse) } returns
                WebRedemptionResponse(
                    codes = emptyList(),
                    allCodes = emptyList(),
                    customerInfo = webCustomerInfo,
                )

            val externalManager =
                CustomerInfoManager(
                    storage = storage,
                    updateCustomerInfo = { customerInfoFlow.value = it },
                    ioScope = ioScope,
                    hasExternalPurchaseController = { true },
                    getSubscriptionStatus = { SubscriptionStatus.Unknown },
                )

            externalManager.updateMergedCustomerInfo()
            testDispatcher.scheduler.advanceUntilIdle()

            val result = customerInfoFlow.value

            // Should have both transactions merged
            assert(result.subscriptions.size == 2) { "Expected 2 subscriptions, got ${result.subscriptions.size}" }
            assert(result.userId == "user123") { "userId should be from device" }
        }

    @Test
    fun `forExternalPurchaseController returns empty entitlements when status is Inactive`() =
        runTest(testDispatcher) {
            every { storage.read(LatestDeviceCustomerInfo) } returns null
            every { storage.read(LatestRedemptionResponse) } returns null

            val externalManager =
                CustomerInfoManager(
                    storage = storage,
                    updateCustomerInfo = { customerInfoFlow.value = it },
                    ioScope = ioScope,
                    hasExternalPurchaseController = { true },
                    getSubscriptionStatus = { SubscriptionStatus.Inactive },
                )

            externalManager.updateMergedCustomerInfo()
            testDispatcher.scheduler.advanceUntilIdle()

            val result = customerInfoFlow.value

            assert(result.entitlements.isEmpty()) { "Should have no entitlements when Inactive" }
        }
}
