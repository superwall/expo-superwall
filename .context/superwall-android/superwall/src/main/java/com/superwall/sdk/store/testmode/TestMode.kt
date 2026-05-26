package com.superwall.sdk.store.testmode

import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent.TestModeModal.State
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.ActivityProvider
import com.superwall.sdk.misc.CurrentActivityTracker
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.fold
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.storage.IsTestModeActiveSubscription
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.StoredTestModeSettings
import com.superwall.sdk.storage.TestModeSettings
import com.superwall.sdk.store.Entitlements
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.testmode.models.SuperwallEntitlementRef
import com.superwall.sdk.store.testmode.models.SuperwallProduct
import com.superwall.sdk.store.testmode.models.SuperwallProductPlatform
import com.superwall.sdk.store.testmode.models.SuperwallProductsResponse
import com.superwall.sdk.store.testmode.models.TestStoreUserType
import com.superwall.sdk.store.testmode.ui.EntitlementSelection
import com.superwall.sdk.store.testmode.ui.EntitlementStateOption
import com.superwall.sdk.store.testmode.ui.TestModeModal
import kotlin.time.Duration.Companion.seconds

/**
 * The single test-mode surface: holds the activation state (products,
 * entitlement selections, settings persistence) AND runs the activation UI
 * flow (`activate` → refresh products → present modal).
 *
 * Not exactly a "manager" — the UI flow pieces (activity lookup, subscription
 * products fetch, modal presentation) are injected as thin lambdas so this
 * class stays testable and config-slice-free.
 */
class TestMode(
    private val storage: Storage,
    private val isTestEnvironment: Boolean = Companion.isTestEnvironment,
    // Activation UI hooks — all default to no-ops so unit tests exercising
    // state management can construct `TestMode(storage)` without wiring the
    // whole UI/network surface.
    private val getSuperwallProducts: suspend () -> Either<SuperwallProductsResponse, NetworkError> = {
        Either.Failure(NetworkError.Unknown())
    },
    private val entitlements: Entitlements? = null,
    private val activityProvider: () -> ActivityProvider? = { null },
    private val activityTracker: () -> CurrentActivityTracker? = { null },
    private val hasExternalPurchaseController: () -> Boolean = { false },
    private val apiKey: () -> String = { "" },
    private val dashboardBaseUrl: () -> String = { "" },
    private val track: suspend (InternalSuperwallEvent) -> Unit = { },
) {
    companion object {
        val isTestEnvironment: Boolean by lazy {
            try {
                runCatching {
                    Class.forName("org.junit.Test")
                    true
                }.getOrNull() ?: false ||
                    runCatching {
                        Class.forName("androidx.test.espresso.Espresso")
                        true
                    }.getOrNull() ?: false ||
                    runCatching {
                        Class.forName("android.support.test.espresso.Espresso")
                        true
                    }.getOrNull() ?: false
            } catch (_: ClassNotFoundException) {
                false
            }
        }
    }

    var state: TestModeState = TestModeState.Inactive
        private set

    // Convenience accessors
    val isTestMode: Boolean get() = state is TestModeState.Active
    val testModeReason: TestModeReason? get() = (state as? TestModeState.Active)?.reason
    private val session: TestModeSessionData? get() = (state as? TestModeState.Active)?.session

    // Backward-compatible session data accessors (return sensible defaults when inactive)
    val products: List<SuperwallProduct> get() = session?.products ?: emptyList()
    internal val testProductsByFullId: Map<String, StoreProduct> get() = session?.testProductsByFullId ?: emptyMap()
    val testEntitlementIds: Set<String> get() = session?.entitlementIds ?: emptySet()
    val testEntitlementSelections: List<EntitlementSelection> get() = session?.entitlementSelections ?: emptyList()
    val freeTrialOverride: FreeTrialOverride get() = session?.freeTrialOverride ?: FreeTrialOverride.UseDefault
    val overriddenSubscriptionStatus: SubscriptionStatus? get() = session?.overriddenSubscriptionStatus

    fun evaluateTestMode(
        config: Config,
        bundleId: String,
        appUserId: String?,
        aliasId: String?,
        testModeBehavior: TestModeBehavior = TestModeBehavior.AUTOMATIC,
    ) {
        when (testModeBehavior) {
            TestModeBehavior.NEVER -> {
                deactivateIfActive()
                return
            }

            TestModeBehavior.ALWAYS -> {
                state = TestModeState.Active(reason = TestModeReason.TestModeOption)
                Logger.debug(
                    LogLevel.info,
                    LogScope.superwallCore,
                    "Test mode activated: ${testModeReason?.description}",
                )
                return
            }

            TestModeBehavior.WHEN_ENABLED_FOR_USER -> {
                if (checkConfigMatch(config, appUserId, aliasId)) return
                deactivateIfActive()
                return
            }

            TestModeBehavior.AUTOMATIC -> {
                // Skip in test environments (JUnit on classpath)
                if (isTestEnvironment) {
                    deactivateIfActive()
                    return
                }
                if (checkConfigMatch(config, appUserId, aliasId)) return
                if (checkPackageNameMismatch(config, bundleId)) return
                deactivateIfActive()
            }
        }
    }

    private fun deactivateIfActive() {
        if (isTestMode) {
            clearTestModeState()
        }
    }

    private fun checkConfigMatch(
        config: Config,
        appUserId: String?,
        aliasId: String?,
    ): Boolean {
        val testUsers = config.testModeUserIds ?: return false
        for (testUser in testUsers) {
            val match =
                when (testUser.type) {
                    TestStoreUserType.UserId -> appUserId == testUser.value
                    TestStoreUserType.AliasId -> aliasId == testUser.value
                }
            if (match) {
                state = TestModeState.Active(reason = TestModeReason.ConfigMatch(matchedId = testUser.value))
                Logger.debug(
                    LogLevel.info,
                    LogScope.superwallCore,
                    "Test mode activated: ${testModeReason?.description}",
                )
                return true
            }
        }
        return false
    }

    private fun checkPackageNameMismatch(
        config: Config,
        actualPackageName: String,
    ): Boolean {
        val expectedPackageName = config.bundleIdConfig
        if (expectedPackageName.isNullOrEmpty()) return false
        if (expectedPackageName == actualPackageName) return false
        // Treat as extension if actual starts with expected + "."
        if (actualPackageName.startsWith("$expectedPackageName.")) return false

        state =
            TestModeState.Active(
                reason =
                    TestModeReason.ApplicationIdMismatch(
                        expected = expectedPackageName,
                        actual = actualPackageName,
                    ),
            )
        Logger.debug(
            LogLevel.info,
            LogScope.superwallCore,
            "Test mode activated: ${testModeReason?.description}",
        )
        return true
    }

    fun setProducts(products: List<SuperwallProduct>) {
        session?.products = products
    }

    fun setTestProducts(productsByFullId: Map<String, StoreProduct>) {
        session?.testProductsByFullId = productsByFullId
    }

    fun fakePurchase(entitlementRefs: List<SuperwallEntitlementRef>) {
        val ids = entitlementRefs.map { it.identifier }
        session?.entitlementIds?.addAll(ids)
        storage.write(IsTestModeActiveSubscription, testEntitlementIds.isNotEmpty())
    }

    fun setEntitlements(selections: List<EntitlementSelection>) {
        val s = session ?: return
        s.entitlementSelections = selections
        s.entitlementIds.clear()
        s.entitlementIds.addAll(
            selections.filter { it.state.isActive }.map { it.identifier },
        )
        storage.write(IsTestModeActiveSubscription, s.entitlementIds.isNotEmpty())
    }

    fun setEntitlements(ids: Set<String>) {
        setEntitlements(
            ids.map { EntitlementSelection(identifier = it, state = EntitlementStateOption.Subscribed) },
        )
    }

    fun resetEntitlements() {
        session?.entitlementIds?.clear()
        session?.entitlementSelections = emptyList()
        storage.write(IsTestModeActiveSubscription, false)
    }

    fun setFreeTrialOverride(override: FreeTrialOverride) {
        session?.freeTrialOverride = override
    }

    fun shouldShowFreeTrial(hasFreeTrial: Boolean): Boolean =
        when (freeTrialOverride) {
            FreeTrialOverride.UseDefault -> hasFreeTrial
            FreeTrialOverride.ForceAvailable -> true
            FreeTrialOverride.ForceUnavailable -> false
        }

    fun clearTestModeState() {
        state = TestModeState.Inactive
        storage.delete(IsTestModeActiveSubscription)
        clearSettings()
    }

    fun buildSubscriptionStatus(): SubscriptionStatus {
        if (testEntitlementIds.isEmpty()) {
            return SubscriptionStatus.Inactive
        }
        val activeSelections = testEntitlementSelections.filter { it.state.isActive }
        return if (activeSelections.isNotEmpty()) {
            SubscriptionStatus.Active(
                activeSelections.map { it.toEntitlement() }.toSet(),
            )
        } else {
            SubscriptionStatus.Active(
                testEntitlementIds.map { Entitlement(it) }.toSet(),
            )
        }
    }

    fun setOverriddenSubscriptionStatus(status: SubscriptionStatus?) {
        session?.overriddenSubscriptionStatus = status
    }

    fun entitlementsForProduct(product: SuperwallProduct): List<SuperwallEntitlementRef> = product.entitlements

    fun allEntitlements(): Set<String> = products.flatMap { it.entitlements.map { e -> e.identifier } }.toSet()

    fun saveSettings() {
        val settings =
            TestModeSettings(
                entitlementSelections = testEntitlementSelections,
                freeTrialOverride = freeTrialOverride,
            )
        storage.write(StoredTestModeSettings, settings)
    }

    fun loadSettings(): TestModeSettings? = storage.read(StoredTestModeSettings)

    fun clearSettings() {
        storage.delete(StoredTestModeSettings)
    }

    // ---- Activation UI flow ------------------------------------------------

    /**
     * Refresh the test product catalog and (when [justActivated] is true)
     * present the test-mode modal. Must be called off the actor queue —
     * [presentModal] blocks on user interaction.
     */
    suspend fun activate(
        config: Config,
        justActivated: Boolean,
    ) {
        refreshProducts()
        if (justActivated) {
            presentModal(config)
        }
    }

    private suspend fun refreshProducts() {
        getSuperwallProducts().fold(
            onSuccess = { response ->
                val androidProducts =
                    response.data.filter {
                        it.platform == SuperwallProductPlatform.ANDROID && it.price != null
                    }
                setProducts(androidProducts)

                val productsByFullId =
                    androidProducts.associate { superwallProduct ->
                        val testProduct = TestStoreProduct(superwallProduct)
                        superwallProduct.identifier to StoreProduct(testProduct)
                    }
                setTestProducts(productsByFullId)

                Logger.debug(
                    LogLevel.info,
                    LogScope.superwallCore,
                    "Test mode: loaded ${androidProducts.size} products",
                )
            },
            onFailure = { error ->
                Logger.debug(
                    LogLevel.error,
                    LogScope.superwallCore,
                    "Test mode: failed to fetch products - ${error.message}",
                )
            },
        )
    }

    private suspend fun presentModal(config: Config) {
        val activity =
            activityTracker()?.getCurrentActivity()
                ?: activityProvider()?.getCurrentActivity()
                ?: activityTracker()?.awaitActivity(10.seconds)
        if (activity == null) {
            Logger.debug(
                LogLevel.warn,
                LogScope.superwallCore,
                "Test mode modal could not be presented: no activity available. Setting default subscription status.",
            )
            val status = buildSubscriptionStatus()
            setOverriddenSubscriptionStatus(status)
            entitlements?.setSubscriptionStatus(status)
            return
        }

        track(InternalSuperwallEvent.TestModeModal(State.Open))

        val reason = testModeReason?.description ?: "Test mode activated"
        val allEntitlements =
            config.productsV3
                ?.flatMap { it.entitlements.map { e -> e.id } }
                ?.distinct()
                ?.sorted()
                ?: emptyList()

        val savedSettings = loadSettings()

        val result =
            TestModeModal.show(
                activity = activity,
                reason = reason,
                hasPurchaseController = hasExternalPurchaseController(),
                availableEntitlements = allEntitlements,
                apiKey = apiKey(),
                dashboardBaseUrl = dashboardBaseUrl(),
                savedSettings = savedSettings,
            )

        setFreeTrialOverride(result.freeTrialOverride)
        setEntitlements(result.entitlements)
        saveSettings()
        val status = buildSubscriptionStatus()
        setOverriddenSubscriptionStatus(status)
        entitlements?.setSubscriptionStatus(status)

        track(InternalSuperwallEvent.TestModeModal(State.Close))
    }
}
