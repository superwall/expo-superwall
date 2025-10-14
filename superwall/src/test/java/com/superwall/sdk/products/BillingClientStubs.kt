package com.superwall.sdk.products

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.OneTimePurchaseOfferDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import io.mockk.every
import io.mockk.mockk

fun mockProductDetails(
    productId: String = "sample_product_id",
    @BillingClient.ProductType type: String = BillingClient.ProductType.SUBS,
    oneTimePurchaseOfferDetails: ProductDetails.OneTimePurchaseOfferDetails? = null,
    subscriptionOfferDetails: List<ProductDetails.SubscriptionOfferDetails>? =
        listOf(
            mockSubscriptionOfferDetails(),
        ),
    name: String = "subscription_mock_name",
    description: String = "subscription_mock_description",
    title: String = "subscription_mock_title",
): ProductDetails =
    mockk<ProductDetails>().apply {
        every { getProductId() } returns productId
        every { productType } returns type
        every { getName() } returns name
        every { getDescription() } returns description
        every { getTitle() } returns title
        every { getOneTimePurchaseOfferDetails() } returns oneTimePurchaseOfferDetails
        every { getSubscriptionOfferDetails() } returns subscriptionOfferDetails
        every { zza() } returns "mock-package-name" // This seems to return the packageName property from the response json
    }

fun mockOneTimePurchaseOfferDetails(
    price: Double = 4.99,
    priceCurrencyCodeValue: String = "USD",
): OneTimePurchaseOfferDetails =
    mockk<OneTimePurchaseOfferDetails>().apply {
        every { formattedPrice } returns "${'$'}$price"
        every { priceAmountMicros } returns price.times(1_000_000).toLong()
        every { priceCurrencyCode } returns priceCurrencyCodeValue
    }

fun mockSubscriptionOfferDetails(
    tags: List<String> = emptyList(),
    token: String = "mock-subscription-offer-token",
    offerId: String = "mock-offer-id",
    basePlanId: String = "mock-base-plan-id",
    pricingPhases: List<PricingPhase> = listOf(mockPricingPhase()),
): ProductDetails.SubscriptionOfferDetails =
    mockk<ProductDetails.SubscriptionOfferDetails>().apply {
        every { offerTags } returns tags
        every { offerToken } returns token
        every { getOfferId() } returns offerId
        every { getBasePlanId() } returns basePlanId
        every { getPricingPhases() } returns
            mockk<ProductDetails.PricingPhases>().apply {
                every { pricingPhaseList } returns pricingPhases
            }
    }

fun mockPricingPhase(
    price: Double = 4.99,
    priceCurrencyCodeValue: String = "USD",
    billingPeriod: String = "P1M",
    billingCycleCount: Int = 0,
    recurrenceMode: Int = RecurrenceMode.INFINITE_RECURRING,
): PricingPhase =
    mockk<PricingPhase>().apply {
        every { formattedPrice } returns "${'$'}$price"
        every { priceAmountMicros } returns price.times(1_000_000).toLong()
        every { priceCurrencyCode } returns priceCurrencyCodeValue
        every { getBillingPeriod() } returns billingPeriod
        every { getBillingCycleCount() } returns billingCycleCount
        every { getRecurrenceMode() } returns recurrenceMode
    }
