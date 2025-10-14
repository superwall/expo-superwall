package com.superwall.sdk.billing.observer

import com.android.billingclient.api.ProductDetails
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SuperwallBillingFlowParamsTest {
    @Test
    fun test_builder_sets_all_parameters_correctly() {
        // Mock ProductDetails
        val mockProductDetails = mockk<ProductDetails>(relaxed = true, relaxUnitFun = true)
        every { mockProductDetails.oneTimePurchaseOfferDetails } returns
            mockk {
                every { this@mockk.offerToken } returns "test_offer_token"
            }
        every { mockProductDetails.subscriptionOfferDetails } returns mockk {}
        every { mockProductDetails.productType } returns "subs"
        every { mockProductDetails.zza() } returns "test_product_id"

        // Create ProductDetailsParams
        val productDetailsParams =
            SuperwallBillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setOfferToken("test_offer_token")
                .setProductDetails(mockProductDetails)
                .build()

        // Create SubscriptionUpdateParams
        val subscriptionUpdateParams =
            SuperwallBillingFlowParams.SubscriptionUpdateParams
                .newBuilder()
                .setSubscriptionReplacementMode(SuperwallBillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION)
                .setOriginalExternalTransactionId("external_transaction_id")
                .build()

        // Build main params
        val params =
            SuperwallBillingFlowParams
                .newBuilder()
                .setIsOfferPersonalized(true)
                .setObfuscatedAccountId("test_account_id")
                .setObfuscatedProfileId("test_profile_id")
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .setSubscriptionUpdateParams(subscriptionUpdateParams)
                .build()

        // Verify the built object
        assertNotNull(params)
        assertNotNull(params.toOriginal())
        assertEquals(1, params.productDetailsParams.size)

        // Verify ProductDetails
        val storedProductDetails = params.productDetailsParams[0].details
        assertEquals(mockProductDetails, storedProductDetails)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_product_details_params_builder_throws_when_product_details_is_missing() {
        SuperwallBillingFlowParams.ProductDetailsParams
            .newBuilder()
            .setOfferToken("test_offer_token")
            .build()
    }

    @Test
    fun test_subscription_update_params_replacement_modes() {
        val params =
            SuperwallBillingFlowParams.SubscriptionUpdateParams
                .newBuilder()
                .setSubscriptionReplacementMode(SuperwallBillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION)
                .setOldPurchaseToken("test_token")
                .build()

        assertNotNull(params)
        assertNotNull(params.toOriginal())
    }
}
