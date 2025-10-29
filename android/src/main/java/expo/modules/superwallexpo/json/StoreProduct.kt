package expo.modules.superwallexpo.json


import com.superwall.sdk.store.abstractions.product.StoreProductType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun StoreProductType.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  val dateFormatter = DateTimeFormatter.ISO_DATE_TIME.withLocale(Locale.getDefault())

  map["fullIdentifier"] = fullIdentifier
  map["productIdentifier"] = productIdentifier
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
  map["trialPeriodEndDateString"] = trialPeriodEndDateString
  map["localizedTrialPeriodPrice"] = localizedTrialPeriodPrice
  map["trialPeriodPrice"] = trialPeriodPrice.toDouble()
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
  map["languageCode"] = languageCode
  map["currencyCode"] = currencyCode
  map["currencySymbol"] = currencySymbol
  map["regionCode"] = regionCode
  map["price"] = price.toDouble()

  subscriptionPeriod?.let {
    map["subscriptionPeriod"] = mapOf(
      "value" to it.value,
      "unit" to it.unit.name
    )
  } ?: run { map["subscriptionPeriod"] = null }

  trialPeriodEndDate?.let {
    val instant = it.toInstant()
    map["trialPeriodEndDate"] = instant.atZone(ZoneId.systemDefault()).format(dateFormatter)
  } ?: run { map["trialPeriodEndDate"] = null }

  return map
}
