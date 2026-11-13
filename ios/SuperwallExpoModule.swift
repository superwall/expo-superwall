import ExpoModulesCore
import SuperwallKit
import UIKit
import UserNotifications

/// Thread-safe boolean flag that can only be set once.
private final class AtomicFlag {
  private var _value = false
  private let lock = NSLock()

  /// Sets the flag and returns the previous value.
  /// Returns `false` on the first call, `true` on subsequent calls.
  func testAndSet() -> Bool {
    lock.lock()
    defer { lock.unlock() }
    let prev = _value
    _value = true
    return prev
  }
}

public class SuperwallExpoModule: Module {
  public static var shared: SuperwallExpoModule?
  private static let instancesLock = NSLock()
  private static let instances = NSHashTable<SuperwallExpoModule>.weakObjects()

  private let purchaseController = PurchaseControllerBridge.shared
  private var delegate: SuperwallDelegateBridge?

  // Paywall Events
  private let onPaywallPresent = "onPaywallPresent"
  private let onPaywallDismiss = "onPaywallDismiss"
  private let onPaywallWillDismiss = "onPaywallWillDismiss"
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
  private let userAttributesDidChange = "userAttributesDidChange"
  private let handleSuperwallDeepLink = "handleSuperwallDeepLink"

  // Customer Info Events
  private let customerInfoDidChange = "customerInfoDidChange"

  // Configuration Events
  private let configurationStatusDidChange = "configurationStatusDidChange"

  // Customer Center Events
  private let onCustomerCenterAction = "onCustomerCenterAction"
  private let onCustomerCenterSurveyComplete = "onCustomerCenterSurveyComplete"
  private let onCustomerCenterRefundRequestComplete = "onCustomerCenterRefundRequestComplete"
  private let onCustomerCenterShouldRestorePurchases = "onCustomerCenterShouldRestorePurchases"

  // Whether a Customer Center presented through this module is on screen. Only touched on
  // the main actor.
  private var isCustomerCenterPresented = false

  public required init(appContext: AppContext) {
    super.init(appContext: appContext)
    SuperwallExpoModule.shared = self
    SuperwallExpoModule.instancesLock.lock()
    SuperwallExpoModule.instances.add(self)
    SuperwallExpoModule.instancesLock.unlock()
  }

  // Events must reach every live module instance. More than one app context can
  // exist at once (e.g. expo-dev-client's launcher plus the app), and `shared`
  // may point at an instance whose JS runtime never subscribed - sending only
  // through it silently drops events like `onPurchase`, leaving the native
  // purchase continuation suspended forever.
  public static func emitEvent(_ name: String, _ data: [String: Any]?) {
    instancesLock.lock()
    let liveInstances = instances.allObjects
    instancesLock.unlock()
    for instance in liveInstances {
      instance.sendEvent(name, data ?? [:])
    }
  }

  public func definition() -> ModuleDefinition {
    Name("SuperwallExpo")

    Events(
      onPaywallPresent,
      onPaywallDismiss,
      onPaywallWillDismiss,
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
      didRedeemLink,
      userAttributesDidChange,
      handleSuperwallDeepLink,

      // Customer info events
      customerInfoDidChange,

      // Configuration events
      configurationStatusDidChange,

      // Customer Center events
      onCustomerCenterAction,
      onCustomerCenterSurveyComplete,
      onCustomerCenterRefundRequestComplete,
      onCustomerCenterShouldRestorePurchases
    )

    View(SuperwallExpoPaywallView.self) {
      ViewName("PaywallView")
      Events(
        "onPaywallLoadStart",
        "onPaywallPresent",
        "onPaywallDismiss",
        "onPaywallSkip",
        "onPaywallError",
        "onPaywallCustomCallback",
        "onPaywallLoadingStateChange"
      )
      Prop("placement") { (view: SuperwallExpoPaywallView, placement: String) in
        view.setPlacement(placement)
      }
      Prop("params") { (view: SuperwallExpoPaywallView, params: [String: Any]?) in
        view.setParams(params)
      }
      Prop("paywallOverrides") { (view: SuperwallExpoPaywallView, overrides: [String: Any]?) in
        view.setPaywallOverrides(overrides)
      }
      OnViewDidUpdateProps { (view: SuperwallExpoPaywallView) in
        view.loadPaywallIfNeeded()
      }
    }

    View(SuperwallExpoCustomerCenterView.self) {
      ViewName("CustomerCenterView")
      Events("onCustomerCenterError")
      Prop("configuration") { (view: SuperwallExpoCustomerCenterView, configuration: [String: Any]?) in
        view.setConfiguration(configuration)
      }
      Prop("handlerId") { (view: SuperwallExpoCustomerCenterView, handlerId: String?) in
        view.setHandlerId(handlerId)
      }
      Prop("asksBeforeRestoring") { (view: SuperwallExpoCustomerCenterView, asks: Bool?) in
        view.setAsksBeforeRestoring(asks ?? false)
      }
      OnViewDidUpdateProps { (view: SuperwallExpoCustomerCenterView) in
        view.loadCustomerCenterIfNeeded()
      }
    }

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

      // A key that's missing or malformed keeps its native default.
      let superwallOptions = options.map { SuperwallOptions.fromJson($0) }

      let promiseSettled = AtomicFlag()

      Superwall.configure(
        apiKey: apiKey,
        purchaseController: usingPurchaseController ? purchaseController : nil,
        options: superwallOptions,
        completion: {
          self.delegate = SuperwallDelegateBridge()
          Superwall.shared.delegate = self.delegate

          Superwall.shared.setPlatformWrapper("Expo", version: sdkVersion ?? "0.0.0")

          if promiseSettled.testAndSet() == false {
            promise.resolve(nil)
          }
        }
      )

      // `Superwall.shared` exists as soon as `configure` returns, before it has finished.
      ConfigurationStatusObserver.shared.start()
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

        handler?.onWillDismiss { [weak self] paywallInfo, result in
          guard let self = self else { return }
          let data =
            [
              "paywallInfoJson": paywallInfo.toJson(),
              "result": result.toJson(),
              "handlerId": handlerId,
            ] as [String: Any]
          self.sendEvent(self.onPaywallWillDismiss, data)
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

    AsyncFunction("getEntitlementsByProductIds") { (productIds: [String], promise: Promise) in
      let entitlements = Superwall.shared.entitlements.byProductIds(Set(productIds))
      promise.resolve(entitlements.map { $0.toJson() })
    }

    AsyncFunction("getCustomerInfo") { (promise: Promise) in
      Task {
        let customerInfo = await Superwall.shared.getCustomerInfo()
        promise.resolve(customerInfo.toJson())
      }
    }

    AsyncFunction("refreshCustomerInfo") { (promise: Promise) in
      Task {
        #if SUPERWALL_SPM
        let customerInfo = await Superwall.shared.refreshCustomerInfo()
        #else
        // CocoaPods builds use SuperwallKit 4.18.3, which can't force a refresh, so this
        // returns the customer info the SDK already has.
        let customerInfo = await Superwall.shared.getCustomerInfo()
        #endif
        promise.resolve(customerInfo.toJson())
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

    AsyncFunction("setUserAttributes") { (userAttributes: [String: Any?], promise: Promise) in
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
      let result: CustomCallbackResult
      if status == "success" {
        result = .success(data: data)
      } else {
        result = .failure(data: data)
      }
      if let continuation = continuation {
        continuation.resume(returning: result)
      } else {
        // Not from `registerPlacement`, so it may belong to an embedded paywall.
        PendingCustomCallbacks.shared.resolve(callbackId: callbackId, result: result)
      }
      promise.resolve(nil)
    }

    AsyncFunction("restorePurchases") { (promise: Promise) in
      Task {
        let result = await Superwall.shared.restorePurchases()
        promise.resolve(result.toJson())
      }
    }

    AsyncFunction("dismiss") { (promise: Promise) in
      Superwall.shared.dismiss {
        promise.resolve(nil)
      }
    }

    Function("togglePaywallSpinner") { (isHidden: Bool) in
      Superwall.shared.togglePaywallSpinner(isHidden: isHidden)
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

    Function("setEventTrackingBehavior") { (behavior: String) in
      switch behavior {
      case "all": Superwall.shared.eventTrackingBehavior = .all
      case "superwallOnly": Superwall.shared.eventTrackingBehavior = .superwallOnly
      case "none": Superwall.shared.eventTrackingBehavior = .none
      default: break
      }
    }

    Function("setLocaleIdentifier") { (localeIdentifier: String?) in
      Superwall.shared.localeIdentifier = localeIdentifier
    }

    AsyncFunction("setIntegrationAttributes") { (attributes: [String: Any?], promise: Promise) in
      // A `nil` value removes the attribute.
      var converted: [IntegrationAttribute: String?] = [:]

      for (key, value) in attributes {
        if let attribute = IntegrationAttribute.fromString(key) {
          converted[attribute] = .some(value as? String)
        }
      }

      Superwall.shared.setIntegrationAttributes(converted)
      promise.resolve(nil)
    }

    AsyncFunction("setIntegrationAttribute") { (attribute: String, value: String?, promise: Promise) in
      // A `nil` value removes the attribute.
      if let attribute = IntegrationAttribute.fromString(attribute) {
        Superwall.shared.setIntegrationAttribute(attribute, value)
      }
      promise.resolve(nil)
    }

    AsyncFunction("getIntegrationAttributes") { (promise: Promise) in
      let attributes = Superwall.shared.integrationAttributes
      promise.resolve(attributes)
    }

    AsyncFunction("consume") { (purchaseToken: String, promise: Promise) in
      // Consumables are finished by StoreKit on iOS, so there is nothing to consume.
      promise.resolve(purchaseToken)
    }

    AsyncFunction("purchase") { (productId: String, promise: Promise) in
      Task {
        let products = await Superwall.shared.products(for: [productId])
        guard let storeProduct = products.first(where: { $0.productIdentifier == productId }) else {
          promise.resolve(
            ["type": "failed", "error": "Product not found for identifier: \(productId)"]
          )
          return
        }
        let result = await Superwall.shared.purchase(storeProduct)
        promise.resolve(result.toJson())
      }
    }

    AsyncFunction("products") { (productIds: [String], promise: Promise) in
      Task {
        let products = await Superwall.shared.products(for: Set(productIds))
        let productsById = Dictionary(
          products.map { ($0.productIdentifier, $0) },
          uniquingKeysWith: { first, _ in first }
        )
        // Preserve the requested order and omit identifiers the store didn't return.
        var seen = Set<String>()
        let result = productIds
          .filter { seen.insert($0).inserted }
          .compactMap { productsById[$0]?.toJson() }
        promise.resolve(result)
      }
    }

    AsyncFunction("queryInAppPurchases") { (promise: Promise) in
      promise.reject(
        "ERR_UNSUPPORTED",
        "queryInAppPurchases is only available on Android."
      )
    }

    AsyncFunction("presentCustomerCenter") {
      (configuration: [String: Any]?, handlerId: String, asksBeforeRestoring: Bool, promise: Promise) in
      Task { @MainActor in
        // The native SDK ignores a second presentation without calling `onDismiss`, so
        // settle straight away rather than leave the promise pending.
        if self.isCustomerCenterPresented {
          promise.resolve(nil)
          return
        }

        let promiseSettled = AtomicFlag()
        let finish: () -> Void = { [weak self] in
          self?.isCustomerCenterPresented = false
          if promiseSettled.testAndSet() == false {
            promise.resolve(nil)
          }
        }

        self.isCustomerCenterPresented = true
        Superwall.shared.presentCustomerCenter(
          configuration: configuration.map { CustomerCenterConfiguration.fromJson($0) },
          delegate: CustomerCenterDelegateBridge(
            handlerId: handlerId,
            asksBeforeRestoring: asksBeforeRestoring
          ),
          onDismiss: finish
        )

        // The SDK only logs when it can't present (not configured, nothing to present from,
        // or UIKit refused), and then never calls `onDismiss`. UIKit sets up a presentation
        // synchronously, so if no Customer Center is in the hierarchy now, none is coming.
        if !SuperwallExpoModule.isCustomerCenterInHierarchy() {
          finish()
        }
      }
    }

    AsyncFunction("dismissCustomerCenter") { (promise: Promise) in
      Task { @MainActor in
        // Before configure the SDK returns without calling the completion.
        guard Superwall.isInitialized else {
          promise.resolve(nil)
          return
        }
        Superwall.shared.dismissCustomerCenter {
          promise.resolve(nil)
        }
      }
    }

    Function("didHandleCustomerCenterShouldRestorePurchases") { (requestId: String, proceed: Bool) in
      PendingCustomerCenterRestores.shared.resolve(requestId: requestId, proceed: proceed)
    }

    Function("setAdConsent") { (consent: [String: Any]) in
      Superwall.shared.adConsent = AdConsent.fromJson(consent)
    }

    AsyncFunction("getAdConsent") { (promise: Promise) in
      promise.resolve(Superwall.shared.adConsent.toJson())
    }

    AsyncFunction("getEventTrackingBehavior") { (promise: Promise) in
      promise.resolve(Superwall.shared.eventTrackingBehavior.toJson())
    }

    AsyncFunction("getLocaleIdentifier") { (promise: Promise) in
      promise.resolve(Superwall.shared.localeIdentifier)
    }

    AsyncFunction("getLogLevel") { (promise: Promise) in
      promise.resolve(Superwall.shared.logLevel.toJson())
    }

    AsyncFunction("getUserId") { (promise: Promise) in
      promise.resolve(Superwall.shared.userId)
    }

    AsyncFunction("getIsLoggedIn") { (promise: Promise) in
      promise.resolve(Superwall.shared.isLoggedIn)
    }

    AsyncFunction("getIsPaywallPresented") { (promise: Promise) in
      DispatchQueue.main.async {
        promise.resolve(Superwall.shared.isPaywallPresented)
      }
    }

    AsyncFunction("getLatestPaywallInfo") { (promise: Promise) in
      DispatchQueue.main.async {
        promise.resolve(Superwall.shared.latestPaywallInfo?.toJson())
      }
    }

    AsyncFunction("getOverrideProductsByName") { (promise: Promise) in
      promise.resolve(Superwall.shared.overrideProductsByName ?? [:])
    }

    Function("setOverrideProductsByName") { (overrides: [String: String]) in
      Superwall.shared.overrideProductsByName = overrides
    }

    AsyncFunction("refreshConfiguration") { (promise: Promise) in
      Superwall.shared.refreshConfiguration {
        promise.resolve(nil)
      }
    }

    Function("cancelAllScheduledNotifications") {
      UNUserNotificationCenter.current().removeAllPendingSuperwallNotificationRequests()
    }

    // Push tokens and notification opens are only reported by the Android SDK.
    Function("setPushToken") { (_: String) in }

    Function("optOutOfNotifications") {}

    Function("optInToNotifications") {}

    Function("handleNotificationOpen") { (_: [String: String]) in }

    Function("removeAllPendingNonSuperwallNotificationRequests") {
      UNUserNotificationCenter.current().removeAllPendingNonSuperwallNotificationRequests()
    }

    Function("removeAllDeliveredSuperwallNotifications") {
      UNUserNotificationCenter.current().removeAllDeliveredSuperwallNotifications()
    }

    Function("removeAllDeliveredNonSuperwallNotifications") {
      UNUserNotificationCenter.current().removeAllDeliveredNonSuperwallNotifications()
    }

    AsyncFunction("getGrantedEntitlements") { (promise: Promise) in
      promise.resolve(Superwall.shared.grantedEntitlements.map { $0.toJson() })
    }

    AsyncFunction("setGrantedEntitlements") { (entitlements: [[String: Any]], promise: Promise) in
      // Replaces the previous set; an empty array revokes them all.
      Superwall.shared.grantedEntitlements = Set(
        entitlements.compactMap { item in
          (item["id"] as? String).map { Entitlement(id: $0) }
        }
      )
      promise.resolve(nil)
    }

    // The functions below exist only in the Android SDK. They're defined here so the JS API is
    // the same on both platforms, and do nothing.
    AsyncFunction("showAlert") {
      (_: String?, _: String?, _: String?, _: String?, promise: Promise) in
      promise.resolve(false)
    }

    Function("openRestoreOnWeb") {}

    AsyncFunction("getExternalAccountId") { (promise: Promise) in
      promise.resolve(nil)
    }

    AsyncFunction("observe") { (_: [String: Any], promise: Promise) in
      promise.resolve(nil)
    }

    AsyncFunction("teardown") { (promise: Promise) in
      promise.resolve(false)
    }

    AsyncFunction("dismissForNextPaywall") { (promise: Promise) in
      promise.resolve(nil)
    }
  }

  @MainActor
  private static func isCustomerCenterInHierarchy() -> Bool {
    let windows = UIApplication.shared.connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .flatMap { $0.windows }
    for window in windows {
      var controller = window.rootViewController
      while let current = controller {
        if current is CustomerCenterViewController {
          return true
        }
        controller = current.presentedViewController
      }
    }
    return false
  }
}
