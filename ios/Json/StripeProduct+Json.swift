import SuperwallKit

extension StripeProduct {
  func toJson() -> [String: Any] {
    var map: [String: Any] = [
      "id": id
    ]
    if let trialDays = trialDays {
      map["trialDays"] = trialDays
    }
    return map
  }
}
