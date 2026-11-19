import SuperwallKit

struct PurchaseResultError: LocalizedError {
  let message: String

  var errorDescription: String? {
    return message
  }
}

extension PurchaseResult {
  // Never fails: the SDK waits for an answer, so anything unreadable becomes a failure, as on Android.
  static func fromJson(_ dictionary: [String: Any]) -> PurchaseResult {
    switch dictionary["type"] as? String {
    case "cancelled":
      return  PurchaseResult.cancelled
    case "purchased":
      return PurchaseResult.purchased
    case "pending":
      return PurchaseResult.pending
    case "failed":
      let message = dictionary["error"] as? String ?? "Unknown error"
      return PurchaseResult.failed(PurchaseResultError(message: message))
    default:
      return PurchaseResult.failed(PurchaseResultError(message: "Unknown Purchase Result type"))
    }
  }

  func toJson() -> [String: Any] {
    switch self {
    case .purchased:
      return ["type": "purchased"]
    case .cancelled:
      return ["type": "cancelled"]
    case .pending:
      return ["type": "pending"]
    case .failed(let error):
      return ["type": "failed", "error": error.localizedDescription]
    }
  }
}
