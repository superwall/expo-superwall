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

fun purchaseResultToJson(result: PurchaseResult): Map<String, Any?> {
    return when (result) {
        is PurchaseResult.Purchased -> mapOf("type" to "purchased")
        is PurchaseResult.Cancelled -> mapOf("type" to "cancelled")
        is PurchaseResult.Pending -> mapOf("type" to "pending")
        is PurchaseResult.Failed -> mapOf(
            "type" to "failed",
            "error" to (result.error?.message ?: "Unknown error")
        )
    }
}
