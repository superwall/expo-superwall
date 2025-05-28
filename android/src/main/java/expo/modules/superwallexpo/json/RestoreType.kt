package expo.modules.superwallexpo.json

import com.superwall.sdk.store.transactions.RestoreType

fun RestoreType.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  when (this) {
    is RestoreType.ViaPurchase -> {
      map["type"] = "viaPurchase"
      map["transaction"] = transaction?.toJson()
    }
    is RestoreType.ViaRestore -> map["type"] = "viaRestore"
  }
  return map
}
