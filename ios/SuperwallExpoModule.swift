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

      Superwall.configure(
        apiKey: apiKey,
        purchaseController: usingPurchaseController ? purchaseController : nil,
        options: superwallOptions
      ) {
        promise.resolve(nil)
      }

      Superwall.shared.setPlatformWrapper("React Native", version: sdkVersion)
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
  }
}
