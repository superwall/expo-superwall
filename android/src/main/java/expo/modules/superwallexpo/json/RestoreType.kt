package expo.modules.superwallexpo.json

import com.superwall.sdk.store.transactions.RestoreType

fun RestoreType.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  when (this) {
    is RestoreType.ViaPurchase -> {
      map["type"] = "viaPurchase"
      // `storeTransaction` matches iOS; `transaction` is the old Android-only key.
      val transactionJson = transaction?.toJson()
      map["storeTransaction"] = transactionJson
      map["transaction"] = transactionJson
    }
    is RestoreType.ViaRestore -> map["type"] = "viaRestore"
  }
  return map
}
