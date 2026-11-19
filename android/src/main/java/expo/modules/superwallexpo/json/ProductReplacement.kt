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
    ProductReplacement.Mode.DEFAULT -> "default"
    ProductReplacement.Mode.CHARGE_PRORATED -> "charge_prorated"
    ProductReplacement.Mode.CHARGE_FULL_PRICE -> "charge_full_price"
  }
