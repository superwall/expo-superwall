package com.superwall.sdk.store.transactions

import And
import Given
import Then
import When
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.billing.Billing
import com.superwall.sdk.billing.GoogleBillingWrapper
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.delegate.InternalPurchaseResult
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.misc.ActivityProvider
import com.superwall.sdk.misc.AlertControllerFactory.AlertProps
import com.superwall.sdk.misc.AppLifecycleObserver
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.Offer
import com.superwall.sdk.models.product.PlayStoreProduct
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.PaywallViewState
import com.superwall.sdk.storage.EventsQueue
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.store.InternalPurchaseController
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.store.abstractions.product.OfferType
import com.superwall.sdk.store.abstractions.product.RawStoreProduct
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.abstractions.transactions.GoogleBillingPurchaseTransaction
import com.superwall.sdk.store.abstractions.transactions.StoreTransaction
import com.superwall.sdk.utilities.PurchaseMockBuilder
import com.superwall.sdk.utilities.mockPricingPhase
import com.superwall.sdk.utilities.mockSubscriptionOfferDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class TransactionManagerTest {
    private val playProduct =
        mockk<ProductDetails> {
            every { productId } returns "product1"
            every { oneTimePurchaseOfferDetails } returns
                mockk {
                    every { priceAmountMicros } returns 1000000
                    every { priceCurrencyCode } returns "USD"
                }
        }

    val entitlements = setOf("test-entitlement").map { Entitlement(it) }.toSet()
    private val mockProduct =
        RawStoreProduct(
            playProduct,
            "product1",
            "basePlan",
            OfferType.Auto,
        )

    private val mockItems =
        listOf(
            ProductItem(
                "Item1",
                ProductItem.StoreProductType.PlayStore(
                    PlayStoreProduct(
                        productIdentifier = "product1",
                        basePlanIdentifier = "basePlan",
                        offer = Offer.Automatic(),
                    ),
                ),
                entitlements = entitlements.toSet(),
            ),
        )

    private val pwInfo =
        PaywallInfo.empty().copy(
            products =
            mockItems,
        )

    private val mockedPaywall: Paywall =
        mockk {
            every { getInfo(any()) } returns pwInfo
            every { productIds } returns listOf("product1")
            every { productItems } returns mockItems
            every { isFreeTrialAvailable } returns true
        }
    private val paywallView =
        mockk<PaywallView>(relaxUnitFun = true) {
            every { state } returns
                mockk {
                    every { info } returns pwInfo
                    every { paywall } returns mockedPaywall
                }
        }

    private var purchaseController = mockk<InternalPurchaseController>()
    private var billing: Billing =
        mockk {
            coEvery { awaitGetProducts(any()) } returns
                setOf(
                    StoreProduct(
                        mockProduct,
                    ),
                )
            coEvery { queryAllPurchases() } returns emptyList()
        }
    private var activityProvider =
        mockk<ActivityProvider> {
            every { getCurrentActivity() } returns mockk()
        }
    private val storeManager = spyk(StoreManager(purchaseController, billing))

    private var eventsQueue = mockk<EventsQueue>(relaxUnitFun = true)
    private var transactionManagerFactory =
        mockk<TransactionManager.Factory> {
            every { isWebToAppEnabled() } returns false
            every { makeHasExternalPurchaseController() } returns false
            coEvery { makeSessionDeviceAttributes() } returns hashMapOf()
            every { makeTransactionVerifier() } returns
                mockk<GoogleBillingWrapper> {
                    coEvery { getLatestTransaction(any()) } returns
                        StoreTransaction(
                            GoogleBillingPurchaseTransaction(
                                PurchaseMockBuilder.createDefaultPurchase("product1"),
                            ),
                            "config-req-id",
                            "app-session-id",
                        )
                }
            every { makeHasExternalPurchaseController() } returns false
            every { makeSuperwallOptions() } returns SuperwallOptions()
            every { getCurrentUserAttributes() } returns emptyMap()
        }

    private var storage = mockk<Storage>(relaxUnitFun = true)
    private var entitlementsById: (String) -> Set<Entitlement> = {
        setOf(Entitlement(it))
    }
    private var showRestoreDialogForWeb = {}

    private var mockShowAlert = mockk<(AlertProps) -> Unit>(relaxed = true)
    private var mockUpdateState = mockk<(String, PaywallViewState.Updates) -> Unit>(relaxed = true)

    fun TestScope.manager(
        trManagerFactory: TransactionManager.Factory = transactionManagerFactory,
        track: (TrackableSuperwallEvent) -> Unit = {},
        dismiss: (paywallId: String, result: PaywallResult) -> Unit = { _, _ -> },
        showAlert: (AlertProps) -> Unit = mockShowAlert,
        updateState: (cacheKey: String, update: PaywallViewState.Updates) -> Unit = mockUpdateState,
        subscriptionStatus: () -> SubscriptionStatus = {
            SubscriptionStatus.Active(entitlements)
        },
        _storeManager: StoreManager = storeManager,
        options: SuperwallOptions.() -> Unit = {},
    ): TransactionManager {
        coEvery { transactionManagerFactory.makeSuperwallOptions() } returns
            SuperwallOptions().apply(
                options,
            )
        return TransactionManager(
            purchaseController = purchaseController,
            storeManager = _storeManager,
            activityProvider = activityProvider,
            subscriptionStatus = subscriptionStatus,
            track = { track(it) },
            dismiss = { i, e -> dismiss(i, e) },
            showAlert = { showAlert(it) },
            updateState = { cacheKey, update -> updateState(cacheKey, update) },
            eventsQueue = eventsQueue,
            factory = trManagerFactory,
            ioScope = IOScope(this.coroutineContext),
            storage = storage,
            entitlementsById = entitlementsById,
            showRestoreDialogForWeb = showRestoreDialogForWeb,
            refreshReceipt = {},
        )
    }

    @Test
    fun test_purchase_internal_product_not_found() =
        runTest(timeout = 5.minutes) {
            Given("We try to purchase a product that does not exist") {
                val transactionManager: TransactionManager = manager()
                When("We try to purchase a product from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.Internal(
                                "product1",
                                paywallView.state,
                            ),
                        )
                    Then("The purchase fails") {
                        assert(result is PurchaseResult.Failed && result.errorMessage == "Product not found")
                    }
                }
            }
        }

    @Test
    fun test_purchase_activity_not_found() =
        runTest(timeout = 5.minutes) {
            Given("We have loaded products but no activity") {
                storeManager.getProducts(paywall = mockedPaywall)
                every { activityProvider.getCurrentActivity() } returns null
                val transactionManager: TransactionManager = manager()
                When("We try to purchase a product from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.Internal(
                                "product1",
                                paywallView.state,
                            ),
                        )
                    Then("The purchase fails") {
                        assert(
                            result is PurchaseResult.Failed &&
                                result.errorMessage == "Activity not found - required for starting the billing flow",
                        )
                    }
                }
            }
        }

    @Test
    fun test_purchase_successful_internal() =
        runTest(timeout = 5.minutes) {
            val spy = createBillingWrapper()
            val purchase = PurchaseMockBuilder.createDefaultPurchase("product1")
            every { transactionManagerFactory.makeTransactionVerifier() } returns spy
            coEvery { transactionManagerFactory.makeStoreTransaction(any()) } returns
                StoreTransaction(
                    GoogleBillingPurchaseTransaction(
                        transaction = purchase,
                    ),
                    configRequestId = "123",
                    appSessionId = "1234",
                )

            val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
            Given("We have loaded products and we can purchase successfully") {
                // Pretend a paywall loaded a product
                storeManager.getProducts(paywall = mockedPaywall)
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update {
                            it + e
                        }
                    })
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        "basePlan",
                        any(),
                    )
                } returns PurchaseResult.Purchased()

                When("We try to purchase a product from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.Internal(
                                "product1",
                                paywallView.state,
                            ),
                        )
                    Then("The purchase is successful") {
                        assert(result is PurchaseResult.Purchased)
                        coVerify { storeManager.loadPurchasedProducts() }
                        And("Verify event order") {
                            val transactionEvents =
                                events.value.filterIsInstance<InternalSuperwallEvent.Transaction>()

                            assert(
                                transactionEvents.first().superwallPlacement
                                    is SuperwallEvent.TransactionStart,
                            )
                            assert(transactionEvents.first().product?.fullIdentifier == "product1")
                            assert(
                                transactionEvents.last().superwallPlacement
                                    is SuperwallEvent.TransactionComplete,
                            )
                            assert(
                                (
                                    transactionEvents.last().superwallPlacement
                                        as SuperwallEvent.TransactionComplete
                                ).transaction!!
                                    .originalTransactionIdentifier != null,
                            )
                            val purchase =
                                events.value.filterIsInstance<InternalSuperwallEvent.NonRecurringProductPurchase>()
                            assert(purchase.first().product?.fullIdentifier == "product1")
                        }
                    }
                }
            }
        }

    private suspend fun TestScope.createBillingWrapper(
        withPurchase: Purchase = PurchaseMockBuilder.createDefaultPurchase("product1"),
        options: SuperwallOptions =
            SuperwallOptions().apply
                {
                    shouldObservePurchases = false
                },
    ): GoogleBillingWrapper {
        val mockLifecycle = AppLifecycleObserver()
        val lc = TestLifecycleOwner()
        lc.setCurrentState(Lifecycle.State.STARTED)
        mockLifecycle.onStart(lc)
        val bilingFactory =
            object : GoogleBillingWrapper.Factory {
                override fun makeHasExternalPurchaseController() = true

                override fun makeHasInternalPurchaseController() = false

                override fun makeSuperwallOptions(): SuperwallOptions = options
            }
        val billingWrapper =
            GoogleBillingWrapper(
                InstrumentationRegistry.getInstrumentation().context,
                IOScope(this@createBillingWrapper.coroutineContext),
                mockLifecycle,
                bilingFactory,
            )
        val spy =
            spyk(billingWrapper) {
                every { purchaseResults } returns
                    MutableStateFlow(
                        InternalPurchaseResult.Purchased(
                            withPurchase,
                        ),
                    )
            }
        return spy
    }

    @Test
    fun test_purchase_successful_external() =
        runTest(timeout = 5.minutes) {
            Given("We have loaded products and we can purchase successfully externally") {
                val spy = createBillingWrapper()
                val purchase = PurchaseMockBuilder.createDefaultPurchase("product1")
                every { transactionManagerFactory.makeTransactionVerifier() } returns spy
                coEvery { transactionManagerFactory.makeStoreTransaction(any()) } returns
                    StoreTransaction(
                        GoogleBillingPurchaseTransaction(
                            transaction = purchase,
                        ),
                        configRequestId = "123",
                        appSessionId = "1234",
                    )

                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    }, options = {
                        paywalls.automaticallyDismiss = true
                    })
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        "basePlan",
                        any(),
                    )
                } returns PurchaseResult.Purchased()

                When("We try to purchase a product externally") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.ExternalPurchase(
                                StoreProduct(mockProduct),
                            ),
                        )
                    Then("The purchase is successful") {
                        assert(result is PurchaseResult.Purchased)
                        coVerify { storeManager.loadPurchasedProducts() }
                        And("Verify event order") {
                            val transactionEvents =
                                events.value.filterIsInstance<InternalSuperwallEvent.Transaction>()
                            assert(transactionEvents.first().superwallPlacement is SuperwallEvent.TransactionStart)
                            val complete = transactionEvents.last().superwallPlacement
                            assert(complete is SuperwallEvent.TransactionComplete)
                            assert((complete as SuperwallEvent.TransactionComplete).transaction!!.originalTransactionIdentifier != null)
                            val purchase =
                                events.value.filterIsInstance<InternalSuperwallEvent.NonRecurringProductPurchase>()
                            assert(purchase.first().product?.fullIdentifier == "product1")
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_restored_internal() =
        runTest(timeout = 5.minutes) {
            Given("We have loaded products and a purchase results in restoration") {
                storeManager.getProducts(paywall = mockedPaywall)
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(
                        track = { e ->
                            events.update { it + e }
                        },
                        options = {
                            paywalls.automaticallyDismiss = true
                        },
                        dismiss = { paywallId, res ->
                            assert(paywallId == paywallView.state.cacheKey)
                            assert(res is PaywallResult.Restored)
                        },
                    )
                coEvery {
                    purchaseController.restorePurchases()
                } returns RestorationResult.Restored()

                When("We try to restore a product from the paywall") {
                    val result =
                        transactionManager.tryToRestorePurchases(
                            paywallView,
                        )
                    Then("The restore results in restoration") {
                        assert(result is RestorationResult.Restored)
                        And("Verify restoration event") {
                            val restorationEvent =
                                events.value
                                    .filterIsInstance<InternalSuperwallEvent.Transaction>()
                                    .find { it.state is InternalSuperwallEvent.Transaction.State.Restore }
                            assert(restorationEvent != null)
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_restored_external() =
        runTest(timeout = 5.minutes) {
            Given("We want to restore a product externally") {
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    })
                coEvery {
                    purchaseController.restorePurchases()
                } returns RestorationResult.Restored()

                When("We try to restore a product from the paywall") {
                    val result = transactionManager.tryToRestorePurchases(null)
                    Then("The purchase results in restoration") {
                        assert(result is RestorationResult.Restored)
                        And("Verify restoration event") {
                            advanceUntilIdle()
                            val restorationEvent =
                                events.value
                                    .filterIsInstance<InternalSuperwallEvent.Restore>()
                                    .find { it.state is InternalSuperwallEvent.Restore.State.Complete }
                            assert(restorationEvent != null)
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_failed_with_alert() =
        runTest(timeout = 5.minutes) {
            Given("We have loaded products and a purchase fails") {
                storeManager.getProducts(paywall = mockedPaywall)
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    }, options = {
                        paywalls.shouldShowPurchaseFailureAlert = true
                    })
                coEvery { transactionManagerFactory.makeTriggers() } returns
                    events.value
                        .map { it.rawName }
                        .toSet()
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        "basePlan",
                        any(),
                    )
                } returns PurchaseResult.Failed("Test failure")

                When("We try to purchase a product from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.Internal(
                                "product1",
                                paywallView.state,
                            ),
                        )
                    Then("The purchase fails and an alert is shown") {
                        assert(result is PurchaseResult.Failed)
                        coVerify {
                            mockShowAlert.invoke(
                                any(),
                            )
                        }
                        advanceUntilIdle()
                        And("Verify failure event") {
                            val failureEvent =
                                events.value
                                    .filterIsInstance<InternalSuperwallEvent.Transaction>()
                                    .find { it.state is InternalSuperwallEvent.Transaction.State.Fail }
                            assert(failureEvent != null)
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_failed_without_alert() =
        runTest(timeout = 5.minutes) {
            Given("We have loaded products and a purchase fails") {
                storeManager.getProducts(paywall = mockedPaywall)
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    }, options = {
                        paywalls.shouldShowPurchaseFailureAlert = false
                    })
                coEvery { transactionManagerFactory.makeTriggers() } returns
                    events.value
                        .map { it.rawName }
                        .toSet()
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        "basePlan",
                        any(),
                    )
                } returns PurchaseResult.Failed("Test failure")

                When("We try to purchase a product from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.Internal(
                                "product1",
                                paywallView.state,
                            ),
                        )
                    Then("The purchase fails and no alert is shown") {
                        assert(result is PurchaseResult.Failed)
                        coVerify(exactly = 0) {
                            mockShowAlert.invoke(
                                any(),
                            )
                        }
                        And("Verify failure event") {
                            advanceUntilIdle()
                            val failureEvent =
                                events.value
                                    .filterIsInstance<InternalSuperwallEvent.Transaction>()
                                    .find { it.state is InternalSuperwallEvent.Transaction.State.Fail }
                            assert(failureEvent != null)
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_pending() =
        runTest(timeout = 5.minutes) {
            Given("We have loaded products and a purchase is pending") {
                storeManager.getProducts(paywall = mockedPaywall)
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    })
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        "basePlan",
                        any(),
                    )
                } returns PurchaseResult.Pending()

                When("We try to purchase a product from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.Internal(
                                "product1",
                                paywallView.state,
                            ),
                        )
                    Then("The purchase is pending") {
                        assert(result is PurchaseResult.Pending)
                        coVerify { mockShowAlert.invoke(any()) }
                        And("Verify pending event") {
                            val pendingEvent =
                                events.value
                                    .filterIsInstance<InternalSuperwallEvent.Transaction>()
                                    .find {
                                        it.state is InternalSuperwallEvent.Transaction.State.Fail &&
                                            (it.state as InternalSuperwallEvent.Transaction.State.Fail).error is TransactionError.Pending
                                    }
                            assert(pendingEvent != null)
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_cancelled_internal() =
        runTest(timeout = 5.minutes) {
            Given("We have loaded products and a purchase is pending") {
                storeManager.getProducts(paywall = mockedPaywall)
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    })
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        "basePlan",
                        any(),
                    )
                } returns PurchaseResult.Cancelled()

                When("We try to purchase a product from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.Internal(
                                "product1",
                                paywallView.state,
                            ),
                        )
                    Then("The purchase is pending") {
                        assert(result is PurchaseResult.Cancelled)
                        verify {
                            mockUpdateState.invoke(
                                any(),
                                any<PaywallViewState.Updates.SetLoadingState>(),
                            )
                        }
                        And("Verify pending event") {
                            val pendingEvent =
                                events.value
                                    .filterIsInstance<InternalSuperwallEvent.Transaction>()
                                    .find { it.state is InternalSuperwallEvent.Transaction.State.Abandon }
                            assert(pendingEvent != null)
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_cancelled_external() =
        runTest(timeout = 5.minutes) {
            Given("An external purchase was cancelled") {
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    })
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns PurchaseResult.Cancelled()

                When("We try to purchase a product from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.ExternalPurchase(
                                StoreProduct(mockProduct),
                            ),
                        )
                    Then("The purchase is cancelled") {
                        assert(result is PurchaseResult.Cancelled)
                        verify(exactly = 0) {
                            mockUpdateState.invoke(
                                any(),
                                any<PaywallViewState.Updates.SetLoadingState>(),
                            )
                        }
                        And("Verify pending event") {
                            val pendingEvent =
                                events.value
                                    .filterIsInstance<InternalSuperwallEvent.Transaction>()
                                    .find { it.state is InternalSuperwallEvent.Transaction.State.Abandon }
                            assert(pendingEvent != null)
                        }
                    }
                }
            }
        }

    @Test
    fun test_try_to_restore_purchases_success() =
        runTest(timeout = 5.minutes) {
            Given("We can successfully restore purchases from a paywall") {
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(
                        track = { e ->
                            events.update { it + e }
                        },
                        subscriptionStatus = { SubscriptionStatus.Active(entitlements) },
                    )
                coEvery { purchaseController.restorePurchases() } returns RestorationResult.Restored()

                When("We try to restore purchases") {
                    val result = transactionManager.tryToRestorePurchases(paywallView)
                    Then("The restoration is successful") {
                        assert(result is RestorationResult.Restored)
                        And("Verify restoration events") {
                            val restoreEvents =
                                events.value.filterIsInstance<InternalSuperwallEvent.Restore>()
                            assert(restoreEvents.size == 2)
                            assert(restoreEvents[0].state is InternalSuperwallEvent.Restore.State.Start)
                            assert(restoreEvents[1].state is InternalSuperwallEvent.Restore.State.Complete)
                        }
                    }
                }
            }
        }

    @Test
    fun test_try_to_restore_purchases_success_externally() =
        runTest(timeout = 5.minutes) {
            Given("We can successfully restore purchases without a paywall") {
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(
                        track = { e ->
                            events.update { it + e }
                        },
                        subscriptionStatus = { SubscriptionStatus.Active(entitlements) },
                    )
                coEvery { purchaseController.restorePurchases() } returns RestorationResult.Restored()

                When("We try to restore purchases") {
                    val result = transactionManager.tryToRestorePurchases(null)
                    Then("The restoration is successful") {
                        assert(result is RestorationResult.Restored)
                        And("Verify restoration events") {
                            val restoreEvents =
                                events.value.filterIsInstance<InternalSuperwallEvent.Restore>()
                            assert(restoreEvents.size == 2)
                            assert(restoreEvents[0].state is InternalSuperwallEvent.Restore.State.Start)
                            assert(restoreEvents[1].state is InternalSuperwallEvent.Restore.State.Complete)
                        }
                    }
                }
            }
        }

    @Test
    fun test_try_to_restore_purchases_failure() =
        runTest(timeout = 5.minutes) {
            Given("Restoration of purchases fails") {
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(
                        track = { e ->
                            events.update { it + e }
                        },
                        subscriptionStatus = { SubscriptionStatus.Inactive },
                    )

                coEvery { purchaseController.restorePurchases() } returns
                    RestorationResult.Failed(
                        Exception("Test failure"),
                    )

                When("We try to restore purchases") {
                    val result = transactionManager.tryToRestorePurchases(paywallView)
                    Then("The restoration fails") {
                        assert(result is RestorationResult.Failed)
                        coVerify {
                            mockShowAlert.invoke(
                                any(),
                            )
                        }
                        And("Verify restoration events") {
                            val restoreEvents =
                                events.value.filterIsInstance<InternalSuperwallEvent.Restore>()
                            assert(restoreEvents.size == 2)
                            assert(restoreEvents[0].state is InternalSuperwallEvent.Restore.State.Start)
                            assert(restoreEvents[1].state is InternalSuperwallEvent.Restore.State.Failure)
                        }
                    }
                }
            }
        }

    @Test
    fun test_try_to_restore_purchases_restored_but_inactive() =
        runTest(timeout = 5.minutes) {
            Given("Restoration of purchases fails") {
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(
                        track = { e ->
                            events.update { it + e }
                        },
                        subscriptionStatus = { SubscriptionStatus.Inactive },
                    )

                coEvery { purchaseController.restorePurchases() } returns RestorationResult.Restored()

                When("We try to restore purchases") {
                    val result = transactionManager.tryToRestorePurchases(paywallView)
                    Then("The restoration fails because subscription is inactive") {
                        assert(result is RestorationResult.Restored)
                        coVerify {
                            mockShowAlert.invoke(
                                any(),
                            )
                        }
                        And("Verify restoration events") {
                            val restoreEvents =
                                events.value.filterIsInstance<InternalSuperwallEvent.Restore>()
                            assert(restoreEvents.size == 2)
                            assert(restoreEvents[0].state is InternalSuperwallEvent.Restore.State.Start)
                            val failure: InternalSuperwallEvent.Restore.State.Failure =
                                restoreEvents[1].state as InternalSuperwallEvent.Restore.State.Failure
                            assert(failure.reason.contains("\"inactive\""))
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_with_free_trial_external() =
        runTest(timeout = 5.minutes) {
            Given("We can purchase a product with a free trial externally") {
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    })
                every { playProduct.oneTimePurchaseOfferDetails } returns null
                every { playProduct.subscriptionOfferDetails } returns
                    listOf(
                        mockSubscriptionOfferDetails(
                            basePlanId = "basePlan",
                            pricingPhases = listOf(mockPricingPhase()),
                        ),
                        mockSubscriptionOfferDetails(
                            offerId = "offer1",
                            basePlanId = "basePlan",
                            pricingPhases = listOf(mockPricingPhase(0.0), mockPricingPhase()),
                        ),
                    )
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        "basePlan",
                        any(),
                    )
                } returns PurchaseResult.Purchased()

                When("We try to purchase the product") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.ExternalPurchase(
                                StoreProduct(mockProduct),
                            ),
                        )
                    Then("The purchase is successful") {
                        assert(result is PurchaseResult.Purchased)
                        coVerify { storeManager.loadPurchasedProducts() }
                        And("Verify free trial start event") {
                            val freeTrialStartEvent =
                                events.value.filterIsInstance<InternalSuperwallEvent.FreeTrialStart>()
                            assert(freeTrialStartEvent.isNotEmpty())
                            assert(freeTrialStartEvent.first().product?.fullIdentifier == "product1")
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_non_recurring_product_internal() =
        runTest(timeout = 5.minutes) {
            Given("We have loaded a non-recurring product and we can purchase successfully") {
                storeManager.getProducts(paywall = mockedPaywall)
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    })
                every { playProduct.oneTimePurchaseOfferDetails } returns null
                every { playProduct.subscriptionOfferDetails } returns null
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns PurchaseResult.Purchased()

                When("We try to purchase a non-recurring product from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.Internal(
                                "product1",
                                paywallView.state,
                            ),
                        )
                    Then("The purchase is successful") {
                        assert(result is PurchaseResult.Purchased)
                        coVerify { storeManager.loadPurchasedProducts() }
                        And("Verify non-recurring product purchase event") {
                            val nonRecurringPurchaseEvent =
                                events.value.filterIsInstance<InternalSuperwallEvent.NonRecurringProductPurchase>()
                            assert(nonRecurringPurchaseEvent.isNotEmpty())
                            assert(nonRecurringPurchaseEvent.first().product?.fullIdentifier == "product1")
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_non_recurring_product_external() =
        runTest(timeout = 5.minutes) {
            Given("We can purchase a non-recurring product externally") {
                every { playProduct.oneTimePurchaseOfferDetails } returns null
                every { playProduct.subscriptionOfferDetails } returns null

                val nonRecurringStoreProduct = StoreProduct(mockProduct)

                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    })
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns PurchaseResult.Purchased()

                When("We try to purchase the product") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.ExternalPurchase(
                                nonRecurringStoreProduct,
                            ),
                        )
                    Then("The purchase is successful") {
                        assert(result is PurchaseResult.Purchased)
                        coVerify { storeManager.loadPurchasedProducts() }
                        And("Verify non-recurring product purchase event") {
                            val nonRecurringPurchaseEvent =
                                events.value.filterIsInstance<InternalSuperwallEvent.NonRecurringProductPurchase>()
                            assert(nonRecurringPurchaseEvent.isNotEmpty())
                            assert(nonRecurringPurchaseEvent.first().product?.fullIdentifier == "product1")
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_subscription_without_trial_internal() =
        runTest(timeout = 5.minutes) {
            Given("We have loaded a subscription product without trial and we can purchase successfully") {
                storeManager.getProducts(paywall = mockedPaywall)
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    })
                every { playProduct.oneTimePurchaseOfferDetails } returns null
                every { playProduct.subscriptionOfferDetails } returns
                    listOf(
                        mockSubscriptionOfferDetails(
                            basePlanId = "basePlan",
                            pricingPhases = listOf(mockPricingPhase()),
                        ),
                    )
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        "basePlan",
                        any(),
                    )
                } returns PurchaseResult.Purchased()

                When("We try to purchase a subscription without trial from the paywall") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.Internal(
                                "product1",
                                paywallView.state,
                            ),
                        )
                    Then("The purchase is successful") {
                        assert(result is PurchaseResult.Purchased)
                        coVerify { storeManager.loadPurchasedProducts() }
                        And("Verify subscription start event") {
                            val subscriptionStartEvent =
                                events.value.filterIsInstance<InternalSuperwallEvent.SubscriptionStart>()
                            assert(subscriptionStartEvent.isNotEmpty())
                            assert(subscriptionStartEvent.first().product?.fullIdentifier == "product1")
                        }
                    }
                }
            }
        }

    @Test
    fun test_purchase_subscription_without_trial_external() =
        runTest(timeout = 5.minutes) {
            Given("We can purchase a subscription product without trial externally") {
                val events = MutableStateFlow(emptyList<TrackableSuperwallEvent>())
                val transactionManager: TransactionManager =
                    manager(track = { e ->
                        events.update { it + e }
                    })
                every { playProduct.oneTimePurchaseOfferDetails } returns null
                every { playProduct.subscriptionOfferDetails } returns
                    listOf(
                        mockSubscriptionOfferDetails(
                            basePlanId = "basePlan",
                            pricingPhases = listOf(mockPricingPhase()),
                        ),
                    )
                coEvery {
                    purchaseController.purchase(
                        any(),
                        any(),
                        "basePlan",
                        any(),
                    )
                } returns PurchaseResult.Purchased()

                When("We try to purchase a subscription without trial externally") {
                    val result =
                        transactionManager.purchase(
                            TransactionManager.PurchaseSource.ExternalPurchase(
                                StoreProduct(mockProduct),
                            ),
                        )
                    Then("The purchase is successful") {
                        assert(result is PurchaseResult.Purchased)
                        coVerify { storeManager.loadPurchasedProducts() }
                        And("Verify subscription start event") {
                            val subscriptionStartEvent =
                                events.value.filterIsInstance<InternalSuperwallEvent.SubscriptionStart>()
                            assert(subscriptionStartEvent.isNotEmpty())
                            assert(subscriptionStartEvent.first().product?.fullIdentifier == "product1")
                        }
                    }
                }
            }
        }
}
