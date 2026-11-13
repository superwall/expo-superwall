import ExpoModulesCore
import React
import SuperwallKit
import UIKit

/// A React Native host view for an embedded Customer Center.
///
/// The Customer Center is created in the SDK's `embedded` style, which expects to sit in a
/// navigation stack and pushes its own screens onto it. React Native layouts don't provide a
/// `UINavigationController` to push onto, so this host owns one and contains it as a child view
/// controller. Leaving the window discards it; coming back builds a new one.
final class SuperwallExpoCustomerCenterView: ExpoView {
  let onCustomerCenterError = EventDispatcher()

  private var configuration: [String: Any]?
  private var handlerId: String?
  private var asksBeforeRestoring = false
  private var navigationController: UINavigationController?
  // The view controller doesn't retain its delegate.
  private var delegateBridge: CustomerCenterDelegateBridge?

  required init(appContext: AppContext? = nil) {
    super.init(appContext: appContext)
    clipsToBounds = true
  }

  func setConfiguration(_ configuration: [String: Any]?) {
    self.configuration = configuration
  }

  func setHandlerId(_ handlerId: String?) {
    self.handlerId = handlerId
  }

  func setAsksBeforeRestoring(_ asksBeforeRestoring: Bool) {
    self.asksBeforeRestoring = asksBeforeRestoring
  }

  /// Builds the Customer Center once, with the props set at that point. Later prop changes
  /// don't rebuild it; remount the view to apply them.
  func loadCustomerCenterIfNeeded() {
    guard window != nil, navigationController == nil else {
      return
    }

    // `Superwall.shared` asserts when the SDK isn't configured.
    guard Superwall.isInitialized else {
      onCustomerCenterError([
        "message": "Superwall must be configured before a CustomerCenterView is shown."
      ])
      return
    }

    guard let parentViewController = reactViewController() else {
      onCustomerCenterError([
        "message": "CustomerCenterView couldn't find a view controller to show the Customer Center in."
      ])
      return
    }

    let delegateBridge = CustomerCenterDelegateBridge(
      handlerId: handlerId ?? "",
      asksBeforeRestoring: asksBeforeRestoring
    )
    let customerCenter = CustomerCenterViewController(
      configuration: configuration.map { CustomerCenterConfiguration.fromJson($0) },
      presentationStyle: .embedded,
      delegate: delegateBridge
    )
    let navigationController = UINavigationController(rootViewController: customerCenter)
    self.delegateBridge = delegateBridge
    self.navigationController = navigationController

    parentViewController.addChild(navigationController)

    let customerCenterView = navigationController.view!
    customerCenterView.translatesAutoresizingMaskIntoConstraints = false
    addSubview(customerCenterView)
    NSLayoutConstraint.activate([
      customerCenterView.topAnchor.constraint(equalTo: topAnchor),
      customerCenterView.bottomAnchor.constraint(equalTo: bottomAnchor),
      customerCenterView.leadingAnchor.constraint(equalTo: leadingAnchor),
      customerCenterView.trailingAnchor.constraint(equalTo: trailingAnchor),
    ])
    navigationController.didMove(toParent: parentViewController)
  }

  override func didMoveToWindow() {
    super.didMoveToWindow()

    if window == nil {
      detachCustomerCenter()
    } else {
      loadCustomerCenterIfNeeded()
    }
  }

  private func detachCustomerCenter() {
    if let navigationController {
      navigationController.willMove(toParent: nil)
      navigationController.view.removeFromSuperview()
      navigationController.removeFromParent()
    }
    navigationController = nil
    delegateBridge = nil
  }
}
