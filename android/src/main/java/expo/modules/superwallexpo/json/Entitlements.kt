package expo.modules.superwallexpo.json


import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.store.abstractions.product.receipt.LatestSubscriptionState


fun Set<Entitlement>.toJson(): List<Map<String, Any>> {
  val array = mutableListOf<Map<String, Any>>()
  this@toJson.forEach { entitlement ->
    array.add(entitlement.toJson())
  }
  return array
}

// Keys whose value is null are left out, as in iOS's `Entitlement.toJson()`.
fun Entitlement.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()
  map["id"] = this.id
  map["type"] = this.type.toString()
  map["isActive"] = this.isActive
  map["productIds"] = this.productIds.toList()
  this.latestProductId?.let { map["latestProductId"] = it }
  this.store?.let { map["store"] = it.name }
  this.startsAt?.let { map["startsAt"] = it.toIsoString() }
  this.renewedAt?.let { map["renewedAt"] = it.toIsoString() }
  this.expiresAt?.let { map["expiresAt"] = it.toIsoString() }
  this.isLifetime?.let { map["isLifetime"] = it }
  this.willRenew?.let { map["willRenew"] = it }
  this.renewsAt?.let { map["renewsAt"] = it.toIsoString() }
  this.isRevoked?.let { map["isRevoked"] = it }
  this.isInGracePeriod?.let { map["isInGracePeriod"] = it }
  this.state?.let { map["state"] = it.toJsonString() }
  this.offerType?.let { map["offerType"] = it.toJsonString() }
  return map
}

// iOS's `LatestSubscription.State` raw values, so both platforms send the same strings.
private fun LatestSubscriptionState.toJsonString(): String =
  when (this) {
    LatestSubscriptionState.GRACE_PERIOD -> "inGracePeriod"
    LatestSubscriptionState.EXPIRED -> "expired"
    LatestSubscriptionState.SUBSCRIBED -> "subscribed"
    LatestSubscriptionState.BILLING_RETRY -> "inBillingRetryPeriod"
    LatestSubscriptionState.REVOKED -> "revoked"
    LatestSubscriptionState.UNKNOWN -> "unknown"
  }
