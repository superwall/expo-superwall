package expo.modules.superwallexpo.json

import com.superwall.sdk.store.abstractions.transactions.StoreTransaction
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

fun StoreTransaction.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  val dateFormatter = DateTimeFormatter.ISO_INSTANT

  fun Date.toIsoString(): String? = this.toInstant()
    .atZone(ZoneId.systemDefault())
    .format(dateFormatter)

  map["configRequestId"] = configRequestId
  map["appSessionId"] = appSessionId
  map["transactionDate"] = transactionDate?.toIsoString()
  map["originalTransactionIdentifier"] = originalTransactionIdentifier
  map["storeTransactionId"] = storeTransactionId
  map["originalTransactionDate"] = originalTransactionDate?.toIsoString()
  map["webOrderLineItemID"] = webOrderLineItemID
  map["appBundleId"] = appBundleId
  map["subscriptionGroupId"] = subscriptionGroupId
  map["isUpgraded"] = isUpgraded ?: false
  map["expirationDate"] = expirationDate?.toIsoString()
  map["offerId"] = offerId
  map["revocationDate"] = revocationDate?.toIsoString()

  return map
}
