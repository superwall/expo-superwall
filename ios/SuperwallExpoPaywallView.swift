import ExpoModulesCore
import React
import SuperwallKit

/// A React Native host view for a paywall retrieved through Superwall's `getPaywall` API.
///
/// Each host owns exactly one PaywallViewController. Changing the placement/params or
/// removing the host from the hierarchy discards that controller; native paywall
/// controllers must never be reused in another view hierarchy.
final class SuperwallExpoPaywallView: ExpoView, PaywallViewControllerDelegate {
  let onPaywallLoadStart = EventDispatcher()
  let onPaywallPresent = EventDispatcher()
  let onPaywallDismiss = EventDispatcher()
  let onPaywallSkip = EventDispatcher()
  let onPaywallError = EventDispatcher()
  let onPaywallCustomCallback = EventDispatcher()
  let onPaywallLoadingStateChange = EventDispatcher()

  private var placement: String?
  private var params: [String: Any]?
  private var paywallOverrides: [String: Any]?
  // Custom callbacks sent to JS that it hasn't answered yet.
  private var pendingCustomCallbackIds = Set<String>()
  private var configurationKey: String?
  private var loadGeneration = 0
  private var loadTask: Task<Void, Never>?
  private var paywallViewController: PaywallViewController?

  required init(appContext: AppContext? = nil) {
    super.init(appContext: appContext)
    clipsToBounds = true
  }

  func setPlacement(_ placement: String) {
    self.placement = placement
  }

  func setParams(_ params: [String: Any]?) {
    self.params = params
  }

  func setPaywallOverrides(_ paywallOverrides: [String: Any]?) {
    self.paywallOverrides = paywallOverrides
  }

  func loadPaywallIfNeeded() {
    guard window != nil, let placement, !placement.isEmpty else {
      return
    }

    let nextConfigurationKey = makeConfigurationKey(
      placement: placement,
      params: params,
      paywallOverrides: paywallOverrides
    )
    guard nextConfigurationKey != configurationKey else {
      attachPaywallViewControllerIfNeeded()
      return
    }

    configurationKey = nextConfigurationKey
    loadGeneration += 1
    let generation = loadGeneration
    loadTask?.cancel()
    detachPaywallViewController()
    onPaywallLoadStart([:])

    let params = self.params
    let paywallOverridesJson = self.paywallOverrides

    loadTask = Task { @MainActor [weak self] in
      guard let self else { return }

      do {
        // Overriding products means fetching them from the store first.
        var paywallOverrides: PaywallOverrides?
        if let paywallOverridesJson {
          paywallOverrides = await PaywallOverrides.fromJson(paywallOverridesJson)
          guard !Task.isCancelled, generation == self.loadGeneration else { return }
        }

        let paywall = try await Superwall.shared.getPaywall(
          forPlacement: placement,
          params: params,
          paywallOverrides: paywallOverrides,
          delegate: self,
          onCustomCallback: { [weak self] callback in
            return await self?.handleCustomCallback(callback) ?? .failure()
          }
        )

        guard !Task.isCancelled,
          generation == self.loadGeneration,
          self.window != nil
        else {
          return
        }

        guard paywall.parent == nil,
          paywall.presentingViewController == nil,
          paywall.viewIfLoaded?.superview == nil
        else {
          // Allow a later prop update or reattach to retry once the other host lets go.
          self.configurationKey = nil
          self.onPaywallError([
            "message":
              "The retrieved paywall is already attached to another view. Use a different placement for each mounted PaywallView."
          ])
          return
        }

        self.paywallViewController = paywall
        self.attachPaywallViewControllerIfNeeded()
        self.onPaywallPresent(["paywallInfo": paywall.info.toJson()])
      } catch let reason as PaywallSkippedReason {
        guard !Task.isCancelled, generation == self.loadGeneration else { return }
        self.onPaywallSkip(["reason": reason.toJson()])
      } catch is CancellationError {
        // A new prop set or unmount invalidated this request.
      } catch {
        guard !Task.isCancelled, generation == self.loadGeneration else { return }
        // A failed load is not a settled decision (unlike a skip): clear the key so
        // the same configuration can retry on the next prop update or reattach.
        self.configurationKey = nil
        self.onPaywallError(["message": error.localizedDescription])
      }
    }
  }

  override func didMoveToWindow() {
    super.didMoveToWindow()

    if window == nil {
      invalidateAndDetach()
    } else {
      loadPaywallIfNeeded()
    }
  }

  func paywall(
    _ paywall: PaywallViewController,
    didFinishWith result: PaywallResult,
    shouldDismiss: Bool
  ) {
    onPaywallDismiss([
      "paywallInfo": paywall.info.toJson(),
      "result": result.toJson(),
      "shouldDismiss": shouldDismiss,
    ])

    if shouldDismiss {
      detachPaywallViewController()
    }
  }

  func paywall(
    _ paywall: PaywallViewController,
    loadingStateDidChange loadingState: PaywallLoadingState
  ) {
    // Not limited to the attached paywall: the state can change while the paywall is still
    // being retrieved.
    guard window != nil else {
      return
    }
    onPaywallLoadingStateChange(["state": loadingState.toJson()])
  }

  /// Sends a custom callback from the paywall to JS and waits for
  /// `didHandleCustomCallback`. Fails straight away once the host has left the window, as
  /// nothing is left to answer.
  @MainActor
  private func handleCustomCallback(_ callback: CustomCallback) async -> CustomCallbackResult {
    guard window != nil else {
      return .failure()
    }
    let callbackId = UUID().uuidString
    var data: [String: Any] = [
      "callbackId": callbackId,
      "name": callback.name,
    ]
    if let variables = callback.variables {
      data["variables"] = variables
    }
    return await withCheckedContinuation { continuation in
      PendingCustomCallbacks.shared.add(continuation, for: callbackId)
      pendingCustomCallbackIds.insert(callbackId)
      onPaywallCustomCallback(data)
    }
  }

  /// Answers the callbacks JS hasn't answered, so the paywall doesn't wait on a host that's gone.
  private func failPendingCustomCallbacks() {
    for callbackId in pendingCustomCallbackIds {
      PendingCustomCallbacks.shared.resolve(callbackId: callbackId, result: .failure())
    }
    pendingCustomCallbackIds.removeAll()
  }

  private func makeConfigurationKey(
    placement: String,
    params: [String: Any]?,
    paywallOverrides: [String: Any]?
  ) -> String {
    return [params, paywallOverrides].reduce(placement) { key, value in
      guard let value,
        JSONSerialization.isValidJSONObject(value),
        let data = try? JSONSerialization.data(withJSONObject: value, options: [.sortedKeys]),
        let json = String(data: data, encoding: .utf8)
      else {
        return "\(key):"
      }
      return "\(key):\(json)"
    }
  }

  private func attachPaywallViewControllerIfNeeded() {
    guard let paywallViewController,
      paywallViewController.view.superview !== self,
      let parentViewController = reactViewController()
    else {
      return
    }

    parentViewController.addChild(paywallViewController)

    let paywallView = paywallViewController.view!
    paywallView.translatesAutoresizingMaskIntoConstraints = false
    addSubview(paywallView)
    NSLayoutConstraint.activate([
      paywallView.topAnchor.constraint(equalTo: topAnchor),
      paywallView.bottomAnchor.constraint(equalTo: bottomAnchor),
      paywallView.leadingAnchor.constraint(equalTo: leadingAnchor),
      paywallView.trailingAnchor.constraint(equalTo: trailingAnchor),
    ])
    paywallViewController.didMove(toParent: parentViewController)
  }

  private func detachPaywallViewController() {
    if let paywallViewController {
      paywallViewController.willMove(toParent: nil)
      paywallViewController.view.removeFromSuperview()
      paywallViewController.removeFromParent()
    }
    self.paywallViewController = nil
  }

  private func invalidateAndDetach() {
    loadGeneration += 1
    loadTask?.cancel()
    loadTask = nil
    configurationKey = nil
    detachPaywallViewController()
    failPendingCustomCallbacks()
  }

  deinit {
    loadTask?.cancel()
    for callbackId in pendingCustomCallbackIds {
      PendingCustomCallbacks.shared.resolve(callbackId: callbackId, result: .failure())
    }
  }
}
