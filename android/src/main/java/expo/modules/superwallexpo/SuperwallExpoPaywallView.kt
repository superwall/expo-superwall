package expo.modules.superwallexpo

import android.content.Context
import android.content.res.Configuration
import android.view.ViewGroup
import com.superwall.sdk.paywall.presentation.get_paywall.builder.PaywallBuilder
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.delegate.PaywallViewCallback
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView
import expo.modules.superwallexpo.json.paywallOverridesFromJson
import expo.modules.superwallexpo.json.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A React Native host view that owns one SDK PaywallView at a time.
 *
 * PaywallView instances are cached and shared by the SDK (keyed by paywall, not placement),
 * so this host never destroys a paywall it merely stops showing: reloads and stale results
 * detach only, and the SDK revives the cached instance on the next acquisition. The full
 * destructive teardown runs only when a presentation genuinely ends (dismissal or unmount),
 * and any rebuild waits for it to finish because the SDK cache can hand the same instance
 * straight back.
 */
class SuperwallExpoPaywallView(
  context: Context,
  appContext: AppContext,
) : ExpoView(context, appContext) {
  override val shouldUseAndroidLayout = true

  val onPaywallLoadStart by EventDispatcher<Map<String, Any>>()
  val onPaywallPresent by EventDispatcher<Map<String, Any>>()
  val onPaywallDismiss by EventDispatcher<Map<String, Any>>()
  val onPaywallSkip by EventDispatcher<Map<String, Any>>()
  val onPaywallError by EventDispatcher<Map<String, Any>>()

  private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  // Teardown must survive destroy(): cancelling destroyed()/cleanup() mid-flight would
  // leave the shared cached PaywallView half torn down.
  private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var teardownJob: Job? = null

  private var placement: String? = null
  private var params: Map<String, Any>? = null
  private var loadedPlacement: String? = null
  private var loadedParams: Map<String, Any>? = null
  private var paywallOverrides: Map<String, Any>? = null
  private var loadedPaywallOverrides: Map<String, Any>? = null
  private var loadGeneration = 0
  private var paywallView: PaywallView? = null
  private var lastUiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

  private val delegate =
    object : PaywallViewCallback {
      override fun onFinished(
        paywall: PaywallView,
        result: PaywallResult,
        shouldDismiss: Boolean,
      ) {
        if (paywall !== paywallView) return

        onPaywallDismiss(
          mapOf(
            "paywallInfo" to paywall.info.toJson(),
            "result" to result.toJson(),
            "shouldDismiss" to shouldDismiss,
          ),
        )

        if (shouldDismiss) {
          teardownPaywall()
        }
      }
    }

  fun setPlacement(placement: String) {
    this.placement = placement
  }

  fun setParams(params: Map<String, Any>?) {
    this.params = params
  }

  fun setPaywallOverrides(paywallOverrides: Map<String, Any>?) {
    this.paywallOverrides = paywallOverrides
  }

  fun loadPaywallIfNeeded() {
    val placement = placement ?: return
    if (placement.isEmpty() || !isAttachedToWindow) return
    if (placement == loadedPlacement &&
      params == loadedParams &&
      paywallOverrides == loadedPaywallOverrides
    ) {
      return
    }

    val paramsSnapshot = params?.toMap()
    val paywallOverridesSnapshot = paywallOverrides?.toMap()
    loadedPlacement = placement
    loadedParams = paramsSnapshot
    loadedPaywallOverrides = paywallOverridesSnapshot
    loadGeneration += 1
    val generation = loadGeneration
    detachPaywall()
    onPaywallLoadStart(emptyMap())

    val activity = appContext.currentActivity
    if (activity == null) {
      clearLoadedConfig()
      onPaywallError(mapOf("message" to "No current Activity is available to load the paywall."))
      return
    }

    loadScope.launch {
      // Never acquire from the SDK cache while a previous instance is still tearing
      // down: getPaywall can return that same instance.
      teardownJob?.join()
      if (generation != loadGeneration) return@launch

      // Overriding products means fetching them from the store first.
      val overrides =
        try {
          paywallOverridesSnapshot?.let { paywallOverridesFromJson(it) }
        } catch (error: kotlinx.coroutines.CancellationException) {
          throw error
        } catch (error: Throwable) {
          if (generation == loadGeneration) {
            clearLoadedConfig()
            onPaywallError(mapOf("message" to (error.localizedMessage ?: error.toString())))
          }
          return@launch
        }
      if (generation != loadGeneration) return@launch

      val result =
        PaywallBuilder(placement)
          .params(paramsSnapshot)
          .overrides(overrides)
          .delegate(delegate)
          .activity(activity)
          .build()

      if (generation != loadGeneration || !isAttachedToWindow) {
        // Stale result. The view belongs to the SDK cache; leave it for the next
        // acquisition. If this host is merely detached, allow a reattach to retry.
        if (generation == loadGeneration) clearLoadedConfig()
        return@launch
      }

      result.fold(
        onSuccess = { paywall ->
          if (paywall.parent != null) {
            clearLoadedConfig()
            onPaywallError(
              mapOf(
                "message" to
                  "The retrieved paywall is already attached to another view. " +
                  "Use a different placement for each mounted PaywallView.",
              ),
            )
            return@fold
          }

          paywallView = paywall
          addView(
            paywall,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
          )
          paywall.onViewCreated()
          onPaywallPresent(mapOf("paywallInfo" to paywall.info.toJson()))
        },
        onFailure = { error ->
          if (error is PaywallSkippedReason) {
            onPaywallSkip(mapOf("reason" to error.toJson()))
          } else {
            clearLoadedConfig()
            val message =
              if (error is PaywallPresentationRequestStatusReason) {
                error.info
              } else {
                error.localizedMessage ?: error.toString()
              }
            onPaywallError(mapOf("message" to message))
          }
        },
      )
    }
  }

  fun destroy() {
    loadGeneration += 1
    clearLoadedConfig()
    teardownPaywall()
    loadScope.cancel()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    loadPaywallIfNeeded()
  }

  override fun onConfigurationChanged(newConfig: Configuration?) {
    super.onConfigurationChanged(newConfig)
    val uiMode = (newConfig ?: resources.configuration).uiMode and Configuration.UI_MODE_NIGHT_MASK
    if (uiMode != lastUiMode) {
      lastUiMode = uiMode
      paywallView?.takeIf { it.state.isPresented }?.onThemeChanged()
    }
  }

  private fun clearLoadedConfig() {
    loadedPlacement = null
    loadedParams = null
    loadedPaywallOverrides = null
  }

  /**
   * Stops showing the current paywall without destroying it. Used when swapping
   * configuration: the SDK cache owns the instance and resets its transient
   * presentation state when it is next acquired.
   */
  private fun detachPaywall() {
    val paywall = paywallView ?: return
    paywallView = null
    removeView(paywall)
  }

  /** Ends the presentation for real: dismissal or host unmount. */
  private fun teardownPaywall() {
    val paywall = paywallView ?: return
    paywallView = null
    removeView(paywall)
    paywall.beforeOnDestroy(forceCleanup = true)
    paywall.encapsulatingActivity = null
    teardownJob =
      teardownScope.launch {
        paywall.destroyed(forceCleanup = true)
        paywall.cleanup()
      }
  }
}
