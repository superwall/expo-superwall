import Combine
import Foundation
import SuperwallKit

/// Custom callbacks from embedded paywalls waiting on `didHandleCustomCallback`, keyed by
/// callback ID. Paywalls presented with `registerPlacement` keep theirs on the module.
final class PendingCustomCallbacks {
  static let shared = PendingCustomCallbacks()

  private let lock = NSLock()
  private var continuations: [String: CheckedContinuation<CustomCallbackResult, Never>] = [:]

  func add(_ continuation: CheckedContinuation<CustomCallbackResult, Never>, for callbackId: String) {
    lock.lock()
    continuations[callbackId] = continuation
    lock.unlock()
  }

  /// Answers a pending callback. Returns `false` for an unknown or already-answered ID.
  @discardableResult
  func resolve(callbackId: String, result: CustomCallbackResult) -> Bool {
    lock.lock()
    let continuation = continuations.removeValue(forKey: callbackId)
    lock.unlock()
    continuation?.resume(returning: result)
    return continuation != nil
  }
}

/// Forwards changes of `Superwall.shared.configurationStatus` to JS as
/// `configurationStatusDidChange`.
final class ConfigurationStatusObserver {
  static let shared = ConfigurationStatusObserver()

  private let lock = NSLock()
  private var cancellable: AnyCancellable?

  /// Call once `Superwall.configure` has returned. Calling it again restarts the observation,
  /// which sends the current status first.
  func start() {
    let cancellable = Superwall.shared.$configurationStatus
      .removeDuplicates()
      .sink { status in
        SuperwallExpoModule.emitEvent(
          "configurationStatusDidChange",
          ["status": status.toString()]
        )
      }
    lock.lock()
    self.cancellable = cancellable
    lock.unlock()
  }
}

extension PaywallPresentationStyle {
  /// Takes a style name, or `{ style, height, width, cornerRadius }` for a drawer or popup.
  static func fromJson(_ json: Any?) -> PaywallPresentationStyle? {
    let dictionary = json as? [String: Any]
    guard let style = (json as? String) ?? (dictionary?["style"] as? String) else {
      return nil
    }
    let height = (dictionary?["height"] as? NSNumber)?.doubleValue
    let width = (dictionary?["width"] as? NSNumber)?.doubleValue
    let cornerRadius = (dictionary?["cornerRadius"] as? NSNumber)?.doubleValue

    switch style {
    case "MODAL":
      return .modal
    case "FULLSCREEN":
      return .fullscreen
    case "NO_ANIMATION":
      return .fullscreenNoAnimation
    case "PUSH":
      return .push
    case "DRAWER":
      // The defaults are the ones the native SDK uses when none are given.
      return .drawer(height: height ?? 70, cornerRadius: cornerRadius ?? 15)
    case "POPUP":
      guard let height = height, let width = width else {
        return nil
      }
      return .popup(height: height, width: width, cornerRadius: cornerRadius ?? 15)
    case "NONE":
      return PaywallPresentationStyle.none
    default:
      return nil
    }
  }
}

extension PaywallOverrides {
  /// Builds overrides from `{ productsByName: { name: productId }, presentationStyle }`.
  /// Products the store doesn't return are left out, so the paywall keeps its own.
  static func fromJson(_ json: [String: Any]) async -> PaywallOverrides {
    var productsByName: [String: StoreProduct] = [:]
    if let productIdsByName = json["productsByName"] as? [String: String], !productIdsByName.isEmpty {
      let products = await Superwall.shared.products(for: Set(productIdsByName.values))
      for (name, productId) in productIdsByName {
        if let product = products.first(where: { $0.productIdentifier == productId }) {
          productsByName[name] = product
        }
      }
    }
    return PaywallOverrides(
      productsByName: productsByName,
      presentationStyleOverride: PaywallPresentationStyle.fromJson(json["presentationStyle"])
        ?? PaywallPresentationStyle.none
    )
  }
}

extension PaywallLoadingState {
  func toJson() -> String {
    switch self {
    case .unknown:
      return "unknown"
    case .loadingPurchase:
      return "loadingPurchase"
    case .loadingURL:
      return "loadingURL"
    case .manualLoading:
      return "manualLoading"
    case .ready:
      return "ready"
    @unknown default:
      return "unknown"
    }
  }
}
