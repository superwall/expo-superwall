import SuperwallKit

struct RestorationResultError: Error {
  let message: String
}

extension RestorationResult {
  // Never fails: the SDK waits for an answer, so anything unreadable becomes a failure, as on Android.
  static func fromJson(_ dictionary: [String: Any]) -> RestorationResult {
    switch dictionary["result"] as? String {
    case "restored":
      return RestorationResult.restored
    case "failed":
      let message = dictionary["errorMessage"] as? String ?? "Unknown error"
      return RestorationResult.failed(RestorationResultError(message: message))
    default:
      return RestorationResult.failed(RestorationResultError(message: "Unknown restoration result"))
    }
  }

  func toJson() -> [String: Any] {
    switch self {
    case .restored:
      return ["result": "restored"]
    case .failed(let error):
      return ["result": "failed", "errorMessage": error?.localizedDescription ?? "Unknown error"]
    }
  }
}
