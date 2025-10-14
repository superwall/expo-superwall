package com.superwall.sdk.billing

import java.math.BigDecimal
import java.util.*

interface StoreProductType {
    val productIdentifier: String
    val price: BigDecimal
    val subscriptionGroupIdentifier: String?
    val swProductTemplateVariablesJson: Map<String, Any>

    //    val swProduct: SWProduct //Please replace this with your actual SWProduct type
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
//    val isFamilyShareable: Boolean
//    val subscriptionPeriod: SubscriptionPeriod? //Please replace this with your actual SubscriptionPeriod type
//    val introductoryDiscount: StoreProductDiscount? //Please replace this with your actual StoreProductDiscount type
//    val discounts: List<StoreProductDiscount> //Please replace this with your actual StoreProductDiscount type
}
