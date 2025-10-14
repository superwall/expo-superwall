package com.superwall.sdk.utilities

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.OneTimePurchaseOfferDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.Purchase
import io.mockk.every
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject

class PurchaseMockBuilder {
    private val purchaseJson = JSONObject()

    fun setPurchaseState(state: Int): PurchaseMockBuilder {
        purchaseJson.put("purchaseState", if (state == 2) 4 else state)
        return this
    }

    fun setPurchaseTime(time: Long): PurchaseMockBuilder {
        purchaseJson.put("purchaseTime", time)
        return this
    }

    fun setOrderId(orderId: String?): PurchaseMockBuilder {
        purchaseJson.put("orderId", orderId)
        return this
    }

    fun setProductId(productId: String?): PurchaseMockBuilder {
        val productIds = JSONArray()
        productIds.put(productId)
        purchaseJson.put("productIds", productIds)
        // For backward compatibility
        purchaseJson.put("productId", productId)
        return this
    }

    fun setQuantity(quantity: Int): PurchaseMockBuilder {
        purchaseJson.put("quantity", quantity)
        return this
    }

    fun setPurchaseToken(token: String?): PurchaseMockBuilder {
        purchaseJson.put("token", token)
        purchaseJson.put("purchaseToken", token)
        return this
    }

    fun setPackageName(packageName: String?): PurchaseMockBuilder {
        purchaseJson.put("packageName", packageName)
        return this
    }

    fun setDeveloperPayload(payload: String?): PurchaseMockBuilder {
        purchaseJson.put("developerPayload", payload)
        return this
    }

    fun setAcknowledged(acknowledged: Boolean): PurchaseMockBuilder {
        purchaseJson.put("acknowledged", acknowledged)
        return this
    }

    fun setAutoRenewing(autoRenewing: Boolean): PurchaseMockBuilder {
        purchaseJson.put("autoRenewing", autoRenewing)
        return this
    }

    fun setAccountIdentifiers(
        obfuscatedAccountId: String?,
        obfuscatedProfileId: String?,
    ): PurchaseMockBuilder {
        purchaseJson.put("obfuscatedAccountId", obfuscatedAccountId)
        purchaseJson.put("obfuscatedProfileId", obfuscatedProfileId)
        return this
    }

    fun build(): Purchase = Purchase(purchaseJson.toString(), "dummy-signature")

    companion object {
        fun createDefaultPurchase(id: String): Purchase =
            PurchaseMockBuilder()
                .setPurchaseState(Purchase.PurchaseState.PURCHASED)
                .setPurchaseTime(System.currentTimeMillis())
                .setOrderId("GPA.1234-5678-9012-34567")
                .setProductId(id)
                .setQuantity(1)
                .setPurchaseToken("opaque-token-up-to-1950-characters")
                .setPackageName("com.superwall.sdk")
                .setDeveloperPayload("")
                .setAcknowledged(true)
                .setAutoRenewing(true)
                .build()
    }
}

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
