package com.superwall.sdk.store.testmode

import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.testmode.models.SuperwallProduct
import com.superwall.sdk.store.testmode.ui.EntitlementSelection

sealed class TestModeState {
    data object Inactive : TestModeState()

    data class Active(
        val reason: TestModeReason,
        val session: TestModeSessionData = TestModeSessionData(),
    ) : TestModeState()
}

class TestModeSessionData {
    var products: List<SuperwallProduct> = emptyList()
    var testProductsByFullId: Map<String, StoreProduct> = emptyMap()
    var entitlementIds: MutableSet<String> = mutableSetOf()
    var entitlementSelections: List<EntitlementSelection> = emptyList()
    var freeTrialOverride: FreeTrialOverride = FreeTrialOverride.UseDefault
    var overriddenSubscriptionStatus: SubscriptionStatus? = null
}
