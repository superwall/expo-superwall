package com.superwall.sdk.store.abstractions.product

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.*

// TODO: Consolidate OfferType and Offer without breaking changes
@Serializable
sealed class OfferType {
    object Auto : OfferType()

    data class Offer(
        override val id: String,
    ) : OfferType()

    open val id: String?
        get() =
            when (this) {
                is Offer -> id
                else -> null
            }
}

class StoreProduct(
    val rawStoreProduct: RawStoreProduct,
) : StoreProductType {
    override val fullIdentifier: String
        get() = rawStoreProduct.fullIdentifier

    override val productIdentifier: String
        get() = rawStoreProduct.productIdentifier

    override val price: BigDecimal
        get() = rawStoreProduct.price

    override val localizedPrice: String
        get() = rawStoreProduct.localizedPrice

    override val localizedSubscriptionPeriod: String
        get() = rawStoreProduct.localizedSubscriptionPeriod

    override val period: String
        get() = rawStoreProduct.period

    override val periodly: String
        get() = rawStoreProduct.periodly

    override val periodWeeks: Int
        get() = rawStoreProduct.periodWeeks

    override val periodWeeksString: String
        get() = rawStoreProduct.periodWeeksString

    override val periodMonths: Int
        get() = rawStoreProduct.periodMonths

    override val periodMonthsString: String
        get() = rawStoreProduct.periodMonthsString

    override val periodYears: Int
        get() = rawStoreProduct.periodYears

    override val periodYearsString: String
        get() = rawStoreProduct.periodYearsString

    override val periodDays: Int
        get() = rawStoreProduct.periodDays

    override val periodDaysString: String
        get() = rawStoreProduct.periodDaysString

    override val dailyPrice: String
        get() = rawStoreProduct.dailyPrice

    override val weeklyPrice: String
        get() = rawStoreProduct.weeklyPrice

    override val monthlyPrice: String
        get() = rawStoreProduct.monthlyPrice

    override val yearlyPrice: String
        get() = rawStoreProduct.yearlyPrice

    override val hasFreeTrial: Boolean
        get() = rawStoreProduct.hasFreeTrial

    override val localizedTrialPeriodPrice: String
        get() = rawStoreProduct.localizedTrialPeriodPrice

    override val trialPeriodPrice: BigDecimal
        get() = rawStoreProduct.trialPeriodPrice

    override val trialPeriodEndDate: Date?
        get() = rawStoreProduct.trialPeriodEndDate

    override val trialPeriodEndDateString: String
        get() = rawStoreProduct.trialPeriodEndDateString

    override val trialPeriodDays: Int
        get() = rawStoreProduct.trialPeriodDays

    override val trialPeriodDaysString: String
        get() = rawStoreProduct.trialPeriodDaysString

    override val trialPeriodWeeks: Int
        get() = rawStoreProduct.trialPeriodWeeks

    override val trialPeriodWeeksString: String
        get() = rawStoreProduct.trialPeriodWeeksString

    override val trialPeriodMonths: Int
        get() = rawStoreProduct.trialPeriodMonths

    override val trialPeriodMonthsString: String
        get() = rawStoreProduct.trialPeriodMonthsString

    override val trialPeriodYears: Int
        get() = rawStoreProduct.trialPeriodYears

    override val trialPeriodYearsString: String
        get() = rawStoreProduct.trialPeriodYearsString

    override val trialPeriodText: String
        get() = rawStoreProduct.trialPeriodText

    override val locale: String
        get() = rawStoreProduct.locale

    override val languageCode: String?
        get() = rawStoreProduct.languageCode

    override val currencyCode: String?
        get() = rawStoreProduct.currencyCode

    override val currencySymbol: String?
        get() = rawStoreProduct.currencySymbol

    override val regionCode: String?
        get() = rawStoreProduct.regionCode

    override val subscriptionPeriod: SubscriptionPeriod?
        get() = rawStoreProduct.subscriptionPeriod

    override fun trialPeriodPricePerUnit(unit: SubscriptionPeriod.Unit): String = rawStoreProduct.trialPeriodPricePerUnit(unit)

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
