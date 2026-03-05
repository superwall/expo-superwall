package com.superwall.sdk.store.abstractions.product

import com.superwall.sdk.models.product.Offer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [OfferType] factory method and conversions.
 */
class OfferTypeTest {
    // ==================== FROM() FACTORY METHOD ====================

    @Test
    fun `from null returns Auto`() {
        val result = OfferType.from(null)
        assertEquals(OfferType.Auto, result)
    }

    @Test
    fun `from empty string returns Auto`() {
        val result = OfferType.from("")
        assertEquals(OfferType.Auto, result)
    }

    @Test
    fun `from sw-auto returns Auto`() {
        val result = OfferType.from("sw-auto")
        assertEquals(OfferType.Auto, result)
    }

    @Test
    fun `from sw-none returns None`() {
        val result = OfferType.from("sw-none")
        assertEquals(OfferType.None, result)
    }

    @Test
    fun `from specific value returns Specific with that value`() {
        val result = OfferType.from("monthly_plan")
        assertEquals(OfferType.Specific("monthly_plan"), result)
        assertEquals("monthly_plan", result.specificId)
    }

    @Test
    fun `from whitespace only returns Specific (not treated as empty)`() {
        // Whitespace is treated as a specific value, not empty
        val result = OfferType.from("  ")
        assertTrue(result is OfferType.Specific)
        assertEquals("  ", result.specificId)
    }

    // ==================== SPECIFIC ID ====================

    @Test
    fun `Auto specificId returns null`() {
        assertNull(OfferType.Auto.specificId)
    }

    @Test
    fun `None specificId returns null`() {
        assertNull(OfferType.None.specificId)
    }

    @Test
    fun `Specific specificId returns the id`() {
        val specific = OfferType.Specific("my_plan")
        assertEquals("my_plan", specific.specificId)
    }

    // ==================== TO OFFER CONVERSION ====================

    @Test
    fun `Auto toOffer returns Offer Automatic`() {
        val result = OfferType.Auto.toOffer()
        assertTrue(result is Offer.Automatic)
    }

    @Test
    fun `None toOffer returns Offer NoOffer`() {
        val result = OfferType.None.toOffer()
        assertTrue(result is Offer.NoOffer)
    }

    @Test
    fun `Specific toOffer returns Offer Specified with correct id`() {
        val result = OfferType.Specific("promo_offer").toOffer()
        assertTrue(result is Offer.Specified)
        assertEquals("promo_offer", (result as Offer.Specified).offerIdentifier)
    }

    // ==================== EQUALITY ====================

    @Test
    fun `Auto equals Auto`() {
        assertEquals(OfferType.Auto, OfferType.Auto)
    }

    @Test
    fun `None equals None`() {
        assertEquals(OfferType.None, OfferType.None)
    }

    @Test
    fun `Specific with same id are equal`() {
        val a = OfferType.Specific("plan_a")
        val b = OfferType.Specific("plan_a")
        assertEquals(a, b)
    }

    @Test
    fun `Specific with different ids are not equal`() {
        val a = OfferType.Specific("plan_a")
        val b = OfferType.Specific("plan_b")
        assertTrue(a != b)
    }

    @Test
    fun `Auto and Specific are not equal`() {
        val auto = OfferType.Auto
        val specific = OfferType.Specific("plan")
        assertTrue(auto != specific)
    }

    @Test
    fun `Auto and None are not equal`() {
        assertTrue(OfferType.Auto != OfferType.None)
    }

    @Test
    fun `None and Specific are not equal`() {
        assertTrue(OfferType.None != OfferType.Specific("plan"))
    }
}
