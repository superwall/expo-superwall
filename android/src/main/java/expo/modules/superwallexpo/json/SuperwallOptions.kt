package expo.modules.superwallexpo.json


import com.superwall.sdk.config.options.PaywallOptions
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import java.util.EnumSet

fun superwallOptionsFromJson(json: Map<String, Any?>): SuperwallOptions {
  val options = SuperwallOptions()
  options.localeIdentifier = json["localeIdentifier"] as String?
  options.isExternalDataCollectionEnabled = (json["isExternalDataCollectionEnabled"] as Boolean?)?:true
  options.isGameControllerEnabled = (json["isGameControllerEnabled"] as Boolean?)?:false
  options.passIdentifiersToPlayStore = (json["passIdentifiersToPlayStore"] as Boolean?)?:false
  options.enableExperimentalDeviceVariables = (json["enableExperimentalDeviceVariables"] as Boolean?)?:false
  options.shouldObservePurchases = (json["shouldObservePurchases"] as Boolean?)?:false
  options.useMockReviews = (json["useMockReviews"] as Boolean?)?:false

      val networkEnvironment = when (json["networkEnvironment"] as String?) {
        "release" -> SuperwallOptions.NetworkEnvironment.Release()
        "releaseCandidate" -> SuperwallOptions.NetworkEnvironment.ReleaseCandidate()
        "developer" -> SuperwallOptions.NetworkEnvironment.Developer()
        else -> options.networkEnvironment
      }
      options.networkEnvironment = networkEnvironment

      // Logging
      val loggingMap = json["logging"] as Map<String, Any>?
      val scopesArray = loggingMap?.get("scopes") as List<String>?
      val scopes = scopesArray?.map {
        try {
          LogScope.valueOf(it)
        } catch (e: IllegalArgumentException) {
          null
        }
      }?.filterNotNull() ?: emptyList()

      val levelStr = loggingMap?.get("level") as String?
      val level = LogLevel.values().find { it.toString().equals(levelStr, ignoreCase = true) }
        ?: LogLevel.warn

      val logging = SuperwallOptions.Logging()
      if(scopes.isNotEmpty()) {
        logging.scopes = EnumSet.copyOf(scopes)
      }
      logging.level = level
      options.logging = logging

      // PaywallOptions
      val paywallsMap = json["paywalls"] as Map<String, Any>?
      val isHapticFeedbackEnabled = paywallsMap?.get("isHapticFeedbackEnabled") as Boolean?
      val shouldShowPurchaseFailureAlert = paywallsMap?.get("shouldShowPurchaseFailureAlert") as Boolean?
      val shouldPreload = paywallsMap?.get("shouldPreload") as Boolean?
      val automaticallyDismiss = paywallsMap?.get("automaticallyDismiss") as Boolean?

      val paywalls = PaywallOptions()
      paywalls.isHapticFeedbackEnabled = isHapticFeedbackEnabled ?: paywalls.isHapticFeedbackEnabled
      paywalls.shouldShowPurchaseFailureAlert = shouldShowPurchaseFailureAlert ?: paywalls.shouldShowPurchaseFailureAlert
      paywalls.shouldPreload = shouldPreload ?: paywalls.shouldPreload
      paywalls.automaticallyDismiss = automaticallyDismiss ?: paywalls.automaticallyDismiss
      // Note: shouldShowWebPurchaseConfirmationAlert is iOS-only

      val restoreFailedMap = paywallsMap?.get("restoreFailed") as Map<String, Any>?
      val restoreFailed = PaywallOptions.RestoreFailed().apply {
        restoreFailedMap?.let {
          title = it["title"] as String? ?: title // Keep default if not found
          message = it["message"] as String? ?: message // Keep default if not found
          closeButtonTitle = it["closeButtonTitle"] as String? ?: closeButtonTitle // Keep default if not found
        }
      }
      paywalls.restoreFailed = restoreFailed

      val transactionBackgroundViewStr = paywallsMap?.get("transactionBackgroundView") as String?
      paywalls.transactionBackgroundView = when (transactionBackgroundViewStr) {
        "spinner" -> PaywallOptions.TransactionBackgroundView.SPINNER
        else -> null
      }

  options.paywalls = paywalls

  return options
}
