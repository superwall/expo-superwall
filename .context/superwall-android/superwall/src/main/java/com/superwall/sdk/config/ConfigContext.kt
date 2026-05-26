package com.superwall.sdk.config

import android.content.Context
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.identity.IdentityManager
import com.superwall.sdk.misc.primitives.BaseContext
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.network.SuperwallAPI
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.store.Entitlements
import com.superwall.sdk.store.StoreManager
import com.superwall.sdk.store.testmode.TestMode
import com.superwall.sdk.web.WebPaywallRedeemer

interface ConfigContext : BaseContext<ConfigState, ConfigContext> {
    val context: Context
    val storeManager: StoreManager
    val entitlements: Entitlements
    val network: SuperwallAPI
    val deviceHelper: DeviceHelper
    val options: SuperwallOptions
    val paywallManager: PaywallManager
    val webPaywallRedeemer: () -> WebPaywallRedeemer
    val factory: ConfigManager.Factory
    val assignments: Assignments
    val paywallPreload: PaywallPreload
    val testMode: TestMode?
    val identityManager: (() -> IdentityManager)?
    val setSubscriptionStatus: ((SubscriptionStatus) -> Unit)?
    val awaitUtilNetwork: suspend () -> Unit
    val activateTestMode: suspend (config: Config, justActivated: Boolean) -> Unit

    fun setTriggers(triggers: Map<String, Trigger>)
}
