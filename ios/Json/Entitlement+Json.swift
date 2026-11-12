//
//  Entitlement+Json.swift
//  Superwall
//
//  Created by Yusuf Tör on 20/01/2025.
//

import SuperwallKit

extension Entitlement {
  // Keys whose value is nil are left out, as in the Android bridge.
  func toJson() -> [String: Any] {
    let dateFormatter = ISO8601DateFormatter()

    var json: [String: Any] = [
      "id": id,
      "type": type.toJson(),
      "isActive": isActive,
      "productIds": Array(productIds),
    ]
    if let latestProductId = latestProductId {
      json["latestProductId"] = latestProductId
    }
    if let store = store {
      json["store"] = store.toJson()
    }
    if let startsAt = startsAt {
      json["startsAt"] = dateFormatter.string(from: startsAt)
    }
    if let renewedAt = renewedAt {
      json["renewedAt"] = dateFormatter.string(from: renewedAt)
    }
    if let expiresAt = expiresAt {
      json["expiresAt"] = dateFormatter.string(from: expiresAt)
    }
    if let isLifetime = isLifetime {
      json["isLifetime"] = isLifetime
    }
    if let willRenew = willRenew {
      json["willRenew"] = willRenew
    }
    if let renewsAt = renewsAt {
      json["renewsAt"] = dateFormatter.string(from: renewsAt)
    }
    if let isRevoked = isRevoked {
      json["isRevoked"] = isRevoked
    }
    if let isInGracePeriod = isInGracePeriod {
      json["isInGracePeriod"] = isInGracePeriod
    }
    if let state = state {
      json["state"] = state.rawValue
    }
    if let offerType = offerType {
      json["offerType"] = offerType.rawValue
    }
    return json
  }
}
