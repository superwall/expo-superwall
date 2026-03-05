package com.superwall.sdk.store.abstractions.product

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Tests that verify price conversions use the annualize-first approach consistently.
 *
 * The annualize-first model defines:
 *   1 year = 365 days = 52 weeks = 12 months
 *
 * To convert any subscription price to a target period:
 *   1. Annualize: price * (sourceUnitsPerYear / value)
 *   2. Divide by targetUnitsPerYear
 *
 * These tests specifically verify that year→week conversions use 52 weeks/year
 * (not 365/7 ≈ 52.14), which would be inconsistent with the rest of the model.
 */
class AnnualizedPriceConversionTest {
    // -- pricePerWeek: year → week should use 52, not 365/7 --

    @Test
    fun `yearly subscription pricePerWeek uses 52 weeks per year`() {
        val yearly = SubscriptionPeriod(1, SubscriptionPeriod.Unit.year)
        val price = BigDecimal("100.00")

        // Annualize-first: $100 / 52 = $1.92 (truncated)
        // Bug (365/7): $100 / 52.1428571 = $1.91 (truncated)
        val weeklyPrice = yearly.pricePerWeek(price)
        assertEquals(
            "Yearly→weekly should divide by 52 (annualize-first), not 365/7",
            BigDecimal("1.92"),
            weeklyPrice,
        )
    }

    @Test
    fun `yearly subscription 365 pricePerWeek uses 52 weeks per year`() {
        val yearly = SubscriptionPeriod(1, SubscriptionPeriod.Unit.year)
        val price = BigDecimal("365.00")

        // Annualize-first: $365 / 52 = $7.01 (truncated)
        // Bug (365/7): $365 / 52.1428571 = $7.00 (truncated)
        val weeklyPrice = yearly.pricePerWeek(price)
        assertEquals(
            "Yearly→weekly should divide by 52 (annualize-first), not 365/7",
            BigDecimal("7.01"),
            weeklyPrice,
        )
    }

    @Test
    fun `2-year subscription pricePerWeek uses 52 weeks per year`() {
        val biYearly = SubscriptionPeriod(2, SubscriptionPeriod.Unit.year)
        val price = BigDecimal("200.00")

        // Annualize-first: $200 / (52 * 2) = $200 / 104 = $1.92 (truncated)
        // Bug (365/7 * 2): $200 / 104.2857 = $1.91 (truncated)
        val weeklyPrice = biYearly.pricePerWeek(price)
        assertEquals(
            "2-year→weekly should divide by 52*2=104 (annualize-first)",
            BigDecimal("1.92"),
            weeklyPrice,
        )
    }

    // -- Cross-period consistency: all paths through annualization should agree --

    @Test
    fun `annualize-first is consistent across all period types for weekly conversion`() {
        // All of these represent the same annual cost of $120/year
        val daily = SubscriptionPeriod(1, SubscriptionPeriod.Unit.day)
        val weekly = SubscriptionPeriod(1, SubscriptionPeriod.Unit.week)
        val monthly = SubscriptionPeriod(1, SubscriptionPeriod.Unit.month)
        val yearly = SubscriptionPeriod(1, SubscriptionPeriod.Unit.year)

        // Prices that annualize to ~$120/year under the annualize-first model:
        //   daily:  $120/365 per day
        //   weekly: $120/52 per week
        //   monthly: $120/12 = $10/month
        //   yearly:  $120/year
        val dailyPrice = BigDecimal("120").divide(BigDecimal("365"), 7, RoundingMode.DOWN)
        val weeklyInputPrice = BigDecimal("120").divide(BigDecimal("52"), 7, RoundingMode.DOWN)
        val monthlyInputPrice = BigDecimal("10.00")
        val yearlyInputPrice = BigDecimal("120.00")

        // All should produce the same weekly price: $120/52 = $2.30 (truncated)
        val expectedWeekly = BigDecimal("2.30")

        assertEquals(
            "Daily→weekly via annualize-first",
            expectedWeekly,
            daily.pricePerWeek(dailyPrice),
        )
        assertEquals(
            "Weekly→weekly (identity)",
            expectedWeekly,
            weekly.pricePerWeek(weeklyInputPrice),
        )
        assertEquals(
            "Monthly→weekly via annualize-first",
            expectedWeekly,
            monthly.pricePerWeek(monthlyInputPrice),
        )
        // This is the one that fails with the 365/7 bug
        assertEquals(
            "Yearly→weekly via annualize-first should match other periods",
            expectedWeekly,
            yearly.pricePerWeek(yearlyInputPrice),
        )
    }
}
