package com.superwall.sdk.store.abstractions.product

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.superwall.sdk.billing.DecomposedProductIds
import com.superwall.sdk.contrib.threeteen.AmountFormats
import com.superwall.sdk.utilities.DateUtils
import com.superwall.sdk.utilities.localizedDateFormat
import kotlinx.serialization.Transient
import org.threeten.bp.Period
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Currency
import java.util.Locale

class RawStoreProduct(
    val underlyingProductDetails: ProductDetails,
    override val fullIdentifier: String,
    val basePlanType: BasePlanType,
    private val offerType: OfferType,
) : StoreProductType {
    /** Returns the base plan ID if specific, null if auto */
    val basePlanId: String? get() = basePlanType.specificId

    companion object {
        fun from(details: ProductDetails): RawStoreProduct {
            val ids = DecomposedProductIds.from(details.productId)
            return RawStoreProduct(
                underlyingProductDetails = details,
                fullIdentifier = details.productId,
                basePlanType = ids.basePlanType,
                offerType = ids.offerType,
            )
        }
    }

    @Transient
    private val priceFormatterProvider = PriceFormatterProvider()

    private val priceFormatter by lazy {
        currencyCode?.let {
            priceFormatterProvider.priceFormatter(it)
        }
    }

    internal val offerId: String? by lazy {
        when (val offer = selectedOffer) {
            is SelectedOfferDetails.Subscription -> offer.underlying.offerId
            is SelectedOfferDetails.OneTime -> offer.offerId
            null -> null
        }
    }

    val selectedOffer: SelectedOfferDetails? by lazy {
        getSelectedOfferDetails()
    }

    private val basePriceForSelectedOffer by lazy {
        when (val offer = selectedOffer) {
            is SelectedOfferDetails.Subscription -> {
                val priceAmountMicros =
                    offer.underlying.pricingPhases.pricingPhaseList
                        .last()
                        .priceAmountMicros
                BigDecimal(priceAmountMicros).divide(BigDecimal(1_000_000), 2, RoundingMode.DOWN)
            }
            is SelectedOfferDetails.OneTime -> {
                BigDecimal(offer.underlying.priceAmountMicros).divide(BigDecimal(1_000_000), 2, RoundingMode.DOWN)
            }
            null -> BigDecimal.ZERO
        }
    }

    private val selectedOfferPricingPhase: PricingPhase? by lazy {
        // OneTime products don't have pricing phases - only subscriptions do
        when (val offer = selectedOffer) {
            is SelectedOfferDetails.Subscription -> {
                // Find the first free trial phase or discounted phase.
                offer.underlying.pricingPhases.pricingPhaseList
                    .firstOrNull { it.priceAmountMicros == 0L }
                    ?: offer.underlying.pricingPhases.pricingPhaseList
                        .dropLast(1)
                        .firstOrNull { it.priceAmountMicros != 0L }
            }
            is SelectedOfferDetails.OneTime -> null // No pricing phases for one-time products
            null -> null
        }
    }

    override val productIdentifier by lazy {
        underlyingProductDetails.productId
    }

    override val productType: String
        get() = underlyingProductDetails.productType

    override val price by lazy {
        // Always use the price from the selected offer (handles both subscription and OTP with purchase options)
        // Only fall back to legacy oneTimePurchaseOfferDetails if no selected offer exists
        if (selectedOffer != null) {
            basePriceForSelectedOffer
        } else {
            underlyingProductDetails.oneTimePurchaseOfferDetails?.let { offerDetails ->
                BigDecimal(offerDetails.priceAmountMicros).divide(
                    BigDecimal(1_000_000),
                    2,
                    RoundingMode.DOWN,
                )
            } ?: BigDecimal.ZERO
        }
    }

    override val localizedPrice by lazy {
        priceFormatter?.format(price) ?: ""
    }

    override val localizedSubscriptionPeriod by lazy {
        subscriptionPeriod?.let {
            AmountFormats.wordBased(it.toPeriod(), Locale.getDefault())
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

    override val periodWeeks by lazy {
        subscriptionPeriod?.let {
            val numberOfUnits = it.value
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> (1 * numberOfUnits) / 7
                SubscriptionPeriod.Unit.week -> numberOfUnits
                SubscriptionPeriod.Unit.month -> 4 * numberOfUnits
                SubscriptionPeriod.Unit.year -> 52 * numberOfUnits
            }
        } ?: 0
    }

    override val periodWeeksString by lazy {
        periodWeeks.toString()
    }

    override val periodMonths by lazy {
        subscriptionPeriod?.let {
            val numberOfUnits = it.value
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> numberOfUnits / 30
                SubscriptionPeriod.Unit.week -> numberOfUnits / 4
                SubscriptionPeriod.Unit.month -> numberOfUnits
                SubscriptionPeriod.Unit.year -> 12 * numberOfUnits
            }
        } ?: 0
    }

    override val periodMonthsString: String by lazy {
        periodMonths.toString()
    }

    override val periodYears by lazy {
        subscriptionPeriod?.let {
            val numberOfUnits = it.value
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> numberOfUnits / 365
                SubscriptionPeriod.Unit.week -> numberOfUnits / 52
                SubscriptionPeriod.Unit.month -> numberOfUnits / 12
                SubscriptionPeriod.Unit.year -> numberOfUnits
            }
        } ?: 0
    }

    override val periodYearsString by lazy {
        periodYears.toString()
    }

    override val periodDays by lazy {
        subscriptionPeriod?.let {
            val numberOfUnits = it.value
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> 1 * numberOfUnits
                SubscriptionPeriod.Unit.month -> 30 * numberOfUnits // Assumes 30 days in a month
                SubscriptionPeriod.Unit.week -> 7 * numberOfUnits // Assumes 7 days in a week
                SubscriptionPeriod.Unit.year -> 365 * numberOfUnits // Assumes 365 days in a year
            }
        } ?: 0
    }

    override val periodDaysString by lazy {
        periodDays.toString()
    }

    override val dailyPrice by lazy {
        // One-time purchases don't have period-based pricing (aligned with iOS)
        if (selectedOffer is SelectedOfferDetails.OneTime) {
            return@lazy "n/a"
        }

        val basePrice = basePriceForSelectedOffer

        if (basePrice == BigDecimal.ZERO) {
            return@lazy priceFormatter?.format(BigDecimal.ZERO) ?: "$0.00"
        }
        val subscriptionPeriod = this.subscriptionPeriod ?: return@lazy "n/a"

        val pricePerDay = subscriptionPeriod.pricePerDay(basePrice)

        priceFormatter?.format(pricePerDay) ?: "n/a"
    }

    override val weeklyPrice by lazy {
        // One-time purchases don't have period-based pricing (aligned with iOS)
        if (selectedOffer is SelectedOfferDetails.OneTime) {
            return@lazy "n/a"
        }

        val basePrice = basePriceForSelectedOffer

        if (basePrice == BigDecimal.ZERO) {
            return@lazy priceFormatter?.format(BigDecimal.ZERO) ?: "$0.00"
        }
        val subscriptionPeriod = this.subscriptionPeriod ?: return@lazy "n/a"
        val pricePerWeek = subscriptionPeriod.pricePerWeek(basePrice)
        priceFormatter?.format(pricePerWeek) ?: "n/a"
    }

    override val monthlyPrice by lazy {
        // One-time purchases don't have period-based pricing (aligned with iOS)
        if (selectedOffer is SelectedOfferDetails.OneTime) {
            return@lazy "n/a"
        }

        val basePrice = basePriceForSelectedOffer

        if (basePrice == BigDecimal.ZERO) {
            return@lazy priceFormatter?.format(BigDecimal.ZERO) ?: "$0.00"
        }

        val subscriptionPeriod = this.subscriptionPeriod ?: return@lazy "n/a"

        val pricePerMonth = subscriptionPeriod.pricePerMonth(basePrice)

        priceFormatter?.format(pricePerMonth) ?: "n/a"
    }

    override val yearlyPrice by lazy {
        // One-time purchases don't have period-based pricing (aligned with iOS)
        if (selectedOffer is SelectedOfferDetails.OneTime) {
            return@lazy "n/a"
        }

        val basePrice = basePriceForSelectedOffer

        if (basePrice == BigDecimal.ZERO) {
            return@lazy priceFormatter?.format(BigDecimal.ZERO) ?: "$0.00"
        }

        val subscriptionPeriod = this.subscriptionPeriod ?: return@lazy "n/a"
        val pricePerYear = subscriptionPeriod.pricePerYear(basePrice)

        priceFormatter?.format(pricePerYear) ?: "n/a"
    }

    override val hasFreeTrial by lazy {
        when (val offer = selectedOffer) {
            is SelectedOfferDetails.Subscription -> {
                // Check for free trial phase in pricing phases, excluding the base pricing
                offer.underlying.pricingPhases.pricingPhaseList
                    .dropLast(1)
                    .isNotEmpty()
            }
            is SelectedOfferDetails.OneTime -> false // One-time products don't have trials
            null -> false
        }
    }

    override val localizedTrialPeriodPrice by lazy {
        priceFormatter?.format(trialPeriodPrice) ?: "$0.00"
    }

    override val trialPeriodPrice: BigDecimal by lazy {
        when (val offer = selectedOffer) {
            is SelectedOfferDetails.Subscription -> {
                val pricingWithoutBase =
                    offer.underlying.pricingPhases.pricingPhaseList
                        .dropLast(1)
                if (pricingWithoutBase.isEmpty()) return@lazy BigDecimal.ZERO

                // Check for free trial phase
                val freeTrialPhase = pricingWithoutBase.firstOrNull { it.priceAmountMicros == 0L }
                if (freeTrialPhase != null) return@lazy BigDecimal.ZERO

                // Check for discounted phase
                val discountedPhase = pricingWithoutBase.firstOrNull { it.priceAmountMicros > 0 }
                discountedPhase?.let {
                    BigDecimal(it.priceAmountMicros).divide(BigDecimal(1_000_000), 2, RoundingMode.DOWN)
                } ?: BigDecimal.ZERO
            }
            is SelectedOfferDetails.OneTime -> BigDecimal.ZERO // One-time products don't have trial pricing
            null -> BigDecimal.ZERO
        }
    }

    private fun getSelectedOfferDetails(): SelectedOfferDetails? {
        // Handle one-time purchase products with purchase options list
        if (!underlyingProductDetails.oneTimePurchaseOfferDetailsList.isNullOrEmpty()) {
            val list = underlyingProductDetails.oneTimePurchaseOfferDetailsList ?: emptyList()
            val hasSpecificPurchaseOption = basePlanType is BasePlanType.Specific
            val offerIdFromType = offerType.specificId
            val hasSpecificOffer = offerType is OfferType.Specific
            val isOfferNone = offerType is OfferType.None

            val selected: ProductDetails.OneTimePurchaseOfferDetails? =
                when {
                    // Case 0: Offer is None (sw-none) - select purchase option without any discount offer
                    isOfferNone -> {
                        val optionsWithoutOffer =
                            if (hasSpecificPurchaseOption) {
                                list.filter { it.purchaseOptionId == basePlanId && it.offerId.isNullOrEmpty() }
                            } else {
                                list.filter { it.offerId.isNullOrEmpty() }
                            }
                        optionsWithoutOffer.minByOrNull { it.priceAmountMicros }
                    }

                    // Case 1: Specific purchase option + specific offer
                    hasSpecificPurchaseOption && hasSpecificOffer -> {
                        val exactMatch = list.firstOrNull { it.purchaseOptionId == basePlanId && it.offerId == offerIdFromType }
                        exactMatch ?: list.firstOrNull { it.purchaseOptionId == basePlanId }
                    }

                    // Case 2: Auto purchase option + specific offer
                    !hasSpecificPurchaseOption && hasSpecificOffer -> {
                        list.filter { it.offerId == offerIdFromType }.minByOrNull { it.priceAmountMicros }
                    }

                    // Case 3: Specific purchase option + auto offer
                    hasSpecificPurchaseOption -> {
                        list.filter { it.purchaseOptionId == basePlanId }.minByOrNull { it.priceAmountMicros }
                    }

                    // Case 4: Auto purchase option + auto offer
                    else -> {
                        val optionsWithBoth = list.filter { !it.purchaseOptionId.isNullOrEmpty() && !it.offerId.isNullOrEmpty() }
                        optionsWithBoth.minByOrNull { it.priceAmountMicros } ?: list.minByOrNull { it.priceAmountMicros }
                    }
                }

            // Only use legacy fallback if we're NOT explicitly requesting no offer (sw-none)
            val finalSelected =
                if (selected == null && !isOfferNone) {
                    underlyingProductDetails.oneTimePurchaseOfferDetails
                } else {
                    selected
                }
            return finalSelected?.let { SelectedOfferDetails.OneTime(it, it.purchaseOptionId, it.offerId) }
        }

        // Handle legacy one-time purchase products (without purchase options list)
        underlyingProductDetails.oneTimePurchaseOfferDetails?.let {
            return SelectedOfferDetails.OneTime(it, it.purchaseOptionId, it.offerId)
        }

        // Handle subscription products
        val subscriptionOfferDetails = underlyingProductDetails.subscriptionOfferDetails ?: return null

        // Default to first base plan if base plan is empty
        if (basePlanId.isNullOrEmpty()) {
            val firstBasePlan = subscriptionOfferDetails.firstOrNull { it.pricingPhases.pricingPhaseList.size == 1 }
            return firstBasePlan?.let { SelectedOfferDetails.Subscription(it) }
        }

        val offersForBasePlan = subscriptionOfferDetails.filter { it.basePlanId == basePlanId }
        val basePlan = offersForBasePlan.firstOrNull { it.pricingPhases.pricingPhaseList.size == 1 } ?: return null

        val selectedSubscriptionOffer: SubscriptionOfferDetails =
            when (offerType) {
                is OfferType.Auto -> automaticallySelectSubscriptionOffer()?.underlying ?: basePlan
                is OfferType.None -> basePlan
                is OfferType.Specific -> offersForBasePlan.firstOrNull { it.offerId == offerType.id } ?: basePlan
            }

        return SelectedOfferDetails.Subscription(selectedSubscriptionOffer)
    }

    /**
     * For automatically selecting a SUBSCRIPTION offer:
     *   - Filters out offers with "-ignore-offer" tag
     *   - Uses offer with longest free trial or cheapest first phase
     *   - Falls back to use base plan
     *
     * Note: One-time purchase selection is handled directly in getSelectedOfferDetails()
     */
    private fun automaticallySelectSubscriptionOffer(): SelectedOfferDetails.Subscription? {
        val subscriptionOfferDetails =
            underlyingProductDetails.subscriptionOfferDetails ?: run {
                return null
            }

        // Get the offers that match the given base plan ID.
        val offersForBasePlan = subscriptionOfferDetails.filter { it.basePlanId == basePlanId }

        val validOffers =
            offersForBasePlan
                // Ignore base plan
                .filter { it.pricingPhases.pricingPhaseList.size != 1 }
                // Ignore those with a tag that contains "ignore-offer"
                .filter { !it.offerTags.any { it.contains("-ignore-offer") } }

        val longestTrialOffer = findLongestFreeTrial(validOffers)
        val cheapestOffer = findLowestNonFreeOffer(validOffers)
        val selected = longestTrialOffer ?: cheapestOffer

        return selected?.let { SelectedOfferDetails.Subscription(it) }
    }

    private fun findLongestFreeTrial(offers: List<SubscriptionOfferDetails>): SubscriptionOfferDetails? =
        offers
            .mapNotNull { offer ->
                offer.pricingPhases.pricingPhaseList
                    .dropLast(1)
                    .firstOrNull {
                        it.priceAmountMicros == 0L
                    }?.let { pricingPhase ->
                        val period = Period.parse(pricingPhase.billingPeriod)
                        val totalDays = period.toTotalMonths() * 30 + period.days
                        Pair(offer, totalDays)
                    }
            }.maxByOrNull { it.second }
            ?.first

    private fun findLowestNonFreeOffer(offers: List<SubscriptionOfferDetails>): SubscriptionOfferDetails? {
        val hi =
            offers
                .mapNotNull { offer ->
                    offer.pricingPhases.pricingPhaseList
                        .dropLast(1)
                        .firstOrNull {
                            it.priceAmountMicros > 0L
                        }?.let { pricingPhase ->
                            Pair(offer, pricingPhase.priceAmountMicros)
                        }
                }.minByOrNull { it.second }
                ?.first
        return hi
    }

    override val trialPeriodEndDate by lazy {
        trialSubscriptionPeriod?.let {
            val calendar = Calendar.getInstance()
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> calendar.add(Calendar.DAY_OF_YEAR, it.value)
                SubscriptionPeriod.Unit.week -> calendar.add(Calendar.WEEK_OF_YEAR, it.value)
                SubscriptionPeriod.Unit.month -> calendar.add(Calendar.MONTH, it.value)
                SubscriptionPeriod.Unit.year -> calendar.add(Calendar.YEAR, it.value)
            }
            calendar.time
        }
    }

    override val trialPeriodEndDateString by lazy {
        trialPeriodEndDate?.let {
            val dateFormatter = localizedDateFormat(DateUtils.MMM_dd_yyyy)
            dateFormatter.format(it)
        } ?: ""
    }

    override val trialPeriodDays by lazy {
        trialSubscriptionPeriod?.let {
            val numberOfUnits = it.value

            when (it.unit) {
                SubscriptionPeriod.Unit.day -> 1 * numberOfUnits
                SubscriptionPeriod.Unit.month -> 30 * numberOfUnits // Assumes 30 days in a month
                SubscriptionPeriod.Unit.week -> 7 * numberOfUnits // Assumes 7 days in a week
                SubscriptionPeriod.Unit.year -> 365 * numberOfUnits // Assumes 365 days in a year
            }
        } ?: 0
    }

    override val trialPeriodDaysString by lazy {
        trialPeriodDays.toString()
    }

    override val trialPeriodWeeks by lazy {
        val trialPeriod = trialSubscriptionPeriod ?: return@lazy 0
        val numberOfUnits = trialPeriod.value

        when (trialPeriod.unit) {
            SubscriptionPeriod.Unit.day -> numberOfUnits / 7
            SubscriptionPeriod.Unit.month -> 4 * numberOfUnits // Assumes 4 weeks in a month
            SubscriptionPeriod.Unit.week -> 1 * numberOfUnits
            SubscriptionPeriod.Unit.year -> 52 * numberOfUnits // Assumes 52 weeks in a year
        }
    }

    override val trialPeriodWeeksString by lazy {
        trialPeriodWeeks.toString()
    }

    override val trialPeriodMonths by lazy {
        trialSubscriptionPeriod?.let {
            val numberOfUnits = it.value
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> numberOfUnits / 30
                SubscriptionPeriod.Unit.week -> numberOfUnits / 4
                SubscriptionPeriod.Unit.month -> numberOfUnits
                SubscriptionPeriod.Unit.year -> 12 * numberOfUnits
            }
        } ?: 0
    }

    override val trialPeriodMonthsString by lazy {
        trialPeriodMonths.toString()
    }

    override val trialPeriodYears by lazy {
        trialSubscriptionPeriod?.let {
            val numberOfUnits = it.value
            when (it.unit) {
                SubscriptionPeriod.Unit.day -> numberOfUnits / 365
                SubscriptionPeriod.Unit.week -> numberOfUnits / 52
                SubscriptionPeriod.Unit.month -> numberOfUnits / 12
                SubscriptionPeriod.Unit.year -> numberOfUnits
            }
        } ?: 0
    }

    override val trialPeriodYearsString by lazy {
        trialPeriodYears.toString()
    }

    override val trialPeriodText by lazy {
        val trialPeriod = trialSubscriptionPeriod ?: return@lazy ""
        val units = trialPeriod.value

        when (trialPeriod.unit) {
            SubscriptionPeriod.Unit.day -> "$units-day"
            SubscriptionPeriod.Unit.month -> "${units * 30}-day"
            SubscriptionPeriod.Unit.week -> "${units * 7}-day"
            SubscriptionPeriod.Unit.year -> "${units * 365}-day"
        }
    }

    // TODO: Differs from iOS, using device locale here instead of product locale
    override val locale by lazy {
        Locale.getDefault().toString()
    }

    // TODO: Differs from iOS, using device language code here instead of product language code
    override val languageCode: String? by lazy {
        Locale.getDefault().language
    }

    override val currencyCode by lazy {
        when (val offer = selectedOffer) {
            is SelectedOfferDetails.Subscription -> {
                offer.underlying.pricingPhases.pricingPhaseList
                    .last()
                    .priceCurrencyCode
            }
            is SelectedOfferDetails.OneTime -> offer.underlying.priceCurrencyCode
            null -> null
        }
    }

    override val currencySymbol by lazy {
        currencyCode?.let { Currency.getInstance(it).symbol }
    }

    override val regionCode: String? by lazy {
        Locale.getDefault().country
    }

    override val subscriptionPeriod by lazy {
        when (val offer = selectedOffer) {
            is SelectedOfferDetails.Subscription -> {
                val baseBillingPeriod =
                    offer.underlying.pricingPhases.pricingPhaseList
                        .last()
                        .billingPeriod
                try {
                    SubscriptionPeriod.from(baseBillingPeriod)
                } catch (e: Throwable) {
                    null
                }
            }
            is SelectedOfferDetails.OneTime -> null // One-time products don't have subscription periods
            null -> null
        }
    }

    override fun trialPeriodPricePerUnit(unit: SubscriptionPeriod.Unit): String {
        val pricingPhase = selectedOfferPricingPhase ?: return priceFormatter?.format(0) ?: "$0.00"

        if (pricingPhase.priceAmountMicros == 0L) {
            return priceFormatter?.format(0) ?: "$0.00"
        }

        val introPrice =
            pricePerUnit(
                unit = unit,
                pricingPhase = pricingPhase,
            )

        return priceFormatter?.format(introPrice) ?: "$0.00"
    }

    private fun pricePerUnit(
        unit: SubscriptionPeriod.Unit,
        pricingPhase: PricingPhase,
    ): BigDecimal {
        if (pricingPhase.priceAmountMicros == 0L) {
            return BigDecimal.ZERO
        } else {
            // The total cost that you'll pay
            val trialPeriodPrice =
                BigDecimal(pricingPhase.priceAmountMicros).divide(
                    BigDecimal(1_000_000),
                    6,
                    RoundingMode.DOWN,
                )
            val introCost = trialPeriodPrice.multiply(BigDecimal(pricingPhase.billingCycleCount))

            // The number of total units normalized to the unit you want.
            val billingPeriod = selectedOfferPricingPhase?.billingPeriod

            // Attempt to create a SubscriptionPeriod from billingPeriod.
            // Return null if there's an exception or if billingPeriod is null.
            val trialSubscriptionPeriod =
                try {
                    billingPeriod?.let { SubscriptionPeriod.from(it) }
                } catch (e: Throwable) {
                    null
                }
            val introPeriods =
                periodsPerUnit(unit)
                    .multiply(BigDecimal(pricingPhase.billingCycleCount))
                    .multiply(BigDecimal(trialSubscriptionPeriod?.value ?: 0))

            val introPayment: BigDecimal =
                if (introPeriods < BigDecimal.ONE) {
                    // If less than 1, it means the intro period doesn't exceed a full unit.
                    introCost
                } else {
                    // Otherwise, divide the total cost by the normalized intro periods.
                    introCost.divide(introPeriods, 2, RoundingMode.DOWN)
                }

            return introPayment
        }
    }

    private fun periodsPerUnit(unit: SubscriptionPeriod.Unit): BigDecimal =
        when (unit) {
            SubscriptionPeriod.Unit.day -> {
                when (trialSubscriptionPeriod?.unit) {
                    SubscriptionPeriod.Unit.day -> BigDecimal(1)
                    SubscriptionPeriod.Unit.week -> BigDecimal(365).divide(BigDecimal(52), 6, RoundingMode.DOWN)
                    SubscriptionPeriod.Unit.month -> BigDecimal(365).divide(BigDecimal(12), 6, RoundingMode.DOWN)
                    SubscriptionPeriod.Unit.year -> BigDecimal(365)
                    else -> BigDecimal.ZERO
                }
            }

            SubscriptionPeriod.Unit.week -> {
                when (trialSubscriptionPeriod?.unit) {
                    SubscriptionPeriod.Unit.day ->
                        BigDecimal(52).divide(
                            BigDecimal(365),
                            6,
                            RoundingMode.DOWN,
                        )

                    SubscriptionPeriod.Unit.week -> BigDecimal(1)
                    SubscriptionPeriod.Unit.month -> BigDecimal(52).divide(BigDecimal(12), 6, RoundingMode.DOWN)
                    SubscriptionPeriod.Unit.year -> BigDecimal(52)
                    else -> BigDecimal.ZERO
                }
            }

            SubscriptionPeriod.Unit.month -> {
                when (trialSubscriptionPeriod?.unit) {
                    SubscriptionPeriod.Unit.day ->
                        BigDecimal(12).divide(
                            BigDecimal(365),
                            6,
                            RoundingMode.DOWN,
                        )

                    SubscriptionPeriod.Unit.week ->
                        BigDecimal(12).divide(
                            BigDecimal(52),
                            6,
                            RoundingMode.DOWN,
                        )

                    SubscriptionPeriod.Unit.month -> BigDecimal(1)
                    SubscriptionPeriod.Unit.year -> BigDecimal(12)
                    else -> BigDecimal.ZERO
                }
            }

            SubscriptionPeriod.Unit.year -> {
                when (trialSubscriptionPeriod?.unit) {
                    SubscriptionPeriod.Unit.day ->
                        BigDecimal(1).divide(
                            BigDecimal(365),
                            6,
                            RoundingMode.DOWN,
                        )

                    SubscriptionPeriod.Unit.week ->
                        BigDecimal(1).divide(
                            BigDecimal(52),
                            6,
                            RoundingMode.DOWN,
                        )

                    SubscriptionPeriod.Unit.month ->
                        BigDecimal(1).divide(
                            BigDecimal(12),
                            6,
                            RoundingMode.DOWN,
                        )

                    SubscriptionPeriod.Unit.year -> BigDecimal.ONE
                    else -> BigDecimal.ZERO
                }
            }
        }

    private val trialSubscriptionPeriod by lazy {
        // If oneTimePurchaseOfferDetails is not null, return null for trial subscription period.
        if (underlyingProductDetails.oneTimePurchaseOfferDetails != null) {
            return@lazy null
        }

        val billingPeriod = selectedOfferPricingPhase?.billingPeriod

        // Attempt to create a SubscriptionPeriod from billingPeriod.
        // Return null if there's an exception or if billingPeriod is null.
        try {
            billingPeriod?.let { SubscriptionPeriod.from(it) }
        } catch (e: Throwable) {
            null
        }
    }

    internal val isSubscription by lazy {
        underlyingProductDetails.subscriptionOfferDetails?.isNotEmpty() ?: false
    }

    sealed class SelectedOfferDetails {
        data class Subscription(
            val underlying: SubscriptionOfferDetails,
        ) : SelectedOfferDetails()

        data class OneTime(
            val underlying: ProductDetails.OneTimePurchaseOfferDetails,
            val purchaseOptionId: String?,
            val offerId: String?,
        ) : SelectedOfferDetails()
    }
}
