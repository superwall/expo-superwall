import type { DeviceTier } from "../../SuperwallOptions"
import type { PaywallInfo } from "./PaywallInfo"

/**
 * Helper function to remove undefined values from an object.
 * This is necessary for Android compatibility as the Expo bridge
 * cannot convert undefined to Kotlin types.
 */
function filterUndefined<T extends Record<string, any>>(obj: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(obj).filter(([_, value]) => value !== undefined),
  ) as Partial<T>
}

/**
 * @category Enums
 * @since 0.0.15
 * Defines the different types of views that can appear behind Apple's payment sheet during a transaction.
 */
export enum TransactionBackgroundView {
  spinner = "spinner",
  none = "none",
}

/**
 * @category Enums
 * @since 1.6.0
 * Where a presented paywall is hosted on Android.
 *
 * Android only.
 */
export enum PresentationHost {
  /** The paywall is shown in its own `SuperwallPaywallActivity` (default). */
  activity = "activity",
  /**
   * **Experimental.** The paywall is shown as an overlay in the presenting activity's own
   * window, so that activity is never covered or stopped by the paywall.
   */
  inWindow = "inWindow",
}

/**
 * @category Models
 * @since 0.0.15
 * Defines the messaging of the alert presented to the user when restoring a transaction fails.
 */
export class RestoreFailed {
  title = "No Subscription Found"
  message = "We couldn't find an active subscription for your account."
  closeButtonTitle = "Okay"

  toJson(): object {
    return filterUndefined({
      title: this.title,
      message: this.message,
      closeButtonTitle: this.closeButtonTitle,
    })
  }
}

/**
 * @category Models
 * @since 1.8.0
 * Defines the messaging of the alert presented when a paywall asks for notification permissions
 * that the user has denied. A field left out keeps the native default.
 *
 * iOS only.
 */
export interface NotificationPermissionsDenied {
  /** Defaults to `"Notification Permissions Denied"`. */
  title?: string
  /** Defaults to a message asking the user to enable notifications in the Settings app. */
  message?: string
  /** The button that opens the Settings app. Defaults to `"Open Settings"`. */
  actionButtonTitle?: string
  /** Defaults to `"Not now"`. */
  closeButtonTitle?: string
}

/**
 * @category Models
 * @since 0.0.15
 * Options for configuring the appearance and behavior of paywalls.
 */
export class PaywallOptions {
  isHapticFeedbackEnabled = true
  restoreFailed: RestoreFailed = new RestoreFailed()
  shouldShowPurchaseFailureAlert = true
  shouldPreload = true
  automaticallyDismiss = true
  transactionBackgroundView: TransactionBackgroundView = TransactionBackgroundView.spinner
  shouldShowWebPurchaseConfirmationAlert = true
  onBackPressed?: (paywallInfo: PaywallInfo) => boolean
  /**
   * **Experimental.** Chooses how presented paywalls are hosted. Defaults to
   * {@link PresentationHost.activity}. Set it to {@link PresentationHost.inWindow} to show
   * paywalls as an overlay in the presenting activity's own window instead of launching
   * `SuperwallPaywallActivity`. If the host activity is destroyed while the paywall is shown,
   * the paywall is dismissed. This may change or be removed in a future native SDK release.
   *
   * Android only: ignored on iOS.
   * @since 1.6.0
   */
  presentationHost?: PresentationHost
  /**
   * Replaces products on every paywall by product name, such as `{ primary: "com.app.yearly" }`,
   * where the keys are the product names set in the paywall editor. Change it at runtime with
   * `Superwall.shared.setOverrideProductsByName()`.
   * @since 1.8.0
   */
  overrideProductsByName?: Record<string, string>
  /**
   * Whether to show an alert after the user restores a web purchase. Defaults to `true`.
   *
   * iOS only: ignored on Android.
   * @since 1.8.0
   */
  shouldShowWebRestorationAlert?: boolean
  /**
   * The alert shown when a paywall asks for notification permissions that the user has denied.
   * No alert is shown unless this is set.
   *
   * iOS only: ignored on Android.
   * @since 1.8.0
   */
  notificationPermissionsDenied?: NotificationPermissionsDenied
  /**
   * Shows the paywall without waiting for its loading shimmer to finish. Defaults to `false`.
   *
   * Android only: ignored on iOS.
   * @since 1.8.0
   */
  optimisticLoading?: boolean
  /**
   * How long, in milliseconds, a presented paywall may take to load before it's given up on. It's
   * then dismissed as declined with the `webViewFailedToLoad` close reason. No timeout unless
   * this is set.
   *
   * Android only: ignored on iOS.
   * @since 1.8.0
   */
  timeoutAfter?: number
  /**
   * Overrides `shouldPreload` for devices of the given tiers, such as `{ low: false }` to skip
   * preloading on low-end devices.
   *
   * Android only: ignored on iOS.
   * @since 1.8.0
   */
  preloadDeviceOverrides?: Partial<Record<DeviceTier, boolean>>
  /**
   * The color of the paywall's loading indicator, as the name of a color resource in your
   * Android app, such as `"paywall_loading"` for `R.color.paywall_loading`. The native option
   * is a resource ID, so a hex string isn't accepted. An unknown name keeps the default color.
   *
   * Android only: ignored on iOS.
   * @since 1.8.0
   */
  loadingColor?: string

  constructor(init?: Partial<PaywallOptions>) {
    if (init) {
      if (init.isHapticFeedbackEnabled !== undefined) {
        this.isHapticFeedbackEnabled = init.isHapticFeedbackEnabled
      }
      if (init.shouldShowPurchaseFailureAlert !== undefined) {
        this.shouldShowPurchaseFailureAlert = init.shouldShowPurchaseFailureAlert
      }
      if (init.shouldPreload !== undefined) {
        this.shouldPreload = init.shouldPreload
      }
      if (init.shouldShowWebPurchaseConfirmationAlert !== undefined) {
        this.shouldShowWebPurchaseConfirmationAlert = init.shouldShowWebPurchaseConfirmationAlert
      }
      if (init.automaticallyDismiss !== undefined) {
        this.automaticallyDismiss = init.automaticallyDismiss
      }
      if (init.transactionBackgroundView) {
        this.transactionBackgroundView = init.transactionBackgroundView
      }
      if (init.presentationHost) {
        this.presentationHost = init.presentationHost
      }
      if (init.overrideProductsByName) {
        this.overrideProductsByName = init.overrideProductsByName
      }
      if (init.shouldShowWebRestorationAlert !== undefined) {
        this.shouldShowWebRestorationAlert = init.shouldShowWebRestorationAlert
      }
      if (init.notificationPermissionsDenied) {
        this.notificationPermissionsDenied = init.notificationPermissionsDenied
      }
      if (init.optimisticLoading !== undefined) {
        this.optimisticLoading = init.optimisticLoading
      }
      if (init.timeoutAfter !== undefined) {
        this.timeoutAfter = init.timeoutAfter
      }
      if (init.preloadDeviceOverrides) {
        this.preloadDeviceOverrides = init.preloadDeviceOverrides
      }
      if (init.loadingColor !== undefined) {
        this.loadingColor = init.loadingColor
      }
      if (init.onBackPressed) {
        this.onBackPressed = init.onBackPressed
      }
      if (init.restoreFailed) {
        // Ensure restoreFailed is always a RestoreFailed instance
        this.restoreFailed =
          init.restoreFailed instanceof RestoreFailed
            ? init.restoreFailed
            : Object.assign(new RestoreFailed(), init.restoreFailed)
      }
    }
  }

  toJson(): object {
    return filterUndefined({
      isHapticFeedbackEnabled: this.isHapticFeedbackEnabled,
      restoreFailed: this.restoreFailed.toJson(),
      shouldShowPurchaseFailureAlert: this.shouldShowPurchaseFailureAlert,
      shouldPreload: this.shouldPreload,
      automaticallyDismiss: this.automaticallyDismiss,
      transactionBackgroundView: this.transactionBackgroundView,
      shouldShowWebPurchaseConfirmationAlert: this.shouldShowWebPurchaseConfirmationAlert,
      presentationHost: this.presentationHost,
      overrideProductsByName: this.overrideProductsByName,
      shouldShowWebRestorationAlert: this.shouldShowWebRestorationAlert,
      notificationPermissionsDenied: this.notificationPermissionsDenied
        ? filterUndefined(this.notificationPermissionsDenied)
        : undefined,
      optimisticLoading: this.optimisticLoading,
      timeoutAfter: this.timeoutAfter,
      preloadDeviceOverrides: this.preloadDeviceOverrides
        ? filterUndefined(this.preloadDeviceOverrides)
        : undefined,
      loadingColor: this.loadingColor,
    })
  }
}
