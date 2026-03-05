package com.superwall.sdk.billing

import com.superwall.sdk.store.abstractions.product.BasePlanType
import com.superwall.sdk.store.abstractions.product.OfferType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for [DecomposedProductIds] parsing with all permutations of:
 * - Product ID only
 * - Product ID + base plan (specific, sw-auto, empty)
 * - Product ID + base plan + offer (specific, sw-auto, null)
 */
class DecomposedProductIdsTest {
    // ==================== PRODUCT ID ONLY ====================

    @Test
    fun `productId only - returns Auto for both basePlan and offer`() {
        val result = DecomposedProductIds.from("my_product")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Auto, result.basePlanType)
        assertEquals(OfferType.Auto, result.offerType)
        assertNull(result.basePlanId)
        assertNull(result.offerType.specificId)
        assertEquals("my_product", result.fullId)
    }

    // ==================== SPECIFIC BASE PLAN + NULL OFFER ====================

    @Test
    fun `specific basePlan and null offer - returns Specific basePlan and Auto offer`() {
        val result = DecomposedProductIds.from("my_product:monthly_plan")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Specific("monthly_plan"), result.basePlanType)
        assertEquals(OfferType.Auto, result.offerType)
        assertEquals("monthly_plan", result.basePlanId)
        assertNull(result.offerType.specificId)
    }

    // ==================== SPECIFIC BASE PLAN + SW-AUTO OFFER ====================

    @Test
    fun `specific basePlan and sw-auto offer - returns Specific basePlan and Auto offer`() {
        val result = DecomposedProductIds.from("my_product:monthly_plan:sw-auto")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Specific("monthly_plan"), result.basePlanType)
        assertEquals(OfferType.Auto, result.offerType)
        assertEquals("monthly_plan", result.basePlanId)
        assertNull(result.offerType.specificId)
    }

    // ==================== SPECIFIC BASE PLAN + SPECIFIC OFFER ====================

    @Test
    fun `specific basePlan and specific offer - returns both Specific`() {
        val result = DecomposedProductIds.from("my_product:monthly_plan:free_trial_offer")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Specific("monthly_plan"), result.basePlanType)
        assertEquals(OfferType.Specific("free_trial_offer"), result.offerType)
        assertEquals("monthly_plan", result.basePlanId)
        assertEquals("free_trial_offer", result.offerType.specificId)
    }

    // ==================== SW-AUTO BASE PLAN + NULL OFFER ====================

    @Test
    fun `sw-auto basePlan and null offer - returns Auto for both`() {
        val result = DecomposedProductIds.from("my_product:sw-auto")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Auto, result.basePlanType)
        assertEquals(OfferType.Auto, result.offerType)
        assertNull(result.basePlanId)
        assertNull(result.offerType.specificId)
    }

    // ==================== SW-AUTO BASE PLAN + SW-AUTO OFFER ====================

    @Test
    fun `sw-auto basePlan and sw-auto offer - returns Auto for both`() {
        val result = DecomposedProductIds.from("my_product:sw-auto:sw-auto")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Auto, result.basePlanType)
        assertEquals(OfferType.Auto, result.offerType)
        assertNull(result.basePlanId)
        assertNull(result.offerType.specificId)
    }

    // ==================== SW-AUTO BASE PLAN + SPECIFIC OFFER ====================

    @Test
    fun `sw-auto basePlan and specific offer - returns Auto basePlan and Specific offer`() {
        val result = DecomposedProductIds.from("my_product:sw-auto:promo_offer")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Auto, result.basePlanType)
        assertEquals(OfferType.Specific("promo_offer"), result.offerType)
        assertNull(result.basePlanId)
        assertEquals("promo_offer", result.offerType.specificId)
    }

    // ==================== EMPTY BASE PLAN + NULL OFFER ====================

    @Test
    fun `empty basePlan and null offer - returns Auto for both`() {
        val result = DecomposedProductIds.from("my_product:")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Auto, result.basePlanType)
        assertEquals(OfferType.Auto, result.offerType)
        assertNull(result.basePlanId)
    }

    // ==================== EMPTY BASE PLAN + SW-AUTO OFFER ====================

    @Test
    fun `empty basePlan and sw-auto offer - returns Auto for both`() {
        val result = DecomposedProductIds.from("my_product::sw-auto")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Auto, result.basePlanType)
        assertEquals(OfferType.Auto, result.offerType)
        assertNull(result.basePlanId)
    }

    // ==================== EMPTY BASE PLAN + SPECIFIC OFFER ====================

    @Test
    fun `empty basePlan and specific offer - returns Auto basePlan and Specific offer`() {
        val result = DecomposedProductIds.from("my_product::discount_offer")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Auto, result.basePlanType)
        assertEquals(OfferType.Specific("discount_offer"), result.offerType)
        assertNull(result.basePlanId)
        assertEquals("discount_offer", result.offerType.specificId)
    }

    // ==================== SW-NONE OFFER (NO OFFER) ====================

    @Test
    fun `specific basePlan and sw-none offer - returns Specific basePlan and None offer`() {
        val result = DecomposedProductIds.from("my_product:monthly_plan:sw-none")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Specific("monthly_plan"), result.basePlanType)
        assertEquals(OfferType.None, result.offerType)
        assertEquals("monthly_plan", result.basePlanId)
        assertNull(result.offerType.specificId)
    }

    @Test
    fun `sw-auto basePlan and sw-none offer - returns Auto basePlan and None offer`() {
        val result = DecomposedProductIds.from("my_product:sw-auto:sw-none")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Auto, result.basePlanType)
        assertEquals(OfferType.None, result.offerType)
        assertNull(result.basePlanId)
        assertNull(result.offerType.specificId)
    }

    @Test
    fun `empty basePlan and sw-none offer - returns Auto basePlan and None offer`() {
        val result = DecomposedProductIds.from("my_product::sw-none")

        assertEquals("my_product", result.subscriptionId)
        assertEquals(BasePlanType.Auto, result.basePlanType)
        assertEquals(OfferType.None, result.offerType)
        assertNull(result.basePlanId)
    }

    // ==================== FULL ID PRESERVATION ====================

    @Test
    fun `fullId is preserved exactly as input`() {
        val inputs =
            listOf(
                "product",
                "product:plan",
                "product:plan:offer",
                "product:sw-auto",
                "product:sw-auto:sw-auto",
                "product:plan:sw-none",
                "product::offer",
            )

        inputs.forEach { input ->
            val result = DecomposedProductIds.from(input)
            assertEquals(input, result.fullId)
        }
    }
}
