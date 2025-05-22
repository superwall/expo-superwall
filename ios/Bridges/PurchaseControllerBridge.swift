import StoreKit
import SuperwallKit

final class PurchaseControllerBridge: PurchaseController {
    static let shared = PurchaseControllerBridge()

    var purchaseCompletion: ((PurchaseResult) -> Void)?
    var restoreCompletion: ((RestorationResult) -> Void)?

    private init() {}

    func purchase(product: StoreProduct) async -> PurchaseResult {
        SuperwallExpoModule.emitEvent(
            "onPurchase",
            ["productId": product.productIdentifier, "platform": "ios"]
        )
        return await withCheckedContinuation { continuation in
            self.purchaseCompletion = { [weak self] result in
                continuation.resume(returning: result)
                self?.purchaseCompletion = nil
            }
        }
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
