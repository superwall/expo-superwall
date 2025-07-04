package expo.modules.superwallexpo.json

import com.superwall.sdk.analytics.superwall.TransactionProduct

fun TransactionProduct.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()
  map["id"] = id
  return map
}
