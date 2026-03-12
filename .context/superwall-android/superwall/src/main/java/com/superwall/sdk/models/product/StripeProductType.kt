package com.superwall.sdk.models.product

import com.superwall.sdk.models.serialization.BigDecimalSerializer
import com.superwall.sdk.store.abstractions.product.StoreProductType
import com.superwall.sdk.store.abstractions.product.SubscriptionPeriod
import com.superwall.sdk.utilities.DateUtils
import com.superwall.sdk.utilities.localizedDateFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

@Serializable
data class StripeProductType(
    @SerialName("product_id")
    val id: String,
    @SerialName("raw_price")
    @Serializable(with = BigDecimalSerializer::class)
    override val price: BigDecimal,
    @SerialName("price")
    override val localizedPrice: String,
    @SerialName("currency_code")
    override val currencyCode: String,
    @SerialName("currency_symbol")
    override val currencySymbol: String,
    @SerialName("price_locale")
    val priceLocale: PriceLocale,
    @SerialName("subscription_period")
    val stripeSubscriptionPeriod: StripeSubscriptionPeriod?,
    @SerialName("subscription_introductory_offer")
    val subscriptionIntroOffer: SubscriptionIntroductoryOffer?,
    @SerialName("entitlements")
    val entitlements: List<String>,
) : StoreProductType {
    @Serializable
    data class PriceLocale(
        @SerialName("identifier")
        val identifier: String,
        @SerialName("language_code")
        val languageCode: String,
        @SerialName("currency_code")
        val currencyCode: String,
        @SerialName("currency_symbol")
        val currencySymbol: String,
    )

    @Serializable
    data class StripeSubscriptionPeriod(
        @SerialName("unit")
        val unit: Unit,
        @SerialName("value")
        val value: Int,
    ) {
        @Serializable
        enum class Unit {
            @SerialName("day")
            day,

            @SerialName("week")
            week,

            @SerialName("month")
            month,

            @SerialName("year")
            year,
        }
    }

    @Serializable
    data class SubscriptionIntroductoryOffer(
        @SerialName("period")
        val period: StripeSubscriptionPeriod,
        @SerialName("price")
        val localizedPrice: String,
        @SerialName("raw_price")
        @Serializable(with = BigDecimalSerializer::class)
        val price: BigDecimal,
        @SerialName("period_count")
        val periodCount: Int,
        @SerialName("payment_method")
        val paymentMethod: PaymentMethod,
    ) {
        @Serializable
        enum class PaymentMethod {
            @SerialName("free_trial")
            freeTrial,

            @SerialName("pay_as_you_go")
            payAsYouGo,

            @SerialName("pay_up_front")
            payUpFront,
        }
    }

    // StoreProductType interface implementations
    override val productType: String
        get() = "stripe"

    override val fullIdentifier: String
        get() = id

    override val productIdentifier: String
        get() = id

    override val localizedSubscriptionPeriod: String
        get() =
            stripeSubscriptionPeriod?.let { period ->
                when (period.unit) {
                    StripeSubscriptionPeriod.Unit.day -> if (period.value == 1) "Daily" else "${period.value} days"
                    StripeSubscriptionPeriod.Unit.week -> if (period.value == 1) "Weekly" else "${period.value} weeks"
                    StripeSubscriptionPeriod.Unit.month -> if (period.value == 1) "Monthly" else "${period.value} months"
                    StripeSubscriptionPeriod.Unit.year -> if (period.value == 1) "Yearly" else "${period.value} years"
                }
            } ?: ""

    override val period: String
        get() = stripeSubscriptionPeriod?.let { "P${it.value}${it.unit.name.uppercase()}" } ?: ""

    override val periodly: String
        get() =
            stripeSubscriptionPeriod?.let {
                when (it.unit) {
                    StripeSubscriptionPeriod.Unit.day -> "daily"
                    StripeSubscriptionPeriod.Unit.week -> "weekly"
                    StripeSubscriptionPeriod.Unit.month -> "monthly"
                    StripeSubscriptionPeriod.Unit.year -> "yearly"
                }
            } ?: ""

    override val periodWeeks: Int
        get() =
            stripeSubscriptionPeriod?.let {
                when (it.unit) {
                    StripeSubscriptionPeriod.Unit.day -> it.value / 7
                    StripeSubscriptionPeriod.Unit.week -> it.value
                    StripeSubscriptionPeriod.Unit.month -> it.value * 4
                    StripeSubscriptionPeriod.Unit.year -> it.value * 52
                }
            } ?: 0

    override val periodWeeksString: String
        get() = periodWeeks.toString()

    override val periodMonths: Int
        get() =
            stripeSubscriptionPeriod?.let {
                when (it.unit) {
                    StripeSubscriptionPeriod.Unit.day -> it.value / 30
                    StripeSubscriptionPeriod.Unit.week -> it.value / 4
                    StripeSubscriptionPeriod.Unit.month -> it.value
                    StripeSubscriptionPeriod.Unit.year -> it.value * 12
                }
            } ?: 0

    override val periodMonthsString: String
        get() = periodMonths.toString()

    override val periodYears: Int
        get() =
            stripeSubscriptionPeriod?.let {
                when (it.unit) {
                    StripeSubscriptionPeriod.Unit.day -> it.value / 365
                    StripeSubscriptionPeriod.Unit.week -> it.value / 52
                    StripeSubscriptionPeriod.Unit.month -> it.value / 12
                    StripeSubscriptionPeriod.Unit.year -> it.value
                }
            } ?: 0

    override val periodYearsString: String
        get() = periodYears.toString()

    override val periodDays: Int
        get() =
            stripeSubscriptionPeriod?.let {
                when (it.unit) {
                    StripeSubscriptionPeriod.Unit.day -> it.value
                    StripeSubscriptionPeriod.Unit.week -> it.value * 7
                    StripeSubscriptionPeriod.Unit.month -> it.value * 30
                    StripeSubscriptionPeriod.Unit.year -> it.value * 365
                }
            } ?: 0

    override val periodDaysString: String
        get() = periodDays.toString()

    override val dailyPrice: String
        get() = calculateAnnualizedPrice(365)

    override val weeklyPrice: String
        get() = calculateAnnualizedPrice(52)

    override val monthlyPrice: String
        get() = calculateAnnualizedPrice(12)

    override val yearlyPrice: String
        get() = calculateAnnualizedPrice(1)

    override val hasFreeTrial: Boolean
        get() = subscriptionIntroOffer?.paymentMethod == SubscriptionIntroductoryOffer.PaymentMethod.freeTrial

    override val localizedTrialPeriodPrice: String
        get() = subscriptionIntroOffer?.localizedPrice ?: ""

    override val trialPeriodPrice: BigDecimal
        get() = subscriptionIntroOffer?.price ?: BigDecimal.ZERO

    override val trialPeriodEndDate: Date?
        get() {
            if (!hasFreeTrial) return null
            val calendar = Calendar.getInstance()
            subscriptionIntroOffer?.let { offer ->
                when (offer.period.unit) {
                    StripeSubscriptionPeriod.Unit.day -> calendar.add(Calendar.DAY_OF_MONTH, offer.period.value * offer.periodCount)
                    StripeSubscriptionPeriod.Unit.week -> calendar.add(Calendar.WEEK_OF_YEAR, offer.period.value * offer.periodCount)
                    StripeSubscriptionPeriod.Unit.month -> calendar.add(Calendar.MONTH, offer.period.value * offer.periodCount)
                    StripeSubscriptionPeriod.Unit.year -> calendar.add(Calendar.YEAR, offer.period.value * offer.periodCount)
                }
            }
            return calendar.time
        }

    override val trialPeriodEndDateString: String
        get() = trialPeriodEndDate?.let { localizedDateFormat(DateUtils.MMM_dd_yyyy).format(it) } ?: ""

    override val trialPeriodDays: Int
        get() =
            subscriptionIntroOffer?.let { offer ->
                when (offer.period.unit) {
                    StripeSubscriptionPeriod.Unit.day -> offer.period.value * offer.periodCount
                    StripeSubscriptionPeriod.Unit.week -> offer.period.value * offer.periodCount * 7
                    StripeSubscriptionPeriod.Unit.month -> offer.period.value * offer.periodCount * 30
                    StripeSubscriptionPeriod.Unit.year -> offer.period.value * offer.periodCount * 365
                }
            } ?: 0

    override val trialPeriodDaysString: String
        get() = trialPeriodDays.toString()

    override val trialPeriodWeeks: Int
        get() = trialPeriodDays / 7

    override val trialPeriodWeeksString: String
        get() = trialPeriodWeeks.toString()

    override val trialPeriodMonths: Int
        get() = trialPeriodDays / 30

    override val trialPeriodMonthsString: String
        get() = trialPeriodMonths.toString()

    override val trialPeriodYears: Int
        get() = trialPeriodDays / 365

    override val trialPeriodYearsString: String
        get() = trialPeriodYears.toString()

    override val trialPeriodText: String
        get() =
            subscriptionIntroOffer?.let { offer ->
                val count = offer.periodCount
                when (offer.period.unit) {
                    StripeSubscriptionPeriod.Unit.day -> if (count == 1) "1 day free trial" else "$count days free trial"
                    StripeSubscriptionPeriod.Unit.week -> if (count == 1) "1 week free trial" else "$count weeks free trial"
                    StripeSubscriptionPeriod.Unit.month -> if (count == 1) "1 month free trial" else "$count months free trial"
                    StripeSubscriptionPeriod.Unit.year -> if (count == 1) "1 year free trial" else "$count years free trial"
                }
            } ?: ""

    override val locale: String
        get() = priceLocale.identifier

    override val languageCode: String?
        get() = priceLocale.languageCode

    override val regionCode: String?
        get() = priceLocale.identifier.split("_").getOrNull(1)

    override val subscriptionPeriod: SubscriptionPeriod?
        get() =
            stripeSubscriptionPeriod?.let {
                val unit =
                    when (it.unit) {
                        StripeSubscriptionPeriod.Unit.day -> SubscriptionPeriod.Unit.day
                        StripeSubscriptionPeriod.Unit.week -> SubscriptionPeriod.Unit.week
                        StripeSubscriptionPeriod.Unit.month -> SubscriptionPeriod.Unit.month
                        StripeSubscriptionPeriod.Unit.year -> SubscriptionPeriod.Unit.year
                    }
                SubscriptionPeriod(it.value, unit)
            }

    override fun trialPeriodPricePerUnit(unit: SubscriptionPeriod.Unit): String {
        val totalDays = trialPeriodDays
        if (totalDays == 0) return ""

        val unitsPerYear =
            when (unit) {
                SubscriptionPeriod.Unit.day -> 365
                SubscriptionPeriod.Unit.week -> 52
                SubscriptionPeriod.Unit.month -> 12
                SubscriptionPeriod.Unit.year -> 1
            }

        // Annualize first, then divide by target units per year
        val annualPrice = trialPeriodPrice.multiply(BigDecimal(365)).divide(BigDecimal(totalDays), 6, RoundingMode.DOWN)
        val pricePerUnit = annualPrice.divide(BigDecimal(unitsPerYear), 2, RoundingMode.DOWN)
        return formatPrice(pricePerUnit)
    }

    /**
     * Calculates price per period by annualizing first, then dividing by target units per year.
     * This matches the paywall editor behavior.
     *
     * @param targetUnitsPerYear How many of the target unit fit in a year (e.g. 365 for daily, 52 for weekly)
     */
    private fun calculateAnnualizedPrice(targetUnitsPerYear: Int): String {
        val subscriptionPeriod = stripeSubscriptionPeriod ?: return ""
        if (price == BigDecimal.ZERO) return formatPrice(BigDecimal.ZERO)

        val sourceUnitsPerYear =
            when (subscriptionPeriod.unit) {
                StripeSubscriptionPeriod.Unit.day -> BigDecimal(365)
                StripeSubscriptionPeriod.Unit.week -> BigDecimal(52)
                StripeSubscriptionPeriod.Unit.month -> BigDecimal(12)
                StripeSubscriptionPeriod.Unit.year -> BigDecimal.ONE
            }

        // Annualize: price * (sourceUnitsPerYear / value), then divide by targetUnitsPerYear
        val annualPrice = price.multiply(sourceUnitsPerYear).divide(BigDecimal(subscriptionPeriod.value), 6, RoundingMode.DOWN)
        val targetPrice = annualPrice.divide(BigDecimal(targetUnitsPerYear), 2, RoundingMode.DOWN)
        return formatPrice(targetPrice)
    }

    private fun formatPrice(price: BigDecimal): String {
        val locale = Locale.forLanguageTag(this.locale)
        val format = NumberFormat.getCurrencyInstance(locale)
        try {
            format.currency = Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            // Fallback if currency code is invalid
        }
        return format.format(price.toDouble())
    }
}
