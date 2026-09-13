import SuperwallKit

struct PurchaseResultError: LocalizedError {
  let message: String

  var errorDescription: String? {
    return message
  }
}

extension PurchaseResult {
  static func fromJson(_ dictionary: [String: Any]) -> PurchaseResult? {
    guard let type = dictionary["type"] as? String else {
      return nil
    }
    switch type {
    case "cancelled":
      return  PurchaseResult.cancelled
    case "purchased":
      return PurchaseResult.purchased
    case "pending":
      return PurchaseResult.pending
    case "failed":
      guard let message = dictionary["error"] as? String else {
        return nil
      }
      return PurchaseResult.failed(PurchaseResultError(message: message))
    default:
      return nil
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
