import SuperwallKit

extension CustomerInfo {
  func toJson() -> [String: Any] {
    return [
      "userId": userId,
      "subscriptions": subscriptions.map { $0.toJson() },
      "nonSubscriptions": nonSubscriptions.map { $0.toJson() },
      "entitlements": entitlements.map { $0.toJson() },
    ]
  }
}

extension SubscriptionTransaction {
  func toJson() -> [String: Any?] {
    let dateFormatter = ISO8601DateFormatter()

    var json: [String: Any?] = [
      "transactionId": transactionId,
      "productId": productId,
      "purchaseDate": dateFormatter.string(from: purchaseDate),
      "willRenew": willRenew,
      "isRevoked": isRevoked,
      "isInGracePeriod": isInGracePeriod,
      "isInBillingRetryPeriod": isInBillingRetryPeriod,
      "isActive": isActive,
      "expirationDate": expirationDate.map { dateFormatter.string(from: $0) },
      "store": store.toJson(),
    ]
    // Keys omitted when nil to match the Android serializer's output shape.
    if let offerType = offerType {
      json["offerType"] = offerType.rawValue
    }
    if let subscriptionGroupId = subscriptionGroupId {
      json["subscriptionGroupId"] = subscriptionGroupId
    }
    return json
  }
}

extension NonSubscriptionTransaction {
  func toJson() -> [String: Any] {
    let dateFormatter = ISO8601DateFormatter()

    return [
      "transactionId": transactionId,
      "productId": productId,
      "purchaseDate": dateFormatter.string(from: purchaseDate),
      "isConsumable": isConsumable,
      "isRevoked": isRevoked,
      "store": store.toJson(),
    ]
  }
}
