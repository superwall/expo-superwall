package com.superwall.sdk.store.testmode

import com.superwall.sdk.store.abstractions.product.PriceFormatterProvider
import com.superwall.sdk.store.abstractions.product.StoreProductType
import com.superwall.sdk.store.abstractions.product.SubscriptionPeriod
import com.superwall.sdk.store.testmode.models.SuperwallProduct
import com.superwall.sdk.store.testmode.models.SuperwallSubscriptionPeriod
import com.superwall.sdk.utilities.DateUtils
import com.superwall.sdk.utilities.localizedDateFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TestStoreProduct(
    private val superwallProduct: SuperwallProduct,
) : StoreProductType {
    private val priceFormatterProvider = PriceFormatterProvider()

    private val priceFormatter by lazy {
        currencyCode?.let { priceFormatterProvider.priceFormatter(it) }
    }

    override val fullIdentifier: String
        get() = superwallProduct.identifier

    override val productIdentifier: String
        get() = superwallProduct.identifier

    override val price: BigDecimal by lazy {
        val amount = superwallProduct.price?.amount ?: 0
        BigDecimal(amount).divide(BigDecimal(100), 2, RoundingMode.DOWN)
    }

    override val localizedPrice: String by lazy {
        priceFormatter?.format(price) ?: "${currencySymbol ?: "$"}$price"
    }

    override val subscriptionPeriod: SubscriptionPeriod? by lazy {
        superwallProduct.subscription?.let { sub ->
            val unit =
                when (sub.period) {
                    SuperwallSubscriptionPeriod.DAY -> SubscriptionPeriod.Unit.day
                    SuperwallSubscriptionPeriod.WEEK -> SubscriptionPeriod.Unit.week
                    SuperwallSubscriptionPeriod.MONTH -> SubscriptionPeriod.Unit.month
                    SuperwallSubscriptionPeriod.YEAR -> SubscriptionPeriod.Unit.year
                }
            SubscriptionPeriod(value = sub.periodCount, unit = unit)
        }
    }

    override val localizedSubscriptionPeriod: String by lazy {
        subscriptionPeriod?.let {
            val count = it.value
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> if (count == 1) "1 day" else "$count days"
                SubscriptionPeriod.Unit.week -> if (count == 1) "1 week" else "$count weeks"
                SubscriptionPeriod.Unit.month -> if (count == 1) "1 month" else "$count months"
                SubscriptionPeriod.Unit.year -> if (count == 1) "1 year" else "$count years"
            }
        } ?: ""
    }

    override val period: String by lazy {
        subscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> if (it.value == 7) "week" else "day"
                SubscriptionPeriod.Unit.week -> "week"
                SubscriptionPeriod.Unit.month ->
                    when (it.value) {
                        2 -> "2 months"
                        3 -> "quarter"
                        6 -> "6 months"
                        else -> "month"
                    }
                SubscriptionPeriod.Unit.year -> "year"
            }
        } ?: ""
    }

    override val periodly: String by lazy {
        subscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.month ->
                    when (it.value) {
                        2, 6 -> "every $period"
                        else -> "${period}ly"
                    }
                else -> "${period}ly"
            }
        } ?: ""
    }

    override val periodWeeks: Int by lazy {
        subscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> (it.value) / 7
                SubscriptionPeriod.Unit.week -> it.value
                SubscriptionPeriod.Unit.month -> 4 * it.value
                SubscriptionPeriod.Unit.year -> 52 * it.value
            }
        } ?: 0
    }

    override val periodWeeksString: String by lazy { periodWeeks.toString() }

    override val periodMonths: Int by lazy {
        subscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> it.value / 30
                SubscriptionPeriod.Unit.week -> it.value / 4
                SubscriptionPeriod.Unit.month -> it.value
                SubscriptionPeriod.Unit.year -> 12 * it.value
            }
        } ?: 0
    }

    override val periodMonthsString: String by lazy { periodMonths.toString() }

    override val periodYears: Int by lazy {
        subscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> it.value / 365
                SubscriptionPeriod.Unit.week -> it.value / 52
                SubscriptionPeriod.Unit.month -> it.value / 12
                SubscriptionPeriod.Unit.year -> it.value
            }
        } ?: 0
    }

    override val periodYearsString: String by lazy { periodYears.toString() }

    override val periodDays: Int by lazy {
        subscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> it.value
                SubscriptionPeriod.Unit.week -> 7 * it.value
                SubscriptionPeriod.Unit.month -> 30 * it.value
                SubscriptionPeriod.Unit.year -> 365 * it.value
            }
        } ?: 0
    }

    override val periodDaysString: String by lazy { periodDays.toString() }

    override val dailyPrice: String by lazy {
        val sp = subscriptionPeriod ?: return@lazy "n/a"
        if (price.compareTo(BigDecimal.ZERO) == 0) return@lazy priceFormatter?.format(BigDecimal.ZERO) ?: "$0.00"
        priceFormatter?.format(sp.pricePerDay(price)) ?: "n/a"
    }

    override val weeklyPrice: String by lazy {
        val sp = subscriptionPeriod ?: return@lazy "n/a"
        if (price.compareTo(BigDecimal.ZERO) == 0) return@lazy priceFormatter?.format(BigDecimal.ZERO) ?: "$0.00"
        priceFormatter?.format(sp.pricePerWeek(price)) ?: "n/a"
    }

    override val monthlyPrice: String by lazy {
        val sp = subscriptionPeriod ?: return@lazy "n/a"
        if (price.compareTo(BigDecimal.ZERO) == 0) return@lazy priceFormatter?.format(BigDecimal.ZERO) ?: "$0.00"
        priceFormatter?.format(sp.pricePerMonth(price)) ?: "n/a"
    }

    override val yearlyPrice: String by lazy {
        val sp = subscriptionPeriod ?: return@lazy "n/a"
        if (price == BigDecimal.ZERO) return@lazy priceFormatter?.format(BigDecimal.ZERO) ?: "$0.00"
        priceFormatter?.format(sp.pricePerYear(price)) ?: "n/a"
    }

    private val trialDays: Int
        get() = superwallProduct.subscription?.trialPeriodDays ?: 0

    override val hasFreeTrial: Boolean by lazy {
        trialDays > 0
    }

    override val localizedTrialPeriodPrice: String by lazy {
        priceFormatter?.format(trialPeriodPrice) ?: "$0.00"
    }

    override val trialPeriodPrice: BigDecimal = BigDecimal.ZERO

    private val trialSubscriptionPeriod: SubscriptionPeriod? by lazy {
        if (trialDays <= 0) return@lazy null
        SubscriptionPeriod(value = trialDays, unit = SubscriptionPeriod.Unit.day).normalized()
    }

    override val trialPeriodEndDate: Date? by lazy {
        if (trialDays <= 0) return@lazy null
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, trialDays)
        calendar.time
    }

    override val trialPeriodEndDateString: String by lazy {
        trialPeriodEndDate?.let {
            localizedDateFormat(DateUtils.MMM_dd_yyyy).format(it)
        } ?: ""
    }

    override val trialPeriodDays: Int by lazy {
        trialSubscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> it.value
                SubscriptionPeriod.Unit.week -> 7 * it.value
                SubscriptionPeriod.Unit.month -> 30 * it.value
                SubscriptionPeriod.Unit.year -> 365 * it.value
            }
        } ?: 0
    }

    override val trialPeriodDaysString: String by lazy { trialPeriodDays.toString() }

    override val trialPeriodWeeks: Int by lazy {
        trialSubscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> it.value / 7
                SubscriptionPeriod.Unit.week -> it.value
                SubscriptionPeriod.Unit.month -> 4 * it.value
                SubscriptionPeriod.Unit.year -> 52 * it.value
            }
        } ?: 0
    }

    override val trialPeriodWeeksString: String by lazy { trialPeriodWeeks.toString() }

    override val trialPeriodMonths: Int by lazy {
        trialSubscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> it.value / 30
                SubscriptionPeriod.Unit.week -> it.value / 4
                SubscriptionPeriod.Unit.month -> it.value
                SubscriptionPeriod.Unit.year -> 12 * it.value
            }
        } ?: 0
    }

    override val trialPeriodMonthsString: String by lazy { trialPeriodMonths.toString() }

    override val trialPeriodYears: Int by lazy {
        trialSubscriptionPeriod?.let {
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> it.value / 365
                SubscriptionPeriod.Unit.week -> it.value / 52
                SubscriptionPeriod.Unit.month -> it.value / 12
                SubscriptionPeriod.Unit.year -> it.value
            }
        } ?: 0
    }

    override val trialPeriodYearsString: String by lazy { trialPeriodYears.toString() }

    override val trialPeriodText: String by lazy {
        if (trialDays <= 0) return@lazy ""
        "$trialDays-day"
    }

    override fun trialPeriodPricePerUnit(unit: SubscriptionPeriod.Unit): String = priceFormatter?.format(BigDecimal.ZERO) ?: "$0.00"

    override val locale: String by lazy { Locale.getDefault().toString() }

    override val languageCode: String? by lazy { Locale.getDefault().language }

    override val currencyCode: String? by lazy { superwallProduct.price?.currency }

    override val currencySymbol: String? by lazy {
        currencyCode?.let {
            try {
                java.util.Currency
                    .getInstance(it)
                    .symbol
            } catch (e: Throwable) {
                null
            }
        }
    }

    override val regionCode: String? by lazy { Locale.getDefault().country }

    override val productType: String
        get() = if (superwallProduct.subscription != null) "subs" else "inapp"
}
