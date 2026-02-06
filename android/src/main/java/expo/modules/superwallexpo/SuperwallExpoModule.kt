package expo.modules.superwallexpo

import android.content.pm.PackageManager
import android.net.Uri
import android.app.Application
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.Promise
import expo.modules.superwallexpo.bridges.PurchaseControllerBridge
import expo.modules.superwallexpo.bridges.SuperwallDelegateBridge
import expo.modules.superwallexpo.json.*
import com.superwall.sdk.Superwall
import com.superwall.sdk.delegate.SuperwallDelegate
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.identity.IdentityOptions
import com.superwall.sdk.identity.identify
import com.superwall.sdk.identity.setUserAttributes
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.network.device.InterfaceStyle
import com.superwall.sdk.paywall.presentation.PaywallPresentationHandler
import com.superwall.sdk.paywall.presentation.CustomCallbackResult
import com.superwall.sdk.paywall.presentation.result.PresentationResult
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.paywall.presentation.register
import com.superwall.sdk.paywall.presentation.dismiss
import com.superwall.sdk.paywall.presentation.get_presentation_result.getPresentationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import android.util.Log
import java.util.concurrent.CompletableFuture

class SuperwallExpoModule : Module() {

  companion object {
    var instance: SuperwallExpoModule? = null
  }

  val scope = CoroutineScope(Dispatchers.Main)
  val ioScope = CoroutineScope(Dispatchers.IO)
  var backPressedPromise: CompletableFuture<Boolean>? = null

  init {
    instance = this
  }

  fun emitEvent(name: String, body: Map<String, Any?>?) {
    SuperwallExpoModule.instance?.sendEvent(name, body?:emptyMap())
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
  val purchaseController = PurchaseControllerBridge.instance

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
      didRedeemLink
    )

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
      //TODO SDK version in arguments?
      try{
      val superwallOptions: SuperwallOptions = options?.let {
        superwallOptionsFromJson(options)
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

      Superwall.configure(
        apiKey = apiKey,
        applicationContext = appContext.reactContext?.applicationContext as Application,
        purchaseController = if (usingPurchaseController == true) purchaseController else null,
        activityProvider = ExpoActivityProvider(appContext),
        options = superwallOptions,
        completion = {
          Superwall.instance.setPlatformWrapper("Expo", version = sdkVersion ?: "0.0.0")
          Superwall.instance.delegate = SuperwallDelegateBridge()
          promise.resolve(true)
         }
       )
      } catch (error: Throwable) {
        error.printStackTrace()
        promise.reject(CodedException(error))
      }
    }

    AsyncFunction("getConfigurationStatus") { promise: Promise ->
      try {
        val configurationStatus = Superwall.instance.configurationState.asString()
          promise.resolve(configurationStatus)
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
          sendEvent(onPaywallPresent, data)
        }



        handler?.onDismiss { paywallInfo, result ->
          val data =
          mapOf(
            "paywallInfoJson" to paywallInfo.toJson(),
          "result" to result.toJson(),
          "handlerId" to handlerId)
          
          sendEvent(onPaywallDismiss, data)
        }

        handler?.onError { error ->
          val data =
          mapOf(
            "errorString" to error.localizedMessage,
          "handlerId" to handlerId,
          )
          sendEvent(onPaywallError, data)
        }

        handler?.onSkip { reason ->
          val data =
          mapOf(
            "skippedReason" to reason.toJson(),
             "handlerId" to handlerId)
          sendEvent(onPaywallSkip, data)
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

          sendEvent(onCustomCallback, data)

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
        val map = mutableMapOf<String, List<Map<String, String>>>()
        
        map["all"] = entitlements.all.map { entitlement ->
          mapOf("id" to entitlement.id)
        }
        
        map["inactive"] = entitlements.inactive.map { entitlement ->
          mapOf("id" to entitlement.id)
        }
        
        map["active"] = entitlements.active.map { entitlement ->
          mapOf("id" to entitlement.id)
        }
        promise.resolve(map)
      } catch (error: Exception) {
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


    AsyncFunction("setUserAttributes") { userAttributes: Map<String, Any>, promise: Promise ->
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

    AsyncFunction("dismiss") { promise: Promise ->
      ioScope.launch {
        Superwall.instance.dismiss()
        scope.launch {
          promise.resolve(null)
        }
      }
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

    AsyncFunction("setIntegrationAttributes") { attributes: Map<String, String>, promise: Promise ->
      scope.launch {
        try {
          val converted = attributes.mapNotNull { (key, value) ->
            attributionProviderFromString(key)?.let { it to value }
          }.toMap()

          Superwall.instance.setIntegrationAttributes(converted)
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
  }
}
