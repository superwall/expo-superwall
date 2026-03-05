@file:Suppress("ktlint:standard:function-naming")

package com.superwall.sdk.store.testmode

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.store.abstractions.product.SubscriptionPeriod
import com.superwall.sdk.store.testmode.models.SuperwallEntitlementRef
import com.superwall.sdk.store.testmode.models.SuperwallProduct
import com.superwall.sdk.store.testmode.models.SuperwallProductPlatform
import com.superwall.sdk.store.testmode.models.SuperwallProductPrice
import com.superwall.sdk.store.testmode.models.SuperwallProductSubscription
import com.superwall.sdk.store.testmode.models.SuperwallSubscriptionPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TestStoreProductTest {
    private fun makeProduct(
        identifier: String = "com.test.product",
        amountCents: Int = 999,
        currency: String = "USD",
        period: SuperwallSubscriptionPeriod = SuperwallSubscriptionPeriod.MONTH,
        periodCount: Int = 1,
        trialPeriodDays: Int? = null,
        entitlements: List<SuperwallEntitlementRef> = emptyList(),
    ): SuperwallProduct =
        SuperwallProduct(
            objectType = "product",
            identifier = identifier,
            platform = SuperwallProductPlatform.ANDROID,
            price = SuperwallProductPrice(amount = amountCents, currency = currency),
            subscription =
                SuperwallProductSubscription(
                    period = period,
                    periodCount = periodCount,
                    trialPeriodDays = trialPeriodDays,
                ),
            entitlements = entitlements,
        )

    private fun makeNonSubscriptionProduct(
        identifier: String = "com.test.lifetime",
        amountCents: Int = 4999,
        currency: String = "USD",
    ): SuperwallProduct =
        SuperwallProduct(
            objectType = "product",
            identifier = identifier,
            platform = SuperwallProductPlatform.ANDROID,
            price = SuperwallProductPrice(amount = amountCents, currency = currency),
            subscription = null,
            entitlements = emptyList(),
        )

    // region Price conversion

    @Test
    fun `price converts from cents correctly`() {
        Given("a product with price 999 cents") {
            val testProduct = TestStoreProduct(makeProduct(amountCents = 999))

            Then("price is 9.99") {
                assertEquals(0, testProduct.price.compareTo(BigDecimal("9.99")))
            }
        }
    }

    @Test
    fun `price converts zero cents`() {
        Given("a product with price 0 cents") {
            val testProduct = TestStoreProduct(makeProduct(amountCents = 0))

            Then("price is 0.00") {
                assertEquals(0, testProduct.price.compareTo(BigDecimal.ZERO))
            }
        }
    }

    @Test
    fun `price converts large amount`() {
        Given("a product with price 99999 cents") {
            val testProduct = TestStoreProduct(makeProduct(amountCents = 99999))

            Then("price is 999.99") {
                assertEquals(0, testProduct.price.compareTo(BigDecimal("999.99")))
            }
        }
    }

    @Test
    fun `price converts single digit cents`() {
        Given("a product with price 5 cents") {
            val testProduct = TestStoreProduct(makeProduct(amountCents = 5))

            Then("price is 0.05") {
                assertEquals(0, testProduct.price.compareTo(BigDecimal("0.05")))
            }
        }
    }

    // endregion

    // region Subscription period mapping

    @Test
    fun `subscription period maps daily correctly`() {
        Given("a daily product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.DAY, periodCount = 1))

            Then("subscription period is 1 day") {
                val period = testProduct.subscriptionPeriod
                assertNotNull(period)
                assertEquals(SubscriptionPeriod.Unit.day, period!!.unit)
                assertEquals(1, period.value)
            }
        }
    }

    @Test
    fun `subscription period maps weekly correctly`() {
        Given("a weekly product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.WEEK, periodCount = 1))

            Then("subscription period is 1 week") {
                val period = testProduct.subscriptionPeriod
                assertNotNull(period)
                assertEquals(SubscriptionPeriod.Unit.week, period!!.unit)
                assertEquals(1, period.value)
            }
        }
    }

    @Test
    fun `subscription period maps monthly correctly`() {
        Given("a monthly product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 1))

            Then("subscription period is 1 month") {
                val period = testProduct.subscriptionPeriod
                assertNotNull(period)
                assertEquals(SubscriptionPeriod.Unit.month, period!!.unit)
                assertEquals(1, period.value)
            }
        }
    }

    @Test
    fun `subscription period maps yearly correctly`() {
        Given("a yearly product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.YEAR, periodCount = 1))

            Then("subscription period is 1 year") {
                val period = testProduct.subscriptionPeriod
                assertNotNull(period)
                assertEquals(SubscriptionPeriod.Unit.year, period!!.unit)
                assertEquals(1, period.value)
            }
        }
    }

    @Test
    fun `subscription period respects periodCount`() {
        Given("a product with 3 month period") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 3))

            Then("subscription period has value 3") {
                val period = testProduct.subscriptionPeriod
                assertNotNull(period)
                assertEquals(SubscriptionPeriod.Unit.month, period!!.unit)
                assertEquals(3, period.value)
            }
        }
    }

    @Test
    fun `subscription period is null for non-subscription product`() {
        Given("a non-subscription product") {
            val testProduct = TestStoreProduct(makeNonSubscriptionProduct())

            Then("subscriptionPeriod is null") {
                assertNull(testProduct.subscriptionPeriod)
            }
        }
    }

    // endregion

    // region Free trial detection

    @Test
    fun `free trial is detected correctly`() {
        Given("a product with a 7-day trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = 7))

            Then("hasFreeTrial is true") {
                assertTrue(testProduct.hasFreeTrial)
            }
        }
    }

    @Test
    fun `no free trial when trialPeriodDays is null`() {
        Given("a product with no trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = null))

            Then("hasFreeTrial is false") {
                assertFalse(testProduct.hasFreeTrial)
            }
        }
    }

    @Test
    fun `no free trial when trialPeriodDays is zero`() {
        Given("a product with 0-day trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = 0))

            Then("hasFreeTrial is false") {
                assertFalse(testProduct.hasFreeTrial)
            }
        }
    }

    @Test
    fun `trialPeriodText formats correctly`() {
        Given("a product with a 7-day trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = 7))

            Then("trialPeriodText is '7-day'") {
                assertEquals("7-day", testProduct.trialPeriodText)
            }
        }
    }

    @Test
    fun `trialPeriodText is empty when no trial`() {
        Given("a product with no trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = null))

            Then("trialPeriodText is empty") {
                assertEquals("", testProduct.trialPeriodText)
            }
        }
    }

    @Test
    fun `trialPeriodEndDate is set when trial exists`() {
        Given("a product with a 14-day trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = 14))

            Then("trialPeriodEndDate is not null and in the future") {
                assertNotNull(testProduct.trialPeriodEndDate)
                assertTrue(testProduct.trialPeriodEndDate!!.time > System.currentTimeMillis())
            }
        }
    }

    @Test
    fun `trialPeriodEndDate is null when no trial`() {
        Given("a product with no trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = null))

            Then("trialPeriodEndDate is null") {
                assertNull(testProduct.trialPeriodEndDate)
            }
        }
    }

    @Test
    fun `trialPeriodPrice is zero`() {
        Given("a product with a trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = 7))

            Then("trialPeriodPrice is zero") {
                assertEquals(0, testProduct.trialPeriodPrice.compareTo(BigDecimal.ZERO))
            }
        }
    }

    // endregion

    // region Trial period calculations

    @Test
    fun `trialPeriodDays returns correct value`() {
        Given("a product with a 30-day trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = 30))

            Then("trialPeriodDays is 30") {
                assertEquals(30, testProduct.trialPeriodDays)
                assertEquals("30", testProduct.trialPeriodDaysString)
            }
        }
    }

    @Test
    fun `trialPeriodWeeks calculates from days`() {
        Given("a product with a 14-day trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = 14))

            Then("trialPeriodWeeks is 2") {
                assertEquals(2, testProduct.trialPeriodWeeks)
                assertEquals("2", testProduct.trialPeriodWeeksString)
            }
        }
    }

    @Test
    fun `trial period values are zero when no trial`() {
        Given("a product with no trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = null))

            Then("all trial period values are zero") {
                assertEquals(0, testProduct.trialPeriodDays)
                assertEquals(0, testProduct.trialPeriodWeeks)
                assertEquals(0, testProduct.trialPeriodMonths)
                assertEquals(0, testProduct.trialPeriodYears)
            }
        }
    }

    // endregion

    // region Product identifiers

    @Test
    fun `product identifiers are correct`() {
        Given("a product with identifier 'com.test.premium'") {
            val testProduct = TestStoreProduct(makeProduct(identifier = "com.test.premium"))

            Then("fullIdentifier and productIdentifier are set") {
                assertEquals("com.test.premium", testProduct.fullIdentifier)
                assertEquals("com.test.premium", testProduct.productIdentifier)
            }
        }
    }

    // endregion

    // region Product type

    @Test
    fun `productType is subs for subscription products`() {
        Given("a subscription product") {
            val testProduct = TestStoreProduct(makeProduct())

            Then("productType is 'subs'") {
                assertEquals("subs", testProduct.productType)
            }
        }
    }

    @Test
    fun `productType is inapp for non-subscription products`() {
        Given("a non-subscription product") {
            val testProduct = TestStoreProduct(makeNonSubscriptionProduct())

            Then("productType is 'inapp'") {
                assertEquals("inapp", testProduct.productType)
            }
        }
    }

    // endregion

    // region Period string formatting

    @Test
    fun `period string formats correctly for monthly`() {
        Given("a monthly subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 1))

            Then("period is 'month'") {
                assertEquals("month", testProduct.period)
            }
        }
    }

    @Test
    fun `period string formats correctly for yearly`() {
        Given("a yearly subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.YEAR, periodCount = 1))

            Then("period is 'year'") {
                assertEquals("year", testProduct.period)
            }
        }
    }

    @Test
    fun `period string formats correctly for weekly`() {
        Given("a weekly subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.WEEK, periodCount = 1))

            Then("period is 'week'") {
                assertEquals("week", testProduct.period)
            }
        }
    }

    @Test
    fun `period string for quarterly shows quarter`() {
        Given("a 3-month subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 3))

            Then("period is 'quarter'") {
                assertEquals("quarter", testProduct.period)
            }
        }
    }

    @Test
    fun `period string for semi-annual shows 6 months`() {
        Given("a 6-month subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 6))

            Then("period is '6 months'") {
                assertEquals("6 months", testProduct.period)
            }
        }
    }

    @Test
    fun `period string for bi-monthly shows 2 months`() {
        Given("a 2-month subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 2))

            Then("period is '2 months'") {
                assertEquals("2 months", testProduct.period)
            }
        }
    }

    @Test
    fun `period string for 7-day product shows week`() {
        Given("a 7-day subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.DAY, periodCount = 7))

            Then("period is 'week'") {
                assertEquals("week", testProduct.period)
            }
        }
    }

    @Test
    fun `period is empty for non-subscription product`() {
        Given("a non-subscription product") {
            val testProduct = TestStoreProduct(makeNonSubscriptionProduct())

            Then("period is empty") {
                assertEquals("", testProduct.period)
            }
        }
    }

    // endregion

    // region Periodly formatting

    @Test
    fun `periodly formats monthly correctly`() {
        Given("a monthly product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 1))

            Then("periodly is 'monthly'") {
                assertEquals("monthly", testProduct.periodly)
            }
        }
    }

    @Test
    fun `periodly formats yearly correctly`() {
        Given("a yearly product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.YEAR, periodCount = 1))

            Then("periodly is 'yearly'") {
                assertEquals("yearly", testProduct.periodly)
            }
        }
    }

    @Test
    fun `periodly formats quarterly as every quarter`() {
        Given("a quarterly product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 3))

            Then("periodly is 'quarterly'") {
                assertEquals("quarterly", testProduct.periodly)
            }
        }
    }

    @Test
    fun `periodly formats semi-annual as every 6 months`() {
        Given("a 6-month product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 6))

            Then("periodly is 'every 6 months'") {
                assertEquals("every 6 months", testProduct.periodly)
            }
        }
    }

    @Test
    fun `periodly formats bi-monthly as every 2 months`() {
        Given("a 2-month product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 2))

            Then("periodly is 'every 2 months'") {
                assertEquals("every 2 months", testProduct.periodly)
            }
        }
    }

    // endregion

    // region Period day/week/month/year calculations

    @Test
    fun `periodDays calculates correctly for yearly`() {
        Given("a yearly subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.YEAR, periodCount = 1))

            Then("periodDays is 365") {
                assertEquals(365, testProduct.periodDays)
                assertEquals("365", testProduct.periodDaysString)
            }
        }
    }

    @Test
    fun `periodDays calculates correctly for monthly`() {
        Given("a monthly subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 1))

            Then("periodDays is 30") {
                assertEquals(30, testProduct.periodDays)
            }
        }
    }

    @Test
    fun `periodDays calculates correctly for weekly`() {
        Given("a weekly subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.WEEK, periodCount = 1))

            Then("periodDays is 7") {
                assertEquals(7, testProduct.periodDays)
            }
        }
    }

    @Test
    fun `periodWeeks calculates correctly for yearly`() {
        Given("a yearly subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.YEAR, periodCount = 1))

            Then("periodWeeks is 52") {
                assertEquals(52, testProduct.periodWeeks)
                assertEquals("52", testProduct.periodWeeksString)
            }
        }
    }

    @Test
    fun `periodMonths calculates correctly for yearly`() {
        Given("a yearly subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.YEAR, periodCount = 1))

            Then("periodMonths is 12") {
                assertEquals(12, testProduct.periodMonths)
                assertEquals("12", testProduct.periodMonthsString)
            }
        }
    }

    @Test
    fun `periodYears is 1 for yearly product`() {
        Given("a yearly subscription product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.YEAR, periodCount = 1))

            Then("periodYears is 1") {
                assertEquals(1, testProduct.periodYears)
                assertEquals("1", testProduct.periodYearsString)
            }
        }
    }

    @Test
    fun `period calculations are zero for non-subscription product`() {
        Given("a non-subscription product") {
            val testProduct = TestStoreProduct(makeNonSubscriptionProduct())

            Then("all period values are 0") {
                assertEquals(0, testProduct.periodDays)
                assertEquals(0, testProduct.periodWeeks)
                assertEquals(0, testProduct.periodMonths)
                assertEquals(0, testProduct.periodYears)
            }
        }
    }

    // endregion

    // region Localized subscription period

    @Test
    fun `localizedSubscriptionPeriod for monthly`() {
        Given("a monthly product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 1))

            Then("localizedSubscriptionPeriod is '1 month'") {
                assertEquals("1 month", testProduct.localizedSubscriptionPeriod)
            }
        }
    }

    @Test
    fun `localizedSubscriptionPeriod for plural months`() {
        Given("a 3-month product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.MONTH, periodCount = 3))

            Then("localizedSubscriptionPeriod is '3 months'") {
                assertEquals("3 months", testProduct.localizedSubscriptionPeriod)
            }
        }
    }

    @Test
    fun `localizedSubscriptionPeriod for yearly`() {
        Given("a yearly product") {
            val testProduct = TestStoreProduct(makeProduct(period = SuperwallSubscriptionPeriod.YEAR, periodCount = 1))

            Then("localizedSubscriptionPeriod is '1 year'") {
                assertEquals("1 year", testProduct.localizedSubscriptionPeriod)
            }
        }
    }

    @Test
    fun `localizedSubscriptionPeriod is empty for non-subscription`() {
        Given("a non-subscription product") {
            val testProduct = TestStoreProduct(makeNonSubscriptionProduct())

            Then("localizedSubscriptionPeriod is empty") {
                assertEquals("", testProduct.localizedSubscriptionPeriod)
            }
        }
    }

    // endregion

    // region Currency

    @Test
    fun `currencyCode is set from product`() {
        Given("a product with EUR currency") {
            val testProduct = TestStoreProduct(makeProduct(currency = "EUR"))

            Then("currencyCode is EUR") {
                assertEquals("EUR", testProduct.currencyCode)
            }
        }
    }

    @Test
    fun `currencySymbol resolves for USD`() {
        Given("a product with USD currency") {
            val testProduct = TestStoreProduct(makeProduct(currency = "USD"))

            Then("currencySymbol is not null and contains dollar sign") {
                assertNotNull(testProduct.currencySymbol)
                assertTrue(testProduct.currencySymbol!!.contains("$"))
            }
        }
    }

    @Test
    fun `currencySymbol resolves for EUR`() {
        Given("a product with EUR currency") {
            val testProduct = TestStoreProduct(makeProduct(currency = "EUR"))

            Then("currencySymbol is euro sign") {
                // The euro sign can vary by locale, just verify it's not null
                assertNotNull(testProduct.currencySymbol)
            }
        }
    }

    // endregion

    // region Attributes

    @Test
    fun `attributes map contains all expected keys`() {
        Given("a subscription product with trial") {
            val testProduct = TestStoreProduct(makeProduct(trialPeriodDays = 7))

            When("attributes is accessed") {
                val attrs = testProduct.attributes

                Then("it contains all required keys") {
                    assertTrue(attrs.containsKey("rawPrice"))
                    assertTrue(attrs.containsKey("price"))
                    assertTrue(attrs.containsKey("period"))
                    assertTrue(attrs.containsKey("periodly"))
                    assertTrue(attrs.containsKey("weeklyPrice"))
                    assertTrue(attrs.containsKey("dailyPrice"))
                    assertTrue(attrs.containsKey("monthlyPrice"))
                    assertTrue(attrs.containsKey("yearlyPrice"))
                    assertTrue(attrs.containsKey("trialPeriodDays"))
                    assertTrue(attrs.containsKey("trialPeriodText"))
                    assertTrue(attrs.containsKey("periodDays"))
                    assertTrue(attrs.containsKey("identifier"))
                    assertTrue(attrs.containsKey("productIdentifier"))
                    assertTrue(attrs.containsKey("currencyCode"))
                }
            }
        }
    }

    @Test
    fun `attributes identifier matches product`() {
        Given("a product with identifier 'com.test.pro'") {
            val testProduct = TestStoreProduct(makeProduct(identifier = "com.test.pro"))

            Then("attributes identifier is correct") {
                assertEquals("com.test.pro", testProduct.attributes["identifier"])
                assertEquals("com.test.pro", testProduct.attributes["productIdentifier"])
            }
        }
    }

    // endregion
}
