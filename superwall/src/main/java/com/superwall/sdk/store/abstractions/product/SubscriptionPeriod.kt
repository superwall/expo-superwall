package com.superwall.sdk.store.abstractions.product

import org.threeten.bp.Period
import java.math.BigDecimal
import java.math.RoundingMode

data class SubscriptionPeriod(
    val value: Int,
    val unit: Unit,
) {
    enum class Unit {
        day,
        week,
        month,
        year,
    }

    val daysPerUnit: Double
        get() =
            when (unit) {
                Unit.day -> 1.0
                Unit.week -> 7.0
                Unit.month -> 30.0
                Unit.year -> 365.0
            }

    fun toMillis(): Long {
        val days =
            when (unit) {
                Unit.day -> value
                Unit.week -> value * 7
                Unit.month -> value * 30 // Approximation
                Unit.year -> value * 365 // Approximation
            }
        return days * 24L * 60 * 60 * 1000
    }

    fun normalized(): SubscriptionPeriod =
        when (unit) {
            Unit.day ->
                if (value % 30 == 0) {
                    copy(value = value / 30, unit = Unit.month).normalized()
                } else {
                    if (value % 7 == 0) {
                        copy(value = value / 7, unit = Unit.week).normalized()
                    } else {
                        if (value % 360 == 0) {
                            copy(value = value / 360, unit = Unit.year)
                        } else {
                            this
                        }
                    }
                }

            Unit.month -> if (value % 12 == 0) copy(value = value / 12, unit = Unit.year) else this
            else -> this
        }

    fun toPeriod(): Period =
        when (unit) {
            Unit.day -> Period.ofDays(value)
            Unit.week -> Period.ofWeeks(value)
            Unit.month -> Period.ofMonths(value)
            Unit.year -> Period.ofYears(value)
        }

    companion object {
        fun from(subscriptionPeriodString: String): SubscriptionPeriod? {
            val period =
                try {
                    Period.parse(subscriptionPeriodString)
                } catch (e: Throwable) {
                    return null
                }

            val totalDays = (period.toTotalMonths().toInt() * 30 + period.days).toInt()
            val weeks = (totalDays / 7).toInt()
            val days = (totalDays % 7).toInt()

            return when {
                days > 0 -> SubscriptionPeriod(totalDays, Unit.day)
                weeks > 0 -> SubscriptionPeriod(weeks, Unit.week)
                period.toTotalMonths() > 0 ->
                    SubscriptionPeriod(
                        period.toTotalMonths().toInt(),
                        Unit.month,
                    )

                period.years > 0 -> SubscriptionPeriod(period.years, Unit.year)
                else -> null
            }?.normalized()
        }
    }

    private val roundingMode = RoundingMode.DOWN
    private val calculationScale = 7
    private val outputScale = 2

    val toMillis: Long
        get() {
            val days = (this.value * this.daysPerUnit).toLong()
            return days * 24 * 60 * 60 * 1000 // days → hours → minutes → seconds → milliseconds
        }

    fun pricePerDay(price: BigDecimal): BigDecimal {
        val periodsPerDay: BigDecimal =
            when (this.unit) {
                Unit.day -> BigDecimal.ONE
                Unit.week -> BigDecimal(7)
                Unit.month -> BigDecimal(30)
                Unit.year -> BigDecimal(365)
            } * BigDecimal(this.value)

        return price.divide(periodsPerDay, outputScale, roundingMode)
    }

    fun pricePerWeek(price: BigDecimal): BigDecimal {
        val periodsPerWeek: BigDecimal =
            when (this.unit) {
                Unit.day -> BigDecimal.ONE.divide(BigDecimal(7), calculationScale, roundingMode)
                Unit.week -> BigDecimal.ONE
                Unit.month -> BigDecimal(4)
                Unit.year -> BigDecimal(52)
            } * BigDecimal(this.value)

        return price.divide(periodsPerWeek, outputScale, roundingMode)
    }

    fun pricePerMonth(price: BigDecimal): BigDecimal {
        val periodsPerMonth: BigDecimal =
            when (this.unit) {
                Unit.day -> BigDecimal.ONE.divide(BigDecimal(30), calculationScale, roundingMode)
                Unit.week -> BigDecimal.ONE.divide(BigDecimal(30.0 / 7.0), calculationScale, roundingMode)
                Unit.month -> BigDecimal.ONE
                Unit.year -> BigDecimal(12)
            } * BigDecimal(this.value)

        return price.divide(periodsPerMonth, outputScale, roundingMode)
    }

    fun pricePerYear(price: BigDecimal): BigDecimal {
        val periodsPerYear: BigDecimal =
            when (this.unit) {
                Unit.day -> BigDecimal.ONE.divide(BigDecimal(365), calculationScale, roundingMode)
                Unit.week -> BigDecimal.ONE.divide(BigDecimal(52), calculationScale, roundingMode)
                Unit.month -> BigDecimal.ONE.divide(BigDecimal(12), calculationScale, roundingMode)
                Unit.year -> BigDecimal.ONE
            }.multiply(BigDecimal(this.value))

        return price.divide(periodsPerYear, outputScale, roundingMode)
    }
}
