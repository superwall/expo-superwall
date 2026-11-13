package expo.modules.superwallexpo

import android.content.pm.PackageManager
import android.net.Uri
import android.app.Application
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.Promise
import expo.modules.superwallexpo.bridges.CustomerCenterDelegateBridge
import expo.modules.superwallexpo.bridges.PurchaseControllerBridge
import expo.modules.superwallexpo.bridges.SuperwallDelegateBridge
import expo.modules.superwallexpo.json.*
import com.superwall.sdk.Superwall
import com.superwall.sdk.billing.BillingError
import com.superwall.sdk.models.attribution.AttributionProvider
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.delegate.SuperwallDelegate
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.identity.IdentityOptions
import com.superwall.sdk.identity.identify
import com.superwall.sdk.identity.setUserAttributes
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.config.options.EventTrackingBehavior
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.network.device.InterfaceStyle
import com.superwall.sdk.paywall.presentation.PaywallPresentationHandler
import com.superwall.sdk.paywall.presentation.CustomCallbackResult
import com.superwall.sdk.paywall.presentation.result.PresentationResult
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.paywall.presentation.register
import com.superwall.sdk.paywall.presentation.dismiss
import com.superwall.sdk.paywall.view.webview.PaywallResource
import com.superwall.sdk.paywall.presentation.get_presentation_result.getPresentationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.superwall.sdk.paywall.presentation.dismissForNextPaywall
import com.superwall.sdk.store.PurchasingObserverState
import com.superwall.sdk.web.openRestoreOnWeb
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import android.util.Log
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture

class SuperwallExpoModule : Module() {

  companion object {
    var instance: SuperwallExpoModule? = null
    private val instances = Collections.newSetFromMap(WeakHashMap<SuperwallExpoModule, Boolean>())

    // Events must reach every live module instance. More than one app context can
    // exist at once (e.g. expo-dev-client's launcher plus the app), and `instance`
    // may point at a module whose JS runtime never subscribed - sending only
    // through it silently drops events like `onPurchase`, leaving the native
    // purchase future incomplete forever.
    fun emitEvent(name: String, body: Map<String, Any?>?) {
      val liveInstances = synchronized(instances) { instances.toList() }
      liveInstances.forEach { it.trySendEvent(name, body ?: emptyMap()) }
    }
  }

  // The Superwall SDK can call back on this module after the Expo module registry
  // has been invalidated (e.g. CurrentActivityTracker logging during activity
  // destroy). `sendEvent` then throws from `AppContext.eventEmitter`; dropping the
  // undeliverable event is better than crashing the process mid-teardown.
  internal fun trySendEvent(name: String, body: Map<String, Any?>) {
    try {
      sendEvent(name, body)
    } catch (_: Exception) {
    }
  }

  val scope = CoroutineScope(Dispatchers.Main)
  val ioScope = CoroutineScope(Dispatchers.IO)
  var backPressedPromise: CompletableFuture<Boolean>? = null

  init {
    instance = this
    synchronized(instances) { instances.add(this) }
  }

  private val onPaywallPresent = "onPaywallPresent"
  private val onPaywallDismiss = "onPaywallDismiss"
  private val onPaywallError = "onPaywallError"
  private val onPaywallSkip = "onPaywallSkip"
  private val onCustomCallback = "onCustomCallback"

  val customCallbackFutures = java.util.concurrent.ConcurrentHashMap<String, CompletableFuture<CustomCallbackResult>>()

  // Purchase Events
  private val onPurchase = "onPurchase"
  private val onPurchaseRestore = "onPurchaseRestore"

  // Back Button Event
  private val onBackPressed = "onBackPressed"

  // Legacy Events
  private val subscriptionStatusDidChange = "subscriptionStatusDidChange"
  private val handleSuperwallEvent = "handleSuperwallEvent"
  private val handleCustomPaywallAction = "handleCustomPaywallAction"
  private val willDismissPaywall = "willDismissPaywall"
  private val willPresentPaywall = "willPresentPaywall"
  private val didDismissPaywall = "didDismissPaywall"
  private val didPresentPaywall = "didPresentPaywall"
  private val paywallWillOpenURL = "paywallWillOpenURL"
  private val paywallWillOpenDeepLink = "paywallWillOpenDeepLink"
  private val handleLog = "handleLog"
  private val willRedeemLink = "willRedeemLink"
  private val didRedeemLink = "didRedeemLink"
  private val userAttributesDidChange = "userAttributesDidChange"

  // Customer Info Events
  private val customerInfoDidChange = "customerInfoDidChange"

  // Configuration Events
  private val configurationStatusDidChange = "configurationStatusDidChange"

  // Collects `Superwall.instance.configurationStateListener` for the JS event.
  private var configurationStatusJob: Job? = null

  // How long `getCustomerInfo` waits for the SDK to load the customer info before it resolves
  // with whatever the SDK currently has.
  private val customerInfoTimeoutMillis = 5_000L

  // Customer Center Events
  private val onCustomerCenterAction = "onCustomerCenterAction"
  private val onCustomerCenterSurveyComplete = "onCustomerCenterSurveyComplete"
  private val onCustomerCenterRefundRequestComplete = "onCustomerCenterRefundRequestComplete"
  private val onCustomerCenterShouldRestorePurchases = "onCustomerCenterShouldRestorePurchases"

  // Whether a Customer Center presented through this module is on screen. Only touched on
  // the main thread.
  private var isCustomerCenterPresented = false

  val purchaseController = PurchaseControllerBridge.instance

  private var latestIntegrationAttributes: Map<AttributionProvider, String>? = null

  override fun definition() = ModuleDefinition {


    Name("SuperwallExpo")

    Events(
      onPaywallPresent,
      onPaywallDismiss,
      onPaywallError,
      onPaywallSkip,
      onCustomCallback,
      onPurchase,
      onPurchaseRestore,
      onBackPressed,

      // Legacy events
      subscriptionStatusDidChange,
      handleSuperwallEvent,
      handleCustomPaywallAction,
      willDismissPaywall,
      willPresentPaywall,
      didDismissPaywall,
      didPresentPaywall,
      paywallWillOpenURL,
      paywallWillOpenDeepLink,
      handleLog,
      willRedeemLink,
      didRedeemLink,
      userAttributesDidChange,

      // Customer info events
      customerInfoDidChange,

      // Configuration events
      configurationStatusDidChange,

      // Customer Center events
      onCustomerCenterAction,
      onCustomerCenterSurveyComplete,
      onCustomerCenterRefundRequestComplete,
      onCustomerCenterShouldRestorePurchases
    )

    OnDestroy {
      // Once this module's app context is torn down its event emitter is gone.
      // Deregister eagerly so `emitEvent` stops fanning out to it, rather than
      // waiting for the WeakHashMap entry to be garbage-collected.
      synchronized(instances) { instances.remove(this@SuperwallExpoModule) }
      configurationStatusJob?.cancel()
      if (instance === this@SuperwallExpoModule) {
        instance = null
      }
    }

    View(SuperwallExpoPaywallView::class) {
      Name("PaywallView")
      Events(
        "onPaywallLoadStart",
        "onPaywallPresent",
        "onPaywallDismiss",
        "onPaywallSkip",
        "onPaywallError",
      )
      Prop("placement") { view: SuperwallExpoPaywallView, placement: String ->
        view.setPlacement(placement)
      }
      Prop("params") { view: SuperwallExpoPaywallView, params: Map<String, Any>? ->
        view.setParams(params)
      }
      Prop("paywallOverrides") { view: SuperwallExpoPaywallView, overrides: Map<String, Any>? ->
        view.setPaywallOverrides(overrides)
      }
      OnViewDidUpdateProps { view: SuperwallExpoPaywallView ->
        view.loadPaywallIfNeeded()
      }
      OnViewDestroys { view: SuperwallExpoPaywallView ->
        view.destroy()
      }
    }

    AsyncFunction("identify") { userId: String, options: Map<String, Any>?, promise: Promise ->
      scope.launch {
        try {
          val identityOptions = options?.let { identityOptionsFromJson(it) }
          Superwall.instance.identify(userId = userId, options = identityOptions)
          promise.resolve(null)
        } catch (error: Exception) {
          promise.reject(CodedException(error))
        }
      }
    }

    AsyncFunction("reset") { promise: Promise ->
      scope.launch {
        try {
          Superwall.instance.reset()
          promise.resolve(null)
        } catch (error: Exception) {
          promise.reject(CodedException(error))
        }
      }
    }

    AsyncFunction("configure") {
      apiKey: String,
      options: Map<String, Any>?,
      usingPurchaseController: Boolean?,
      sdkVersion: String?,
      promise: Promise ->
      ioScope.launch {
        val promiseSettled = java.util.concurrent.atomic.AtomicBoolean(false)
        try{
        val superwallOptions: SuperwallOptions = options?.let {
          superwallOptionsFromJson(options, appContext.reactContext?.applicationContext)
        }?:SuperwallOptions()

        // Set up onBackPressed callback to emit event to JS and wait for response
        superwallOptions.paywalls.onBackPressed = { paywallInfo ->
          backPressedPromise = CompletableFuture()

          val data = paywallInfo?.let {
            mapOf("paywallInfo" to it.toJson())
          } ?: emptyMap()

          emitEvent("onBackPressed", data)

          // Block and wait for JS response via didHandleBackPressed
          runBlocking {
            backPressedPromise?.await() ?: false
          }
        }

        @Suppress("UNCHECKED_CAST")
        val localResourcesMap = options?.get("localResources") as? Map<String, String>

        Superwall.configure(
          apiKey = apiKey,
          applicationContext = appContext.reactContext?.applicationContext as Application,
          purchaseController = if (usingPurchaseController == true) purchaseController else null,
          activityProvider = ExpoActivityProvider(appContext),
          options = superwallOptions,
          completion = {
            Superwall.instance.setPlatformWrapper("Expo", version = sdkVersion ?: "0.0.0")
            Superwall.instance.delegate = SuperwallDelegateBridge()
            if (localResourcesMap != null) {
              Superwall.instance.localResources = localResourcesMap.mapValues { (_, value) ->
                PaywallResource.FromUri(Uri.parse(value))
              }
            }
            if (promiseSettled.compareAndSet(false, true)) {
              promise.resolve(true)
            }
           }
         )

        // `Superwall.instance` exists as soon as `configure` returns, before it has finished.
        // The flow sends the current status first.
        configurationStatusJob?.cancel()
        configurationStatusJob = scope.launch {
          Superwall.instance.configurationStateListener.distinctUntilChanged().collect { status ->
            emitEvent(configurationStatusDidChange, mapOf("status" to status.asString()))
          }
        }
        } catch (error: Throwable) {
          error.printStackTrace()
          if (promiseSettled.compareAndSet(false, true)) {
            scope.launch {
              promise.reject(CodedException(error))
            }
          }
        }
      }
    }

    AsyncFunction("getConfigurationStatus") { promise: Promise ->
      try {
        val configurationStatus = Superwall.instance.configurationState.asString()
          promise.resolve(configurationStatus)
        } catch (error: IllegalStateException) {
          // `Superwall.instance` throws until `configure` is called. Report pending, as iOS
          // does, since `getConfigurationStatus` doesn't wait for configure.
          promise.resolve("PENDING")
        } catch (error: Throwable) {
          promise.reject(CodedException(error))
        }
      }

    AsyncFunction("registerPlacement") {
      placement: String,
      params: Map<String, Any>?,
      handlerId: String?,
      promise: Promise ->



      var handler: PaywallPresentationHandler? = handlerId?.let {
        PaywallPresentationHandler()
      }



        handler?.onPresent { paywallInfo ->
          val data =
          mapOf(
            "paywallInfoJson" to paywallInfo.toJson(),
          "handlerId" to handlerId)
          trySendEvent(onPaywallPresent, data)
        }



        handler?.onDismiss { paywallInfo, result ->
          val data =
          mapOf(
            "paywallInfoJson" to paywallInfo.toJson(),
          "result" to result.toJson(),
          "handlerId" to handlerId)
          
          trySendEvent(onPaywallDismiss, data)
        }

        handler?.onError { error ->
          val data =
          mapOf(
            "errorString" to error.localizedMessage,
          "handlerId" to handlerId,
          )
          trySendEvent(onPaywallError, data)
        }

        handler?.onSkip { reason ->
          val data =
          mapOf(
            "skippedReason" to reason.toJson(),
             "handlerId" to handlerId)
          trySendEvent(onPaywallSkip, data)
        }

        handler?.onCustomCallback { callback ->
          val callbackId = java.util.UUID.randomUUID().toString()
          val future = CompletableFuture<CustomCallbackResult>()
          customCallbackFutures[callbackId] = future

          val data = mutableMapOf<String, Any?>(
            "callbackId" to callbackId,
            "name" to callback.name,
            "handlerId" to handlerId,
          )
          callback.variables?.let { data["variables"] = it }

          trySendEvent(onCustomCallback, data)

          future.await()
        }

      Superwall.instance.register(
        placement = placement,
        params = params,
        handler = handler
      ) {
        promise.resolve(null)
      }
    }

    AsyncFunction("getAssignments") { promise: Promise ->
        Superwall.instance.getAssignments()
        .fold({
          promise.resolve(it.map { it.toJson() })
        }, { error ->
          promise.reject(CodedException(error))
        })
      }

    AsyncFunction("getEntitlements") { promise: Promise ->
      try {
        val entitlements = Superwall.instance.entitlements
        val map = mutableMapOf<String, List<Map<String, Any>>>()

        map["all"] = entitlements.all.toJson()
        map["inactive"] = entitlements.inactive.toJson()
        map["active"] = entitlements.active.toJson()
        map["web"] = entitlements.web.toJson()
        promise.resolve(map)
      } catch (error: Exception) {
        promise.reject(CodedException(error))
      }
      }

    AsyncFunction("getEntitlementsByProductIds") { productIds: List<String>, promise: Promise ->
      try {
        promise.resolve(Superwall.instance.entitlements.byProductIds(productIds.toSet()).toJson())
      } catch (error: Exception) {
        promise.reject(CodedException(error))
      }
    }

    AsyncFunction("getCustomerInfo") { promise: Promise ->
      ioScope.launch {
        try {
          // Wait for the SDK to load the real customer info, but not forever: it never arrives
          // if the load fails, so fall back to what the SDK has, which may be the empty
          // placeholder.
          val customerInfo = withTimeoutOrNull(customerInfoTimeoutMillis) {
            Superwall.instance.customerInfo.first { it != CustomerInfo.empty() }
          } ?: Superwall.instance.getCustomerInfo()
          scope.launch { promise.resolve(customerInfo.toJson()) }
        } catch (error: Throwable) {
          scope.launch { promise.reject(CodedException(error)) }
        }
      }
    }

    // The Android SDK has no way to force a refresh, so this resolves with the customer info it
    // already has.
    AsyncFunction("refreshCustomerInfo") { promise: Promise ->
      try {
        promise.resolve(Superwall.instance.getCustomerInfo().toJson())
      } catch (error: Throwable) {
        promise.reject(CodedException(error))
      }
    }

    AsyncFunction("getSubscriptionStatus") { promise: Promise ->
      try {
        val subscriptionStatus = Superwall.instance.subscriptionStatus.value.toJson()
          promise.resolve(subscriptionStatus)
      } catch (error: Exception) {
        promise.reject(CodedException(error))
      }
    }

    AsyncFunction("setSubscriptionStatus") { status: Map<String,Any>, promise: Promise ->
      scope.launch {
        try {
          val statusString = (status["status"] as? String)?.uppercase() ?: "UNKNOWN"
          val subscriptionStatus: SubscriptionStatus

          when (statusString) {
            "UNKNOWN" -> subscriptionStatus = SubscriptionStatus.Unknown
            "INACTIVE" -> subscriptionStatus = SubscriptionStatus.Inactive
            "ACTIVE" -> {
              val entitlementsArray = status["entitlements"] as? List<Map<String, Any>>
              val entitlementsSet: Set<Entitlement> = entitlementsArray?.map { item ->
                val id = item["id"] as? String
                id?.let { Entitlement(id = it) }
              }?.filterNotNull()?.toSet() ?: emptySet()
              subscriptionStatus = SubscriptionStatus.Active(entitlementsSet)
            }
            else -> subscriptionStatus = SubscriptionStatus.Unknown
          }

          Superwall.instance.setSubscriptionStatus(subscriptionStatus)
          promise.resolve(null)
        } catch (error: Exception) {
          promise.reject(CodedException(error))
        }
      }
    }

    Function("setInterfaceStyle") { style: String? ->
      var interfaceStyle: InterfaceStyle? = style?.let { interfaceStyleFromString(it) }
      Superwall.instance.setInterfaceStyle(interfaceStyle)
    }

    AsyncFunction("getUserAttributes") { promise: Promise ->
      val attributes = Superwall.instance.userAttributes
      promise.resolve(attributes)
    }

    AsyncFunction("getDeviceAttributes") { promise: Promise ->
      scope.launch {
        try {
            val attributes = Superwall.instance.deviceAttributes()
            promise.resolve(attributes)
        } catch (e: Exception) {
            promise.reject("GET_DEVICE_ATTRIBUTES_ERROR", e.message, e)
        }
      }
    }


    AsyncFunction("setUserAttributes") { userAttributes: Map<String, Any?>, promise: Promise ->
      scope.launch {
        try {
          Superwall.instance.setUserAttributes(userAttributes)
          promise.resolve(null)
        } catch (error: Exception) {
          promise.reject(CodedException(error))
        }
      }
    }

    AsyncFunction("handleDeepLink") { url: String, promise: Promise ->
      val url = Uri.parse(url)
      val result = Superwall.handleDeepLink(url).fold({
        promise.resolve(it)
      }, { error ->
        promise.resolve(false)
      })
    }

    Function("didPurchase") { result: Map<String, Any> ->
      val purchaseResult = purchaseResultFromJson(result)
      purchaseController.purchasePromise?.complete(purchaseResult)
    }

    Function("didRestore") { result: Map<String, Any> ->
      val restorationResult = restorationResultFromJson(result)
      purchaseController.restorePromise?.complete(restorationResult)
    }

    Function("didHandleBackPressed") { shouldConsume: Boolean ->
      backPressedPromise?.complete(shouldConsume)
    }

    AsyncFunction("didHandleCustomCallback") { callbackId: String, status: String, data: Map<String, Any>?, promise: Promise ->
      val future = customCallbackFutures.remove(callbackId)
      if (future == null) {
        promise.resolve(null)
        return@AsyncFunction
      }
      val result = if (status == "success") {
        CustomCallbackResult.success(data)
      } else {
        CustomCallbackResult.failure(data)
      }
      future.complete(result)
      promise.resolve(null)
    }

    AsyncFunction("restorePurchases") { promise: Promise ->
      ioScope.launch {
        Superwall.instance.restorePurchases().fold({ result ->
          scope.launch {
            promise.resolve(restorationResultToJson(result))
          }
        }, { error ->
          scope.launch {
            promise.resolve(restorationResultToJson(RestorationResult.Failed(error)))
          }
        })
      }
    }

    AsyncFunction("dismiss") { promise: Promise ->
      ioScope.launch {
        Superwall.instance.dismiss()
        scope.launch {
          promise.resolve(null)
        }
      }
    }

    AsyncFunction("presentCustomerCenter") {
      configuration: Map<String, Any>?,
      handlerId: String,
      asksBeforeRestoring: Boolean,
      promise: Promise ->
      scope.launch {
        // The native SDK ignores a second presentation without calling `onDismiss`, so
        // settle straight away rather than leave the promise pending.
        if (isCustomerCenterPresented) {
          promise.resolve(null)
          return@launch
        }
        try {
          val customerCenterConfiguration = configuration?.let { customerCenterConfigurationFromJson(it) }
          val settled = java.util.concurrent.atomic.AtomicBoolean(false)
          isCustomerCenterPresented = true
          Superwall.instance.presentCustomerCenter(
            configuration = customerCenterConfiguration,
            delegate = CustomerCenterDelegateBridge(handlerId, asksBeforeRestoring),
            onDismiss = {
              isCustomerCenterPresented = false
              if (settled.compareAndSet(false, true)) {
                promise.resolve(null)
              }
            }
          )
        } catch (error: Throwable) {
          isCustomerCenterPresented = false
          promise.reject(CodedException(error))
        }
      }
    }

    AsyncFunction("dismissCustomerCenter") { promise: Promise ->
      scope.launch {
        try {
          Superwall.instance.dismissCustomerCenter {
            promise.resolve(null)
          }
        } catch (error: Throwable) {
          promise.reject(CodedException(error))
        }
      }
    }

    Function("didHandleCustomerCenterShouldRestorePurchases") { requestId: String, proceed: Boolean ->
      val pending = CustomerCenterDelegateBridge.pendingRestores.remove(requestId)
      if (pending != null) {
        scope.launch { pending(proceed) }
      }
      Unit
    }

    Function("togglePaywallSpinner") { isHidden: Boolean ->
      Superwall.instance.togglePaywallSpinner(isHidden)
    }

    AsyncFunction("confirmAllAssignments") { promise: Promise ->
      ioScope.launch {
        Superwall.instance.confirmAllAssignments().fold({
          scope.launch {
            promise.resolve(it.map { it.toJson() })
          }
        }, { error ->
          scope.launch {
            promise.reject(CodedException(error))
          }
        })
      }
    }

    AsyncFunction("getPresentationResult") { placement: String, params: Map<String, Any>?, promise: Promise ->
      ioScope.launch {
        Superwall.instance.getPresentationResult(placement = placement, params = params)
        .fold({ result ->
          scope.launch {
            promise.resolve(result.toJson())
          }
        }, { error ->
          scope.launch {
            promise.reject(CodedException(error))
          }
        })
      }
    }

    Function("preloadPaywalls") { placementNames: List<String> ->
      Superwall.instance.preloadPaywalls(placementNames = placementNames.toSet())
    }

    Function("preloadAllPaywalls") {
      Superwall.instance.preloadAllPaywalls()
    }

    Function("setLogLevel") { level: String ->
      val logLevel = LogLevel.values().find { it.toString().equals(level, ignoreCase = true) }
      if (logLevel != null) {
        Superwall.instance.logLevel = logLevel
      }
    }

    Function("setEventTrackingBehavior") { behavior: String ->
      when (behavior) {
        "all" -> Superwall.instance.eventTrackingBehavior = EventTrackingBehavior.ALL
        "superwallOnly" -> Superwall.instance.eventTrackingBehavior = EventTrackingBehavior.SUPERWALL_ONLY
        "none" -> Superwall.instance.eventTrackingBehavior = EventTrackingBehavior.NONE
      }
    }

    Function("setLocaleIdentifier") { localeIdentifier: String? ->
      Superwall.instance.localeIdentifier = localeIdentifier
    }

    Function("setAdConsent") { consent: Map<String, Any?> ->
      Superwall.instance.adConsent = adConsentFromJson(consent)
    }

    AsyncFunction("getAdConsent") { promise: Promise ->
      promise.resolve(Superwall.instance.adConsent.toJson())
    }

    AsyncFunction("getEventTrackingBehavior") { promise: Promise ->
      promise.resolve(Superwall.instance.eventTrackingBehavior.toJson())
    }

    AsyncFunction("getLocaleIdentifier") { promise: Promise ->
      promise.resolve(Superwall.instance.localeIdentifier)
    }

    AsyncFunction("getLogLevel") { promise: Promise ->
      promise.resolve(Superwall.instance.logLevel.name)
    }

    AsyncFunction("getUserId") { promise: Promise ->
      promise.resolve(Superwall.instance.userId)
    }

    AsyncFunction("getIsLoggedIn") { promise: Promise ->
      promise.resolve(Superwall.instance.isLoggedIn)
    }

    AsyncFunction("getIsPaywallPresented") { promise: Promise ->
      promise.resolve(Superwall.instance.isPaywallPresented)
    }

    AsyncFunction("getLatestPaywallInfo") { promise: Promise ->
      promise.resolve(Superwall.instance.latestPaywallInfo?.toJson())
    }

    AsyncFunction("getOverrideProductsByName") { promise: Promise ->
      promise.resolve(Superwall.instance.overrideProductsByName)
    }

    Function("setOverrideProductsByName") { overrides: Map<String, String> ->
      Superwall.instance.overrideProductsByName = overrides
    }

    // The native refresh runs in the background and has no completion, so this resolves once
    // it has started.
    AsyncFunction("refreshConfiguration") { promise: Promise ->
      Superwall.instance.refreshConfiguration()
      promise.resolve(null)
    }

    Function("cancelAllScheduledNotifications") {
      Superwall.instance.cancelAllScheduledNotifications()
    }

    Function("setPushToken") { token: String ->
      Superwall.instance.setPushToken(token)
    }

    Function("optOutOfNotifications") {
      Superwall.instance.optOutOfNotifications()
    }

    Function("optInToNotifications") {
      Superwall.instance.optInToNotifications()
    }

    Function("handleNotificationOpen") { data: Map<String, String> ->
      Superwall.instance.handleNotificationOpen(data)
    }

    AsyncFunction("setIntegrationAttributes") { attributes: Map<String, String?>, promise: Promise ->
      scope.launch {
        try {
          setIntegrationAttributes(attributes)
          promise.resolve(null)
        } catch (error: Exception) {
          promise.reject(CodedException(error))
        }
      }
    }

    AsyncFunction("setIntegrationAttribute") { attribute: String, value: String?, promise: Promise ->
      scope.launch {
        try {
          setIntegrationAttributes(mapOf(attribute to value))
          promise.resolve(null)
        } catch (error: Exception) {
          promise.reject(CodedException(error))
        }
      }
    }

    AsyncFunction("getIntegrationAttributes") { promise: Promise ->
      val attributes = Superwall.instance.integrationAttributes
      promise.resolve(attributes)
    }

    AsyncFunction("consume") { purchaseToken: String, promise: Promise ->
      ioScope.launch {
        Superwall.instance.consume(purchaseToken).fold({ token ->
          scope.launch { promise.resolve(token) }
        }, { error ->
          scope.launch { promise.reject(CodedException(error)) }
        })
      }
    }

    AsyncFunction("purchase") { productId: String, promise: Promise ->
      ioScope.launch {
        // Failures (including an unknown product) resolve as a "failed" result
        // with the native error message, matching iOS.
        val result = Superwall.instance.purchase(productId).fold(
          { purchaseResultToJson(it) },
          { error ->
            purchaseResultToJson(
              PurchaseResult.Failed(error.localizedMessage ?: error.toString())
            )
          }
        )
        scope.launch { promise.resolve(result) }
      }
    }

    AsyncFunction("products") { productIds: List<String>, promise: Promise ->
      ioScope.launch {
        val ids = productIds.distinct()
        val result = if (ids.isEmpty()) {
          Result.success(emptyMap<String, StoreProduct>())
        } else {
          Superwall.instance.getProducts(*ids.toTypedArray()).fold(
            { Result.success(it) },
            { batchError ->
              // An identifier the store previously failed to return fails the whole
              // batch, so retry each identifier on its own and omit unknown ones.
              val individual = ids.map { id ->
                async { Superwall.instance.getProducts(id) }
              }.awaitAll()
              val found = individual.mapNotNull { it.getOrNull() }
                .fold(emptyMap<String, StoreProduct>()) { acc, map -> acc + map }
              if (found.isEmpty() && batchError.isBillingError()) {
                Result.failure(batchError)
              } else {
                Result.success(found)
              }
            }
          )
        }
        result.fold({ productsById ->
          // Preserve the requested order.
          val products = ids.mapNotNull { productsById[it]?.toJson() }
          scope.launch { promise.resolve(products) }
        }, { error ->
          scope.launch { promise.reject(CodedException(error)) }
        })
      }
    }

    // Shows an alert over the presented paywall. Resolves with true if the user tapped the
    // action button, and with false if they closed it or no paywall is presented.
    AsyncFunction("showAlert") {
      title: String?,
      message: String?,
      actionTitle: String?,
      closeActionTitle: String?,
      promise: Promise ->
      scope.launch {
        val settled = java.util.concurrent.atomic.AtomicBoolean(false)
        fun settle(didTapAction: Boolean) {
          if (settled.compareAndSet(false, true)) {
            scope.launch { promise.resolve(didTapAction) }
          }
        }
        try {
          // The SDK calls neither callback when there's no paywall to show the alert over.
          if (Superwall.instance.paywallView == null) {
            settle(false)
            return@launch
          }
          Superwall.instance.showAlert(
            title = title,
            message = message,
            actionTitle = actionTitle,
            closeActionTitle = closeActionTitle ?: "Done",
            action = { settle(true) },
            onClose = { settle(false) },
          )
        } catch (error: Throwable) {
          if (settled.compareAndSet(false, true)) {
            promise.reject(CodedException(error))
          }
        }
      }
    }

    Function("openRestoreOnWeb") {
      Superwall.instance.openRestoreOnWeb()
    }

    AsyncFunction("getExternalAccountId") { promise: Promise ->
      promise.resolve(Superwall.instance.externalAccountId)
    }

    // Observer mode: reports a purchase made outside of Superwall. See `purchasingObserverState`.
    AsyncFunction("observe") { state: Map<String, Any?>, promise: Promise ->
      ioScope.launch {
        try {
          Superwall.instance.observe(purchasingObserverState(state))
          scope.launch { promise.resolve(null) }
        } catch (error: Throwable) {
          scope.launch { promise.reject(CodedException(error)) }
        }
      }
    }

    // Resets the SDK and clears its instance, so `configure` can be called again. Resolves with
    // whether there was an SDK to tear down.
    AsyncFunction("teardown") { promise: Promise ->
      scope.launch {
        try {
          val wasInitialized = Superwall.initialized
          configurationStatusJob?.cancel()
          configurationStatusJob = null
          latestIntegrationAttributes = null
          Superwall.teardown()
          promise.resolve(wasInitialized)
        } catch (error: Throwable) {
          promise.reject(CodedException(error))
        }
      }
    }

    AsyncFunction("dismissForNextPaywall") { promise: Promise ->
      ioScope.launch {
        try {
          Superwall.instance.dismissForNextPaywall()
          scope.launch { promise.resolve(null) }
        } catch (error: Throwable) {
          scope.launch { promise.reject(CodedException(error)) }
        }
      }
    }

    // Granted entitlements and these notification helpers exist only in the iOS SDK. They're
    // defined here so the JS API is the same on both platforms, and do nothing.
    AsyncFunction("getGrantedEntitlements") { promise: Promise ->
      promise.resolve(emptyList<Map<String, Any>>())
    }

    AsyncFunction("setGrantedEntitlements") { _: List<Map<String, Any?>>, promise: Promise ->
      promise.resolve(null)
    }

    Function("removeAllPendingNonSuperwallNotificationRequests") {}

    Function("removeAllDeliveredSuperwallNotifications") {}

    Function("removeAllDeliveredNonSuperwallNotifications") {}

    AsyncFunction("queryInAppPurchases") { promise: Promise ->
      ioScope.launch {
        runCatching {
          val context = appContext.reactContext?.applicationContext
            ?: throw IllegalStateException("React context is not available")
          InAppPurchaseQuery.queryPurchased(context)
        }.fold({ purchases ->
          scope.launch { promise.resolve(purchases) }
        }, { error ->
          scope.launch { promise.reject(CodedException(error)) }
        })
      }
    }
  }

  // The native call replaces every integration attribute, so merge into the current ones, as
  // iOS does. A null value removes the attribute. The SDK stores the new set after a delay, so
  // the last set sent is kept here rather than read back.
  private fun setIntegrationAttributes(attributes: Map<String, String?>) {
    val current = latestIntegrationAttributes
      ?: Superwall.instance.integrationAttributes.mapNotNull { (key, value) ->
        attributionProviderFromString(key)?.let { it to value }
      }.toMap()
    val merged = current.toMutableMap()
    attributes.forEach { (key, value) ->
      val provider = attributionProviderFromString(key) ?: return@forEach
      if (value == null) merged.remove(provider) else merged[provider] = value
    }
    latestIntegrationAttributes = merged

    Superwall.instance.setIntegrationAttributes(merged)
  }

  /**
   * Builds the observer-mode state from JS:
   * - `{ type: "purchaseWillBegin", productId }`
   * - `{ type: "purchaseError", productId, error }`
   * - `{ type: "purchaseResult", responseCode, debugMessage, purchases: [{ originalJson, signature }] }`
   *
   * The SDK takes Play Billing objects, so the product is looked up by its identifier and each
   * purchase is rebuilt from the JSON and signature Play Billing gave the app.
   */
  private suspend fun purchasingObserverState(state: Map<String, Any?>): PurchasingObserverState {
    suspend fun productDetails() =
      (state["productId"] as? String)?.let { productId ->
        Superwall.instance.getProducts(productId).getOrThrow()[productId]
          ?.rawStoreProduct?.underlyingProductDetails
          ?: throw IllegalArgumentException("Product not found for identifier: $productId")
      } ?: throw IllegalArgumentException("observe needs a productId.")

    return when (val type = state["type"] as? String) {
      "purchaseWillBegin" -> PurchasingObserverState.PurchaseWillBegin(productDetails())
      "purchaseError" -> PurchasingObserverState.PurchaseError(
        product = productDetails(),
        error = Throwable(state["error"] as? String ?: "Unknown error"),
      )
      "purchaseResult" -> {
        val result = BillingResult.newBuilder()
          .setResponseCode((state["responseCode"] as? Number)?.toInt() ?: 0)
          .setDebugMessage(state["debugMessage"] as? String ?: "")
          .build()
        val purchases = (state["purchases"] as? List<*>)?.mapNotNull { item ->
          val purchase = item as? Map<*, *> ?: return@mapNotNull null
          val originalJson = purchase["originalJson"] as? String ?: return@mapNotNull null
          Purchase(originalJson, purchase["signature"] as? String ?: "")
        }
        PurchasingObserverState.PurchaseResult(result, purchases)
      }
      else -> throw IllegalArgumentException("Unknown purchasing observer state: $type")
    }
  }
}

private fun Throwable.isBillingError(): Boolean =
  generateSequence(this) { it.cause }.any { it is BillingError }
