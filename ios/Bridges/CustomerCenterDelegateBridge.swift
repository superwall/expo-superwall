import Foundation
import SuperwallKit

// Restores waiting on `didHandleCustomerCenterShouldRestorePurchases`, keyed by request ID.
final class PendingCustomerCenterRestores {
  static let shared = PendingCustomerCenterRestores()

  private let lock = NSLock()
  private var continuations: [String: CheckedContinuation<Bool, Never>] = [:]

  func add(_ continuation: CheckedContinuation<Bool, Never>, for requestId: String) {
    lock.lock()
    continuations[requestId] = continuation
    lock.unlock()
  }

  /// Answers a pending restore. Ignores unknown or already-answered request IDs.
  func resolve(requestId: String, proceed: Bool) {
    lock.lock()
    let continuation = continuations.removeValue(forKey: requestId)
    lock.unlock()
    continuation?.resume(returning: proceed)
  }
}

// Forwards Customer Center delegate callbacks for one presentation to JS, tagged with the
// presentation's `handlerId`. Dismissal is reported through the `presentCustomerCenter` promise.
@MainActor
final class CustomerCenterDelegateBridge: CustomerCenterDelegate {
  private let handlerId: String
  // Only ask JS before restoring when it registered a handler; otherwise restore straight away.
  private let asksBeforeRestoring: Bool

  init(handlerId: String, asksBeforeRestoring: Bool) {
    self.handlerId = handlerId
    self.asksBeforeRestoring = asksBeforeRestoring
  }

  func customerCenterShouldRestorePurchases() async -> Bool {
    guard asksBeforeRestoring else {
      return true
    }
    let requestId = UUID().uuidString
    let handlerId = self.handlerId
    return await withCheckedContinuation { continuation in
      PendingCustomerCenterRestores.shared.add(continuation, for: requestId)
      SuperwallExpoModule.emitEvent(
        "onCustomerCenterShouldRestorePurchases",
        ["handlerId": handlerId, "requestId": requestId]
      )
    }
  }

  func customerCenterDidSelectAction(
    _ action: CustomerCenterAction,
    pathId: String,
    purchase: CustomerCenterPurchase?
  ) {
    var data: [String: Any] = [
      "handlerId": handlerId,
      "action": action.toJson(),
      "pathId": pathId,
    ]
    if let purchase = purchase {
      data["purchase"] = purchase.toJson()
    }
    SuperwallExpoModule.emitEvent("onCustomerCenterAction", data)
  }

  func customerCenterDidCompleteSurvey(
    surveyId: String,
    optionId: String,
    action: CustomerCenterAction,
    pathId: String
  ) {
    SuperwallExpoModule.emitEvent(
      "onCustomerCenterSurveyComplete",
      [
        "handlerId": handlerId,
        "surveyId": surveyId,
        "optionId": optionId,
        "action": action.toJson(),
        "pathId": pathId,
      ]
    )
  }

  func customerCenterDidCompleteRefundRequest(
    productId: String,
    status: CustomerCenterRefundStatus
  ) {
    SuperwallExpoModule.emitEvent(
      "onCustomerCenterRefundRequestComplete",
      [
        "handlerId": handlerId,
        "productId": productId,
        "status": status.toJson(),
      ]
    )
  }
}
