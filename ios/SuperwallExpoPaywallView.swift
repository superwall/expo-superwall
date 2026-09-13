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

  private var placement: String?
  private var params: [String: Any]?
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

  func loadPaywallIfNeeded() {
    guard window != nil, let placement, !placement.isEmpty else {
      return
    }

    let nextConfigurationKey = makeConfigurationKey(placement: placement, params: params)
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

    loadTask = Task { @MainActor [weak self] in
      guard let self else { return }

      do {
        let paywall = try await Superwall.shared.getPaywall(
          forPlacement: placement,
          params: self.params,
          delegate: self
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
  ) {}

  private func makeConfigurationKey(placement: String, params: [String: Any]?) -> String {
    guard let params,
      JSONSerialization.isValidJSONObject(params),
      let data = try? JSONSerialization.data(withJSONObject: params, options: [.sortedKeys]),
      let json = String(data: data, encoding: .utf8)
    else {
      return placement
    }
    return "\(placement):\(json)"
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
  }

  deinit {
    loadTask?.cancel()
  }
}
