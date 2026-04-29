package expo.modules.superwallexpo.json

import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.customer.NonSubscriptionTransaction
import com.superwall.sdk.models.customer.SubscriptionTransaction
import com.superwall.sdk.store.abstractions.product.receipt.LatestPeriodType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

// Mirrors the @SerialName values on `LatestPeriodType`. Exhaustive `when` so a new
// SDK case is a compile error rather than a silently-incorrect string.
private fun LatestPeriodType.toJsonString(): String =
  when (this) {
    LatestPeriodType.TRIAL -> "trial"
    LatestPeriodType.CODE -> "code"
    LatestPeriodType.SUBSCRIPTION -> "subscription"
    LatestPeriodType.PROMOTIONAL -> "promotional"
    LatestPeriodType.WINBACK -> "winback"
    LatestPeriodType.REVOKED -> "revoked"
  }

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

private fun Date.toIsoString(): String =
  this.toInstant().atZone(ZoneId.systemDefault()).format(dateFormatter)

fun CustomerInfo.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  map["userId"] = this.userId
  map["subscriptions"] = this.subscriptions.map { it.toJson() }
  map["nonSubscriptions"] = this.nonSubscriptions.map { it.toJson() }
  map["entitlements"] = this.entitlements.map { it.toJson() }
  return map
}

fun SubscriptionTransaction.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  map["transactionId"] = this.transactionId
  map["productId"] = this.productId
  map["purchaseDate"] = this.purchaseDate.toIsoString()
  map["willRenew"] = this.willRenew
  map["isRevoked"] = this.isRevoked
  map["isInGracePeriod"] = this.isInGracePeriod
  map["isInBillingRetryPeriod"] = this.isInBillingRetryPeriod
  map["isActive"] = this.isActive
  map["expirationDate"] = this.expirationDate?.toIsoString()
  map["store"] = this.store.name
  this.offerType?.let { map["offerType"] = it.toJsonString() }
  this.subscriptionGroupId?.let { map["subscriptionGroupId"] = it }
  return map
}

fun NonSubscriptionTransaction.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  map["transactionId"] = this.transactionId
  map["productId"] = this.productId
  map["purchaseDate"] = this.purchaseDate.toIsoString()
  map["isConsumable"] = this.isConsumable
  map["isRevoked"] = this.isRevoked
  map["store"] = this.store.name
  return map
}
