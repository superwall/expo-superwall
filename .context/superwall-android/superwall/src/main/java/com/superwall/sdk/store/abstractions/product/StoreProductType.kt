package com.superwall.sdk.store.abstractions.product

import java.math.BigDecimal
import java.util.*

interface StoreProductType {
    val fullIdentifier: String
    val productIdentifier: String
    val price: BigDecimal
    val localizedPrice: String
    val localizedSubscriptionPeriod: String
    val period: String
    val periodly: String
    val periodWeeks: Int
    val periodWeeksString: String
    val periodMonths: Int
    val periodMonthsString: String
    val periodYears: Int
    val periodYearsString: String
    val periodDays: Int
    val periodDaysString: String
    val dailyPrice: String
    val weeklyPrice: String
    val monthlyPrice: String
    val yearlyPrice: String
    val hasFreeTrial: Boolean
    val localizedTrialPeriodPrice: String
    val trialPeriodPrice: BigDecimal
    val trialPeriodEndDate: Date?
    val trialPeriodEndDateString: String
    val trialPeriodDays: Int
    val trialPeriodDaysString: String
    val trialPeriodWeeks: Int
    val trialPeriodWeeksString: String
    val trialPeriodMonths: Int
    val trialPeriodMonthsString: String
    val trialPeriodYears: Int
    val trialPeriodYearsString: String
    val trialPeriodText: String
    val locale: String
    val languageCode: String?
    val currencyCode: String?
    val currencySymbol: String?
    val regionCode: String?
    val subscriptionPeriod: SubscriptionPeriod?
    val productType: String

    fun trialPeriodPricePerUnit(unit: SubscriptionPeriod.Unit): String

    val attributes: Map<String, String>
        get() {
            val attributes = mutableMapOf<String, String>()

            attributes["rawPrice"] = price.toString()
            attributes["price"] = localizedPrice
            attributes["periodAlt"] = localizedSubscriptionPeriod
            attributes["localizedPeriod"] = localizedSubscriptionPeriod
            attributes["period"] = period
            attributes["periodly"] = periodly
            attributes["weeklyPrice"] = weeklyPrice
            attributes["dailyPrice"] = dailyPrice
            attributes["monthlyPrice"] = monthlyPrice
            attributes["yearlyPrice"] = yearlyPrice
            attributes["rawTrialPeriodPrice"] = trialPeriodPrice.toString()
            attributes["trialPeriodPrice"] = localizedTrialPeriodPrice
            attributes["trialPeriodDailyPrice"] = trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day)
            attributes["trialPeriodWeeklyPrice"] = trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week)
            attributes["trialPeriodMonthlyPrice"] = trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month)
            attributes["trialPeriodYearlyPrice"] = trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year)
            attributes["trialPeriodDays"] = trialPeriodDaysString
            attributes["trialPeriodWeeks"] = trialPeriodWeeksString
            attributes["trialPeriodMonths"] = trialPeriodMonthsString
            attributes["trialPeriodYears"] = trialPeriodYearsString
            attributes["trialPeriodText"] = trialPeriodText
            attributes["trialPeriodEndDate"] = trialPeriodEndDateString
            attributes["periodDays"] = periodDaysString
            attributes["periodWeeks"] = periodWeeksString
            attributes["periodMonths"] = periodMonthsString
            attributes["periodYears"] = periodYearsString
            attributes["locale"] = locale
            attributes["languageCode"] = languageCode ?: "n/a"
            attributes["currencyCode"] = currencyCode ?: "n/a"
            attributes["currencySymbol"] = currencySymbol ?: "n/a"
            attributes["identifier"] = productIdentifier

            return attributes
        }
}
