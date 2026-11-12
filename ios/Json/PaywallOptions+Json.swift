import SuperwallKit

extension PaywallOptions {
  static func fromJson(_ dictionary: [String: Any]) -> PaywallOptions? {
    guard
      let isHapticFeedbackEnabled = dictionary["isHapticFeedbackEnabled"] as? Bool,
      let restoreFailedValue = dictionary["restoreFailed"] as? [String: Any],
      let shouldShowPurchaseFailureAlert = dictionary["shouldShowPurchaseFailureAlert"] as? Bool,
      let shouldPreload = dictionary["shouldPreload"] as? Bool,
      let automaticallyDismiss = dictionary["automaticallyDismiss"] as? Bool,
      let transactionBackgroundViewValue = dictionary["transactionBackgroundView"] as? String,
      let shouldShowWebPurchaseConfirmationAlert = dictionary["shouldShowWebPurchaseConfirmationAlert"] as? Bool else {
      return nil
    }

    guard let restoreFailed = PaywallOptions.RestoreFailed.fromJson(restoreFailedValue),
          let transactionBackgroundView = PaywallOptions.TransactionBackgroundView.fromJson(transactionBackgroundViewValue) else {
      return nil
    }

    let paywallOptions = PaywallOptions()
    paywallOptions.isHapticFeedbackEnabled = isHapticFeedbackEnabled
    paywallOptions.restoreFailed = restoreFailed
    paywallOptions.shouldShowPurchaseFailureAlert = shouldShowPurchaseFailureAlert
    paywallOptions.shouldPreload = shouldPreload
    paywallOptions.automaticallyDismiss = automaticallyDismiss
    paywallOptions.transactionBackgroundView = transactionBackgroundView
    paywallOptions.shouldShowWebPurchaseConfirmationAlert = shouldShowWebPurchaseConfirmationAlert

    // Optional, so options built before these existed still parse.
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
  static func fromJson(_ dictionary: [String: Any]) -> PaywallOptions.RestoreFailed? {
    guard let title = dictionary["title"] as? String,
          let message = dictionary["message"] as? String,
          let closeButtonTitle = dictionary["closeButtonTitle"] as? String else {
      return nil
    }

    let restoreFailed = PaywallOptions.RestoreFailed()
    restoreFailed.title = title
    restoreFailed.message = message
    restoreFailed.closeButtonTitle = closeButtonTitle

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
