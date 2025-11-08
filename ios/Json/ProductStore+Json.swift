import SuperwallKit

extension ProductStore {
  func toJson() -> String {
    switch self {
    case .appStore:
      return "APP_STORE"
    case .stripe:
      return "STRIPE"
    case .paddle:
      return "PADDLE"
    case .superwall:
      return "SUPERWALL"
    case .playStore:
      return "PLAY_STORE"
    case .other:
      return "OTHER"
    }
  }
}
