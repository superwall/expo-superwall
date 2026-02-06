import ExpoModulesCore
import SuperwallKit

public class SuperwallExpoModule: Module {
  public static var shared: SuperwallExpoModule?

  private let purchaseController = PurchaseControllerBridge.shared
  private var delegate: SuperwallDelegateBridge?

  // Paywall Events
  private let onPaywallPresent = "onPaywallPresent"
  private let onPaywallDismiss = "onPaywallDismiss"
  private let onPaywallError = "onPaywallError"
  private let onPaywallSkip = "onPaywallSkip"
  private let onCustomCallback = "onCustomCallback"

  // Custom callback continuations keyed by callbackId
  private let callbackLock = NSLock()
  private var customCallbackContinuations: [String: CheckedContinuation<CustomCallbackResult, Never>] = [:]

  // Purchase Events
  private let onPurchase = "onPurchase"
  private let onPurchaseRestore = "onPurchaseRestore"

  // Legacy Events
  private let subscriptionStatusDidChange = "subscriptionStatusDidChange"
  private let handleSuperwallEvent = "handleSuperwallEvent"
  private let handleCustomPaywallAction = "handleCustomPaywallAction"
  private let willDismissPaywall = "willDismissPaywall"
  private let willPresentPaywall = "willPresentPaywall"
  private let didDismissPaywall = "didDismissPaywall"
  private let didPresentPaywall = "didPresentPaywall"
  private let paywallWillOpenURL = "paywallWillOpenURL"
  private let paywallWillOpenDeepLink = "paywallWillOpenDeepLink"
  private let handleLog = "handleLog"
  private let willRedeemLink = "willRedeemLink"
  private let didRedeemLink = "didRedeemLink"

  public required init(appContext: AppContext) {
    super.init(appContext: appContext)
    SuperwallExpoModule.shared = self
  }

  public static func emitEvent(_ name: String, _ data: [String: Any]?) {
    SuperwallExpoModule.shared?.sendEvent(name, data ?? [:])
  }

  public func definition() -> ModuleDefinition {
    Name("SuperwallExpo")

    Events(
      onPaywallPresent,
      onPaywallDismiss,
      onPaywallError,
      onPaywallSkip,
      onCustomCallback,
      onPurchase,
      onPurchaseRestore,

      // Legacy events
      subscriptionStatusDidChange,
      handleSuperwallEvent,
      handleCustomPaywallAction,
      willDismissPaywall,
      willPresentPaywall,
      didDismissPaywall,
      didPresentPaywall,
      paywallWillOpenURL,
      paywallWillOpenDeepLink,
      handleLog,
      willRedeemLink,
      didRedeemLink
    )

    AsyncFunction("identify") { (userId: String, options: [String: Any]?, promise: Promise) in
      DispatchQueue.main.async {
        let identityOptions = IdentityOptions.fromJson(options)
        Superwall.shared.identify(userId: userId, options: identityOptions)
        promise.resolve(nil)
      }
    }

    AsyncFunction("reset") { (promise: Promise) in
      DispatchQueue.main.async {
        Superwall.shared.reset()
        promise.resolve(nil)
      }
    }

    AsyncFunction("configure") {
      (
        apiKey: String,
        options: [String: Any]?,
        usingPurchaseController: Bool,
        sdkVersion: String?,
        promise: Promise
      ) in

      var superwallOptions: SuperwallOptions?

      if let options = options {
        superwallOptions = SuperwallOptions.fromJson(options)
      }

      Superwall.configure(
        apiKey: apiKey,
        purchaseController: usingPurchaseController ? purchaseController : nil,
        options: superwallOptions,
        completion: {
          self.delegate = SuperwallDelegateBridge()
          Superwall.shared.delegate = self.delegate

          Superwall.shared.setPlatformWrapper("Expo", version: sdkVersion ?? "0.0.0")

          promise.resolve(nil)
        }
      )
    }

    AsyncFunction("getConfigurationStatus") { (promise: Promise) in
      let configurationStatus = Superwall.shared.configurationStatus.toString()
      promise.resolve(configurationStatus)
    }

    AsyncFunction("registerPlacement") {
      (
        placement: String,
        params: [String: Any]?,
        handlerId: String?,
        promise: Promise
      ) in
      var handler: PaywallPresentationHandler?

      if let handlerId = handlerId {
        handler = PaywallPresentationHandler()

        handler?.onPresent { [weak self] paywallInfo in
          guard let self = self else { return }
          let data =
            [
              "paywallInfoJson": paywallInfo.toJson(),
              "handlerId": handlerId,
            ] as [String: Any]
          self.sendEvent(self.onPaywallPresent, data)
        }

        handler?.onDismiss { [weak self] paywallInfo, result in
          guard let self = self else { return }
          let data =
            [
              "paywallInfoJson": paywallInfo.toJson(),
              "result": result.toJson(),
              "handlerId": handlerId,
            ] as [String: Any]
          self.sendEvent(self.onPaywallDismiss, data)
        }

        handler?.onError { [weak self] error in
          guard let self = self else { return }
          let data =
            [
              "errorString": error.localizedDescription,
              "handlerId": handlerId,
            ] as [String: Any]
          self.sendEvent(self.onPaywallError, data)
        }

        handler?.onSkip { [weak self] reason in
          guard let self = self else { return }
          let data =
            [
              "skippedReason": reason.toJson(),
              "handlerId": handlerId,
            ] as [String: Any]
          self.sendEvent(self.onPaywallSkip, data)
        }

        handler?.onCustomCallback { [weak self] callback async -> CustomCallbackResult in
          guard let self = self else { return .failure() }
          let callbackId = UUID().uuidString
          let data: [String: Any] = [
            "callbackId": callbackId,
            "name": callback.name,
            "variables": callback.variables as Any,
            "handlerId": handlerId,
          ]
          return await withCheckedContinuation { continuation in
            self.callbackLock.lock()
            self.customCallbackContinuations[callbackId] = continuation
            self.callbackLock.unlock()
            self.sendEvent(self.onCustomCallback, data)
          }
        }
      }

      Superwall.shared.register(
        placement: placement,
        params: params,
        handler: handler
      ) {
        promise.resolve(nil)
      }
    }

    AsyncFunction("getAssignments") { (promise: Promise) in
      do {
        let assignments = try Superwall.shared.getAssignments()
        promise.resolve(assignments.map { $0.toJson() })
      } catch {
        promise.reject(error)
      }
    }

    AsyncFunction("getEntitlements") { (promise: Promise) in
      do {
        let entitlements = try Superwall.shared.entitlements.toJson()
        promise.resolve(entitlements)
      } catch {
        promise.reject(error)
      }
    }

    AsyncFunction("getSubscriptionStatus") { (promise: Promise) in
      print("Getting subscription status")
      let subscriptionStatus = Superwall.shared.subscriptionStatus
      promise.resolve(subscriptionStatus.toJson())
    }

    AsyncFunction("setSubscriptionStatus") { (status: [String: Any], promise: Promise) in
      DispatchQueue.main.async {
        let statusString = (status["status"] as? String)?.uppercased() ?? "UNKNOWN"
        let subscriptionStatus: SubscriptionStatus

        print("Setting subscription status to: \(statusString)")

        switch statusString {
        case "UNKNOWN":
          subscriptionStatus = .unknown
        case "INACTIVE":
          subscriptionStatus = .inactive
        case "ACTIVE":
          if let entitlementsArray = status["entitlements"] as? [[String: Any]] {
            let entitlementsSet: Set<Entitlement> = Set(
              entitlementsArray.compactMap { item in
                if let id = item["id"] as? String {
                  return Entitlement(id: id)
                }
                return nil
              }
            )
            subscriptionStatus = .active(entitlementsSet)
          } else {
            subscriptionStatus = .inactive
          }
        default:
          subscriptionStatus = .unknown
        }

        Superwall.shared.subscriptionStatus = subscriptionStatus
        promise.resolve(nil)
      }
    }

    Function("setInterfaceStyle") { (style: String?) in
      var interfaceStyle: InterfaceStyle?
      if let style = style {
        interfaceStyle = InterfaceStyle.fromString(style: style)
      }
      Superwall.shared.setInterfaceStyle(to: interfaceStyle)
    }

    AsyncFunction("getUserAttributes") { (promise: Promise) in
      let attributes = Superwall.shared.userAttributes
      promise.resolve(attributes)
    }

    AsyncFunction("getDeviceAttributes") {
      return await Superwall.shared.getDeviceAttributes()
    }

    AsyncFunction("setUserAttributes") { (userAttributes: [String: Any], promise: Promise) in
      DispatchQueue.main.async {
        Superwall.shared.setUserAttributes(userAttributes)
        promise.resolve(nil)
      }
    }

    AsyncFunction("handleDeepLink") { (url: String, promise: Promise) in
      guard let url = URL(string: url) else {
        promise.resolve(false)
        return
      }
      let result = Superwall.shared.handleDeepLink(url)
      promise.resolve(result)
    }

    Function("didPurchase") { (result: [String: Any]) in
      guard let purchaseResult = PurchaseResult.fromJson(result) else {
        return
      }
      purchaseController.purchaseCompletion?(purchaseResult)
    }

    Function("didRestore") { (result: [String: Any]) in
      guard let restorationResult = RestorationResult.fromJson(result) else {
        return
      }
      purchaseController.restoreCompletion?(restorationResult)
    }

    AsyncFunction("didHandleCustomCallback") { (callbackId: String, status: String, data: [String: Any]?, promise: Promise) in
      self.callbackLock.lock()
      let continuation = self.customCallbackContinuations.removeValue(forKey: callbackId)
      self.callbackLock.unlock()
      guard let continuation = continuation else {
        promise.resolve(nil)
        return
      }
      let result: CustomCallbackResult
      if status == "success" {
        result = .success(data: data)
      } else {
        result = .failure(data: data)
      }
      continuation.resume(returning: result)
      promise.resolve(nil)
    }

    AsyncFunction("dismiss") { (promise: Promise) in
      Superwall.shared.dismiss {
        promise.resolve(nil)
      }
    }

    AsyncFunction("confirmAllAssignments") { (promise: Promise) in
      Superwall.shared.confirmAllAssignments { assignments in
        promise.resolve(assignments.map { $0.toJson() })
      }
    }

    AsyncFunction("getPresentationResult") {
      (placement: String, params: [String: Any]?, promise: Promise) in
      Superwall.shared.getPresentationResult(forPlacement: placement, params: params) { result in
        promise.resolve(result.toJson())
      }
    }

    Function("preloadPaywalls") { (placementNames: [String]) in
      Superwall.shared.preloadPaywalls(forPlacements: Set(placementNames))
    }

    Function("preloadAllPaywalls") {
      Superwall.shared.preloadAllPaywalls()
    }

    Function("setLogLevel") { (level: String) in
      let logLevel = LogLevel.fromJson(level)
      if let logLevel = logLevel {
        Superwall.shared.logLevel = logLevel
      }
    }

    AsyncFunction("setIntegrationAttributes") { (attributes: [String: String], promise: Promise) in
      var converted: [IntegrationAttribute: String] = [:]

      for (key, value) in attributes {
        if let attribute = IntegrationAttribute.fromString(key) {
          converted[attribute] = value
        }
      }

      Superwall.shared.setIntegrationAttributes(converted)
      promise.resolve(nil)
    }

    AsyncFunction("getIntegrationAttributes") { (promise: Promise) in
      let attributes = Superwall.shared.integrationAttributes
      promise.resolve(attributes)
    }
  }
}
