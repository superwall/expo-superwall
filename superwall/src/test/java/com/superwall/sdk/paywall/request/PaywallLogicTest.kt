package com.superwall.sdk.paywall.request

import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.store.abstractions.product.StoreProduct
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaywallLogicTest {
    @Test
    fun test_requestHash_withIdentifier() {
        val hash =
            PaywallLogic.requestHash(
                identifier = "test_paywall",
                event = null,
                locale = "en_US",
                joinedSubstituteProductIds = null,
            )
        assertEquals("test_paywall_en_US_", hash)
    }

    @Test
    fun test_requestHash_withEvent() {
        val eventData =
            mockk<EventData> {
                every { name } returns "campaign_trigger"
            }
        val hash =
            PaywallLogic.requestHash(
                identifier = null,
                event = eventData,
                locale = "en_US",
                joinedSubstituteProductIds = null,
            )
        assertEquals("campaign_trigger_en_US_", hash)
    }

    @Test
    fun test_requestHash_withSubstituteProducts() {
        val hash =
            PaywallLogic.requestHash(
                identifier = "test_paywall",
                event = null,
                locale = "en_US",
                joinedSubstituteProductIds = "product1product2",
            )
        assertEquals("test_paywall_en_US_product1product2", hash)
    }

    @Test
    fun test_requestHash_calledManually() {
        val hash =
            PaywallLogic.requestHash(
                identifier = null,
                event = null,
                locale = "en_US",
                joinedSubstituteProductIds = null,
            )
        assertEquals("\$called_manually_en_US_", hash)
    }

    @Test
    fun test_requestHash_differentLocales() {
        val hashEN =
            PaywallLogic.requestHash(
                identifier = "test_paywall",
                event = null,
                locale = "en_US",
                joinedSubstituteProductIds = null,
            )
        val hashES =
            PaywallLogic.requestHash(
                identifier = "test_paywall",
                event = null,
                locale = "es_ES",
                joinedSubstituteProductIds = null,
            )
        assertEquals("test_paywall_en_US_", hashEN)
        assertEquals("test_paywall_es_ES_", hashES)
    }

    @Test
    fun test_handlePaywallError_returnsException() {
        val inputError = RuntimeException("Test error")
        val result = PaywallLogic.handlePaywallError(inputError, null)

        assertTrue(result is Exception)
        assertEquals("Not Found", result.message)
    }

    @Test
    fun test_handlePaywallError_withEventData() {
        val eventData =
            mockk<EventData> {
                every { name } returns "test_event"
            }
        val inputError = RuntimeException("Test error")
        val result = PaywallLogic.handlePaywallError(inputError, eventData)

        assertTrue(result is Exception)
        assertEquals("Not Found", result.message)
    }

    @Test
    fun test_getVariablesAndFreeTrial_withFreeTrialProduct() {
        val storeProduct =
            mockk<StoreProduct> {
                every { hasFreeTrial } returns true
                every { attributes } returns mapOf("price" to "9.99")
            }

        val productItem =
            mockk<ProductItem> {
                every { name } returns "primary"
                every { fullProductId } returns "com.example.product1"
            }

        val productsByFullId = mapOf("com.example.product1" to storeProduct)

        val outcome =
            PaywallLogic.getVariablesAndFreeTrial(
                productItems = listOf(productItem),
                productsByFullId = productsByFullId,
                isFreeTrialAvailableOverride = null,
            )

        assertEquals(1, outcome.productVariables.size)
        assertEquals("primary", outcome.productVariables[0].name)
        assertTrue(outcome.isFreeTrialAvailable)
    }

    @Test
    fun test_getVariablesAndFreeTrial_withoutFreeTrialProduct() {
        val storeProduct =
            mockk<StoreProduct> {
                every { hasFreeTrial } returns false
                every { attributes } returns mapOf("price" to "9.99")
            }

        val productItem =
            mockk<ProductItem> {
                every { name } returns "primary"
                every { fullProductId } returns "com.example.product1"
            }

        val productsByFullId = mapOf("com.example.product1" to storeProduct)

        val outcome =
            PaywallLogic.getVariablesAndFreeTrial(
                productItems = listOf(productItem),
                productsByFullId = productsByFullId,
                isFreeTrialAvailableOverride = null,
            )

        assertEquals(1, outcome.productVariables.size)
        assertFalse(outcome.isFreeTrialAvailable)
    }

    @Test
    fun test_getVariablesAndFreeTrial_withOverride_true() {
        val storeProduct =
            mockk<StoreProduct> {
                every { hasFreeTrial } returns false
                every { attributes } returns mapOf("price" to "9.99")
            }

        val productItem =
            mockk<ProductItem> {
                every { name } returns "primary"
                every { fullProductId } returns "com.example.product1"
            }

        val productsByFullId = mapOf("com.example.product1" to storeProduct)

        val outcome =
            PaywallLogic.getVariablesAndFreeTrial(
                productItems = listOf(productItem),
                productsByFullId = productsByFullId,
                isFreeTrialAvailableOverride = true,
            )

        assertTrue(outcome.isFreeTrialAvailable)
    }

    @Test
    fun test_getVariablesAndFreeTrial_withOverride_false() {
        val storeProduct =
            mockk<StoreProduct> {
                every { hasFreeTrial } returns true
                every { attributes } returns mapOf("price" to "9.99")
            }

        val productItem =
            mockk<ProductItem> {
                every { name } returns "primary"
                every { fullProductId } returns "com.example.product1"
            }

        val productsByFullId = mapOf("com.example.product1" to storeProduct)

        val outcome =
            PaywallLogic.getVariablesAndFreeTrial(
                productItems = listOf(productItem),
                productsByFullId = productsByFullId,
                isFreeTrialAvailableOverride = false,
            )

        assertFalse(outcome.isFreeTrialAvailable)
    }

    @Test
    fun test_getVariablesAndFreeTrial_multipleProducts() {
        val storeProduct1 =
            mockk<StoreProduct> {
                every { hasFreeTrial } returns false
                every { attributes } returns mapOf("price" to "9.99")
            }

        val storeProduct2 =
            mockk<StoreProduct> {
                every { hasFreeTrial } returns true
                every { attributes } returns mapOf("price" to "19.99")
            }

        val productItem1 =
            mockk<ProductItem> {
                every { name } returns "primary"
                every { fullProductId } returns "com.example.product1"
            }

        val productItem2 =
            mockk<ProductItem> {
                every { name } returns "secondary"
                every { fullProductId } returns "com.example.product2"
            }

        val productsByFullId =
            mapOf(
                "com.example.product1" to storeProduct1,
                "com.example.product2" to storeProduct2,
            )

        val outcome =
            PaywallLogic.getVariablesAndFreeTrial(
                productItems = listOf(productItem1, productItem2),
                productsByFullId = productsByFullId,
                isFreeTrialAvailableOverride = null,
            )

        assertEquals(2, outcome.productVariables.size)
        assertTrue(outcome.isFreeTrialAvailable)
    }

    @Test
    fun test_getVariablesAndFreeTrial_productNotInMap() {
        val productItem =
            mockk<ProductItem> {
                every { name } returns "primary"
                every { fullProductId } returns "com.example.product1"
            }

        val productsByFullId = emptyMap<String, StoreProduct>()

        val outcome =
            PaywallLogic.getVariablesAndFreeTrial(
                productItems = listOf(productItem),
                productsByFullId = productsByFullId,
                isFreeTrialAvailableOverride = null,
            )

        assertEquals(0, outcome.productVariables.size)
        assertFalse(outcome.isFreeTrialAvailable)
    }

    @Test
    fun test_getVariablesAndFreeTrial_emptyProductItems() {
        val outcome =
            PaywallLogic.getVariablesAndFreeTrial(
                productItems = emptyList(),
                productsByFullId = emptyMap(),
                isFreeTrialAvailableOverride = null,
            )

        assertEquals(0, outcome.productVariables.size)
        assertFalse(outcome.isFreeTrialAvailable)
    }
}
