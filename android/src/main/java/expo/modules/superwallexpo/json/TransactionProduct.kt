package expo.modules.superwallexpo.json

import com.superwall.sdk.analytics.superwall.TransactionProduct
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun TransactionProduct.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
  }

  // Core identifiers
  map["id"] = id
  map["productIdentifier"] = id
  map["fullIdentifier"] = id

  // Price
  map["price"] = price.raw.toDouble()
  map["localizedPrice"] = price.localized
  map["dailyPrice"] = price.daily
  map["weeklyPrice"] = price.weekly
  map["monthlyPrice"] = price.monthly
  map["yearlyPrice"] = price.yearly

  // Locale and currency
  map["locale"] = locale
  map["languageCode"] = languageCode
  map["currencyCode"] = currency.code
  map["currencySymbol"] = currency.symbol

  // Attributes
  map["attributes"] = emptyMap<String, Any>()

  // Period properties
  period?.let { period ->
    map["localizedSubscriptionPeriod"] = period.alt
    map["period"] = period.unit.name.lowercase()
    map["periodly"] = period.ly
    map["periodDays"] = period.days
    map["periodDaysString"] = period.days.toString()
    map["periodWeeks"] = period.weeks
    map["periodWeeksString"] = period.weeks.toString()
    map["periodMonths"] = period.months
    map["periodMonthsString"] = period.months.toString()
    map["periodYears"] = period.years
    map["periodYearsString"] = period.years.toString()

    // Subscription period object
    val unitString = when (period.unit) {
      com.superwall.sdk.store.abstractions.product.SubscriptionPeriod.Unit.day -> "day"
      com.superwall.sdk.store.abstractions.product.SubscriptionPeriod.Unit.week -> "week"
      com.superwall.sdk.store.abstractions.product.SubscriptionPeriod.Unit.month -> "month"
      com.superwall.sdk.store.abstractions.product.SubscriptionPeriod.Unit.year -> "year"
      else -> "unknown"
    }

    val value = when (period.unit) {
      com.superwall.sdk.store.abstractions.product.SubscriptionPeriod.Unit.day -> period.days
      com.superwall.sdk.store.abstractions.product.SubscriptionPeriod.Unit.week -> period.weeks
      com.superwall.sdk.store.abstractions.product.SubscriptionPeriod.Unit.month -> period.months
      com.superwall.sdk.store.abstractions.product.SubscriptionPeriod.Unit.year -> period.years
      else -> 1
    }

    map["subscriptionPeriod"] = mapOf(
      "unit" to unitString,
      "value" to value
    )
  } ?: run {
    map["localizedSubscriptionPeriod"] = ""
    map["period"] = ""
    map["periodly"] = ""
    map["periodDays"] = 0
    map["periodDaysString"] = "0"
    map["periodWeeks"] = 0
    map["periodWeeksString"] = "0"
    map["periodMonths"] = 0
    map["periodMonthsString"] = "0"
    map["periodYears"] = 0
    map["periodYearsString"] = "0"
    map["subscriptionPeriod"] = null
  }

  // Trial period properties
  trialPeriod?.let { trialPeriod ->
    map["hasFreeTrial"] = true
    map["trialPeriodDays"] = trialPeriod.days
    map["trialPeriodDaysString"] = trialPeriod.days.toString()
    map["trialPeriodWeeks"] = trialPeriod.weeks
    map["trialPeriodWeeksString"] = trialPeriod.weeks.toString()
    map["trialPeriodMonths"] = trialPeriod.months
    map["trialPeriodMonthsString"] = trialPeriod.months.toString()
    map["trialPeriodYears"] = trialPeriod.years
    map["trialPeriodYearsString"] = trialPeriod.years.toString()
    map["trialPeriodText"] = trialPeriod.text

    trialPeriod.endAt?.let { endAt ->
      val dateString = dateFormatter.format(endAt)
      map["trialPeriodEndDate"] = dateString
      map["trialPeriodEndDateString"] = dateString
    } ?: run {
      map["trialPeriodEndDate"] = null
      map["trialPeriodEndDateString"] = ""
    }

    // Trial period price (free trial, so 0)
    map["localizedTrialPeriodPrice"] = ""
    map["trialPeriodPrice"] = 0
  } ?: run {
    map["hasFreeTrial"] = false
    map["trialPeriodDays"] = 0
    map["trialPeriodDaysString"] = "0"
    map["trialPeriodWeeks"] = 0
    map["trialPeriodWeeksString"] = "0"
    map["trialPeriodMonths"] = 0
    map["trialPeriodMonthsString"] = "0"
    map["trialPeriodYears"] = 0
    map["trialPeriodYearsString"] = "0"
    map["trialPeriodText"] = ""
    map["trialPeriodEndDate"] = null
    map["trialPeriodEndDateString"] = ""
    map["localizedTrialPeriodPrice"] = ""
    map["trialPeriodPrice"] = 0
  }

  return map
}
