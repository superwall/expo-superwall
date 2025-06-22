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
import com.superwall.sdk.paywall.presentation.result.PresentationResult
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.paywall.presentation.register
import com.superwall.sdk.paywall.presentation.dismiss
import com.superwall.sdk.paywall.presentation.get_presentation_result.getPresentationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class SuperwallExpoModule : Module() {

  companion object {
    var instance: SuperwallExpoModule? = null
  }

  val scope = CoroutineScope(Dispatchers.Main)
  val ioScope = CoroutineScope(Dispatchers.IO)
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

  // Purchase Events
  private val onPurchase = "onPurchase"
  private val onPurchaseRestore = "onPurchaseRestore"

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
      onPurchase,
      onPurchaseRestore,

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

    Function("getApiKey") {
      val applicationInfo = appContext?.reactContext?.packageManager?.getApplicationInfo(appContext?.reactContext?.packageName.toString(), PackageManager.GET_META_DATA)
      return@Function applicationInfo?.metaData?.getString("SUPERWALL_API_KEY")
    }

    Function("identify") { userId: String, options: Map<String, Any> ->
      try {
        val options = identityOptionsFromJson(options)
        Superwall.instance.identify(userId = userId, options = options)
      } catch (error: Exception) {
        error.printStackTrace()
      }
    }

    Function("reset") {
      Superwall.instance.reset()
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

    Function("setSubscriptionStatus") { status: Map<String,Any> ->
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
    }
    
    Function("setInterfaceStyle") { style: String? ->
      var interfaceStyle: InterfaceStyle? = style?.let { interfaceStyleFromString(it) }
      Superwall.instance.setInterfaceStyle(interfaceStyle)
    }

    AsyncFunction("getUserAttributes") { promise: Promise ->
      val attributes = Superwall.instance.userAttributes
      promise.resolve(attributes)
    }

    Function("setUserAttributes") { userAttributes: Map<String, Any> ->
      Superwall.instance.setUserAttributes(userAttributes)
    }

    AsyncFunction("handleDeepLink") { url: String, promise: Promise ->
      val url = Uri.parse(url)
      val result = Superwall.instance.handleDeepLink(url).fold({
        promise.resolve(it)
      }, { error ->
        promise.reject(CodedException(error))
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
  }
}
