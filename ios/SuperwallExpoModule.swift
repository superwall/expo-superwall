import ExpoModulesCore
import SuperwallKit

public class SuperwallExpoModule: Module {
  public static var shared: SuperwallExpoModule?

  private let purchaseController = PurchaseControllerBridge.shared

  // Paywall Events
  private let onPaywallPresent = "onPaywallPresent"
  private let onPaywallDismiss = "onPaywallDismiss"
  private let onPaywallError = "onPaywallError"
  private let onPaywallSkip = "onPaywallSkip"

  // Purchase Events
  private let onPurchase = "onPurchase"
  private let onPurchaseRestore = "onPurchaseRestore"

  // Corrected initializer
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
      onPurchase,
      onPurchaseRestore
    )

    Function("getApiKey") {
      return Bundle.main.object(forInfoDictionaryKey: "SUPERWALL_API_KEY")
        as? String
    }

    Function("identify") {
      (userId: String, options: [String: Any]?) in
      let options = IdentityOptions.fromJson(options)
      Superwall.shared.identify(userId: userId, options: options)
    }

    Function("reset") {
      Superwall.shared.reset()
    }

    // Function("setDelegate") {
    //   (isUndefined: Bool) in
    //   self.delegate = isUndefined ? nil : SuperwallDelegateBridge()
    //   Superwall.shared.delegate = self.delegate
    // }

    AsyncFunction("configure") {
      (
        apiKey: String,
        options: [String: Any]?,
        usingPurchaseController: Bool,
        sdkVersion: String,
        promise: Promise
      ) in

      var superwallOptions: SuperwallOptions?

      if let options = options {
        superwallOptions = SuperwallOptions.fromJson(options)
      }

      print("SuperwallExpoModule.configure()")
      print("apiKey: \(apiKey)")

      Superwall.configure(
        apiKey: apiKey,
        purchaseController: usingPurchaseController ? purchaseController : nil,
        options: superwallOptions
      ) {
        promise.resolve(nil)
      }

      Superwall.shared.setPlatformWrapper("React Native", version: sdkVersion)
    }

    AsyncFunction("getConfigurationStatus") { (promise: Promise) in
      do {
        let configurationStatus = try Superwall.shared.configurationStatus.toString()
        promise.resolve(configurationStatus)
      } catch {
        promise.reject(error)
      }
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
      }

      Superwall.shared.register(
        placement: placement,
        params: params,
        handler: handler
      ) {
        promise.resolve(nil)
      }
    }

    AsyncFunction("getAssignments") {
      (promise: Promise) in
      do {
        let assignments = try Superwall.shared.getAssignments()
        promise.resolve(assignments.map { $0.toJson() })
      } catch {
        promise.reject(error)
      }
    }

    AsyncFunction("getEntitlements") {
      (promise: Promise) in
      do {
        let entitlements = try Superwall.shared.entitlements.toJson()
        promise.resolve(entitlements)
      } catch {
        promise.reject(error)
      }
    }

    AsyncFunction("getSubscriptionStatus") {
      (promise: Promise) in
      let subscriptionStatus = Superwall.shared.subscriptionStatus
      promise.resolve(subscriptionStatus)
    }

    Function("setSubscriptionStatus") {
      (status: [String: Any]) in
      let statusString = (status["status"] as? String)?.uppercased() ?? "UNKNOWN"
      let subscriptionStatus: SubscriptionStatus

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
    }
  }
}
