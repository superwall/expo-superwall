import SuperwallKit

extension AppStoreProduct {
  func toJson() -> [String: Any] {
    var json: [String: Any] = [
      "id": id
    ]
    if let billingPlanType = billingPlanType {
      json["billingPlanType"] = billingPlanType.toJson()
    }
    return json
  }
}

extension AppStoreProduct.BillingPlanType {
  // The dashboard's names for the plans.
  func toJson() -> String {
    switch self {
    case .upFront:
      return "UP_FRONT"
    case .monthly:
      return "MONTHLY"
    @unknown default:
      return "UP_FRONT"
    }
  }
}
