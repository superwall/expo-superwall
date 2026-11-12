package expo.modules.superwallexpo.json

import com.superwall.sdk.store.ProductReplacement

fun ProductReplacement.toJson(): Map<String, Any?> =
  mapOf(
    "mode" to mode.toJson(),
    "oldProductId" to oldProductId,
    "oldPurchaseToken" to oldPurchaseToken,
  )

private fun ProductReplacement.Mode.toJson(): String =
  when (this) {
    ProductReplacement.Mode.DEFAULT -> "storeDefault"
    ProductReplacement.Mode.CHARGE_PRORATED -> "chargeProrated"
    ProductReplacement.Mode.CHARGE_FULL_PRICE -> "chargeFullPrice"
  }
