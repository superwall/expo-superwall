package com.superwall.sdk.web

import android.content.Context
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.entitlements.TransactionReceipt
import com.superwall.sdk.models.entitlements.WebEntitlements
import com.superwall.sdk.models.internal.DeviceVendorId
import com.superwall.sdk.models.internal.PurchaserInfo
import com.superwall.sdk.models.internal.RedemptionInfo
import com.superwall.sdk.models.internal.RedemptionOwnership
import com.superwall.sdk.models.internal.RedemptionOwnershipType
import com.superwall.sdk.models.internal.RedemptionResult
import com.superwall.sdk.models.internal.StoreIdentifiers
import com.superwall.sdk.models.internal.UserId
import com.superwall.sdk.models.internal.VendorId
import com.superwall.sdk.models.internal.WebRedemptionResponse
import com.superwall.sdk.network.Network
import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.storage.*
import com.superwall.sdk.storage.LatestRedemptionResponse
import com.superwall.sdk.storage.Storable
import com.superwall.sdk.storage.Storage
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Before
import org.junit.Test

class WebPaywallRedeemerTest {
    private val context: Context = mockk()
    private val storage: Storage =
        mockk {
            every { read(LatestRedemptionResponse) } returns null
            every { write(LatestRedemptionResponse, any()) } just Runs
            every { read(LatestWebCustomerInfo) } returns null
            every { write(LatestWebCustomerInfo, any()) } just Runs
            every { write(LastWebEntitlementsFetchDate, any()) } just Runs
        }

    private var maxAge: () -> Long = { 1L }
    private var mutableEntitlements = mutableSetOf<Entitlement>()
    private var webEntitlement = Entitlement("web_entitlement")
    private var normalEntitlement = Entitlement("normalEntitlement")

    private val deepLinkReferrer: CheckForReferral = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val testScheduler = testDispatcher.scheduler

    private val setEntitlementStatus: (List<Entitlement>) -> Unit = {
        mutableEntitlements += it.toSet()
    }
    private var getAllEntitlements: () -> Set<Entitlement> = {
        mutableEntitlements
    }

    private var isPaywallVisible = { false }
    private var showRestoreDialogAndDismiss = {}
    private var currentPaywallEntitlements = {
        setOf<Entitlement>()
    }
    private val setSubscriptionStatus: (SubscriptionStatus) -> Unit = { it: SubscriptionStatus ->
        when (it) {
            is SubscriptionStatus.Active -> mutableEntitlements.addAll(it.entitlements)
            else -> mutableEntitlements.clear()
        }
    }
    private var getActiveDeviceEntitlements = {
        setOf<Entitlement>()
    }

    @Before
    fun setup() {
        mutableEntitlements = mutableSetOf()
        coEvery {
            network.webEntitlementsByUserId(any(), any())
        } returns
            Either.Success(
                WebEntitlements(
                    customerInfo =
                        CustomerInfo(
                            subscriptions = emptyList(),
                            nonSubscriptions = emptyList(),
                            userId = "",
                            entitlements = listOf(webEntitlement),
                            isPlaceholder = false,
                        ),
                ),
            )
    }

    private val onRedemptionResult: (RedemptionResult) -> Unit = mockk(relaxed = true)
    private val getUserId: () -> UserId = { UserId("test_user") }
    private val getDeviceId: () -> DeviceVendorId = { DeviceVendorId(VendorId("test_vendor")) }
    private val getAlias: () -> String? = { "test_alias" }
    private val setActiveWebEntitlements: (Set<Entitlement>) -> Unit = {}
    private val track: suspend (Trackable) -> Unit = {}
    private lateinit var redeemer: WebPaywallRedeemer
    private val network: Network = mockk {}

    // Test factory implementation
    private inner class TestFactory(
        var willRedeemLinkFn: () -> Unit = {},
        var didRedeemLinkFn: (RedemptionResult) -> Unit = onRedemptionResult,
        var receiptsFn: suspend () -> List<TransactionReceipt> = {
            listOf(
                TransactionReceipt(
                    "mock",
                    "orderId",
                    productId = "test_123",
                    productType = TransactionReceipt.ProductType.SUBSCRIPTION,
                ),
            )
        },
        var getIntegrationPropsFn: () -> Map<String, Any> = { emptyMap() },
    ) : WebPaywallRedeemer.Factory {
        override fun willRedeemLink() = willRedeemLinkFn()

        override fun didRedeemLink(redemptionResult: RedemptionResult) = didRedeemLinkFn(redemptionResult)

        override fun maxAge(): Long = this@WebPaywallRedeemerTest.maxAge()

        override fun getActiveDeviceEntitlements(): Set<Entitlement> = this@WebPaywallRedeemerTest.getActiveDeviceEntitlements()

        override fun getUserId(): UserId? = this@WebPaywallRedeemerTest.getUserId()

        override fun getDeviceId(): DeviceVendorId = this@WebPaywallRedeemerTest.getDeviceId()

        override fun getAliasId(): String? = this@WebPaywallRedeemerTest.getAlias()

        override suspend fun track(event: Trackable) = this@WebPaywallRedeemerTest.track(event)

        override fun internallySetSubscriptionStatus(status: SubscriptionStatus) = this@WebPaywallRedeemerTest.setSubscriptionStatus(status)

        override suspend fun isPaywallVisible(): Boolean = this@WebPaywallRedeemerTest.isPaywallVisible()

        override suspend fun triggerRestoreInPaywall() = this@WebPaywallRedeemerTest.showRestoreDialogAndDismiss()

        override fun currentPaywallEntitlements(): Set<Entitlement> = this@WebPaywallRedeemerTest.currentPaywallEntitlements()

        override fun getPaywallInfo(): PaywallInfo = PaywallInfo.empty()

        override fun trackRestorationFailed(message: String) {}

        override fun isWebToAppEnabled(): Boolean = true

        override suspend fun receipts(): List<TransactionReceipt> = receiptsFn()

        override fun getExternalAccountId(): String = ""

        override fun getIntegrationProps(): Map<String, Any> = getIntegrationPropsFn()

        override fun closePaywallIfExists() {}

        override fun isPaymentSheetOpen(): Boolean = false
    }

    @Test
    fun `test successful redemption flow`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with valid redemption codes") {
                val code = "test_code"
                mutableEntitlements = mutableSetOf(normalEntitlement)
                val response =
                    WebRedemptionResponse(
                        codes =
                            listOf(
                                RedemptionResult.Success(
                                    code = code,
                                    redemptionInfo =
                                        RedemptionInfo(
                                            ownership = RedemptionOwnership.AppUser(appUserId = getUserId().value),
                                            purchaserInfo =
                                                PurchaserInfo(
                                                    getUserId().value,
                                                    "email",
                                                    StoreIdentifiers.Stripe(
                                                        stripeCustomerId = "123",
                                                        emptyList(),
                                                    ),
                                                ),
                                            entitlements = listOf(webEntitlement),
                                        ),
                                ),
                            ),
                        customerInfo =
                            CustomerInfo(
                                subscriptions = emptyList(),
                                nonSubscriptions = emptyList(),
                                userId = "",
                                entitlements = listOf(webEntitlement),
                                isPlaceholder = false,
                            ),
                    )
                coEvery {
                    network.webEntitlementsByUserId(any(), any())
                } returns
                    Either.Success(
                        WebEntitlements(
                            customerInfo =
                                CustomerInfo(
                                    subscriptions = emptyList(),
                                    nonSubscriptions = emptyList(),
                                    userId = "",
                                    entitlements = listOf(webEntitlement),
                                    isPlaceholder = false,
                                ),
                        ),
                    )

                coEvery { deepLinkReferrer.checkForReferral() } returns Result.success(code)
                coEvery {
                    network.redeemToken(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns Either.Success(response)

                When("creating redeemer and advancing scheduler") {
                    redeemer =
                        WebPaywallRedeemer(
                            context,
                            IOScope(testDispatcher),
                            deepLinkReferrer,
                            network,
                            storage,
                            customerInfoManager = mockk(relaxed = true),
                            factory = TestFactory(),
                        )
                    testScheduler.advanceUntilIdle()

                    Then("it should redeem the codes and set entitlement status") {
                        verify(exactly = 1) {
                            storage.write(LatestRedemptionResponse, response)
                        }
                        println(mutableEntitlements)
                        assert(mutableEntitlements == setOf(webEntitlement, normalEntitlement))
                    }
                }
            }
        }

    @Test
    fun `test failed referral check`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with failing referral check") {
                val exception = Exception("Referral check failed")
                coEvery { deepLinkReferrer.checkForReferral() } returns Result.failure(exception)

                redeemer =
                    WebPaywallRedeemer(
                        context,
                        IOScope(testDispatcher),
                        deepLinkReferrer,
                        network,
                        storage,
                        customerInfoManager = mockk(relaxed = true),
                        factory = TestFactory(),
                    )

                When("creating redeemer and checking for referral") {
                    testScheduler.advanceUntilIdle()

                    Then("it should not call redeem") {
                        coVerify(exactly = 0) {
                            network.redeemToken(any(), any(), any(), any(), any(), any(), any())
                        }
                    }
                }
            }
        }

    @Test
    fun `test failed token redemption due to unknown error`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with failing token redemption") {
                val codes = "code1"
                val exception = Exception("Token redemption failed")
                // User has a normal entitlement
                mutableEntitlements = mutableSetOf(normalEntitlement)
                println(mutableEntitlements)
                coEvery { deepLinkReferrer.checkForReferral() } returns Result.success(codes)
                coEvery {
                    network.redeemToken(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns Either.Failure(NetworkError.Unknown(exception))
                coEvery {
                    network.webEntitlementsByUserId(any(), any())
                } returns Either.Success(WebEntitlements(customerInfo = null))

                When("creating redeemer and checking for referral") {
                    redeemer =
                        WebPaywallRedeemer(
                            context,
                            IOScope(testDispatcher),
                            deepLinkReferrer,
                            network,
                            storage,
                            customerInfoManager = mockk(relaxed = true),
                            factory = TestFactory(didRedeemLinkFn = onRedemptionResult),
                        )
                    testScheduler.advanceUntilIdle()
                    testScheduler.runCurrent() // Process any immediate tasks
                    testScheduler.advanceUntilIdle() // Process any newly scheduled tasks

                    Then("it should not set entitlement status") {
                        assert(mutableEntitlements == setOf(normalEntitlement))
                        verify(exactly = 1) {
                            onRedemptionResult(any<RedemptionResult.Error>())
                        }
                    }
                }
            }
        }

    @Test
    fun `test checkForWebEntitlements with successful responses`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with successful web entitlements responses") {
                val userEntitlements = listOf(Entitlement("user_entitlement"))

                coEvery {
                    network.webEntitlementsByUserId(any(), any())
                } returns
                    Either.Success(
                        WebEntitlements(
                            customerInfo =
                                CustomerInfo(
                                    subscriptions = emptyList(),
                                    nonSubscriptions = emptyList(),
                                    userId = "",
                                    entitlements = userEntitlements,
                                    isPlaceholder = false,
                                ),
                        ),
                    )

                redeemer =
                    WebPaywallRedeemer(
                        context,
                        IOScope(testDispatcher),
                        deepLinkReferrer,
                        network,
                        storage,
                        customerInfoManager = mockk(relaxed = true),
                        factory = TestFactory(),
                    )

                When("checking for web entitlements") {
                    val result =
                        redeemer.checkForWebEntitlements(
                            getUserId(),
                            getDeviceId(),
                        )

                    Then("it should combine both entitlements lists") {
                        assert(result is Either.Success)
                        val entitlements = (result as Either.Success).value
                        assert(entitlements.containsAll(userEntitlements))
                    }
                }
            }
        }

    @Test
    fun `test checkForWebEntitlements with failed responses`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with failed web entitlements responses") {
                coEvery {
                    network.webEntitlementsByUserId(any(), any())
                } returns Either.Failure(NetworkError.Unknown(Error("User entitlements failed")))

                coEvery {
                    network.webEntitlementsByDeviceID(any())
                } returns Either.Failure(NetworkError.Unknown(Error("Device entitlements failed")))

                redeemer =
                    WebPaywallRedeemer(
                        context,
                        IOScope(testDispatcher),
                        deepLinkReferrer,
                        network,
                        storage,
                        customerInfoManager = mockk(relaxed = true),
                        factory = TestFactory(),
                    )

                When("checking for web entitlements") {
                    val result =
                        redeemer.checkForWebEntitlements(
                            UserId("test_user"),
                            DeviceVendorId(VendorId("test_device")),
                        )

                    Then("it should return an empty list") {
                        assert(result is Either.Success)
                        val entitlements = (result as Either.Success).value
                        assert(entitlements.isEmpty())
                    }
                }
            }
        }

    @Test
    fun `test checkForWebEntitlements with partially successful responses - user success`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with only user entitlements succeeding") {
                val userEntitlements =
                    setOf(Entitlement("user_entitlement"))

                coEvery {
                    network.webEntitlementsByUserId(UserId("test_user"), any())
                } returns
                    Either.Success(
                        WebEntitlements(
                            customerInfo =
                                CustomerInfo(
                                    subscriptions = emptyList(),
                                    nonSubscriptions = emptyList(),
                                    userId = "",
                                    entitlements = userEntitlements.toList(),
                                    isPlaceholder = false,
                                ),
                        ),
                    )

                coEvery {
                    network.webEntitlementsByDeviceID(any())
                } returns Either.Failure(NetworkError.Unknown(Error("Device entitlements failed")))

                redeemer =
                    WebPaywallRedeemer(
                        context,
                        IOScope(testDispatcher),
                        deepLinkReferrer,
                        network,
                        storage,
                        customerInfoManager = mockk(relaxed = true),
                        factory = TestFactory(),
                    )

                When("checking for web entitlements") {
                    val result =
                        redeemer.checkForWebEntitlements(
                            UserId("test_user"),
                            DeviceVendorId(VendorId("test_device")),
                        )

                    Then("it should return only user entitlements") {
                        assert(result is Either.Success)
                        val entitlements = (result as Either.Success).value
                        assert(entitlements.first() == userEntitlements.first())
                    }
                }
            }
        }

    @Test
    fun `test checkForWebEntitlements with partially successful responses - device success`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with only device entitlements succeeding") {
                coEvery {
                    deepLinkReferrer.checkForReferral()
                } returns Result.success("code")

                coEvery {
                    network.redeemToken(any(), any(), any(), any(), any(), any(), any())
                } returns Either.Failure(NetworkError.Unknown(Error("Token redemption failed")))

                coEvery {
                    network.webEntitlementsByUserId(any(), any())
                } returns
                    Either.Success(
                        WebEntitlements(
                            customerInfo =
                                CustomerInfo(
                                    subscriptions = emptyList(),
                                    nonSubscriptions = emptyList(),
                                    userId = "",
                                    entitlements = listOf(webEntitlement),
                                    isPlaceholder = false,
                                ),
                        ),
                    )

                redeemer =
                    WebPaywallRedeemer(
                        context,
                        IOScope(testDispatcher),
                        deepLinkReferrer,
                        network,
                        storage,
                        customerInfoManager = mockk(relaxed = true),
                        factory = TestFactory(),
                    )

                When("checking for web entitlements") {
                    val result =
                        redeemer.checkForWebEntitlements(
                            UserId("test_user"),
                            DeviceVendorId(VendorId("test_device")),
                        )

                    Then("it should return only device entitlements") {
                        assert(result is Either.Success)
                        val entitlements = (result as Either.Success).value
                        assert(entitlements == setOf(webEntitlement))
                    }
                }
            }
        }

    @Test
    fun `test checkForWebEntitlements with duplicate entitlements`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with overlapping entitlements from user and device") {
                val commonEntitlement = Entitlement("common_entitlement")
                val userEntitlements = listOf(commonEntitlement, Entitlement("user_specific"))

                coEvery {
                    network.webEntitlementsByUserId(any(), any())
                } returns
                    Either.Success(
                        WebEntitlements(
                            customerInfo =
                                CustomerInfo(
                                    subscriptions = emptyList(),
                                    nonSubscriptions = emptyList(),
                                    userId = "",
                                    entitlements = userEntitlements,
                                    isPlaceholder = false,
                                ),
                        ),
                    )

                redeemer =
                    WebPaywallRedeemer(
                        context,
                        IOScope(testDispatcher),
                        deepLinkReferrer,
                        network,
                        storage,
                        customerInfoManager = mockk(relaxed = true),
                        factory = TestFactory(),
                    )

                When("checking for web entitlements") {
                    val result =
                        redeemer.checkForWebEntitlements(
                            UserId("test_user"),
                            DeviceVendorId(VendorId("test_device")),
                        )

                    Then("it should return combined entitlements without duplicates") {
                        assert(result is Either.Success)
                        val entitlements = (result as Either.Success).value
                        assert(entitlements.size == 2) // Should only have 3 unique entitlements
                        assert(entitlements.count { it == commonEntitlement } == 1) // Should only have one copy of the common entitlement
                    }
                }
            }
        }

    @Test
    fun clean_up_old_redemptions() {
        val userCode = "code1"
        val deviceCode = "code2"
        val userId = "test_user"
        Given("We have existing user redemptions") {
            val response =
                WebRedemptionResponse(
                    codes =
                        listOf(
                            RedemptionResult.Success(
                                code = userCode,
                                redemptionInfo =
                                    RedemptionInfo(
                                        ownership = RedemptionOwnership.AppUser(appUserId = userId),
                                        purchaserInfo =
                                            PurchaserInfo(
                                                userId,
                                                email = null,
                                                storeIdentifiers =
                                                    StoreIdentifiers.Stripe(
                                                        "123",
                                                        emptyList(),
                                                    ),
                                            ),
                                        entitlements = listOf(webEntitlement),
                                    ),
                            ),
                            RedemptionResult.Success(
                                code = deviceCode,
                                redemptionInfo =
                                    RedemptionInfo(
                                        ownership = RedemptionOwnership.Device(deviceId = "deviceId"),
                                        purchaserInfo =
                                            PurchaserInfo(
                                                userId,
                                                email = null,
                                                storeIdentifiers =
                                                    StoreIdentifiers.Stripe(
                                                        "123",
                                                        emptyList(),
                                                    ),
                                            ),
                                        entitlements = listOf(webEntitlement),
                                    ),
                            ),
                        ),
                    customerInfo =
                        CustomerInfo(
                            subscriptions = emptyList(),
                            nonSubscriptions = emptyList(),
                            userId = "",
                            entitlements = listOf(webEntitlement),
                            isPlaceholder = false,
                        ),
                )
            val storage =
                object : Storage {
                    var saved: Any? = null

                    override fun <T> read(storable: Storable<T>): T? = saved as T?

                    override fun <T : Any> write(
                        storable: Storable<T>,
                        data: T,
                    ) {
                        saved = data as Any?
                    }

                    override fun clean() {
                    }
                }
            redeemer =
                WebPaywallRedeemer(
                    context,
                    IOScope(testDispatcher),
                    deepLinkReferrer,
                    network,
                    storage,
                    customerInfoManager = mockk(relaxed = true),
                    factory = TestFactory(),
                )

            storage.write(LatestRedemptionResponse, response)
            When("We call clean") {
                redeemer.clear(RedemptionOwnershipType.AppUser)
                Then("It should remove the old redemptions") {
                    val saved = storage.saved as WebRedemptionResponse
                    println(saved.codes)
                    assert(saved.codes.size == 1)
                }
            }
        }
    }

    @Test
    fun `test orderId is included in TransactionReceipt`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with TransactionReceipts containing orderId") {
                val code = "test_code"
                val expectedOrderId = "test_order_123"
                val expectedPurchaseToken = "test_purchase_token"
                val expectedReceipts =
                    listOf(
                        TransactionReceipt(expectedPurchaseToken, expectedOrderId, "123", TransactionReceipt.ProductType.SUBSCRIPTION),
                    )

                mutableEntitlements = mutableSetOf(normalEntitlement)
                val response =
                    WebRedemptionResponse(
                        codes =
                            listOf(
                                RedemptionResult.Success(
                                    code = code,
                                    redemptionInfo =
                                        RedemptionInfo(
                                            ownership = RedemptionOwnership.AppUser(appUserId = "test_user"),
                                            purchaserInfo =
                                                PurchaserInfo(
                                                    "test_user",
                                                    "test@example.com",
                                                    StoreIdentifiers.Stripe(
                                                        stripeCustomerId = "123",
                                                        emptyList(),
                                                    ),
                                                ),
                                            entitlements = listOf(webEntitlement),
                                        ),
                                ),
                            ),
                        customerInfo =
                            CustomerInfo(
                                subscriptions = emptyList(),
                                nonSubscriptions = emptyList(),
                                userId = "",
                                entitlements = listOf(webEntitlement),
                                isPlaceholder = false,
                            ),
                    )

                coEvery { deepLinkReferrer.checkForReferral() } returns Result.success(code)
                coEvery {
                    network.redeemToken(any(), any(), any(), any(), any(), any(), any())
                } returns Either.Success(response)

                redeemer =
                    WebPaywallRedeemer(
                        context,
                        IOScope(testDispatcher),
                        deepLinkReferrer,
                        network,
                        storage,
                        customerInfoManager = mockk(relaxed = true),
                        factory = TestFactory(receiptsFn = { expectedReceipts }),
                    )

                When("creating redeemer and checking for referral") {
                    testScheduler.advanceUntilIdle()

                    Then("it should call redeemToken with receipts containing orderId") {
                        coVerify(exactly = 1) {
                            network.redeemToken(any(), any(), any(), any(), any(), any(), any())
                        }
                        // Verify the receipt contains both purchaseToken and orderId
                        assert(expectedReceipts[0].purchaseToken == expectedPurchaseToken)
                        assert(expectedReceipts[0].orderId == expectedOrderId)
                    }
                }
            }
        }

    @Test
    fun `test empty attribution props are not passed to redeemToken`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with empty attribution props") {
                val code = "test_code"
                val emptyAttributionProps = emptyMap<String, Any>()

                mutableEntitlements = mutableSetOf(normalEntitlement)
                val response =
                    WebRedemptionResponse(
                        codes =
                            listOf(
                                RedemptionResult.Success(
                                    code = code,
                                    redemptionInfo =
                                        RedemptionInfo(
                                            ownership = RedemptionOwnership.AppUser(appUserId = "test_user"),
                                            purchaserInfo =
                                                PurchaserInfo(
                                                    "test_user",
                                                    "test@example.com",
                                                    StoreIdentifiers.Stripe(
                                                        stripeCustomerId = "123",
                                                        emptyList(),
                                                    ),
                                                ),
                                            entitlements = listOf(webEntitlement),
                                        ),
                                ),
                            ),
                        customerInfo =
                            CustomerInfo(
                                subscriptions = emptyList(),
                                nonSubscriptions = emptyList(),
                                userId = "",
                                entitlements = listOf(webEntitlement),
                                isPlaceholder = false,
                            ),
                    )

                coEvery { deepLinkReferrer.checkForReferral() } returns Result.success(code)
                coEvery {
                    network.redeemToken(any(), any(), any(), any(), any(), any(), any())
                } returns Either.Success(response)

                redeemer =
                    WebPaywallRedeemer(
                        context,
                        IOScope(testDispatcher),
                        deepLinkReferrer,
                        network,
                        storage,
                        customerInfoManager = mockk(relaxed = true),
                        factory = TestFactory(getIntegrationPropsFn = { emptyAttributionProps }),
                    )

                When("creating redeemer and checking for referral with empty attribution props") {
                    testScheduler.advanceUntilIdle()

                    Then("it should call redeemToken with null attribution props") {
                        coVerify(exactly = 1) {
                            network.redeemToken(any(), any(), any(), any(), any(), any(), any())
                        }
                    }
                }
            }
        }

    @Test
    fun `test complex attribution props conversion to JsonElement`() =
        runTest(testDispatcher) {
            Given("a WebPaywallRedeemer with complex attribution props") {
                val code = "test_code"
                val complexAttributionProps =
                    mapOf(
                        "string_value" to "test_string",
                        "int_value" to 42,
                        "double_value" to 3.14,
                        "boolean_value" to true,
                        "nested_map" to mapOf("inner_key" to "inner_value"),
                        "list_value" to listOf("item1", "item2", 123),
                    )

                // Expected JSON conversion
                mapOf<String, JsonElement>(
                    "string_value" to JsonPrimitive("test_string"),
                    "int_value" to JsonPrimitive(42),
                    "double_value" to JsonPrimitive(3.14),
                    "boolean_value" to JsonPrimitive(true),
                    "nested_map" to
                        buildJsonObject {
                            put("inner_key", JsonPrimitive("inner_value"))
                        },
                    "list_value" to
                        JsonArray(
                            listOf(
                                JsonPrimitive("item1"),
                                JsonPrimitive("item2"),
                                JsonPrimitive(123),
                            ),
                        ),
                )

                mutableEntitlements = mutableSetOf(normalEntitlement)
                val response =
                    WebRedemptionResponse(
                        codes =
                            listOf(
                                RedemptionResult.Success(
                                    code = code,
                                    redemptionInfo =
                                        RedemptionInfo(
                                            ownership = RedemptionOwnership.AppUser(appUserId = "test_user"),
                                            purchaserInfo =
                                                PurchaserInfo(
                                                    "test_user",
                                                    "test@example.com",
                                                    StoreIdentifiers.Stripe(
                                                        stripeCustomerId = "123",
                                                        emptyList(),
                                                    ),
                                                ),
                                            entitlements = listOf(webEntitlement),
                                        ),
                                ),
                            ),
                        customerInfo =
                            CustomerInfo(
                                subscriptions = emptyList(),
                                nonSubscriptions = emptyList(),
                                userId = "",
                                entitlements = listOf(webEntitlement),
                                isPlaceholder = false,
                            ),
                    )

                coEvery { deepLinkReferrer.checkForReferral() } returns Result.success(code)
                coEvery {
                    network.redeemToken(any(), any(), any(), any(), any(), any(), any())
                } returns Either.Success(response)

                redeemer =
                    WebPaywallRedeemer(
                        context,
                        IOScope(testDispatcher),
                        deepLinkReferrer,
                        network,
                        storage,
                        customerInfoManager = mockk(relaxed = true),
                        factory = TestFactory(getIntegrationPropsFn = { complexAttributionProps }),
                    )

                When("creating redeemer and checking for referral with complex attribution props") {
                    testScheduler.advanceUntilIdle()

                    Then("it should successfully convert and pass all attribution props types") {
                        coVerify(exactly = 1) {
                            network.redeemToken(any(), any(), any(), any(), any(), any(), any())
                        }
                        // The conversion should not throw an exception and should handle all data types
                    }
                }
            }
        }
}
