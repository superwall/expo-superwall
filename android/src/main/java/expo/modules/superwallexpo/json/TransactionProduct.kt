package expo.modules.superwallexpo.json

import com.superwall.sdk.analytics.superwall.TransactionProduct

fun TransactionProduct.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()
  map["id"] = id
  map["locale"] = locale
  
  // Price information
  map["price"] = mapOf(
    "raw" to price.raw,
    "localized" to price.localized,
    "daily" to price.daily,
    "weekly" to price.weekly,
    "monthly" to price.monthly,
    "yearly" to price.yearly
  )
  
  // Currency information
  map["currency"] = mapOf(
    "code" to currency.code,
    "symbol" to currency.symbol
  )
  
  // Language code
  languageCode?.let { map["languageCode"] = it }
  
  // Trial period
  trialPeriod?.let { trial ->
    val trialMap = mutableMapOf<String, Any>(
      "days" to trial.days,
      "weeks" to trial.weeks,
      "months" to trial.months,
      "years" to trial.years,
      "text" to trial.text
    )
    trial.endAt?.let { endAt ->
      trialMap["endAt"] = endAt.time // Convert to milliseconds
    }
    map["trialPeriod"] = trialMap
  }
  
  // Subscription period
  period?.let { period ->
    map["period"] = mapOf(
      "alt" to period.alt,
      "ly" to period.ly,
      "unit" to period.unit.name.lowercase(),
      "days" to period.days,
      "weeks" to period.weeks,
      "months" to period.months,
      "years" to period.years
    )
  }
  
  return map
}
