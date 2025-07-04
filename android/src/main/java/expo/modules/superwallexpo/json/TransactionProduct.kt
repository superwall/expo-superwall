package expo.modules.superwallexpo.json

import com.superwall.sdk.analytics.superwall.TransactionProduct

fun TransactionProduct.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()
  map["id"] = id
  map["productIdentifier"] = productIdentifier
  map["fullIdentifier"] = fullIdentifier
  map["price"] = price
  map["localizedPrice"] = localizedPrice
  map["localizedSubscriptionPeriod"] = localizedSubscriptionPeriod
  map["period"] = period
  map["periodly"] = periodly
  map["periodWeeks"] = periodWeeks
  map["periodWeeksString"] = periodWeeksString
  map["periodMonths"] = periodMonths
  map["periodMonthsString"] = periodMonthsString
  map["periodYears"] = periodYears
  map["periodYearsString"] = periodYearsString
  map["periodDays"] = periodDays
  map["periodDaysString"] = periodDaysString
  map["dailyPrice"] = dailyPrice
  map["weeklyPrice"] = weeklyPrice
  map["monthlyPrice"] = monthlyPrice
  map["yearlyPrice"] = yearlyPrice
  map["hasFreeTrial"] = hasFreeTrial
  map["localizedTrialPeriodPrice"] = localizedTrialPeriodPrice
  map["trialPeriodPrice"] = trialPeriodPrice
  map["trialPeriodEndDateString"] = trialPeriodEndDateString
  map["trialPeriodDays"] = trialPeriodDays
  map["trialPeriodDaysString"] = trialPeriodDaysString
  map["trialPeriodWeeks"] = trialPeriodWeeks
  map["trialPeriodWeeksString"] = trialPeriodWeeksString
  map["trialPeriodMonths"] = trialPeriodMonths
  map["trialPeriodMonthsString"] = trialPeriodMonthsString
  map["trialPeriodYears"] = trialPeriodYears
  map["trialPeriodYearsString"] = trialPeriodYearsString
  map["trialPeriodText"] = trialPeriodText
  map["locale"] = locale
  map["attributes"] = attributes
  
  // Handle optional properties
  map["trialPeriodEndDate"] = trialPeriodEndDate?.let { 
    java.time.format.DateTimeFormatter.ISO_INSTANT.format(it) 
  }
  map["languageCode"] = languageCode
  map["currencyCode"] = currencyCode
  map["currencySymbol"] = currencySymbol
  map["regionCode"] = regionCode
  
  map["subscriptionPeriod"] = subscriptionPeriod?.let { period ->
    mapOf(
      "unit" to period.unit.name.lowercase(),
      "value" to period.value
    )
  }
  
  map["subscriptionGroupIdentifier"] = subscriptionGroupIdentifier
  map["isFamilyShareable"] = isFamilyShareable ?: false
  
  return map
}
