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
  onBackPressed?: (paywallInfo: PaywallInfo) => boolean

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
      if (init.automaticallyDismiss !== undefined) {
        this.automaticallyDismiss = init.automaticallyDismiss
      }
      if (init.transactionBackgroundView) {
        this.transactionBackgroundView = init.transactionBackgroundView
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
    })
  }
}
