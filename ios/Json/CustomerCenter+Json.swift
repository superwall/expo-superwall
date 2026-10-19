import SuperwallKit

// Builds a configuration from the JS `CustomerCenterConfiguration`. Anything left out keeps
// the native default, so `{}` is the same as `CustomerCenterConfiguration.default`.
extension CustomerCenterConfiguration {
  static func fromJson(_ json: [String: Any]) -> CustomerCenterConfiguration {
    let configuration = CustomerCenterConfiguration.default

    if let managementScreen = json["managementScreen"] as? [String: Any] {
      configuration.managementScreen = Screen.fromJson(managementScreen)
    }
    if let noPurchasesScreen = json["noPurchasesScreen"] as? [String: Any] {
      configuration.noPurchasesScreen = Screen.fromJson(noPurchasesScreen)
    }
    if let support = json["support"] as? [String: Any] {
      configuration.support = Support.fromJson(support)
    }
    if let appearance = json["appearance"] as? [String: Any] {
      configuration.appearance = Appearance.fromJson(appearance)
    }
    if let showsAccountDetails = json["showsAccountDetails"] as? Bool {
      configuration.showsAccountDetails = showsAccountDetails
    }
    if let warnsAboutDuplicateSubscriptions = json["warnsAboutDuplicateSubscriptions"] as? Bool {
      configuration.warnsAboutDuplicateSubscriptions = warnsAboutDuplicateSubscriptions
    }

    return configuration
  }
}

extension CustomerCenterConfiguration.Screen {
  static func fromJson(_ json: [String: Any]) -> CustomerCenterConfiguration.Screen {
    let paths = (json["paths"] as? [Any] ?? []).compactMap { path -> CustomerCenterConfiguration.Path? in
      guard let path = path as? [String: Any] else { return nil }
      return CustomerCenterConfiguration.Path.fromJson(path)
    }
    return CustomerCenterConfiguration.Screen(
      title: json["title"] as? String,
      subtitle: json["subtitle"] as? String,
      paths: paths
    )
  }
}

extension CustomerCenterConfiguration.Path {
  // Returns nil for an unknown `type`, or a url/custom path missing its required field, so a
  // bad entry is dropped rather than failing the whole configuration.
  static func fromJson(_ json: [String: Any]) -> CustomerCenterConfiguration.Path? {
    let type: CustomerCenterConfiguration.PathType
    switch json["type"] as? String {
    case "restore":
      type = .restore
    case "manageSubscription":
      type = .manageSubscription
    case "refund":
      // JS passes milliseconds, matching Android; iOS takes seconds.
      let window = (json["windowMillis"] as? NSNumber).map { $0.doubleValue / 1000 }
      type = .refund(window: window)
    case "changePlan":
      let productIds = (json["productIds"] as? [Any])?.compactMap { $0 as? String }
      type = .changePlan(productIds: productIds)
    case "contactSupport":
      type = .contactSupport
    case "url":
      guard
        let urlString = json["url"] as? String,
        let url = URL(string: urlString)
      else {
        return nil
      }
      let openMethod: CustomerCenterConfiguration.OpenMethod =
        json["openMethod"] as? String == "external" ? .external : .inApp
      type = .url(url, openMethod: openMethod)
    case "custom":
      guard let identifier = json["identifier"] as? String else { return nil }
      type = .custom(identifier: identifier)
    default:
      return nil
    }

    let survey = (json["survey"] as? [String: Any]).flatMap {
      CustomerCenterConfiguration.FeedbackSurvey.fromJson($0)
    }

    return CustomerCenterConfiguration.Path(
      id: json["id"] as? String,
      type: type,
      title: json["title"] as? String,
      survey: survey
    )
  }
}

extension CustomerCenterConfiguration.FeedbackSurvey {
  static func fromJson(_ json: [String: Any]) -> CustomerCenterConfiguration.FeedbackSurvey? {
    guard let id = json["id"] as? String else { return nil }
    let options = (json["options"] as? [Any] ?? []).compactMap {
      option -> CustomerCenterConfiguration.FeedbackSurvey.Option? in
      guard
        let option = option as? [String: Any],
        let optionId = option["id"] as? String
      else {
        return nil
      }
      return CustomerCenterConfiguration.FeedbackSurvey.Option(
        id: optionId,
        title: option["title"] as? String
      )
    }
    return CustomerCenterConfiguration.FeedbackSurvey(
      id: id,
      title: json["title"] as? String,
      options: options
    )
  }
}

extension CustomerCenterConfiguration.Support {
  static func fromJson(_ json: [String: Any]) -> CustomerCenterConfiguration.Support {
    return CustomerCenterConfiguration.Support(
      email: json["email"] as? String,
      latestAppVersion: json["latestAppVersion"] as? String,
      warnsAboutUpdates: json["warnsAboutUpdates"] as? Bool ?? true,
      checksAppStoreForUpdates: json["checksAppStoreForUpdates"] as? Bool ?? true,
      webManagementURL: (json["webManagementUrl"] as? String).flatMap { URL(string: $0) }
    )
  }
}

extension CustomerCenterConfiguration.Appearance {
  static func fromJson(_ json: [String: Any]) -> CustomerCenterConfiguration.Appearance {
    var accent: ColorPair?
    if let accentJson = json["accent"] as? [String: Any],
      let light = accentJson["light"] as? String,
      let dark = accentJson["dark"] as? String
    {
      accent = ColorPair(light: light, dark: dark)
    }
    return CustomerCenterConfiguration.Appearance(accent: accent)
  }
}

extension CustomerCenterAction {
  func toJson() -> [String: Any] {
    switch self {
    case .restore:
      return ["type": "restore"]
    case .manageSubscription:
      return ["type": "manageSubscription"]
    case .refund:
      return ["type": "refund"]
    case .changePlan:
      return ["type": "changePlan"]
    case .contactSupport:
      return ["type": "contactSupport"]
    case .url(let url):
      return ["type": "url", "url": url.absoluteString]
    case .custom(let identifier):
      return ["type": "custom", "identifier": identifier]
    }
  }
}

extension CustomerCenterRefundStatus {
  func toJson() -> String {
    switch self {
    case .success:
      return "success"
    case .userCancelled:
      return "userCancelled"
    case .error:
      return "error"
    @unknown default:
      return "error"
    }
  }
}

extension CustomerCenterScreenType {
  func toJson() -> String {
    switch self {
    case .management:
      return "management"
    case .noPurchases:
      return "noPurchases"
    }
  }
}

extension CustomerCenterPresentationStyle {
  func toJson() -> String {
    switch self {
    case .sheet:
      return "sheet"
    case .embedded:
      return "embedded"
    @unknown default:
      return "sheet"
    }
  }
}

extension CustomerCenterPurchase {
  func toJson() -> [String: Any] {
    var json: [String: Any] = [
      "store": store.toJson(),
      "entitlements": entitlements.map { $0.toJson() },
    ]
    // `productId` is always present in the JS type, as `null` when there's no product.
    if let productId = productId {
      json["productId"] = productId
    } else {
      json["productId"] = NSNull()
    }
    if let subscription = subscription {
      json["subscription"] = subscription.toJson()
    }
    if let nonSubscription = nonSubscription {
      json["nonSubscription"] = nonSubscription.toJson()
    }
    return json
  }
}
