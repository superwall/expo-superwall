@file:Suppress("ktlint:standard:max-line-length")

package com.superwall.sdk.products

import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.ProductDetails.RecurrenceMode
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
                        basePlanId = "test-2",
                        offerType = OfferType.Offer(id = "free-trial-offer"),
                    ),
            )
        assert(storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.fullIdentifier == "com.ui_tests.quarterly2:test-2:free-trial-offer")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == currencySymbol) // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.33")
        assert(storeProduct.weeklyPrice == "${currencySymbol}2.49")
        assert(storeProduct.monthlyPrice == "${currencySymbol}9.99")
        assert(storeProduct.yearlyPrice == "${currencySymbol}119.88")
        assert(storeProduct.periodDays == 30)
        assert(storeProduct.periodMonths == 1)
        assert(storeProduct.periodWeeks == 4)
        assert(storeProduct.periodYears == 0)
        assert(storeProduct.localizedSubscriptionPeriod == "1 month")
        assert(storeProduct.periodly == "monthly")
        assert(storeProduct.trialPeriodMonths == 1)
        assert(storeProduct.trialPeriodWeeks == 4)
        assert(storeProduct.trialPeriodText == "30-day")
        assert(storeProduct.price == BigDecimal("9.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set
        assert(storeProduct.trialPeriodPrice == BigDecimal.ZERO)
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}0.00")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}0.00")

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.US)
        val formattedDate = dateIn30Days.format(dateFormatter)
        println("Comparing -${storeProduct.trialPeriodEndDateString}- with -$formattedDate-")
        assert(storeProduct.trialPeriodEndDateString == formattedDate)
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
                        basePlanId = "test-2",
                        offerType = OfferType.Offer(id = "trial-and-paid-offer"),
                    ),
            )
        assert(storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.fullIdentifier == "com.ui_tests.quarterly2:test-2:trial-and-paid-offer")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == currencySymbol) // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.33")
        assert(storeProduct.weeklyPrice == "${currencySymbol}2.49")
        assert(storeProduct.monthlyPrice == "${currencySymbol}9.99")
        assert(storeProduct.yearlyPrice == "${currencySymbol}119.88")
        assert(storeProduct.periodDays == 30)
        assert(storeProduct.periodMonths == 1)
        assert(storeProduct.periodWeeks == 4)
        assert(storeProduct.periodYears == 0)
        assert(storeProduct.localizedSubscriptionPeriod == "1 month")
        assert(storeProduct.periodly == "monthly")
        assert(storeProduct.trialPeriodMonths == 1)
        assert(storeProduct.trialPeriodWeeks == 4)
        assert(storeProduct.trialPeriodText == "30-day")
        assert(storeProduct.price == BigDecimal("9.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set
        assert(storeProduct.trialPeriodPrice == BigDecimal.ZERO)
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}0.00")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}0.00")

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.US)
        val formattedDate = dateIn30Days.format(dateFormatter)
        assert(storeProduct.trialPeriodEndDateString == formattedDate)
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
                        basePlanId = "test-4",
                        offerType = OfferType.Offer(id = "paid-offer"),
                    ),
            )
        assert(storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.fullIdentifier == "com.ui_tests.quarterly2:test-4:paid-offer")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == "$currencySymbol") // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.29")
        assert(storeProduct.weeklyPrice == "${currencySymbol}2.24")
        assert(storeProduct.monthlyPrice == "${currencySymbol}8.99")
        assert(storeProduct.yearlyPrice == "${currencySymbol}107.88")
        assert(storeProduct.periodDays == 30)
        assert(storeProduct.periodMonths == 1)
        assert(storeProduct.periodWeeks == 4)
        assert(storeProduct.periodYears == 0)
        assert(storeProduct.localizedSubscriptionPeriod == "1 month")
        assert(storeProduct.periodly == "monthly")
        assert(storeProduct.trialPeriodMonths == 1)
        assert(storeProduct.trialPeriodWeeks == 4)
        assert(storeProduct.trialPeriodText == "30-day")
        assert(storeProduct.price == BigDecimal("8.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set
        assert(storeProduct.trialPeriodPrice == BigDecimal("6.74"))
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.22")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}6.74")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}1.68")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}20.22")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}6.74")

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.US)
        val formattedDate = dateIn30Days.format(dateFormatter)
        assert(storeProduct.trialPeriodEndDateString == formattedDate)
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
                        basePlanId = "test-2",
                        offerType = OfferType.Auto,
                    ),
            )
        assert(storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.fullIdentifier == "com.ui_tests.quarterly2:test-2:sw-auto")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == "$currencySymbol") // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.33")
        assert(storeProduct.weeklyPrice == "${currencySymbol}2.49")
        assert(storeProduct.monthlyPrice == "${currencySymbol}9.99")
        assert(storeProduct.yearlyPrice == "${currencySymbol}119.88")
        assert(storeProduct.periodDays == 30)
        assert(storeProduct.periodMonths == 1)
        assert(storeProduct.periodWeeks == 4)
        assert(storeProduct.periodYears == 0)
        assert(storeProduct.localizedSubscriptionPeriod == "1 month")
        assert(storeProduct.periodly == "monthly")
        assert(storeProduct.trialPeriodMonths == 1)
        assert(storeProduct.trialPeriodWeeks == 4)
        assert(storeProduct.trialPeriodText == "30-day")
        assert(storeProduct.price == BigDecimal("9.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set
        assert(storeProduct.trialPeriodPrice == BigDecimal.ZERO)
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}0.00")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}0.00")

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.US)
        val formattedDate = dateIn30Days.format(dateFormatter)
        assert(storeProduct.trialPeriodEndDateString == formattedDate)
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
                        basePlanId = "test-3",
                        offerType = OfferType.Auto,
                    ),
            )
        assert(storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.fullIdentifier == "com.ui_tests.quarterly2:test-3:sw-auto")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == "$currencySymbol") // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.09")
        assert(storeProduct.weeklyPrice == "${currencySymbol}0.74")
        assert(storeProduct.monthlyPrice == "${currencySymbol}2.99")
        assert(storeProduct.yearlyPrice == "${currencySymbol}35.88")
        assert(storeProduct.periodDays == 30)
        assert(storeProduct.periodMonths == 1)
        assert(storeProduct.periodWeeks == 4)
        assert(storeProduct.periodYears == 0)
        assert(storeProduct.localizedSubscriptionPeriod == "1 month")
        assert(storeProduct.periodly == "monthly")

        assert(storeProduct.trialPeriodMonths == 1)
        assert(storeProduct.trialPeriodWeeks == 4)
        assert(storeProduct.trialPeriodText == "30-day")
        assert(storeProduct.price == BigDecimal("2.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set

        assert(storeProduct.trialPeriodPrice == BigDecimal("1.99"))
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.06")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}1.99")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}0.49")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}1.99")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}1.99")

        val currentDate = LocalDate.now()
        val dateIn30Days = currentDate.plusMonths(1)
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtils.MMM_dd_yyyy, Locale.US)
        val formattedDate = dateIn30Days.format(dateFormatter)
        assert(storeProduct.trialPeriodEndDateString == formattedDate)
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
                        basePlanId = "test-5",
                        offerType = OfferType.Auto,
                    ),
            )
        assert(!storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.fullIdentifier == "com.ui_tests.quarterly2:test-5:sw-auto")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == "$currencySymbol") // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.01")
        assert(storeProduct.weeklyPrice == "${currencySymbol}0.07")
        assert(storeProduct.monthlyPrice == "${currencySymbol}0.33")
        assert(storeProduct.yearlyPrice == "${currencySymbol}3.99")
        assert(storeProduct.periodDays == 365)
        assert(storeProduct.periodMonths == 12)
        assert(storeProduct.periodWeeks == 52)
        assert(storeProduct.periodYears == 1)
        assert(storeProduct.localizedSubscriptionPeriod == "1 year")
        assert(storeProduct.periodly == "yearly")
        assert(storeProduct.trialPeriodMonths == 0)
        assert(storeProduct.trialPeriodWeeks == 0)
        assert(storeProduct.trialPeriodText.isEmpty())
        assert(storeProduct.price == BigDecimal("3.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set
        assert(storeProduct.trialPeriodPrice == BigDecimal.ZERO)
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}0.00")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodEndDateString.isEmpty())
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
                        basePlanId = "test-5",
                        offerType = null,
                    ),
            )
        assert(!storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.fullIdentifier == "com.ui_tests.quarterly2:test-5")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == "$currencySymbol") // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.01")
        assert(storeProduct.weeklyPrice == "${currencySymbol}0.07")
        assert(storeProduct.monthlyPrice == "${currencySymbol}0.33")
        assert(storeProduct.yearlyPrice == "${currencySymbol}3.99")
        assert(storeProduct.periodDays == 365)
        assert(storeProduct.periodMonths == 12)
        assert(storeProduct.periodWeeks == 52)
        assert(storeProduct.periodYears == 1)
        assert(storeProduct.localizedSubscriptionPeriod == "1 year")
        assert(storeProduct.periodly == "yearly")
        assert(storeProduct.trialPeriodMonths == 0)
        assert(storeProduct.trialPeriodWeeks == 0)
        assert(storeProduct.trialPeriodText.isEmpty())
        assert(storeProduct.price == BigDecimal("3.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set
        assert(storeProduct.trialPeriodPrice == BigDecimal.ZERO)
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}0.00")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodEndDateString.isEmpty())
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
                        basePlanId = "test-5",
                        offerType = OfferType.Offer(id = "doesnt-exist"),
                    ),
            )
        assert(!storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.fullIdentifier == "com.ui_tests.quarterly2:test-5")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == "$currencySymbol") // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.01")
        assert(storeProduct.weeklyPrice == "${currencySymbol}0.07")
        assert(storeProduct.monthlyPrice == "${currencySymbol}0.33")
        assert(storeProduct.yearlyPrice == "${currencySymbol}3.99")
        assert(storeProduct.periodDays == 365)
        assert(storeProduct.periodMonths == 12)
        assert(storeProduct.periodWeeks == 52)
        assert(storeProduct.periodYears == 1)
        assert(storeProduct.localizedSubscriptionPeriod == "1 year")
        assert(storeProduct.periodly == "yearly")
        assert(storeProduct.trialPeriodMonths == 0)
        assert(storeProduct.trialPeriodWeeks == 0)
        assert(storeProduct.trialPeriodText.isEmpty())
        assert(storeProduct.price == BigDecimal("3.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set
        assert(storeProduct.trialPeriodPrice == BigDecimal.ZERO)
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}0.00")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodEndDateString.isEmpty())
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
                        basePlanId = null,
                        offerType = null,
                    ),
            )
        assert(!storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.fullIdentifier == "com.ui_tests.quarterly2")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == "$currencySymbol") // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.33")
        assert(storeProduct.weeklyPrice == "${currencySymbol}2.49")
        assert(storeProduct.monthlyPrice == "${currencySymbol}9.99")
        assert(storeProduct.yearlyPrice == "${currencySymbol}119.88")
        assert(storeProduct.periodDays == 30)
        assert(storeProduct.periodMonths == 1)
        assert(storeProduct.periodWeeks == 4)
        assert(storeProduct.periodYears == 0)
        assert(storeProduct.localizedSubscriptionPeriod == "1 month")
        assert(storeProduct.periodly == "monthly")
        assert(storeProduct.trialPeriodMonths == 0)
        assert(storeProduct.trialPeriodWeeks == 0)
        assert(storeProduct.trialPeriodText.isEmpty())
        assert(storeProduct.price == BigDecimal("9.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set
        assert(storeProduct.trialPeriodPrice == BigDecimal.ZERO)
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}0.00")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodEndDateString.isEmpty())
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
                        basePlanId = null,
                        offerType = null,
                    ),
            )
        assert(!storeProduct.hasFreeTrial)
        assert(storeProduct.productIdentifier == "pro_test_8999_year")
        assert(storeProduct.fullIdentifier == "pro_test_8999_year")
        assert(storeProduct.currencyCode == "USD")
        assert(storeProduct.currencySymbol == "$currencySymbol") // This actually just returns "$" in the main app.
        assert(storeProduct.dailyPrice == "${currencySymbol}0.00")
        assert(storeProduct.weeklyPrice == "${currencySymbol}0.00")
        assert(storeProduct.monthlyPrice == "${currencySymbol}0.00")
        assert(storeProduct.yearlyPrice == "${currencySymbol}0.00")
        assert(storeProduct.periodDays == 0)
        assert(storeProduct.periodMonths == 0)
        assert(storeProduct.periodWeeks == 0)
        assert(storeProduct.periodYears == 0)
        assert(storeProduct.localizedSubscriptionPeriod.isEmpty())
        assert(storeProduct.periodly.isEmpty())
        assert(storeProduct.trialPeriodMonths == 0)
        assert(storeProduct.trialPeriodWeeks == 0)
        assert(storeProduct.trialPeriodText.isEmpty())
        assert(storeProduct.price == BigDecimal("89.99"))
        assert(storeProduct.trialPeriodYears == 0)
        assert(storeProduct.languageCode == "en")
        val defaultLocale = Locale.getDefault().toString()
        assert(storeProduct.locale == defaultLocale) // Change this depending on what your computer is set
        assert(storeProduct.trialPeriodPrice == BigDecimal.ZERO)
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.day) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.month) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.week) == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodPricePerUnit(SubscriptionPeriod.Unit.year) == "${currencySymbol}0.00")
        assert(storeProduct.localizedTrialPeriodPrice == "${currencySymbol}0.00")
        assert(storeProduct.trialPeriodEndDateString.isEmpty())
    }
}
