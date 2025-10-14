package com.superwall.sdk.dependencies

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.TrackingResult
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.billing.GoogleBillingWrapper
import com.superwall.sdk.config.ConfigManager
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.debug.DebugView
import com.superwall.sdk.identity.IdentityInfo
import com.superwall.sdk.identity.IdentityManager
import com.superwall.sdk.misc.AppLifecycleObserver
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.MainScope
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.config.FeatureFlags
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.network.Api
import com.superwall.sdk.network.JsonFactory
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.network.device.DeviceInfo
import com.superwall.sdk.paywall.manager.PaywallViewCache
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.request.PaywallRequest
import com.superwall.sdk.paywall.request.ResponseIdentifiers
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.ViewStorage
import com.superwall.sdk.paywall.view.delegate.PaywallViewDelegateAdapter
import com.superwall.sdk.paywall.view.webview.templating.models.JsonVariables
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.storage.core_data.CoreDataManager
import com.superwall.sdk.store.abstractions.transactions.StoreTransaction
import kotlinx.coroutines.flow.StateFlow

interface ApiFactory : JsonFactory {
    // TODO: Think of an alternative way such that we don't need to do this:
    var api: Api
    var storage: LocalStorage

    //    var storage: Storage! { get }
    var deviceHelper: DeviceHelper
    var configManager: ConfigManager
    var identityManager: IdentityManager
    var appLifecycleObserver: AppLifecycleObserver

    suspend fun track(event: Trackable) {
        Superwall.instance.track(event)
    }

    suspend fun makeHeaders(
        isForDebugging: Boolean,
        requestId: String,
    ): Map<String, String>
}

interface DeviceInfoFactory {
    fun makeDeviceInfo(): DeviceInfo
}

interface FeatureFlagsFactory {
    fun makeFeatureFlags(): FeatureFlags?
}

interface ComputedPropertyRequestsFactory {
    fun makeComputedPropertyRequests(): List<ComputedPropertyRequest>
}

// RequestFactory interface
interface RequestFactory {
    fun makePaywallRequest(
        eventData: EventData?,
        responseIdentifiers: ResponseIdentifiers,
        overrides: PaywallRequest.Overrides?,
        isDebuggerLaunched: Boolean,
        presentationSourceType: String?,
    ): PaywallRequest

    fun makePresentationRequest(
        presentationInfo: PresentationInfo,
        paywallOverrides: PaywallOverrides? = null,
        presenter: Activity? = null,
        isDebuggerLaunched: Boolean? = null,
        subscriptionStatus: StateFlow<SubscriptionStatus?>? = null,
        isPaywallPresented: Boolean,
        type: PresentationRequestType,
    ): PresentationRequest
}

interface RuleAttributesFactory {
    suspend fun makeRuleAttributes(
        event: EventData?,
        computedPropertyRequests: List<ComputedPropertyRequest>,
    ): Map<String, Any>
}

interface IdentityInfoFactory {
    suspend fun makeIdentityInfo(): IdentityInfo
}

interface IdentityManagerFactory {
    suspend fun makeIdentityManager(): IdentityManager
}

interface LocaleIdentifierFactory {
    fun makeLocaleIdentifier(): String?
}

interface TransactionVerifierFactory {
    fun makeTransactionVerifier(): GoogleBillingWrapper
}

interface DeviceHelperFactory {
    fun makeDeviceInfo(): DeviceInfo

    fun makeIsSandbox(): Boolean

    suspend fun makeSessionDeviceAttributes(): HashMap<String, Any>
}

interface EnrichmentFactory {
    fun demandTier(): String?

    fun demandScore(): Int?
}

fun interface TrackingFactory {
    suspend fun track(event: TrackableSuperwallEvent): Result<TrackingResult>
}

interface ConfigAttributesFactory {
    fun makeConfigAttributes(): InternalSuperwallEvent.ConfigAttributes
}

interface UserAttributesEventFactory {
    fun makeUserAttributesEvent(): InternalSuperwallEvent.Attributes
}

interface HasExternalPurchaseControllerFactory {
    fun makeHasExternalPurchaseController(): Boolean
}

interface HasInternalPurchaseControllerFactory {
    fun makeHasInternalPurchaseController(): Boolean
}

interface WebToAppFactory {
    fun isWebToAppEnabled(): Boolean

    fun restoreUrl(): String
}

interface ViewFactory {
    // NOTE: THIS MUST BE EXECUTED ON THE MAIN THREAD (no way to enforce in Kotlin)
    suspend fun makePaywallView(
        paywall: Paywall,
        cache: PaywallViewCache?,
        delegate: PaywallViewDelegateAdapter?,
    ): PaywallView

    fun makeDebugView(id: String?): DebugView
}

interface CacheFactory {
    fun makeCache(): PaywallViewCache
}

interface VariablesFactory {
    suspend fun makeJsonVariables(
        products: List<ProductVariable>?,
        computedPropertyRequests: List<ComputedPropertyRequest>,
        event: EventData?,
    ): JsonVariables
}

interface ConfigManagerFactory {
    fun makeStaticPaywall(
        paywallId: String?,
        isDebuggerLaunched: Boolean,
    ): Paywall?
}

// interface ProductPurchaserFactory {
//    fun makeSK1ProductPurchaser(): ProductPurchaserSK1
// }

interface StoreTransactionFactory {
    suspend fun makeStoreTransaction(transaction: Purchase): StoreTransaction

    suspend fun activeProductIds(): List<String>
}

interface ExperimentalPropertiesFactory {
    fun experimentalProperties(): Map<String, Any>
}

interface OptionsFactory {
    fun makeSuperwallOptions(): SuperwallOptions
}

interface AttributesFactory {
    fun getCurrentUserAttributes(): Map<String, Any>
}

interface TriggerFactory {
    suspend fun makeTriggers(): Set<String>
}

internal interface ViewStoreFactory {
    fun makeViewStore(): ViewStorage
}

interface SuperwallScopeFactory {
    fun mainScope(): MainScope

    fun ioScope(): IOScope
}

interface CoreDataManagerFactory {
    fun makeCoreDataManager(): CoreDataManager
}
