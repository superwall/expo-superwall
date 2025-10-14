package com.superwall.sdk.contrib.threeteen

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import org.threeten.bp.Duration
import org.threeten.bp.Period
import org.threeten.bp.format.DateTimeParseException
import java.util.*
import java.util.regex.Pattern

/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Provides the ability to format a temporal amount.
 *
 *
 * This allows a [TemporalAmount], such as [Duration] or [Period],
 * to be formatted. Only selected formatting options are provided.
 *
 * <h3>Implementation Requirements:</h3>
 * This class is immutable and thread-safe.
 */
object AmountFormats {
    fun interface IntPredicate {
        fun test(value: Int): Boolean
    }

    /**
     * The number of days per week.
     */
    private const val DAYS_PER_WEEK = 7

    /**
     * The number of hours per day.
     */
    private const val HOURS_PER_DAY = 24

    /**
     * The number of minutes per hour.
     */
    private const val MINUTES_PER_HOUR = 60

    /**
     * The number of seconds per minute.
     */
    private const val SECONDS_PER_MINUTE = 60

    /**
     * The number of nanosecond per millisecond.
     */
    private const val NANOS_PER_MILLIS = 1000000

    /**
     * The resource bundle name.
     */
    private const val BUNDLE_NAME = "com.superwall.extra.wordbased"

    /**
     * The pattern to split lists with.
     */
    private val SPLITTER = Pattern.compile("[|][|][|]")

    /**
     * The property file key for the separator ", ".
     */
    private const val WORDBASED_COMMASPACE = "WordBased.commaspace"

    /**
     * The property file key for the separator " and ".
     */
    private const val WORDBASED_SPACEANDSPACE = "WordBased.spaceandspace"

    /**
     * The property file key for the word "year".
     */
    private const val WORDBASED_YEAR = "WordBased.year"

    /**
     * The property file key for the word "month".
     */
    private const val WORDBASED_MONTH = "WordBased.month"

    /**
     * The property file key for the word "week".
     */
    private const val WORDBASED_WEEK = "WordBased.week"

    /**
     * The property file key for the word "day".
     */
    private const val WORDBASED_DAY = "WordBased.day"

    /**
     * The property file key for the word "hour".
     */
    private const val WORDBASED_HOUR = "WordBased.hour"

    /**
     * The property file key for the word "minute".
     */
    private const val WORDBASED_MINUTE = "WordBased.minute"

    /**
     * The property file key for the word "second".
     */
    private const val WORDBASED_SECOND = "WordBased.second"

    /**
     * The property file key for the word "millisecond".
     */
    private const val WORDBASED_MILLISECOND = "WordBased.millisecond"

    /**
     * The predicate that matches 1 or -1.
     */
    private val PREDICATE_1 =
        IntPredicate { value: Int -> value == 1 || value == -1 }

    /**
     * The predicate that matches numbers ending 1 but not ending 11.
     */
    private val PREDICATE_END1_NOT11 =
        IntPredicate { value: Int ->
            val abs = Math.abs(value)
            val last = abs % 10
            val secondLast = abs % 100 / 10
            last == 1 && secondLast != 1
        }

    /**
     * The predicate that matches numbers ending 2, 3 or 4, but not ending 12, 13 or 14.
     */
    private val PREDICATE_END234_NOTTEENS =
        IntPredicate { value: Int ->
            val abs = Math.abs(value)
            val last = abs % 10
            val secondLast = abs % 100 / 10
            last >= 2 && last <= 4 && secondLast != 1
        }

    /**
     * List of DurationUnit values ordered by longest suffix first.
     */
    private val DURATION_UNITS =
        listOf(
            DurationUnit("ns", Duration.ofNanos(1)),
            DurationUnit("µs", Duration.ofNanos(1000)), // U+00B5 = micro symbol
            DurationUnit("μs", Duration.ofNanos(1000)), // U+03BC = Greek letter mu
            DurationUnit("us", Duration.ofNanos(1000)),
            DurationUnit("ms", Duration.ofMillis(1)),
            DurationUnit("s", Duration.ofSeconds(1)),
            DurationUnit("m", Duration.ofMinutes(1)),
            DurationUnit("h", Duration.ofHours(1)),
        )

    /**
     * Zero value for an absent fractional component of a numeric duration string.
     */
    private val EMPTY_FRACTION = FractionScalarPart(0, 0)
    // -----------------------------------------------------------------------

    /**
     * Formats a period and duration to a string in ISO-8601 format.
     *
     *
     * To obtain the ISO-8601 format of a `Period` or `Duration`
     * individually, simply call `toString()`.
     * See also [PeriodDuration].
     *
     * @param period  the period to format
     * @param duration  the duration to format
     * @return the ISO-8601 format for the period and duration
     */
    fun iso8601(
        period: Period,
        duration: Duration,
    ): String {
        Objects.requireNonNull(period, "period must not be null")
        Objects.requireNonNull(duration, "duration must not be null")
        if (period.isZero) {
            return duration.toString()
        }
        return if (duration.isZero) {
            period.toString()
        } else {
            period.toString() + duration.toString().substring(1)
        }
    }

    fun getResourceBundle(locale: Locale): ResourceBundle =
        try {
            ResourceBundle.getBundle(BUNDLE_NAME, locale)
        } catch (e: MissingResourceException) {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.localizationManager,
                message = "Resource not found: $BUNDLE_NAME",
                info = mapOf("locale" to locale),
                error = e,
            )
            // Fallback to English
            ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH)
        }

    // -------------------------------------------------------------------------

    /**
     * Formats a period to a string in a localized word-based format.
     *
     *
     * This returns a word-based format for the period.
     * The year and month are printed as supplied unless the signs differ, in which case they are normalized.
     * The words are configured in a resource bundle text file -
     * `org.threeten.extra.wordbased.properties` - with overrides per language.
     *
     * @param period  the period to format
     * @param locale  the locale to use
     * @return the localized word-based format for the period
     */
    fun wordBased(
        period: Period,
        locale: Locale,
    ): String {
        Objects.requireNonNull(period, "period must not be null")
        Objects.requireNonNull(locale, "locale must not be null")
        val bundle = getResourceBundle(locale)
        val formats =
            arrayOf(
                UnitFormat.of(bundle, WORDBASED_YEAR),
                UnitFormat.of(bundle, WORDBASED_MONTH),
                UnitFormat.of(bundle, WORDBASED_WEEK),
                UnitFormat.of(bundle, WORDBASED_DAY),
            )
        val wb =
            WordBased(
                formats,
                bundle.getString(WORDBASED_COMMASPACE),
                bundle.getString(
                    WORDBASED_SPACEANDSPACE,
                ),
            )
        val normPeriod =
            if (oppositeSigns(period.months, period.years)) period.normalized() else period
        var weeks = 0
        var days = 0
        if (normPeriod.days % DAYS_PER_WEEK == 0) {
            weeks = normPeriod.days / DAYS_PER_WEEK
        } else {
            days = normPeriod.days
        }
        val values = intArrayOf(normPeriod.years, normPeriod.months, weeks, days)
        return wb.format(values)
    }

    /**
     * Formats a duration to a string in a localized word-based format.
     *
     *
     * This returns a word-based format for the duration.
     * The words are configured in a resource bundle text file -
     * `org.threeten.extra.wordbased.properties` - with overrides per language.
     *
     * @param duration  the duration to format
     * @param locale  the locale to use
     * @return the localized word-based format for the duration
     */
    fun wordBased(
        duration: Duration,
        locale: Locale,
    ): String {
        Objects.requireNonNull(duration, "duration must not be null")
        Objects.requireNonNull(locale, "locale must not be null")
        val bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale)
        val formats =
            arrayOf(
                UnitFormat.of(bundle, WORDBASED_HOUR),
                UnitFormat.of(bundle, WORDBASED_MINUTE),
                UnitFormat.of(bundle, WORDBASED_SECOND),
                UnitFormat.of(bundle, WORDBASED_MILLISECOND),
            )
        val wb =
            WordBased(
                formats,
                bundle.getString(WORDBASED_COMMASPACE),
                bundle.getString(
                    WORDBASED_SPACEANDSPACE,
                ),
            )
        val hours = duration.toHours()
        val mins = duration.toMinutes() % MINUTES_PER_HOUR
        val secs = duration.seconds % SECONDS_PER_MINUTE
        val millis = duration.nano / NANOS_PER_MILLIS
        val values = intArrayOf(hours.toInt(), mins.toInt(), secs.toInt(), millis)
        return wb.format(values)
    }

    /**
     * Formats a period and duration to a string in a localized word-based format.
     *
     *
     * This returns a word-based format for the period.
     * The year and month are printed as supplied unless the signs differ, in which case they are normalized.
     * The words are configured in a resource bundle text file -
     * `org.threeten.extra.wordbased.properties` - with overrides per language.
     *
     * @param period  the period to format
     * @param duration  the duration to format
     * @param locale  the locale to use
     * @return the localized word-based format for the period and duration
     */
    fun wordBased(
        period: Period,
        duration: Duration,
        locale: Locale,
    ): String {
        Objects.requireNonNull(period, "period must not be null")
        Objects.requireNonNull(duration, "duration must not be null")
        Objects.requireNonNull(locale, "locale must not be null")
        val bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale)
        val formats =
            arrayOf(
                UnitFormat.of(bundle, WORDBASED_YEAR),
                UnitFormat.of(bundle, WORDBASED_MONTH),
                UnitFormat.of(bundle, WORDBASED_WEEK),
                UnitFormat.of(bundle, WORDBASED_DAY),
                UnitFormat.of(bundle, WORDBASED_HOUR),
                UnitFormat.of(bundle, WORDBASED_MINUTE),
                UnitFormat.of(bundle, WORDBASED_SECOND),
                UnitFormat.of(bundle, WORDBASED_MILLISECOND),
            )
        val wb =
            WordBased(
                formats,
                bundle.getString(WORDBASED_COMMASPACE),
                bundle.getString(
                    WORDBASED_SPACEANDSPACE,
                ),
            )
        val normPeriod =
            if (oppositeSigns(period.months, period.years)) period.normalized() else period
        var weeks = 0
        var days = 0
        if (normPeriod.days % DAYS_PER_WEEK == 0) {
            weeks = normPeriod.days / DAYS_PER_WEEK
        } else {
            days = normPeriod.days
        }
        val totalHours = duration.toHours()
        days += (totalHours / HOURS_PER_DAY).toInt()
        val hours = (totalHours % HOURS_PER_DAY).toInt()
        val mins = (duration.toMinutes() % MINUTES_PER_HOUR).toInt()
        val secs = (duration.seconds % SECONDS_PER_MINUTE).toInt()
        val millis = duration.nano / NANOS_PER_MILLIS
        val values =
            intArrayOf(
                normPeriod.years,
                normPeriod.months,
                weeks,
                days,
                hours,
                mins,
                secs,
                millis,
            )
        return wb.format(values)
    }

    // are the signs opposite
    private fun oppositeSigns(
        a: Int,
        b: Int,
    ): Boolean = if (a < 0) b >= 0 else b < 0
    // -------------------------------------------------------------------------

    /**
     * Parses formatted durations based on units.
     *
     *
     * The behaviour matches the [Golang](https://golang.org/pkg/time/#ParseDuration)
     * duration parser, however, infinite durations are not supported.
     *
     *
     * The duration format is a possibly signed sequence of decimal numbers, each with optional
     * fraction and a unit suffix, such as "300ms", "-1.5h" or "2h45m". Valid time units are
     * "ns", "us" (or "µs"), "ms", "s", "m", "h".
     *
     *
     * Note, the value "0" is specially supported as `Duration.ZERO`.
     *
     * @param durationText the formatted unit-based duration string.
     * @return the `Duration` value represented by the string, if possible.
     */
    fun parseUnitBasedDuration(durationText: CharSequence): Duration {
        var durationText = durationText
        Objects.requireNonNull(durationText, "durationText must not be null")

        // variables for tracking error positions during parsing.
        var offset = 0
        val original = durationText

        // consume the leading sign - or + if one is present.
        var sign = 1
        var updatedText = consumePrefix(durationText, '-')
        if (updatedText.isSuccess) {
            sign = -1
            offset += 1
            durationText = updatedText.getOrNull()!!
        } else {
            updatedText = consumePrefix(durationText, '+')
            if (updatedText.isSuccess) {
                offset += 1
            }
            durationText = updatedText.getOrNull() ?: durationText
        }
        // special case for a string of "0"
        if (durationText == "0") {
            return Duration.ZERO
        }
        // special case, empty string as an invalid duration.
        if (durationText.isEmpty()) {
            throw DateTimeParseException("Not a numeric value", original, 0)
        }
        var value = Duration.ZERO
        var durationTextLength = durationText.length
        while (durationTextLength > 0) {
            val integerPart = consumeDurationLeadingInt(durationText, original, offset)
            offset += durationText.length - integerPart.remainingText().length
            durationText = integerPart.remainingText()
            val leadingInt: DurationScalar = integerPart
            var fraction: DurationScalar = EMPTY_FRACTION
            val dot = consumePrefix(durationText, '.')
            if (dot.isSuccess) {
                offset += 1
                durationText = dot.getOrNull()!!
                val fractionPart = consumeDurationFraction(durationText, original, offset)
                // update the remaining string and fraction.
                offset += durationText.length - fractionPart.remainingText().length
                durationText = fractionPart.remainingText()
                fraction = fractionPart
            }
            val optUnit = findUnit(durationText)
            if (optUnit.isFailure) {
                throw DateTimeParseException(
                    "Invalid duration unit",
                    original,
                    offset,
                )
            }
            val unit = optUnit.getOrNull()!!
            try {
                var unitValue = leadingInt.applyTo(unit)
                val fractionValue = fraction.applyTo(unit)
                unitValue = unitValue.plus(fractionValue)
                value = value.plus(unitValue)
            } catch (e: ArithmeticException) {
                throw DateTimeParseException(
                    "Duration string exceeds valid numeric range",
                    original,
                    offset,
                    e,
                )
            }
            // update the remaining text and text length.
            val remainingText = unit.consumeDurationUnit(durationText)
            offset += durationText.length - remainingText.length
            durationText = remainingText
            durationTextLength = durationText.length
        }
        return if (sign < 0) value.negated() else value
    }

    // consume the fractional part of a unit-based duration, e.g.
    // <int>.<fraction><unit>.
    private fun consumeDurationLeadingInt(
        text: CharSequence,
        original: CharSequence,
        offset: Int,
    ): ParsedUnitPart {
        var integerPart: Long = 0
        var i = 0
        val valueLength = text.length
        while (i < valueLength) {
            val c = text[i]
            if (c < '0' || c > '9') {
                break
            }
            // overflow of a single numeric specifier for a duration.
            if (integerPart > Long.MAX_VALUE / 10) {
                throw DateTimeParseException(
                    "Duration string exceeds valid numeric range",
                    original,
                    i + offset,
                )
            }
            integerPart *= 10
            integerPart += (c.code - '0'.code).toLong()
            // overflow of a single numeric specifier for a duration.
            if (integerPart < 0) {
                throw DateTimeParseException(
                    "Duration string exceeds valid numeric range",
                    original,
                    i + offset,
                )
            }
            i++
        }
        // if no text was consumed, return empty.
        if (i == 0) {
            throw DateTimeParseException("Missing leading integer", original, offset)
        }
        return ParsedUnitPart(
            text.subSequence(i, text.length),
            IntegerScalarPart(integerPart),
        )
    }

    // consume the fractional part of a unit-based duration, e.g.
    // <int>.<fraction><unit>.
    private fun consumeDurationFraction(
        text: CharSequence,
        original: CharSequence,
        offset: Int,
    ): ParsedUnitPart {
        var i = 0
        var fraction: Long = 0
        var scale: Long = 1
        var overflow = false
        while (i < text.length) {
            val c = text[i]
            if (c < '0' || c > '9') {
                break
            }
            // for the fractional part, it's possible to overflow; however,
            // this does not invalidate the duration, but rather it means that
            // the precision of the fractional part is truncated to 999,999,999.
            if (overflow || fraction > Long.MAX_VALUE / 10) {
                i++
                continue
            }
            val tmp = fraction * 10 + (c.code - '0'.code).toLong()
            if (tmp < 0) {
                overflow = true
                i++
                continue
            }
            fraction = tmp
            scale *= 10
            i++
        }
        if (i == 0) {
            throw DateTimeParseException(
                "Missing numeric fraction after '.'",
                original,
                offset,
            )
        }
        return ParsedUnitPart(
            text.subSequence(i, text.length),
            FractionScalarPart(fraction, scale),
        )
    }

    // find the duration unit at the beginning of the input text, if present.
    private fun findUnit(text: CharSequence): Result<DurationUnit> =
        DURATION_UNITS
            .firstOrNull { du: DurationUnit ->
                du.prefixMatchesUnit(
                    text,
                )
            }?.let { Result.success(it) } ?: Result.failure(Exception("No matching duration unit found"))

    // consume the indicated {@code prefix} if it exists at the beginning of the
    // text, returning the remaining string if the prefix was consumed.
    private fun consumePrefix(
        text: CharSequence,
        prefix: Char,
    ): Result<CharSequence> =
        if (text.isNotEmpty() && text[0] == prefix) {
            Result.success(text.subSequence(1, text.length))
        } else {
            Result.failure(Exception("Prefix not found"))
        }

    // -------------------------------------------------------------------------
    // data holder for word-based formats
    internal class WordBased(
        private val units: Array<UnitFormat>,
        private val separator: String,
        private val lastSeparator: String,
    ) {
        fun format(values: IntArray): String {
            val buf = StringBuilder(32)
            var nonZeroCount = 0
            for (i in values.indices) {
                if (values[i] != 0) {
                    nonZeroCount++
                }
            }
            var count = 0
            for (i in values.indices) {
                if (values[i] != 0 || count == 0 && i == values.size - 1) {
                    units[i].formatTo(values[i], buf)
                    if (count < nonZeroCount - 2) {
                        buf.append(separator)
                    } else if (count == nonZeroCount - 2) {
                        buf.append(lastSeparator)
                    }
                    count++
                }
            }
            return buf.toString()
        }
    }

    // data holder for single/plural formats
    internal interface UnitFormat {
        fun formatTo(
            value: Int,
            buf: StringBuilder,
        ) {}

        companion object {
            fun of(
                bundle: ResourceBundle,
                keyStem: String,
            ): UnitFormat =
                if (bundle.containsKey(keyStem + "s.predicates")) {
                    val predicateList = bundle.getString(keyStem + "s.predicates")
                    val textList = bundle.getString(keyStem + "s.list")
                    val regexes = SPLITTER.split(predicateList)
                    val text = SPLITTER.split(textList)
                    PredicateFormat(regexes, text)
                } else {
                    val single = bundle.getString(keyStem)
                    val plural = bundle.getString(keyStem + "s")
                    SinglePluralFormat(single, plural)
                }
        }
    }

    // data holder for single/plural formats
    internal class SinglePluralFormat(
        private val single: String,
        private val plural: String,
    ) : UnitFormat {
        override fun formatTo(
            value: Int,
            buf: StringBuilder,
        ) {
            buf.append(value).append(if (value == 1 || value == -1) single else plural)
        }
    }

    // data holder for predicate formats
    internal class PredicateFormat(
        predicateStrs: Array<String?>,
        text: Array<String?>,
    ) : UnitFormat {
        private val predicates: Array<IntPredicate>
        private val text: Array<String?>

        init {
            check(predicateStrs.size + 1 == text.size) { "Invalid word-based resource" }
            predicates =
                predicateStrs
                    .map { predicateStr ->
                        findPredicate(predicateStr!!)
                    }.toTypedArray()
            this.text = text
        }

        private fun findPredicate(predicateStr: String): IntPredicate =
            when (predicateStr) {
                "One" -> PREDICATE_1
                "End234NotTeens" -> PREDICATE_END234_NOTTEENS
                "End1Not11" -> PREDICATE_END1_NOT11
                else -> throw IllegalStateException("Invalid word-based resource")
            }

        override fun formatTo(
            value: Int,
            buf: StringBuilder,
        ) {
            for (i in predicates.indices) {
                if (predicates[i].test(value)) {
                    buf.append(value).append(text[i])
                    return
                }
            }
            buf.append(value).append(text[predicates.size])
        }
    }

    // -------------------------------------------------------------------------
    // data holder for a duration unit string and its associated Duration value.
    internal class DurationUnit constructor(
        private val abbrev: String,
        private val value: Duration,
    ) {
        // whether the input text starts with the unit abbreviation.
        fun prefixMatchesUnit(text: CharSequence): Boolean = text.length >= abbrev.length && abbrev == text.subSequence(0, abbrev.length)

        // consume the duration unit and returning the remaining text.
        fun consumeDurationUnit(text: CharSequence): CharSequence = text.subSequence(abbrev.length, text.length)

        // scale the unit by the input scalingFunction, returning a value if
        // one is produced, or an empty result when the operation results in an
        // arithmetic overflow.
        fun scaleBy(scaleFunc: (Duration) -> Duration?): Duration? = scaleFunc(value)
    }

    // interface for computing a duration from a duration unit and a scalar.
    internal interface DurationScalar {
        // returns a duration value on a successful computation, and an empty
        // result otherwise.
        fun applyTo(unit: DurationUnit): Duration
    }

    // data holder for parsed fragments of a floating point duration scalar.
    internal class ParsedUnitPart constructor(
        private val remainingText: CharSequence,
        private val scalar: DurationScalar,
    ) : DurationScalar {
        override fun applyTo(unit: DurationUnit): Duration = scalar.applyTo(unit)

        fun remainingText(): CharSequence = remainingText
    }

    // data holder for the leading integer value of a duration scalar.
    internal class IntegerScalarPart constructor(
        private val value: Long,
    ) : DurationScalar {
        override fun applyTo(unit: DurationUnit): Duration =
            unit.scaleBy { d: Duration ->
                d.multipliedBy(
                    value,
                )
            } as Duration
    }

    // data holder for the fractional floating point value of a duration
    // scalar.
    internal class FractionScalarPart(
        private val value: Long,
        private val scale: Long,
    ) : DurationScalar {
        override fun applyTo(unit: DurationUnit): Duration =
            if (value == 0L) {
                Duration.ZERO
            } else {
                unit.scaleBy { d: Duration ->
                    d
                        .multipliedBy(
                            value,
                        ).dividedBy(scale) as Duration
                }!!
            }
    }
}
