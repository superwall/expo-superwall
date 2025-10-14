package com.superwall.sdk.store.abstractions.product

import org.junit.Test
import java.math.BigDecimal

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

fun truncateDecimal(
    decimal: BigDecimal,
    places: Int,
): BigDecimal {
    val factor = BigDecimal.TEN.pow(places) // Create a factor of 10^decimalPlaces
    val result: BigDecimal =
        decimal
            .multiply(factor) // Multiply the original number by the factor
            .setScale(0, BigDecimal.ROUND_DOWN) // Set scale to 0 and ROUND_DOWN to truncate
            .divide(factor) // Divide back by the factor to get the truncated number
    return result
}

class SubscriptionPeriodUnitTest {
    @Test
    fun double_period_test() {
        val period = "P4W3D"
        val res = SubscriptionPeriod.from(period)
        println(res)
        assert(res == SubscriptionPeriod(31, SubscriptionPeriod.Unit.day))
    }
/* TODO: Re-enable these in CI
    @Test
    fun singleDaily_isCorrect() {
        val period = SubscriptionPeriod(1, SubscriptionPeriod.Unit.day)
        assert(period.periodDays == 1)
        assert(period.periodWeeks == 0)
        assert(period.periodMonths == 0)
        assert(period.periodYears == 0)


        var correctDailyPrice = truncateDecimal(BigDecimal(4.99), 2)
        var dailyPrice = period.pricePerDay(BigDecimal(4.99) )
        println("!!! dailyPrice: $dailyPrice $correctDailyPrice")
        assertEquals(0, dailyPrice.compareTo(correctDailyPrice))
        val weeklyPrice = truncateDecimal(BigDecimal(4.99).multiply(BigDecimal(7)), 2)
        assertEquals(0, period.pricePerWeek(BigDecimal(4.99)).compareTo(weeklyPrice))
        val monthlyPrice = truncateDecimal(BigDecimal(4.99).multiply(BigDecimal(30)), 2)
        assertEquals(0, period.pricePerMonth(BigDecimal(4.99)).compareTo(monthlyPrice))
        var yearlyPrice = truncateDecimal(BigDecimal(4.99).multiply(BigDecimal(365)), 2)
        assertEquals(0, period.pricePerYear(BigDecimal(4.99)).compareTo(yearlyPrice))

        assertEquals("$4.99", period.dailyPrice(BigDecimal(4.99)))
        assertEquals("$34.93", period.weeklyPrice(BigDecimal(4.99)))
        assertEquals("$149.70", period.monthlyPrice(BigDecimal(4.99)))
        assertEquals("$1,821.35", period.yearlyPrice(BigDecimal(4.99)))
    }

    @Test
    fun multiDaily_isCorrect() {
        val period = SubscriptionPeriod(2, SubscriptionPeriod.Unit.day)
        assert(period.periodDays == 2)
        assert(period.periodWeeks == 0)
        assert(period.periodMonths == 0)
        assert(period.periodYears == 0)

        assertEquals(0, period.pricePerDay(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99).divide(
            BigDecimal((2))
        ) , 2)))
        assertEquals(0, period.pricePerWeek(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 * 7 / 2), 2)))
        var monthlyPrice = truncateDecimal(BigDecimal(4.99).multiply(BigDecimal(30.0 / 2.0)), 2)
        assertEquals(0, period.pricePerMonth(BigDecimal(4.99)).compareTo(monthlyPrice))
        var yearlyPrice = truncateDecimal(BigDecimal(4.99).multiply(BigDecimal(365.0 / 2.0)), 2)
        assertEquals(0, period.pricePerYear(BigDecimal(4.99)).compareTo(yearlyPrice))

        assertEquals("$2.49", period.dailyPrice(BigDecimal(4.99)))
        assertEquals("$17.46", period.weeklyPrice(BigDecimal(4.99)))
        assertEquals("$74.85", period.monthlyPrice(BigDecimal(4.99)))
        assertEquals("$910.67", period.yearlyPrice(BigDecimal(4.99)))
    }

    @Test
    fun singleWeek_isCorrect() {
        val period = SubscriptionPeriod(1, SubscriptionPeriod.Unit.week)
        assert(period.periodDays == 7)
        assert(period.periodWeeks == 1)
        assert(period.periodMonths == 0)
        assert(period.periodYears == 0)

        val dailyPrice = period.pricePerDay(BigDecimal(4.99))
        val correctDailyPrice = truncateDecimal(BigDecimal(4.99 / 7.0), 2)
        assertEquals(0, dailyPrice.compareTo(correctDailyPrice))
        assertEquals(0, period.pricePerWeek(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99), 2)))
        val monthlyPrice = period.pricePerMonth(BigDecimal(4.99))
        val correctMonthlyPrice = truncateDecimal(BigDecimal(4.99 * 30 / 7), 2)
        assertEquals(0, monthlyPrice.compareTo(correctMonthlyPrice))
        var yearlyPrice = period.pricePerYear(BigDecimal(4.99))
        var correctYearlyPrice = truncateDecimal(BigDecimal(4.99).multiply(BigDecimal(365.0 / 7.0)), 2)

        assertEquals("$0.71", period.dailyPrice(BigDecimal(4.99)))
        assertEquals("$4.99", period.weeklyPrice(BigDecimal(4.99)))
        assertEquals("$21.38", period.monthlyPrice(BigDecimal(4.99)))
        assertEquals("$260.19", period.yearlyPrice(BigDecimal(4.99)))
    }

    @Test
    fun multiWeek_isCorrect() {
        val period = SubscriptionPeriod(2, SubscriptionPeriod.Unit.week)
        assert(period.periodDays == 14)
        assert(period.periodWeeks == 2)
        assert(period.periodMonths == 0)
        assert(period.periodYears == 0)

        assertEquals(0, period.pricePerDay(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 14), 2)))
        assertEquals(0, period.pricePerWeek(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 2), 2)))
        var monthlyPrice = period.pricePerMonth(BigDecimal(4.99))
        var correctMonthlyPrice = truncateDecimal(BigDecimal(4.99 * 30 / 14), 2)
        assertEquals(0, monthlyPrice.compareTo(correctMonthlyPrice))
        var yearlyPrice = period.pricePerYear(BigDecimal(4.99))
        var correctYearlyPrice = truncateDecimal(BigDecimal(4.99).multiply(BigDecimal(365.0 / 14.0)), 2)
        assertEquals(0, yearlyPrice.compareTo(correctYearlyPrice))

        assertEquals("$0.35", period.dailyPrice(BigDecimal(4.99)))
        assertEquals("$2.49", period.weeklyPrice(BigDecimal(4.99)))
        assertEquals("$10.69", period.monthlyPrice(BigDecimal(4.99)))
        assertEquals("$130.09", period.yearlyPrice(BigDecimal(4.99)))
    }


    @Test
    fun singleMonth_isCorrect() {
        val period = SubscriptionPeriod(1, SubscriptionPeriod.Unit.month)
        assert(period.periodDays == 30)
        assert(period.periodWeeks == 4)
        assert(period.periodMonths == 1)
        assert(period.periodYears == 0)

        assertEquals(0, period.pricePerDay(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 30), 2)))
        assertEquals(0, period.pricePerWeek(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 4), 2)))
        assertEquals(0, period.pricePerMonth(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99), 2)))
        var yearlyPrice = period.pricePerYear(BigDecimal(4.99))
        var correctYearlyPrice = truncateDecimal(BigDecimal(4.99).multiply(BigDecimal(12)), 2)
        assertEquals(0, yearlyPrice.compareTo(correctYearlyPrice))

        assertEquals("$0.16", period.dailyPrice(BigDecimal(4.99)))
        assertEquals("$1.24", period.weeklyPrice(BigDecimal(4.99)))
        assertEquals("$4.99", period.monthlyPrice(BigDecimal(4.99)))
        assertEquals("$59.88", period.yearlyPrice(BigDecimal(4.99)))
    }

    @Test
    fun multiMonth_isCorrect() {
        val period = SubscriptionPeriod(2, SubscriptionPeriod.Unit.month)
        assert(period.periodDays == 60)
        assert(period.periodWeeks == 8)
        assert(period.periodMonths == 2)
        assert(period.periodYears == 0)

        assertEquals(0, period.pricePerDay(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 60), 2)))
        assertEquals(0, period.pricePerWeek(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 8), 2)))
        assertEquals(0, period.pricePerMonth(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 2), 2)))
        var yearlyPrice = period.pricePerYear(BigDecimal(4.99))
        var correctYearlyPrice = truncateDecimal(BigDecimal(4.99).multiply(BigDecimal(6)), 2)
        assertEquals(0, yearlyPrice.compareTo(correctYearlyPrice))

        assertEquals("$0.08", period.dailyPrice(BigDecimal(4.99)))
        assertEquals("$0.62", period.weeklyPrice(BigDecimal(4.99)))
        assertEquals("$2.49", period.monthlyPrice(BigDecimal(4.99)))
        assertEquals("$29.94", period.yearlyPrice(BigDecimal(4.99)))
    }

    @Test
    fun singleYear_isCorrect() {
        val period = SubscriptionPeriod(1, SubscriptionPeriod.Unit.year)
        assert(period.periodDays == 365)
        assert(period.periodWeeks == 52)
        assert(period.periodMonths == 12)
        assert(period.periodYears == 1)

        assertEquals(0, period.pricePerDay(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 365), 2)))
        assertEquals(0, period.pricePerWeek(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 52), 2)))
        assertEquals(0, period.pricePerMonth(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 12), 2)))
        assertEquals(0, period.pricePerYear(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99), 2)))

        assertEquals("$0.01", period.dailyPrice(BigDecimal(4.99)))
        assertEquals("$0.09", period.weeklyPrice(BigDecimal(4.99)))
        assertEquals("$0.41", period.monthlyPrice(BigDecimal(4.99)))
        assertEquals("$4.99", period.yearlyPrice(BigDecimal(4.99)))
    }

    @Test
    fun multiYear_isCorrect() {
        val period = SubscriptionPeriod(2, SubscriptionPeriod.Unit.year)
        assert(period.periodDays == 730)
        assert(period.periodWeeks == 104)
        assert(period.periodMonths == 24)
        assert(period.periodYears == 2)

        assertEquals(0, period.pricePerDay(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 730), 2)))
        assertEquals(0, period.pricePerWeek(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 104), 2)))
        assertEquals(0, period.pricePerMonth(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 24), 2)))
        assertEquals(0, period.pricePerYear(BigDecimal(4.99)).compareTo(truncateDecimal(BigDecimal(4.99 / 2), 2)))

        // TODO: Fix this to be something like < $0.01 instead of $0.00
//        assertEquals("$0.01", period.dailyPrice(BigDecimal(4.99)))
        assertEquals("$0.04", period.weeklyPrice(BigDecimal(4.99)))
        assertEquals("$0.20", period.monthlyPrice(BigDecimal(4.99)))
        assertEquals("$2.49", period.yearlyPrice(BigDecimal(4.99)))
    }

 */
}
