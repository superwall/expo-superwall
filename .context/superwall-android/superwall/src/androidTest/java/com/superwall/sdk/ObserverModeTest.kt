package com.superwall.sdk

import Given
import Then
import When
import android.app.Application
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo
import com.superwall.sdk.billing.BillingError
import com.superwall.sdk.config.options.PaywallOptions
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.delegate.SuperwallDelegate
import com.superwall.sdk.dependencies.DependencyContainer
import com.superwall.sdk.store.PurchasingObserverState
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.store.abstractions.product.RawStoreProduct
import com.superwall.sdk.store.abstractions.product.SubscriptionPeriod
import com.superwall.sdk.store.transactions.TransactionManager
import com.superwall.sdk.utilities.PurchaseMockBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ObserverModeTest {
    private lateinit var transactionManager: TransactionManager
    private lateinit var storeKitManager: StoreManager
    private lateinit var dependencyContainer: DependencyContainer
    val mockPricingPhases =
        mockk<ProductDetails.PricingPhases> {
            every { pricingPhaseList } returns
                listOf(
                    mockk {
                        every { billingPeriod } returns "P1M"
                        every { priceAmountMicros } returns 999000L
                        every { priceCurrencyCode } returns "USD"
                        every { billingCycleCount } returns 1
                        every { this@mockk.formattedPrice } returns "$9.99"
                        every { this@mockk.recurrenceMode } returns 0
                    },
                )
        }
    val mockSubscriptionOfferDetails =
        mockk<ProductDetails.SubscriptionOfferDetails> {
            every { basePlanId } returns "test_base_plan"
            every { offerId } returns "test_offer"
            every { pricingPhases } returns mockPricingPhases
            every { this@mockk.offerToken } returns "test_offer_token"
            every { this@mockk.offerTags } returns listOf("test_offer_tag")
        }

    val mockProductDetails =
        mockk<ProductDetails> {
            every { productId } returns "test_product"
            every { productType } returns "subs"
            every { subscriptionOfferDetails } returns listOf(mockSubscriptionOfferDetails)
            every { this@mockk.oneTimePurchaseOfferDetails } returns null
            every { this@mockk.name } returns "Test Product"
            every { description } returns "Test Product Description"
        }
    val mockProduct: RawStoreProduct =
        spyk<RawStoreProduct>(RawStoreProduct.from(mockProductDetails)) {
            every { underlyingProductDetails } returns mockProductDetails
            every { fullIdentifier } returns "test_product:test_base_plan:test_offer"
            every { productIdentifier } returns "test_product"
            every { hasFreeTrial } returns false
            every { subscriptionPeriod } returns
                SubscriptionPeriod(
                    1,
                    SubscriptionPeriod.Unit.month,
                )
            every { localizedSubscriptionPeriod } returns "1 month"
            every { price } returns BigDecimal.valueOf(9.99)
            every { localizedPrice } returns "$9.99"
            every { period } returns "P1M"
        }

    private lateinit var mockDelegate: MockDelegate

    val CONSTANT_API_KEY = "pk_0ff90006c5c2078e1ce832bd2343ba2f806ca510a0a1696a"
    var configured = false

    @Before
    fun setup() {
        mockkObject(RawStoreProduct.Companion)
        every { RawStoreProduct.from(any()) } returns mockProduct
        if (!configured) {
            Superwall.configure(
                InstrumentationRegistry.getInstrumentation().context.applicationContext as Application,
                CONSTANT_API_KEY,
                options =
                    SuperwallOptions().apply {
                        shouldObservePurchases = true
                        paywalls =
                            PaywallOptions().apply {
                                shouldPreload = false
                            }
                    },
                completion = {
                    configured = true
                },
            )
        }
        dependencyContainer = Superwall.instance.dependencyContainer
        transactionManager = dependencyContainer.transactionManager
        storeKitManager = dependencyContainer.storeManager
    }

    @After
    fun tearDown() {
        Superwall.initialized = false
    }

    @Test
    fun test_observe_purchase_will_begin_with_controller() =
        runTest(timeout = 5.minutes) {
            setup()
            Given("a configured Superwall instance with purchase observation enabled") {
                mockDelegate = MockDelegate(this@runTest)
                Superwall.instance.delegate = mockDelegate

                When("observing purchase will begin") {
                    Superwall.instance.observe(
                        PurchasingObserverState.PurchaseWillBegin(mockProductDetails),
                    )

                    Then("it should delegate to transaction manager and emit transaction start event") {
                        val event =
                            mockDelegate.events.first {
                                it is SuperwallEvent.TransactionStart
                            }
                    }
                }
            }
        }

    @Test
    fun test_observe_purchase_complete_with_controller() =
        runTest(timeout = 30.seconds) {
            setup()
            Given("a configured Superwall instance and completed purchase") {
                mockDelegate = MockDelegate(this@runTest)
                Superwall.instance.delegate = mockDelegate

                When("observing purchase completion") {
                    Superwall.instance.observe(
                        PurchasingObserverState.PurchaseWillBegin(mockProductDetails),
                    )
                    delayFor(1.seconds)
                    Superwall.instance.observe(
                        PurchasingObserverState.PurchaseResult(
                            result =
                                BillingResult
                                    .newBuilder()
                                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                                    .build(),
                            purchases =
                                listOf(
                                    PurchaseMockBuilder.createDefaultPurchase(
                                        "test_product:test_base_plan:test_offer",
                                    ),
                                ),
                        ),
                    )

                    Then("it should handle successful purchase and emit transaction complete event") {
                        mockDelegate.events.first {
                            it is SuperwallEvent.TransactionComplete
                        }
                    }
                }
            }
        }

    @Test
    fun test_observe_purchase_failed_with_controller() =
        runTest(timeout = 30.seconds) {
            setup()
            Given("a configured Superwall instance and failed purchase") {
                mockDelegate = MockDelegate(this@runTest)
                Superwall.instance.delegate = mockDelegate

                val error = BillingError.BillingNotAvailable("Test error")

                When("observing purchase failure") {
                    Superwall.instance.observe(
                        PurchasingObserverState.PurchaseWillBegin(mockProductDetails),
                    )
                    delayFor(1.seconds)
                    Superwall.instance.observe(
                        PurchasingObserverState.PurchaseError(
                            error = error,
                            product = mockProductDetails,
                        ),
                    )

                    Then("it should handle failure and emit transaction fail event") {
                        mockDelegate.events.first {
                            it is SuperwallEvent.TransactionFail
                        }
                    }
                }
            }
        }
}

class MockDelegate(
    val scope: CoroutineScope,
) : SuperwallDelegate {
    val events = MutableSharedFlow<SuperwallEvent>(extraBufferCapacity = 20)

    override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
        Log.e("test", "handle event is ${eventInfo.event}")
        scope.launch {
            events.emit(eventInfo.event)
        }
    }
}

suspend fun CoroutineScope.delayFor(duration: Duration) =
    async(Dispatchers.IO) {
        delay(duration)
    }.await()
