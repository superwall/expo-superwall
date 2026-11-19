import SuperwallKit

extension SuperwallOptions {
  // Every key is optional: one that's missing or has the wrong type keeps the native default,
  // so a partial dictionary is never discarded as a whole.
  static func fromJson(_ dictionary: [String: Any]) -> SuperwallOptions {
    let superwallOptions = SuperwallOptions()

    if let paywallsValue = dictionary["paywalls"] as? [String: Any] {
      superwallOptions.paywalls = PaywallOptions.fromJson(paywallsValue)
    }
    if let networkEnvironmentValue = dictionary["networkEnvironment"],
      let networkEnvironment = NetworkEnvironment.fromJson(networkEnvironmentValue) {
      superwallOptions.networkEnvironment = networkEnvironment
    }
    if let isExternalDataCollectionEnabled = dictionary["isExternalDataCollectionEnabled"] as? Bool {
      superwallOptions.isExternalDataCollectionEnabled = isExternalDataCollectionEnabled
    }
    if let localeIdentifier = dictionary["localeIdentifier"] as? String {
      superwallOptions.localeIdentifier = localeIdentifier
    }
    if let isGameControllerEnabled = dictionary["isGameControllerEnabled"] as? Bool {
      superwallOptions.isGameControllerEnabled = isGameControllerEnabled
    }
    if let loggingValue = dictionary["logging"] as? [String: Any] {
      superwallOptions.logging = Logging.fromJson(loggingValue)
    }
    if let storeKitVersion = dictionary["storeKitVersion"] as? String {
      superwallOptions.storeKitVersion = storeKitVersion == "STOREKIT1" ? .storeKit1 : .storeKit2
    }
    if let enableExperimentalDeviceVariables = dictionary["enableExperimentalDeviceVariables"] as? Bool {
      superwallOptions.enableExperimentalDeviceVariables = enableExperimentalDeviceVariables
    }
    if let shouldObservePurchases = dictionary["shouldObservePurchases"] as? Bool {
      superwallOptions.shouldObservePurchases = shouldObservePurchases
    }
    if let shouldBypassAppTransactionCheck = dictionary["shouldBypassAppTransactionCheck"] as? Bool {
      superwallOptions.shouldBypassAppTransactionCheck = shouldBypassAppTransactionCheck
    }
    if let maxConfigRetryCount = dictionary["maxConfigRetryCount"] as? Int {
      superwallOptions.maxConfigRetryCount = maxConfigRetryCount
    }
    switch dictionary["testModeBehavior"] as? String {
    case "automatic": superwallOptions.testModeBehavior = .automatic
    case "whenEnabledForUser": superwallOptions.testModeBehavior = .whenEnabledForUser
    case "never": superwallOptions.testModeBehavior = .never
    case "always": superwallOptions.testModeBehavior = .always
    default: break
    }

    if let eventTrackingBehaviorString = dictionary["eventTrackingBehavior"] as? String {
      switch eventTrackingBehaviorString {
      case "all": superwallOptions.eventTrackingBehavior = .all
      case "superwallOnly": superwallOptions.eventTrackingBehavior = .superwallOnly
      case "none": superwallOptions.eventTrackingBehavior = .none
      default: break
      }
    }

    if let customerCenterValue = dictionary["customerCenter"] as? [String: Any] {
      superwallOptions.customerCenter = CustomerCenterConfiguration.fromJson(customerCenterValue)
    }

    // Only assign when given: assigning, even the default, overrides a TCF banner's answer.
    if let adConsentValue = dictionary["adConsent"] as? [String: Any] {
      superwallOptions.adConsent = AdConsent.fromJson(adConsentValue)
    }

    if let localResourcesValue = dictionary["localResources"] as? [String: String] {
      superwallOptions.localResources = localResourcesValue.compactMapValues { value in
        if let url = URL(string: value), url.scheme != nil {
          return url
        }
        return URL(fileURLWithPath: value)
      }
    }

    // `true` finds the dev server on localhost; a string is the address `superwall dev` prints.
    if let devServerURL = dictionary["devServer"] as? String {
      if let url = URL(string: devServerURL), url.scheme != nil {
        superwallOptions.devServer = .url(url)
      }
    } else if dictionary["devServer"] as? Bool == true {
      superwallOptions.devServer = .default
    }

    return superwallOptions
  }
}

extension SuperwallOptions.NetworkEnvironment {
  /// Takes an environment name, or `{ "custom": "<domain or URL>" }` for a custom one.
  static func fromJson(_ json: Any) -> SuperwallOptions.NetworkEnvironment? {
    if let dictionary = json as? [String: Any] {
      // `domain` is the key the Android-style custom environment shares with this one.
      if let domain = (dictionary["custom"] ?? dictionary["domain"]) as? String, !domain.isEmpty {
        return .custom(domain)
      }
      return nil
    }
    switch json as? String {
    case "release":
      return .release
    case "releaseCandidate":
      return .releaseCandidate
    case "developer":
      return .developer
    case "local":
      return .local
    default:
      return nil
    }
  }
}

extension SuperwallOptions.Logging {
  // A missing or unknown level or scope list keeps the native default.
  static func fromJson(_ dictionary: [String: Any]) -> SuperwallOptions.Logging {
    let logging = SuperwallOptions.Logging()
    if let levelString = dictionary["level"] as? String,
      let level = LogLevel.fromJson(levelString) {
      logging.level = level
    }
    if let scopeStrings = dictionary["scopes"] as? [String] {
      logging.scopes = Set(scopeStrings.compactMap { LogScope.fromJson($0) })
    }
    return logging
  }
}

extension AdConsent {
  // Anything left out is granted, matching `AdConsent()`.
  static func fromJson(_ dictionary: [String: Any]) -> AdConsent {
    return AdConsent(
      adUserData: AdConsentStatus.fromJson(dictionary["adUserData"] as? String),
      adPersonalization: AdConsentStatus.fromJson(dictionary["adPersonalization"] as? String)
    )
  }
}

extension AdConsent {
  func toJson() -> [String: Any] {
    return [
      "adUserData": adUserData.toJson(),
      "adPersonalization": adPersonalization.toJson(),
    ]
  }
}

extension AdConsentStatus {
  static func fromJson(_ json: String?) -> AdConsentStatus {
    return json == "denied" ? .denied : .granted
  }

  func toJson() -> String {
    switch self {
    case .granted:
      return "granted"
    case .denied:
      return "denied"
    }
  }
}

extension EventTrackingBehavior {
  func toJson() -> String {
    switch self {
    case .all:
      return "all"
    case .superwallOnly:
      return "superwallOnly"
    case .none:
      return "none"
    }
  }
}
