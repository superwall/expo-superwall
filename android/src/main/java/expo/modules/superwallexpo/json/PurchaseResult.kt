package expo.modules.superwallexpo.json

import com.superwall.sdk.delegate.PurchaseResult

fun purchaseResultFromJson(json: Map<String, Any>): PurchaseResult {
    return when (json["type"] as String) {
        "cancelled" -> PurchaseResult.Cancelled()
        "purchased" -> PurchaseResult.Purchased()
        "pending" -> PurchaseResult.Pending()
        "failed" -> {
            // Assuming there's an error message for failed purchases
            val errorMessage = (json["error"] as String?) ?: "Unknown error"
            PurchaseResult.Failed(errorMessage)
        }

        else -> PurchaseResult.Failed("Unknown Purchase Result type")
    }
}
