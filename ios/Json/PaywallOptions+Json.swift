import SuperwallKit

extension PaywallOptions {
  // Every key is optional: one that's missing or has the wrong type keeps the native default.
  static func fromJson(_ dictionary: [String: Any]) -> PaywallOptions {
    let paywallOptions = PaywallOptions()
    if let isHapticFeedbackEnabled = dictionary["isHapticFeedbackEnabled"] as? Bool {
      paywallOptions.isHapticFeedbackEnabled = isHapticFeedbackEnabled
    }
    if let restoreFailedValue = dictionary["restoreFailed"] as? [String: Any] {
      paywallOptions.restoreFailed = PaywallOptions.RestoreFailed.fromJson(restoreFailedValue)
    }
    if let shouldShowPurchaseFailureAlert = dictionary["shouldShowPurchaseFailureAlert"] as? Bool {
      paywallOptions.shouldShowPurchaseFailureAlert = shouldShowPurchaseFailureAlert
    }
    if let shouldPreload = dictionary["shouldPreload"] as? Bool {
      paywallOptions.shouldPreload = shouldPreload
    }
    if let automaticallyDismiss = dictionary["automaticallyDismiss"] as? Bool {
      paywallOptions.automaticallyDismiss = automaticallyDismiss
    }
    if let transactionBackgroundViewValue = dictionary["transactionBackgroundView"] as? String,
      let transactionBackgroundView = PaywallOptions.TransactionBackgroundView.fromJson(
        transactionBackgroundViewValue
      ) {
      paywallOptions.transactionBackgroundView = transactionBackgroundView
    }
    if let shouldShowWebPurchaseConfirmationAlert =
      dictionary["shouldShowWebPurchaseConfirmationAlert"] as? Bool {
      paywallOptions.shouldShowWebPurchaseConfirmationAlert = shouldShowWebPurchaseConfirmationAlert
    }

    if let shouldShowWebRestorationAlert = dictionary["shouldShowWebRestorationAlert"] as? Bool {
      paywallOptions.shouldShowWebRestorationAlert = shouldShowWebRestorationAlert
    }
    if let overrideProductsByName = dictionary["overrideProductsByName"] as? [String: String] {
      paywallOptions.overrideProductsByName = overrideProductsByName
    }
    if let notificationPermissionsDeniedValue = dictionary["notificationPermissionsDenied"] as? [String: Any] {
      paywallOptions.notificationPermissionsDenied = PaywallOptions.NotificationPermissionsDenied.fromJson(
        notificationPermissionsDeniedValue
      )
    }

    return paywallOptions
  }
}

extension PaywallOptions.NotificationPermissionsDenied {
  // A field left out keeps the native default.
  static func fromJson(_ dictionary: [String: Any]) -> PaywallOptions.NotificationPermissionsDenied {
    let notificationPermissionsDenied = PaywallOptions.NotificationPermissionsDenied()
    if let title = dictionary["title"] as? String {
      notificationPermissionsDenied.title = title
    }
    if let message = dictionary["message"] as? String {
      notificationPermissionsDenied.message = message
    }
    if let actionButtonTitle = dictionary["actionButtonTitle"] as? String {
      notificationPermissionsDenied.actionButtonTitle = actionButtonTitle
    }
    if let closeButtonTitle = dictionary["closeButtonTitle"] as? String {
      notificationPermissionsDenied.closeButtonTitle = closeButtonTitle
    }
    return notificationPermissionsDenied
  }
}

extension PaywallOptions.RestoreFailed {
  // A field left out keeps the native default.
  static func fromJson(_ dictionary: [String: Any]) -> PaywallOptions.RestoreFailed {
    let restoreFailed = PaywallOptions.RestoreFailed()
    if let title = dictionary["title"] as? String {
      restoreFailed.title = title
    }
    if let message = dictionary["message"] as? String {
      restoreFailed.message = message
    }
    if let closeButtonTitle = dictionary["closeButtonTitle"] as? String {
      restoreFailed.closeButtonTitle = closeButtonTitle
    }
    return restoreFailed
  }
}

extension PaywallOptions.TransactionBackgroundView {
  static func fromJson(_ json: String) -> PaywallOptions.TransactionBackgroundView? {
    switch json {
      case "spinner":
        return PaywallOptions.TransactionBackgroundView.spinner
      case "none":
        return PaywallOptions.TransactionBackgroundView.none
      default:
        return nil
    }
  }
}
