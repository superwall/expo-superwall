package com.superwall.sdk.contrib

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.contrib.threeteen.AmountFormats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.Period
import org.threeten.bp.format.DateTimeParseException
import java.util.*

class AmountFormatsTest {
    @Test
    fun testIso8601Format() {
        Given("a period and duration") {
            val period = Period.of(1, 2, 3)
            val duration = Duration.ofHours(4).plusMinutes(5).plusSeconds(6)

            When("formatting to ISO-8601") {
                val result = AmountFormats.iso8601(period, duration)

                Then("it should return the correct ISO-8601 string") {
                    assertEquals("P1Y2M3DT4H5M6S", result)
                }
            }
        }

        Given("a zero period and non-zero duration") {
            val period = Period.ZERO
            val duration = Duration.ofMinutes(30)

            When("formatting to ISO-8601") {
                val result = AmountFormats.iso8601(period, duration)

                Then("it should return only the duration string") {
                    assertEquals("PT30M", result)
                }
            }
        }

        Given("a non-zero period and zero duration") {
            val period = Period.ofMonths(3)
            val duration = Duration.ZERO

            When("formatting to ISO-8601") {
                val result = AmountFormats.iso8601(period, duration)

                Then("it should return only the period string") {
                    assertEquals("P3M", result)
                }
            }
        }
    }

    @Test
    fun testWordBasedPeriod() {
        Given("a period with multiple units") {
            val period = Period.of(2, 3, 15)

            When("formatting to word-based with English locale") {
                val result = AmountFormats.wordBased(period, Locale.ENGLISH)

                Then("it should return the correct word-based string") {
                    assertEquals("2 years, 3 months and 15 days", result)
                }
            }
        }

        Given("a period with opposite signs") {
            val period = Period.of(1, -2, 0)

            When("formatting to word-based") {
                val result = AmountFormats.wordBased(period, Locale.ENGLISH)

                Then("it should normalize the period") {
                    assertEquals("10 months", result)
                }
            }
        }
    }

    @Test
    fun testWordBasedDuration() {
        Given("a duration with multiple units") {
            val duration =
                Duration
                    .ofHours(25)
                    .plusMinutes(30)
                    .plusSeconds(45)
                    .plusMillis(500)

            When("formatting to word-based with English locale") {
                val result = AmountFormats.wordBased(duration, Locale.ENGLISH)

                Then("it should return the correct word-based string") {
                    assertEquals("25 hours, 30 minutes, 45 seconds and 500 milliseconds", result)
                }
            }
        }
    }

    @Test
    fun testWordBasedPeriodAndDuration() {
        Given("a period and duration with multiple units") {
            val period = Period.of(1, 2, 3)
            val duration = Duration.ofHours(4).plusMinutes(5).plusSeconds(6)

            When("formatting to word-based with English locale") {
                val result = AmountFormats.wordBased(period, duration, Locale.ENGLISH)

                Then("it should return the correct word-based string") {
                    assertEquals("1 year, 2 months, 3 days, 4 hours, 5 minutes and 6 seconds", result)
                }
            }
        }
    }

    @Test
    fun testParseUnitBasedDuration() {
        Given("a valid duration string") {
            val durationString = "2h45m30s"

            When("parsing the unit-based duration") {
                val result = AmountFormats.parseUnitBasedDuration(durationString)

                Then("it should return the correct Duration") {
                    assertEquals(Duration.ofHours(2).plusMinutes(45).plusSeconds(30), result)
                }
            }
        }

        Given("a duration string with a negative value") {
            val durationString = "-1.5h"

            When("parsing the unit-based duration") {
                val result = AmountFormats.parseUnitBasedDuration(durationString)

                Then("it should return the correct negative Duration") {
                    assertEquals(Duration.ofMinutes(-90), result)
                }
            }
        }

        Given("a duration string with mixed units") {
            val durationString = "2h30m500ms"

            When("parsing the unit-based duration") {
                val result = AmountFormats.parseUnitBasedDuration(durationString)

                Then("it should return the correct Duration") {
                    assertEquals(Duration.ofHours(2).plusMinutes(30).plusMillis(500), result)
                }
            }
        }

        Given("an invalid duration string") {
            val durationString = "2h30x"

            When("parsing the unit-based duration") {
                Then("it should throw a DateTimeParseException") {
                    assertThrows(DateTimeParseException::class.java) {
                        AmountFormats.parseUnitBasedDuration(durationString)
                    }
                }
            }
        }

        Given("a duration string with an empty value") {
            val durationString = ""

            When("parsing the unit-based duration") {
                Then("it should throw a DateTimeParseException") {
                    assertThrows(DateTimeParseException::class.java) {
                        AmountFormats.parseUnitBasedDuration(durationString)
                    }
                }
            }
        }

        Given("a duration string with only a zero") {
            val durationString = "0"

            When("parsing the unit-based duration") {
                val result = AmountFormats.parseUnitBasedDuration(durationString)

                Then("it should return Duration.ZERO") {
                    assertEquals(Duration.ZERO, result)
                }
            }
        }

        Given("a duration string with a very large value") {
            val durationString = "9223372036854775807ns"

            When("parsing the unit-based duration") {
                val result = AmountFormats.parseUnitBasedDuration(durationString)

                Then("it should return the correct Duration") {
                    assertEquals(Duration.ofNanos(9223372036854775807), result)
                }
            }
        }

        Given("a duration string that exceeds the valid range") {
            val durationString = "9223372036854775808ns"

            When("parsing the unit-based duration") {
                Then("it should throw a DateTimeParseException") {
                    assertThrows(DateTimeParseException::class.java) {
                        AmountFormats.parseUnitBasedDuration(durationString)
                    }
                }
            }
        }
    }
}
