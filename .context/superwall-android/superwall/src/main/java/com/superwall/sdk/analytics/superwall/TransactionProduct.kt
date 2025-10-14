package com.superwall.sdk.analytics.superwall

import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.abstractions.product.SubscriptionPeriod
import java.math.BigDecimal
import java.util.*

data class TransactionProduct(
    val id: String,
    val price: Price,
    val trialPeriod: TrialPeriod?,
    val period: Period?,
    val locale: String,
    val languageCode: String?,
    val currency: Currency,
) {
    data class Price(
        val raw: BigDecimal,
        val localized: String,
        val daily: String,
        val weekly: String,
        val monthly: String,
        val yearly: String,
    )

    data class TrialPeriod(
        val days: Int,
        val weeks: Int,
        val months: Int,
        val years: Int,
        val text: String,
        val endAt: Date?,
    )

    data class Period(
        val alt: String,
        val ly: String,
        val unit: SubscriptionPeriod.Unit,
        val days: Int,
        val weeks: Int,
        val months: Int,
        val years: Int,
    )

    data class Currency(
        val code: String?,
        val symbol: String?,
    )

    constructor(product: StoreProduct) : this(
        id = product.productIdentifier,
        price =
            Price(
                raw = product.price,
                localized = product.localizedPrice,
                daily = product.dailyPrice,
                weekly = product.weeklyPrice,
                monthly = product.monthlyPrice,
                yearly = product.yearlyPrice,
            ),
        trialPeriod =
            product.trialPeriodEndDate?.let {
                TrialPeriod(
                    days = product.trialPeriodDays,
                    weeks = product.trialPeriodWeeks,
                    months = product.trialPeriodMonths,
                    years = product.trialPeriodYears,
                    text = product.trialPeriodText,
                    endAt = it,
                )
            },
        period =
            product.subscriptionPeriod?.let {
                Period(
                    alt = product.localizedSubscriptionPeriod,
                    ly = "${product.period}ly",
                    unit = it.unit,
                    days = product.periodDays,
                    weeks = product.periodWeeks,
                    months = product.periodMonths,
                    years = product.periodYears,
                )
            },
        locale = product.locale,
        languageCode = product.languageCode,
        currency =
            Currency(
                code = product.currencyCode,
                symbol = product.currencySymbol,
            ),
    )
}
