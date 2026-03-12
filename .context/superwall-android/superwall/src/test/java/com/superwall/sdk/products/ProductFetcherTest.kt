@file:Suppress("ktlint:standard:max-line-length")

package com.superwall.sdk.products

import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.superwall.sdk.store.abstractions.product.BasePlanType
import com.superwall.sdk.store.abstractions.product.OfferType
import com.superwall.sdk.store.abstractions.product.RawStoreProduct
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.abstractions.product.SubscriptionPeriod
import com.superwall.sdk.utilities.DateUtils
import kotlinx.coroutines.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductFetcherInstrumentedTest {
    private val oneTimePurchaseProduct =
        mockProductDetails(
            productId = "pro_test_8999_year",
            type = ProductType.INAPP,
            oneTimePurchaseOfferDetails =
                mockOneTimePurchaseOfferDetails(
                    price = 89.99,
                ),
            subscriptionOfferDetails = null,
        )
    private val productDetails =
        mockProductDetails(
            productId = "com.ui_tests.quarterly2",
            type = ProductType.SUBS,
            oneTimePurchaseOfferDetails = null,
            subscriptionOfferDetails =
                listOf(
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/Yhg80Kaqu23qZ1VFz4JyBXUmtWv3wqIaGosS9ofPc6hdbl8ALUDdn+du4AXMfogPJw6ZFop9MO3oDth6XTtJfURtKTSjapPZJnWbuBUnMK20pVPUm4RGSXD9Ke0fa8AECDQDPCn+UrDQ",
                        offerId = "free-trial-offer",
                        basePlanId = "test-2",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 0.0,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 1,
                                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                                ),
                                mockPricingPhase(
                                    price = 9.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/Yhg80Kaqu23qZ1VFz4JyBXUmtWv3wqIaGosS9ofPc6hdbl8ALUDdn+du4AXMfogPJw6ZFop9MO3oDth6XTtJfURtKTSjapPZJnWbuBUnMK20pVPUm4RGSXD9Ke0fa8AECDQDPCn+UrDQ",
                        offerId = "free-trial-one-week",
                        basePlanId = "test-2",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 0.0,
                                    billingPeriod = "P1W",
                                    billingCycleCount = 1,
                                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                                ),
                                mockPricingPhase(
                                    price = 9.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        tags = listOf("sw-ignore-offer"),
                        token = "AUj\\/Yhg80Kaqu23qZ1VFz4JyBXUmtWv3wqIaGosS9ofPc6hdbl8ALUDdn+du4AXMfogPJw6ZFop9MO3oDth6XTtJfURtKTSjapPZJnWbuBUnMK20pVPUm4RGSXD9Ke0fa8AECDQDPCn+UrDQ",
                        offerId = "ignored-offer",
                        basePlanId = "test-2",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 0.0,
                                    billingPeriod = "P1Y",
                                    billingCycleCount = 1,
                                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                                ),
                                mockPricingPhase(
                                    price = 9.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/Yhg80Kaqu23qZ1VFz4JyBXUmtWv3wqIaGosS9ofPc6hdbl8ALUDdn+du4AXMfogPJw6ZFop9MO3oDth6XTtJfURtKTSjapPZJnWbuBUnMK20pVPUm4RGSXD9Ke0fa8AECDQDPCn+UrDQ",
                        offerId = "trial-and-paid-offer",
                        basePlanId = "test-2",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 0.0,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 1,
                                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                                ),
                                mockPricingPhase(
                                    price = 2.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 1,
                                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                                ),
                                mockPricingPhase(
                                    price = 9.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/YhhnamOJsY2iGIxhIw8PAbLGNIPfUt4s4QfSiabWa8hpBx4B84ImQ\\/SL3L8xPpVPUxQ4f3L6wfun5QfZwZNzv0GHrzzIy4wMFdnXUyYOWW8=",
                        offerId = "",
                        basePlanId = "test-2",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 9.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/YhgfPv4IORV8Jz3HeZYkMDpka2WDPNmqSojJawgy9c4ZuBE5h5osgTVTO3hDwhglT\\/9px8G4qrj508lVbYxCRIA\\/fjw7UM56K7UUNoFEWQQ=",
                        offerId = "paid-offer",
                        basePlanId = "test-3",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 1.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 1,
                                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                                ),
                                mockPricingPhase(
                                    price = 2.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/YhgfPv4IORV8Jz3HeZYkMDpka2WDPNmqSojJawgy9c4ZuBE5h5osgTVTO3hDwhglT\\/9px8G4qrj508lVbYxCRIA\\/fjw7UM56K7UUNoFEWQQ=",
                        offerId = "paid-offer-2",
                        basePlanId = "test-3",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 5.99,
                                    billingPeriod = "P1Y",
                                    billingCycleCount = 1,
                                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                                ),
                                mockPricingPhase(
                                    price = 2.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/YhhnamOJsY2iGIxhIw8PAbLGNIPfUt4s4QfSiabWa8hpBx4B84ImQ\\/SL3L8xPpVPUxQ4f3L6wfun5QfZwZNzv0GHrzzIy4wMFdnXUyYOWW8=",
                        offerId = "",
                        basePlanId = "test-3",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 2.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/YhhJLdYaLBKb5nkiptBeLCq18pcvWLRTjF6LHIo+w\\/fgVuzM87Vc4bC+UTY210xCmM\\/EU9BCfUthXRb6EFLwYP6lWbXBZQFJI6443iocMV1nJ5\\/iTE2rQigvbDuAlSX8HW1mG4m8NS0s1R+MWwfd+zbeMLCMbJ7mNTaI8YqZeIyJep\\/riA==",
                        offerId = "base-plan-free-trial",
                        basePlanId = "com-ui-tests-quarterly2",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 0.0,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 1,
                                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                                ),
                                mockPricingPhase(
                                    price = 18.99,
                                    billingPeriod = "P3M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/YhhSSehWYpmbL39WoarzWiQVZrd3oQOhYmE0GfLboQEdMUqIyy1yIhf7ZIwrCyh7KqZhwExFD7ZXMW\\/wd6c\\/8xBWK8lsRPs68138G3eAtsHjl2gCsgDhFb6RAnWERF1o+UWi2mdYh4o=",
                        offerId = "",
                        basePlanId = "com-ui-tests-quarterly2",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 18.99,
                                    billingPeriod = "P3M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/Yhg80Kaqu23qZ1VFz4JyBXUmtWv3wqIaGosS9ofPc6hdbl8ALUDdn+du4AXMfogPJw6ZFop9MO3oDth6XTtJfURtKTSjapPZJnWbuBUnMK20pVPUm4RGSXD9Ke0fa8AECDQDPCn+UrDQ",
                        offerId = "paid-offer",
                        basePlanId = "test-4",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 6.74,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 3,
                                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                                ),
                                mockPricingPhase(
                                    price = 8.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token = "AUj\\/YhhnamOJsY2iGIxhIw8PAbLGNIPfUt4s4QfSiabWa8hpBx4B84ImQ\\/SL3L8xPpVPUxQ4f3L6wfun5QfZwZNzv0GHrzzIy4wMFdnXUyYOWW8=",
                        offerId = "",
                        basePlanId = "test-4",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 8.99,
                                    billingPeriod = "P1M",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                    mockSubscriptionOfferDetails(
                        token =
                            "AUj\\/YhhnamOJsY2iGIxhIw8PAbLGNIPfUt4s4QfSiabWa8hpBx4B84ImQ\\/SL3L8xPpVPUxQ4f3L6wfun5QfZwZNzv0GHrzzIy4wMFdnXUyYOWW8=",
                        offerId = "",
                        basePlanId = "test-5",
                        pricingPhases =
                            listOf(
                                mockPricingPhase(
                                    price = 3.99,
                                    billingPeriod = "P1Y",
                                    billingCycleCount = 0,
                                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                                ),
                            ),
                    ),
                ),
            title = "com.ui_tests.quarterly2 (com.superwall.superapp (unreviewed))",
        )
    val currencySymbol by lazy {
        Currency.getInstance("USD").symbol
    }

    /**
     * subscription + base plan + offer: Free Trial phase ----> DONE
     * subscription + base plan + offer: Free Trial Phase + Paid Phase in one offer -> DONE
     * subscription + base plan + offer: Paid Phase ----> DONE
     * subscription + base plan + offer: Offer not found ---->
     * subscription + base plan + auto-offer: one with free trial 1 year but with sw-ignore-offer tag,
     *                                          one with free trial 1 month,
     *                                          one with free trial 1 week ---> DONE
     * subscription + base plan + auto-offer: one with paid offer 5.99,
     *                                        one with paid offer 1.99 ---> DONE
     * subscription + base plan + auto-offer: no offers, just base plan ---> DONE
     * subscription + base plan ---> DONE
     * subscription ---> DONE (gives wrong value but to be expected)
     * oneTimePurchaseOffer ---> DONE
     */

    @Test
    fun test_storeProduct_basePlan_withFreeTrialOffer() {
        // subscription + base plan + offer: Free Trial phase
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = productDetails,
                        fullIdentifier = "com.ui_tests.quarterly2:test-2:free-trial-offer",
                        basePlanType = BasePlanType.Specific("test-2"),
                        offerType = OfferType.Specific("free-trial-offer"),
                    ),
            )
        assertTrue(storeProduct.hasFreeTrial)
        assertEquals("com.ui_tests.quarterly2", storeProduct.productIdentifier)
        assertEquals("com.ui_tests.quarterly2:test-2:free-trial-offer", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        assertEquals("${currencySymbol}0.32", storeProduct.dailyPrice)
        assertEquals("${currencySymbol}2.30", storeProduct.weeklyPrice)
        assertEquals("${currencySymbol}9.99", storeProduct.monthlyPrice)
        assertEquals("${currencySymbol}119.88", storeProduct.yearlyPrice)
        assertEquals(30, storeProduct.periodDays)
        assertEquals(1, storeProduct.periodMonths)
        assertEquals(4, storeProduct.periodWeeks)
        assertEquals(0, storeProduct.periodYears)
        assertEquals("1 month", storeProduct.localizedSubscriptionPeriod)
        assertEquals("monthly", storeProduct.periodly)
        assertEquals(1, storeProduct.trialPeriodMonths)
        assertEquals(4, storeProduct.trialPeriodWeeks)
        assertEquals("30-day", storeProduct.trialPeriodText)
        assertEquals(BigDecimal("9.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set
        assertEquals(BigDecimal.ZERO, storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}0.00", storeProduct.localizedTrialPeriodPrice)

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.getDefault())
        val formattedDate = dateIn30Days.format(dateFormatter)
        println("Comparing -${storeProduct.trialPeriodEndDateString}- with -$formattedDate-")
        assertEquals(formattedDate, storeProduct.trialPeriodEndDateString)
    }

    @Test
    fun test_storeProduct_basePlan_withFreeTrialOfferAndPaid() {
        // subscription + base plan + offer: Free Trial Phase + Paid Phase in one offer
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = productDetails,
                        fullIdentifier = "com.ui_tests.quarterly2:test-2:trial-and-paid-offer",
                        basePlanType = BasePlanType.Specific("test-2"),
                        offerType = OfferType.Specific("trial-and-paid-offer"),
                    ),
            )
        assertTrue(storeProduct.hasFreeTrial)
        assertEquals("com.ui_tests.quarterly2", storeProduct.productIdentifier)
        assertEquals("com.ui_tests.quarterly2:test-2:trial-and-paid-offer", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        assertEquals("${currencySymbol}0.32", storeProduct.dailyPrice)
        assertEquals("${currencySymbol}2.30", storeProduct.weeklyPrice)
        assertEquals("${currencySymbol}9.99", storeProduct.monthlyPrice)
        assertEquals("${currencySymbol}119.88", storeProduct.yearlyPrice)
        assertEquals(30, storeProduct.periodDays)
        assertEquals(1, storeProduct.periodMonths)
        assertEquals(4, storeProduct.periodWeeks)
        assertEquals(0, storeProduct.periodYears)
        assertEquals("1 month", storeProduct.localizedSubscriptionPeriod)
        assertEquals("monthly", storeProduct.periodly)
        assertEquals(1, storeProduct.trialPeriodMonths)
        assertEquals(4, storeProduct.trialPeriodWeeks)
        assertEquals("30-day", storeProduct.trialPeriodText)
        assertEquals(BigDecimal("9.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set
        assertEquals(BigDecimal.ZERO, storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}0.00", storeProduct.localizedTrialPeriodPrice)

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.getDefault())
        val formattedDate = dateIn30Days.format(dateFormatter)
        assertEquals(formattedDate, storeProduct.trialPeriodEndDateString)
    }

    @Test
    fun test_storeProduct_basePlan_withPaidOffer() {
        // subscription + base plan + offer: Free Trial phase
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = productDetails,
                        fullIdentifier = "com.ui_tests.quarterly2:test-4:paid-offer",
                        basePlanType = BasePlanType.Specific("test-4"),
                        offerType = OfferType.Specific("paid-offer"),
                    ),
            )
        assertTrue(storeProduct.hasFreeTrial)
        assertEquals("com.ui_tests.quarterly2", storeProduct.productIdentifier)
        assertEquals("com.ui_tests.quarterly2:test-4:paid-offer", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        assertEquals("${currencySymbol}0.29", storeProduct.dailyPrice)
        assertEquals("${currencySymbol}2.07", storeProduct.weeklyPrice)
        assertEquals("${currencySymbol}8.99", storeProduct.monthlyPrice)
        assertEquals("${currencySymbol}107.88", storeProduct.yearlyPrice)
        assertEquals(30, storeProduct.periodDays)
        assertEquals(1, storeProduct.periodMonths)
        assertEquals(4, storeProduct.periodWeeks)
        assertEquals(0, storeProduct.periodYears)
        assertEquals("1 month", storeProduct.localizedSubscriptionPeriod)
        assertEquals("monthly", storeProduct.periodly)
        assertEquals(1, storeProduct.trialPeriodMonths)
        assertEquals(4, storeProduct.trialPeriodWeeks)
        assertEquals("30-day", storeProduct.trialPeriodText)
        assertEquals(BigDecimal("8.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set
        assertEquals(BigDecimal("6.74"), storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.22", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}6.74", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}1.55", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}20.22", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}6.74", storeProduct.localizedTrialPeriodPrice)

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.getDefault())
        val formattedDate = dateIn30Days.format(dateFormatter)
        assertEquals(formattedDate, storeProduct.trialPeriodEndDateString)
    }

    @Test
    fun test_storeProduct_basePlan_autoOffer_threeFreeTrials() {
        // subscription + base plan + auto-offer: one with free trial 1 year but with sw-ignore-offer tag,
        //                                        one with free trial 1 month, <- Chooses this one
        //                                        one with free trial 1 week
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = productDetails,
                        fullIdentifier = "com.ui_tests.quarterly2:test-2:sw-auto",
                        basePlanType = BasePlanType.Specific("test-2"),
                        offerType = OfferType.Auto,
                    ),
            )
        assertTrue(storeProduct.hasFreeTrial)
        assertEquals("com.ui_tests.quarterly2", storeProduct.productIdentifier)
        assertEquals("com.ui_tests.quarterly2:test-2:sw-auto", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        assertEquals("${currencySymbol}0.32", storeProduct.dailyPrice)
        assertEquals("${currencySymbol}2.30", storeProduct.weeklyPrice)
        assertEquals("${currencySymbol}9.99", storeProduct.monthlyPrice)
        assertEquals("${currencySymbol}119.88", storeProduct.yearlyPrice)
        assertEquals(30, storeProduct.periodDays)
        assertEquals(1, storeProduct.periodMonths)
        assertEquals(4, storeProduct.periodWeeks)
        assertEquals(0, storeProduct.periodYears)
        assertEquals("1 month", storeProduct.localizedSubscriptionPeriod)
        assertEquals("monthly", storeProduct.periodly)
        assertEquals(1, storeProduct.trialPeriodMonths)
        assertEquals(4, storeProduct.trialPeriodWeeks)
        assertEquals("30-day", storeProduct.trialPeriodText)
        assertEquals(BigDecimal("9.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set
        assertEquals(BigDecimal.ZERO, storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}0.00", storeProduct.localizedTrialPeriodPrice)

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.getDefault())
        val formattedDate = dateIn30Days.format(dateFormatter)
        assertEquals(formattedDate, storeProduct.trialPeriodEndDateString)
    }

    @Test
    fun test_storeProduct_basePlan_autoOffer_twoPaidOffers() {
        // subscription + base plan + auto-offer: one with paid offer 5.99,
        //                                        one with paid offer 1.99 <- chooses this one
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = productDetails,
                        fullIdentifier = "com.ui_tests.quarterly2:test-3:sw-auto",
                        basePlanType = BasePlanType.Specific("test-3"),
                        offerType = OfferType.Auto,
                    ),
            )
        assertTrue(storeProduct.hasFreeTrial)
        assertEquals("com.ui_tests.quarterly2", storeProduct.productIdentifier)
        assertEquals("com.ui_tests.quarterly2:test-3:sw-auto", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        assertEquals("${currencySymbol}0.09", storeProduct.dailyPrice)
        assertEquals("${currencySymbol}0.69", storeProduct.weeklyPrice)
        assertEquals("${currencySymbol}2.99", storeProduct.monthlyPrice)
        assertEquals("${currencySymbol}35.88", storeProduct.yearlyPrice)
        assertEquals(30, storeProduct.periodDays)
        assertEquals(1, storeProduct.periodMonths)
        assertEquals(4, storeProduct.periodWeeks)
        assertEquals(0, storeProduct.periodYears)
        assertEquals("1 month", storeProduct.localizedSubscriptionPeriod)
        assertEquals("monthly", storeProduct.periodly)

        assertEquals(1, storeProduct.trialPeriodMonths)
        assertEquals(4, storeProduct.trialPeriodWeeks)
        assertEquals("30-day", storeProduct.trialPeriodText)
        assertEquals(BigDecimal("2.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set

        assertEquals(BigDecimal("1.99"), storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.06", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}1.99", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}0.45", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}1.99", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}1.99", storeProduct.localizedTrialPeriodPrice)

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.getDefault())
        val formattedDate = dateIn30Days.format(dateFormatter)
        assertEquals(formattedDate, storeProduct.trialPeriodEndDateString)
    }

    @Test
    fun test_storeProduct_basePlan_autoOffer_noOffers() {
        // subscription + base plan + auto-offer
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = productDetails,
                        fullIdentifier = "com.ui_tests.quarterly2:test-5:sw-auto",
                        basePlanType = BasePlanType.Specific("test-5"),
                        offerType = OfferType.Auto,
                    ),
            )
        assertFalse(storeProduct.hasFreeTrial)
        assertEquals("com.ui_tests.quarterly2", storeProduct.productIdentifier)
        assertEquals("com.ui_tests.quarterly2:test-5:sw-auto", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        assertEquals("${currencySymbol}0.01", storeProduct.dailyPrice)
        assertEquals("${currencySymbol}0.07", storeProduct.weeklyPrice)
        assertEquals("${currencySymbol}0.33", storeProduct.monthlyPrice)
        assertEquals("${currencySymbol}3.99", storeProduct.yearlyPrice)
        assertEquals(365, storeProduct.periodDays)
        assertEquals(12, storeProduct.periodMonths)
        assertEquals(52, storeProduct.periodWeeks)
        assertEquals(1, storeProduct.periodYears)
        assertEquals("1 year", storeProduct.localizedSubscriptionPeriod)
        assertEquals("yearly", storeProduct.periodly)
        assertEquals(0, storeProduct.trialPeriodMonths)
        assertEquals(0, storeProduct.trialPeriodWeeks)
        assertTrue(storeProduct.trialPeriodText.isEmpty())
        assertEquals(BigDecimal("3.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set
        assertEquals(BigDecimal.ZERO, storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}0.00", storeProduct.localizedTrialPeriodPrice)
        assertTrue(storeProduct.trialPeriodEndDateString.isEmpty())
    }

    @Test
    fun test_storeProduct_basePlan() {
        // subscription + base plan
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = productDetails,
                        fullIdentifier = "com.ui_tests.quarterly2:test-5",
                        basePlanType = BasePlanType.Specific("test-5"),
                        offerType = OfferType.Auto,
                    ),
            )
        assertFalse(storeProduct.hasFreeTrial)
        assertEquals("com.ui_tests.quarterly2", storeProduct.productIdentifier)
        assertEquals("com.ui_tests.quarterly2:test-5", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        assertEquals("${currencySymbol}0.01", storeProduct.dailyPrice)
        assertEquals("${currencySymbol}0.07", storeProduct.weeklyPrice)
        assertEquals("${currencySymbol}0.33", storeProduct.monthlyPrice)
        assertEquals("${currencySymbol}3.99", storeProduct.yearlyPrice)
        assertEquals(365, storeProduct.periodDays)
        assertEquals(12, storeProduct.periodMonths)
        assertEquals(52, storeProduct.periodWeeks)
        assertEquals(1, storeProduct.periodYears)
        assertEquals("1 year", storeProduct.localizedSubscriptionPeriod)
        assertEquals("yearly", storeProduct.periodly)
        assertEquals(0, storeProduct.trialPeriodMonths)
        assertEquals(0, storeProduct.trialPeriodWeeks)
        assertTrue(storeProduct.trialPeriodText.isEmpty())
        assertEquals(BigDecimal("3.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set
        assertEquals(BigDecimal.ZERO, storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}0.00", storeProduct.localizedTrialPeriodPrice)
        assertTrue(storeProduct.trialPeriodEndDateString.isEmpty())
    }

    @Test
    fun test_storeProduct_basePlan_invalidOfferId() {
        // subscription + base plan + offer: Offer not found
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = productDetails,
                        fullIdentifier = "com.ui_tests.quarterly2:test-5",
                        basePlanType = BasePlanType.Specific("test-5"),
                        offerType = OfferType.Specific("doesnt-exist"),
                    ),
            )
        assertFalse(storeProduct.hasFreeTrial)
        assertEquals("com.ui_tests.quarterly2", storeProduct.productIdentifier)
        assertEquals("com.ui_tests.quarterly2:test-5", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        assertEquals("${currencySymbol}0.01", storeProduct.dailyPrice)
        assertEquals("${currencySymbol}0.07", storeProduct.weeklyPrice)
        assertEquals("${currencySymbol}0.33", storeProduct.monthlyPrice)
        assertEquals("${currencySymbol}3.99", storeProduct.yearlyPrice)
        assertEquals(365, storeProduct.periodDays)
        assertEquals(12, storeProduct.periodMonths)
        assertEquals(52, storeProduct.periodWeeks)
        assertEquals(1, storeProduct.periodYears)
        assertEquals("1 year", storeProduct.localizedSubscriptionPeriod)
        assertEquals("yearly", storeProduct.periodly)
        assertEquals(0, storeProduct.trialPeriodMonths)
        assertEquals(0, storeProduct.trialPeriodWeeks)
        assertTrue(storeProduct.trialPeriodText.isEmpty())
        assertEquals(BigDecimal("3.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set
        assertEquals(BigDecimal.ZERO, storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}0.00", storeProduct.localizedTrialPeriodPrice)
        assertTrue(storeProduct.trialPeriodEndDateString.isEmpty())
    }

    @Test
    fun test_storeProduct_subscriptionOnly() {
        // subscription
        // Note: This returns the wrong one. We expect 18.99, as that's the backwards compatible one
        // However, there's no way of us knowing this so we just pick the first base plan we come
        // across.
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = productDetails,
                        fullIdentifier = "com.ui_tests.quarterly2",
                        basePlanType = BasePlanType.Auto,
                        offerType = OfferType.Auto,
                    ),
            )
        assertFalse(storeProduct.hasFreeTrial)
        assertEquals("com.ui_tests.quarterly2", storeProduct.productIdentifier)
        assertEquals("com.ui_tests.quarterly2", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        assertEquals("${currencySymbol}0.32", storeProduct.dailyPrice)
        assertEquals("${currencySymbol}2.30", storeProduct.weeklyPrice)
        assertEquals("${currencySymbol}9.99", storeProduct.monthlyPrice)
        assertEquals("${currencySymbol}119.88", storeProduct.yearlyPrice)
        assertEquals(30, storeProduct.periodDays)
        assertEquals(1, storeProduct.periodMonths)
        assertEquals(4, storeProduct.periodWeeks)
        assertEquals(0, storeProduct.periodYears)
        assertEquals("1 month", storeProduct.localizedSubscriptionPeriod)
        assertEquals("monthly", storeProduct.periodly)
        assertEquals(0, storeProduct.trialPeriodMonths)
        assertEquals(0, storeProduct.trialPeriodWeeks)
        assertTrue(storeProduct.trialPeriodText.isEmpty())
        assertEquals(BigDecimal("9.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set
        assertEquals(BigDecimal.ZERO, storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}0.00", storeProduct.localizedTrialPeriodPrice)
        assertTrue(storeProduct.trialPeriodEndDateString.isEmpty())
    }

    @Test
    fun test_storeProduct_oneTimePurchase() {
        // One-time purchase
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = oneTimePurchaseProduct,
                        fullIdentifier = "pro_test_8999_year",
                        basePlanType = BasePlanType.Auto,
                        offerType = OfferType.Auto,
                    ),
            )
        assertFalse(storeProduct.hasFreeTrial)
        assertEquals("pro_test_8999_year", storeProduct.productIdentifier)
        assertEquals("pro_test_8999_year", storeProduct.fullIdentifier)
        assertEquals("USD", storeProduct.currencyCode)
        assertEquals(currencySymbol, storeProduct.currencySymbol) // This actually just returns "$" in the main app.
        // One-time purchases return "n/a" for period-based prices (aligned with iOS)
        assertEquals("n/a", storeProduct.dailyPrice)
        assertEquals("n/a", storeProduct.weeklyPrice)
        assertEquals("n/a", storeProduct.monthlyPrice)
        assertEquals("n/a", storeProduct.yearlyPrice)
        assertEquals(0, storeProduct.periodDays)
        assertEquals(0, storeProduct.periodMonths)
        assertEquals(0, storeProduct.periodWeeks)
        assertEquals(0, storeProduct.periodYears)
        assertTrue(storeProduct.localizedSubscriptionPeriod.isEmpty())
        assertTrue(storeProduct.periodly.isEmpty())
        assertEquals(0, storeProduct.trialPeriodMonths)
        assertEquals(0, storeProduct.trialPeriodWeeks)
        assertTrue(storeProduct.trialPeriodText.isEmpty())
        assertEquals(BigDecimal("89.99"), storeProduct.price)
        assertEquals(0, storeProduct.trialPeriodYears)
        assertEquals("en", storeProduct.languageCode)
        val defaultLocale = Locale.getDefault().toString()
        assertEquals(defaultLocale, storeProduct.locale) // Change this depending on what your computer is set
        assertEquals(BigDecimal.ZERO, storeProduct.trialPeriodPrice)
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week))
        assertEquals("${currencySymbol}0.00", storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year))
        assertEquals("${currencySymbol}0.00", storeProduct.localizedTrialPeriodPrice)
        assertTrue(storeProduct.trialPeriodEndDateString.isEmpty())
    }

    // ==================== OTP with Purchase Options Tests ====================

    /**
     * OTP product with multiple purchase options for testing selection logic.
     * Options:
     * - first-buy-option (no offer): $42.90
     * - second-buy-option (no offer): $20.90
     * - first-buy-option + fifty-off offer: $21.50
     */
    private val otpWithPurchaseOptionsProduct by lazy {
        mockProductDetails(
            productId = "otp_with_options",
            type = ProductType.INAPP,
            oneTimePurchaseOfferDetails =
                mockOneTimePurchaseOfferDetails(
                    price = 42.90,
                    purchaseOptionId = "first-buy-option",
                    offerId = null,
                    offerToken = "token-first-no-offer",
                ),
            oneTimePurchaseOfferDetailsList =
                listOf(
                    mockOneTimePurchaseOfferDetails(
                        price = 42.90,
                        purchaseOptionId = "first-buy-option",
                        offerId = null,
                        offerToken = "token-first-no-offer",
                    ),
                    mockOneTimePurchaseOfferDetails(
                        price = 20.90,
                        purchaseOptionId = "second-buy-option",
                        offerId = null,
                        offerToken = "token-second-no-offer",
                    ),
                    mockOneTimePurchaseOfferDetails(
                        price = 21.50,
                        purchaseOptionId = "first-buy-option",
                        offerId = "fifty-off",
                        offerToken = "token-first-fifty-off",
                    ),
                ),
            subscriptionOfferDetails = null,
        )
    }

    /**
     * Case 1: Specific purchase option + specific offer
     * Should find exact match (first-buy-option + fifty-off)
     */
    @Test
    fun test_storeProduct_otp_specificOption_specificOffer_exactMatch() {
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpWithPurchaseOptionsProduct,
                        fullIdentifier = "otp_with_options:first-buy-option:fifty-off",
                        basePlanType = BasePlanType.Specific("first-buy-option"),
                        offerType = OfferType.Specific("fifty-off"),
                    ),
            )

        // Should select the discounted offer
        assertEquals(BigDecimal("21.50"), storeProduct.price, "Expected price 21.50, got ${storeProduct.price}")
        assertEquals("otp_with_options", storeProduct.productIdentifier)
        assertEquals("otp_with_options:first-buy-option:fifty-off", storeProduct.fullIdentifier)
        assertFalse(storeProduct.hasFreeTrial) // OTP doesn't have trials
        assertEquals("USD", storeProduct.currencyCode)
    }

    /**
     * Case 1: Specific purchase option + specific offer (offer not found)
     * Should fallback to purchase option without offer
     */
    @Test
    fun test_storeProduct_otp_specificOption_specificOffer_fallbackToOption() {
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpWithPurchaseOptionsProduct,
                        fullIdentifier = "otp_with_options:second-buy-option:nonexistent-offer",
                        basePlanType = BasePlanType.Specific("second-buy-option"),
                        offerType = OfferType.Specific("nonexistent-offer"),
                    ),
            )

        // Should fallback to second-buy-option without offer (no fifty-off on second option)
        assertEquals(BigDecimal("20.90"), storeProduct.price, "Expected price 20.90, got ${storeProduct.price}")
    }

    /**
     * Case 2: Auto purchase option + specific offer
     * Should find all options with that offer and select cheapest
     */
    @Test
    fun test_storeProduct_otp_autoOption_specificOffer() {
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpWithPurchaseOptionsProduct,
                        fullIdentifier = "otp_with_options::fifty-off",
                        basePlanType = BasePlanType.Auto,
                        offerType = OfferType.Specific("fifty-off"),
                    ),
            )

        // Should find the only option with fifty-off offer (first-buy-option at $21.50)
        assertEquals(BigDecimal("21.50"), storeProduct.price, "Expected price 21.50, got ${storeProduct.price}")
    }

    /**
     * Case 3: Specific purchase option + auto offer
     * Should find all offers for that option and select cheapest
     */
    @Test
    fun test_storeProduct_otp_specificOption_autoOffer_selectsCheapest() {
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpWithPurchaseOptionsProduct,
                        fullIdentifier = "otp_with_options:first-buy-option:sw-auto",
                        basePlanType = BasePlanType.Specific("first-buy-option"),
                        offerType = OfferType.Auto,
                    ),
            )

        // Should select cheapest for first-buy-option: fifty-off at $21.50 (vs $42.90 without offer)
        assertEquals(BigDecimal("21.50"), storeProduct.price, "Expected price 21.50, got ${storeProduct.price}")
    }

    /**
     * Case 3: Specific purchase option + auto offer (option has no offers)
     * Should select the only option available for that purchase option
     */
    @Test
    fun test_storeProduct_otp_specificOption_autoOffer_noOffers() {
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpWithPurchaseOptionsProduct,
                        fullIdentifier = "otp_with_options:second-buy-option:sw-auto",
                        basePlanType = BasePlanType.Specific("second-buy-option"),
                        offerType = OfferType.Auto,
                    ),
            )

        // second-buy-option has no offers, so should select the base option at $20.90
        assertEquals(BigDecimal("20.90"), storeProduct.price, "Expected price 20.90, got ${storeProduct.price}")
    }

    /**
     * Case 4: Auto purchase option + auto offer
     * Should prefer cheapest with both purchaseOptionId AND offerId, fallback to cheapest overall
     */
    @Test
    fun test_storeProduct_otp_autoOption_autoOffer_prefersBothSet() {
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpWithPurchaseOptionsProduct,
                        fullIdentifier = "otp_with_options::sw-auto",
                        basePlanType = BasePlanType.Auto,
                        offerType = OfferType.Auto,
                    ),
            )

        // Should prefer options with both purchaseOptionId AND offerId set
        // Only first-buy-option + fifty-off has both, so $21.50
        assertEquals(BigDecimal("21.50"), storeProduct.price, "Expected price 21.50, got ${storeProduct.price}")
    }

    /**
     * Case 4: Auto purchase option + auto offer (no options with both set)
     * Should fallback to cheapest overall
     */
    @Test
    fun test_storeProduct_otp_autoOption_autoOffer_fallbackToCheapest() {
        // Create a product where no options have both purchaseOptionId AND offerId
        val otpNoOffersProduct =
            mockProductDetails(
                productId = "otp_no_offers",
                type = ProductType.INAPP,
                oneTimePurchaseOfferDetails =
                    mockOneTimePurchaseOfferDetails(
                        price = 50.00,
                        purchaseOptionId = "option-a",
                        offerId = null,
                    ),
                oneTimePurchaseOfferDetailsList =
                    listOf(
                        mockOneTimePurchaseOfferDetails(
                            price = 50.00,
                            purchaseOptionId = "option-a",
                            offerId = null,
                            offerToken = "token-a",
                        ),
                        mockOneTimePurchaseOfferDetails(
                            price = 30.00,
                            purchaseOptionId = "option-b",
                            offerId = null,
                            offerToken = "token-b",
                        ),
                    ),
                subscriptionOfferDetails = null,
            )

        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpNoOffersProduct,
                        fullIdentifier = "otp_no_offers::sw-auto",
                        basePlanType = BasePlanType.Auto,
                        offerType = OfferType.Auto,
                    ),
            )

        // No options have both set, so fallback to cheapest overall: option-b at $30.00
        assertEquals(BigDecimal("30.00"), storeProduct.price, "Expected price 30.00, got ${storeProduct.price}")
    }

    /**
     * Test that selectedOffer returns correct type for OTP with purchase options
     */
    @Test
    fun test_storeProduct_otp_selectedOffer_isOneTimeType() {
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpWithPurchaseOptionsProduct,
                        fullIdentifier = "otp_with_options:first-buy-option:fifty-off",
                        basePlanType = BasePlanType.Specific("first-buy-option"),
                        offerType = OfferType.Specific("fifty-off"),
                    ),
            )

        val selectedOffer = storeProduct.rawStoreProduct!!.selectedOffer
        assertTrue(
            selectedOffer is RawStoreProduct.SelectedOfferDetails.OneTime,
            "Expected SelectedOfferDetails.OneTime, got ${selectedOffer?.javaClass?.simpleName}",
        )

        val oneTimeOffer = selectedOffer as RawStoreProduct.SelectedOfferDetails.OneTime
        assertEquals("first-buy-option", oneTimeOffer.purchaseOptionId, "Expected purchaseOptionId first-buy-option")
        assertEquals("fifty-off", oneTimeOffer.offerId, "Expected offerId fifty-off")
    }

    /**
     * Test that OTP with specific purchase option but no discount offer
     * has purchaseOptionId set and offerId null.
     * This is important because the offerToken is still needed to purchase
     * the correct purchase option even without a discount offer.
     */
    @Test
    fun test_storeProduct_otp_specificOption_noOffer_hasPurchaseOptionIdWithNullOfferId() {
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpWithPurchaseOptionsProduct,
                        fullIdentifier = "otp_with_options:second-buy-option:sw-auto",
                        basePlanType = BasePlanType.Specific("second-buy-option"),
                        offerType = OfferType.Auto,
                    ),
            )

        val selectedOffer = storeProduct.rawStoreProduct!!.selectedOffer
        assertNotNull(selectedOffer, "Expected selectedOffer to be non-null")
        assertTrue(
            selectedOffer is RawStoreProduct.SelectedOfferDetails.OneTime,
            "Expected SelectedOfferDetails.OneTime, got ${selectedOffer?.javaClass?.simpleName}",
        )

        val oneTimeOffer = selectedOffer as RawStoreProduct.SelectedOfferDetails.OneTime
        // purchaseOptionId should be set even without a discount offer
        assertEquals("second-buy-option", oneTimeOffer.purchaseOptionId, "Expected purchaseOptionId second-buy-option")
        // offerId should be null since we're using auto offer and there's no discount
        assertNull(oneTimeOffer.offerId, "Expected offerId to be null for purchase option without discount")
        // The offerToken should still be available for Google Play billing
        assertNotNull(oneTimeOffer.underlying.offerToken, "Expected offerToken to be non-null")
    }

    /**
     * Test that OTP properties return expected values (no subscription-specific values)
     */
    @Test
    fun test_storeProduct_otp_withOptions_noSubscriptionValues() {
        val storeProduct =
            StoreProduct(
                rawStoreProduct =
                    RawStoreProduct(
                        underlyingProductDetails = otpWithPurchaseOptionsProduct,
                        fullIdentifier = "otp_with_options:first-buy-option:fifty-off",
                        basePlanType = BasePlanType.Specific("first-buy-option"),
                        offerType = OfferType.Specific("fifty-off"),
                    ),
            )

        // OTP should not have subscription-specific values
        assertFalse(storeProduct.hasFreeTrial)
        assertEquals(null, storeProduct.subscriptionPeriod)
        assertEquals(BigDecimal.ZERO, storeProduct.trialPeriodPrice)
        assertEquals(0, storeProduct.periodDays)
        assertEquals(0, storeProduct.periodMonths)
        assertEquals(0, storeProduct.periodWeeks)
        assertEquals(0, storeProduct.periodYears)
        assertTrue(storeProduct.localizedSubscriptionPeriod.isEmpty())
        assertTrue(storeProduct.periodly.isEmpty())
    }
}
