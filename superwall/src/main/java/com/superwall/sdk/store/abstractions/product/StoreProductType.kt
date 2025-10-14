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

    fun trialPeriodPricePerUnit(unit: SubscriptionPeriod.Unit): String
}
