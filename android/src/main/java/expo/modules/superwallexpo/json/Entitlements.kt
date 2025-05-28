package expo.modules.superwallexpo.json


import com.superwall.sdk.models.entitlements.Entitlement


fun Set<Entitlement>.toJson(): List<Map<String, Any>> {
  val array = mutableListOf<Map<String, Any>>()
  this@toJson.forEach { entitlement ->
    array.add(entitlement.toJson())
  }
  return array
}

fun Entitlement.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()
  map["id"] = this.id
  map["type"] = this.type.toString()
  return map
}