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
    case .custom:
      return "CUSTOM"
    case .other:
      return "OTHER"
    @unknown default:
      return "OTHER"
    }
  }
}
