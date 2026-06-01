import SuperwallKit

extension Product {
  func toJson() -> [String: Any] {
    var map: [String: Any] = [
      "id": id,
      "entitlements": entitlements.map { $0.toJson() },
      "store": objcAdapter.store.toJson()
    ]

    if let name = name {
      map["name"] = name
    }

    switch type {
    case .appStore(let appStoreProduct):
      map["appStoreProduct"] = appStoreProduct.toJson()
    case .stripe(let stripeProduct):
      map["stripeProduct"] = stripeProduct.toJson()
    case .paddle(let paddleProduct):
      map["paddleProduct"] = paddleProduct.toJson()
    case .custom(let customProduct):
      map["customProduct"] = customProduct.toJson()
    }

    return map
  }
}
