package expo.modules.superwallexpo.json


import com.superwall.sdk.models.entitlements.SubscriptionStatus

fun SubscriptionStatus.toJson(): Map<String, Any> {
  val status = this
  val map = mutableMapOf<String, Any>()
  map["status"] = when (status) {
    is SubscriptionStatus.Active -> "ACTIVE"
    is SubscriptionStatus.Inactive -> "INACTIVE"
    is SubscriptionStatus.Unknown -> "UNKNOWN"
  }
  when (status) {
    is SubscriptionStatus.Active -> {
      map["entitlements"] =  status.entitlements.map { entitlement ->
          val entitlementMap = mutableMapOf<String, Any>()
          entitlementMap["id"] = entitlement.id
          entitlementMap
      }
    }
    is SubscriptionStatus.Inactive -> {
    }
    is SubscriptionStatus.Unknown -> {
    }
  }
  return map
}