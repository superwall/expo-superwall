package expo.modules.superwallexpo.json


import android.content.Context
import com.superwall.sdk.analytics.Tier
import com.superwall.sdk.config.options.AdConsent
import com.superwall.sdk.config.options.AdConsentStatus
import com.superwall.sdk.config.options.EventTrackingBehavior
import com.superwall.sdk.config.options.PaywallOptions
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.store.testmode.TestModeBehavior
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import java.util.EnumSet
import kotlin.time.Duration.Companion.milliseconds

// `context` resolves `paywalls.loadingColor`, which names a color resource.
fun superwallOptionsFromJson(json: Map<String, Any?>, context: Context? = null): SuperwallOptions {
  val options = SuperwallOptions()
  options.localeIdentifier = json["localeIdentifier"] as String?
  options.isExternalDataCollectionEnabled = (json["isExternalDataCollectionEnabled"] as Boolean?)?:true
  (json["eventTrackingBehavior"] as String?)?.let {
    options.eventTrackingBehavior = when (it) {
      "all" -> EventTrackingBehavior.ALL
      "superwallOnly" -> EventTrackingBehavior.SUPERWALL_ONLY
      "none" -> EventTrackingBehavior.NONE
      else -> options.eventTrackingBehavior
    }
  }
  options.isGameControllerEnabled = (json["isGameControllerEnabled"] as Boolean?)?:false
  options.passIdentifiersToPlayStore = (json["passIdentifiersToPlayStore"] as Boolean?)?:false
  options.enableExperimentalDeviceVariables = (json["enableExperimentalDeviceVariables"] as Boolean?)?:false
  options.shouldObservePurchases = (json["shouldObservePurchases"] as Boolean?)?:false
  options.useMockReviews = (json["useMockReviews"] as Boolean?)?:false
  options.testModeBehavior = when ((json["testModeBehavior"] as String?)?.lowercase()) {
    "automatic" -> TestModeBehavior.AUTOMATIC
    "whenenabledforuser" -> TestModeBehavior.WHEN_ENABLED_FOR_USER
    "never" -> TestModeBehavior.NEVER
    "always" -> TestModeBehavior.ALWAYS
    else -> TestModeBehavior.AUTOMATIC
  }

      options.networkEnvironment =
        networkEnvironmentFromJson(json["networkEnvironment"]) ?: options.networkEnvironment

      // Logging
      val loggingMap = json["logging"] as Map<String, Any>?
      val scopesArray = loggingMap?.get("scopes") as List<String>?
      val scopes = scopesArray?.map {
        try {
          LogScope.valueOf(androidLogScopeNames[it] ?: it)
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

      @Suppress("UNCHECKED_CAST")
      (paywallsMap?.get("overrideProductsByName") as? Map<String, String>)?.let {
        paywalls.overrideProductsByName = it
      }
      (paywallsMap?.get("optimisticLoading") as? Boolean)?.let {
        paywalls.optimisticLoading = it
      }
      // JS numbers arrive as Double.
      (paywallsMap?.get("timeoutAfter") as? Number)?.let {
        paywalls.timeoutAfter = it.toLong().milliseconds
      }

      // Keys are device tiers, such as `{ "low": false }`.
      (paywallsMap?.get("preloadDeviceOverrides") as? Map<*, *>)?.let { overrides ->
        paywalls.preloadDeviceOverrides = overrides.mapNotNull { (raw, value) ->
          val tier = Tier.values().firstOrNull { it.raw == raw || it.name.equals(raw as? String, ignoreCase = true) }
          if (tier != null && value is Boolean) tier to value else null
        }.toMap()
      }
      // The native option is a color resource, so this is the name of one in the app, such as
      // `"paywall_loading"` for `R.color.paywall_loading`. An unknown name keeps the default.
      (paywallsMap?.get("loadingColor") as? String)?.let { name ->
        val resourceId = context?.resources?.getIdentifier(name, "color", context.packageName) ?: 0
        if (resourceId != 0) {
          paywalls.loadingColor = resourceId
        }
      }

      paywalls.presentationHost = when (paywallsMap?.get("presentationHost") as String?) {
        "inWindow" -> PaywallOptions.PresentationHost.IN_WINDOW
        "activity" -> PaywallOptions.PresentationHost.ACTIVITY
        else -> paywalls.presentationHost
      }

  options.paywalls = paywalls

  (json["customerCenter"] as? Map<String, Any?>)?.let {
    options.customerCenter = customerCenterConfigurationFromJson(it)
  }

  (json["adConsent"] as? Map<String, Any?>)?.let {
    options.adConsent = adConsentFromJson(it)
  }

  return options
}

/**
 * Takes an environment name, or a map for a custom one: either `{ custom: "<domain>" }`, whose
 * hosts are derived from the domain as for the built-in environments, or the individual hosts
 * `{ baseHost, collectorHost, scheme, port, subscriptionHost, enrichmentHost, mmpHost }`.
 * `"local"` exists only on iOS and returns null here.
 */
fun networkEnvironmentFromJson(json: Any?): SuperwallOptions.NetworkEnvironment? {
  if (json is Map<*, *>) {
    val baseHost = json["baseHost"] as? String
    val collectorHost = json["collectorHost"] as? String
    if (baseHost != null && collectorHost != null) {
      val scheme = json["scheme"] as? String ?: "https"
      val port = (json["port"] as? Number)?.toInt()
      val defaults = SuperwallOptions.NetworkEnvironment.Custom(baseHost, collectorHost, scheme, port)
      return SuperwallOptions.NetworkEnvironment.Custom(
        baseHost = baseHost,
        collectorHost = collectorHost,
        scheme = scheme,
        port = port,
        subscriptionHost = json["subscriptionHost"] as? String ?: defaults.subscriptionHost,
        enrichmentHost = json["enrichmentHost"] as? String ?: defaults.enrichmentHost,
        mmpHost = json["mmpHost"] as? String ?: defaults.mmpHost,
      )
    }
    val domain = (json["custom"] ?: json["domain"]) as? String
    return domain?.takeIf { it.isNotEmpty() }?.let { SuperwallOptions.NetworkEnvironment(it) }
  }
  return when (json as? String) {
    "release" -> SuperwallOptions.NetworkEnvironment.Release()
    "releaseCandidate" -> SuperwallOptions.NetworkEnvironment.ReleaseCandidate()
    "developer" -> SuperwallOptions.NetworkEnvironment.Developer()
    else -> null
  }
}

// Anything left out is granted, matching `AdConsent.Custom()` and iOS.
fun adConsentFromJson(json: Map<String, Any?>): AdConsent =
  AdConsent.Custom(
    adUserData = adConsentStatusFromJson(json["adUserData"] as? String),
    adPersonalization = adConsentStatusFromJson(json["adPersonalization"] as? String),
  )

private fun adConsentStatusFromJson(json: String?): AdConsentStatus =
  if (json == "denied") AdConsentStatus.Denied else AdConsentStatus.Granted

// `Unspecified` reports both purposes as granted, as `AdConsent()` does on iOS.
fun AdConsent.toJson(): Map<String, String> {
  val consent = this as? AdConsent.Custom ?: AdConsent.Custom()
  return mapOf(
    "adUserData" to consent.adUserData.toString(),
    "adPersonalization" to consent.adPersonalization.toString(),
  )
}

fun EventTrackingBehavior.toJson(): String =
  when (this) {
    EventTrackingBehavior.ALL -> "all"
    EventTrackingBehavior.SUPERWALL_ONLY -> "superwallOnly"
    EventTrackingBehavior.NONE -> "none"
  }

// The JS `LogScope` uses the iOS names; these three are named differently on Android.
private val androidLogScopeNames = mapOf(
  "debugViewController" to "debugView",
  "localizationViewController" to "localizationView",
  "paywallViewController" to "paywallView",
)
