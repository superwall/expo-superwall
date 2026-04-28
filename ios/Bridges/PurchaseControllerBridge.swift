import StoreKit
import SuperwallKit

final class PurchaseControllerBridge: PurchaseController {
    static let shared = PurchaseControllerBridge()

    var purchaseCompletion: ((PurchaseResult) -> Void)?
    var restoreCompletion: ((RestorationResult) -> Void)?

    private init() {}

    func purchase(product: StoreProduct) async -> PurchaseResult {
        var payload: [String: Any] = [
            "productId": product.productIdentifier,
            "platform": "ios"
        ]

        if let store = await Self.storeForProductIdentifier(product.productIdentifier) {
            payload["store"] = store
        }

        SuperwallExpoModule.emitEvent("onPurchase", payload)
        return await withCheckedContinuation { continuation in
            self.purchaseCompletion = { [weak self] result in
                continuation.resume(returning: result)
                self?.purchaseCompletion = nil
            }
        }
    }

    /// Looks up the product's store from the most recently presented paywall so JS can route
    /// custom products to its own purchase logic instead of StoreKit.
    @MainActor
    private static func storeForProductIdentifier(_ productIdentifier: String) -> String? {
        let product = Superwall.shared.latestPaywallInfo?.products.first { $0.id == productIdentifier }
        return product?.objcAdapter.store.toJson()
    }

    func restorePurchases() async -> RestorationResult {
        SuperwallExpoModule.emitEvent("onPurchaseRestore", nil)

        return await withCheckedContinuation { continuation in
            self.restoreCompletion = { [weak self] result in
                continuation.resume(returning: result)
                self?.restoreCompletion = nil
            }
        }
    }
}
