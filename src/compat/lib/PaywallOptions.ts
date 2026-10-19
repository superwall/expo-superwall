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
 * @since 0.0.15
 * Options for configuring the appearance and behavior of paywalls.
 */
export class PaywallOptions {
  isHapticFeedbackEnabled = true
  restoreFailed: RestoreFailed = new RestoreFailed()
  shouldShowPurchaseFailureAlert = true
  shouldPreload = false
  automaticallyDismiss = true
  transactionBackgroundView: TransactionBackgroundView = TransactionBackgroundView.spinner
  shouldShowWebPurchaseConfirmationAlert = false
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
    })
  }
}
