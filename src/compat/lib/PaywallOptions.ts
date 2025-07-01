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
    return {
      title: this.title,
      message: this.message,
      closeButtonTitle: this.closeButtonTitle,
    }
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
      if (init.restoreFailed) {
        this.restoreFailed = init.restoreFailed
      }
    }
  }

  toJson(): object {
    return {
      isHapticFeedbackEnabled: this.isHapticFeedbackEnabled,
      restoreFailed: this.restoreFailed.toJson(),
      shouldShowPurchaseFailureAlert: this.shouldShowPurchaseFailureAlert,
      shouldPreload: this.shouldPreload,
      automaticallyDismiss: this.automaticallyDismiss,
      transactionBackgroundView: this.transactionBackgroundView,
    }
  }
}
