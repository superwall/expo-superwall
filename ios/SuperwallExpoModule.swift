import ExpoModulesCore
import SuperwallKit

public class SuperwallExpoModule: Module {
  // private lazy var delegateBridge = SuperwallDelegateBridge { [weak self] name, body in
  //   self?.sendEvent(name, body)
  // }

  private let onSuperwallDidTrack = "onSuperwallDidTrack"
  private let onPaywallWillOpen = "onPaywallWillOpen"
  private let onPaywallDidOpen = "onPaywallDidOpen"
  private let onPaywallWillClose = "onPaywallWillClose"
  private let onPaywallDidClose = "onPaywallDidClose"
  private let onPaywallWillFailToOpen = "onPaywallWillFailToOpen"
  private let onPaywallDidFailToOpen = "onPaywallDidFailToOpen"
  private let onHandleCustomPaywallAction = "onHandleCustomPaywallAction"
  private let onWillPresentPaywall = "onWillPresentPaywall"
  private let onDidPresentPaywall = "onDidPresentPaywall"
  private let onWillDismissPaywall = "onWillDismissPaywall"
  private let onDidDismissPaywall = "onDidDismissPaywall"
  private let onHandleRestoration = "onHandleRestoration"
  private let onHandleLog = "onHandleLog"

  public func definition() -> ModuleDefinition {
    Name("SuperwallExpo")

    Events(
      onSuperwallDidTrack,
      onPaywallWillOpen,
      onPaywallDidOpen,
      onPaywallWillClose,
      onPaywallDidClose,
      onPaywallWillFailToOpen,
      onPaywallDidFailToOpen,
      onHandleCustomPaywallAction,
      onWillPresentPaywall,
      onDidPresentPaywall,
      onWillDismissPaywall,
      onDidDismissPaywall,
      onHandleRestoration,
      onHandleLog
    )

    Function("getApiKey") {
      return Bundle.main.object(forInfoDictionaryKey: "SUPERWALL_API_KEY") as? String
    }

    AsyncFunction("registerPlacement") {
      (
        placement: String,
        params: [String: Any]?,
        handlerId: String?
      ) -> String in
      sendEvent(
        onWillPresentPaywall,
        [
          "placement": placement,
          "params": params ?? [:],
          "handlerId": handlerId ?? "",
        ])
      return placement
    }
  }
}
