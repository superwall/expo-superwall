package com.superwall.sdk.store.testmode

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.storage.IsTestModeActiveSubscription
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.StoredTestModeSettings
import com.superwall.sdk.storage.TestModeSettings
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.testmode.models.SuperwallEntitlementRef
import com.superwall.sdk.store.testmode.models.SuperwallProduct
import com.superwall.sdk.store.testmode.models.TestStoreUserType
import com.superwall.sdk.store.testmode.ui.EntitlementSelection
import com.superwall.sdk.store.testmode.ui.EntitlementStateOption

class TestModeManager(
    private val storage: Storage,
    private val isTestEnvironment: Boolean = Companion.isTestEnvironment,
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
}
