package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.models.config.FeatureGatingBehavior
import com.superwall.sdk.models.paywall.PaywallPresentationInfo
import com.superwall.sdk.models.paywall.PaywallPresentationStyle
import com.superwall.sdk.models.paywall.PaywallURL
import com.superwall.sdk.models.product.Offer
import com.superwall.sdk.models.product.PlayStoreProduct
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.models.product.ProductItem.StoreProductType
import com.superwall.sdk.models.product.Store
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.store.abstractions.product.StoreProduct
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaywallInfoTest {
    @Test
    fun audienceFilterParams_populatesProductIdentifiersAndFlags() {
        val products =
            listOf(
                createProductItem("primary", "monthly"),
                createProductItem("secondary", "annual"),
                createProductItem("tertiary", "lifetime"),
            )
        val info =
            PaywallInfo.empty().copy(
                databaseId = "db_123",
                name = "Superwall",
                productIds = listOf("primary-plan", "secondary-plan", "tertiary-plan"),
                products = products,
                presentedByEventWithName = "launch",
                presentedBy = "event",
                isFreeTrialAvailable = true,
                featureGatingBehavior = FeatureGatingBehavior.Gated,
            )

        val params = info.audienceFilterParams()

        assertEquals("db_123", params["paywall_id"])
        assertEquals("Superwall", params["paywall_name"])
        assertEquals("launch", params["presented_by_event_name"])
        assertEquals("primary-plan,secondary-plan,tertiary-plan", params["paywall_product_ids"])
        assertEquals(true, params["is_free_trial_available"])
        assertTrue((params["feature_gating"] as String).contains("GATED"))
        assertEquals(products[0].fullProductId, params["primary_product_id"])
        assertEquals(products[1].fullProductId, params["secondary_product_id"])
        assertEquals(products[2].fullProductId, params["tertiary_product_id"])
        assertEquals(products[1].fullProductId, params["${products[1].name}_product_id"])
    }

    @Test
    fun eventParams_mergesAudienceParamsProductAttributesAndExtras() {
        val experiment =
            Experiment(
                id = "exp123",
                groupId = "group",
                variant = Experiment.Variant(id = "variant", type = Experiment.Variant.VariantType.TREATMENT, paywallId = "paywall"),
            )
        val info =
            PaywallInfo.empty().copy(
                identifier = "identifier",
                name = "Superwall",
                url = PaywallURL("https://example.com"),
                experiment = experiment,
                paywalljsVersion = "4.5.6",
                products = listOf(createProductItem("primary", "monthly")),
                productIds = listOf("primary-plan"),
                presentedByEventWithId = "evt-001",
                presentedByEventAt = "2024-05-01T12:00:00Z",
                presentationSourceType = "register",
                responseLoadStartTime = "2024-05-01T11:59:59Z",
                responseLoadCompleteTime = "2024-05-01T12:00:01Z",
                webViewLoadStartTime = "2024-05-01T12:00:02Z",
                webViewLoadCompleteTime = "2024-05-01T12:00:05Z",
                productsLoadStartTime = "2024-05-01T12:00:06Z",
                productsLoadCompleteTime = "2024-05-01T12:00:07Z",
                productsLoadFailTime = "",
                productsLoadDuration = 1.0,
                webViewLoadDuration = 3.0,
                responseLoadDuration = 2.0,
                presentation = PaywallPresentationInfo(PaywallPresentationStyle.None, 0),
                featureGatingBehavior = FeatureGatingBehavior.NonGated,
                presentedBy = "event",
                isScrollEnabled = false,
                closeReason = PaywallCloseReason.None,
            )

        val storeProduct =
            mockk<StoreProduct> {
                every { fullIdentifier } returns "store.product"
                every {
                    attributes
                } returns
                    mapOf(
                        "trialPeriodDays" to "7",
                        "localizedPrice" to "9.99",
                    )
            }

        val extraParams = mapOf("custom_key" to "customValue", "null_key" to null)

        val params = info.eventParams(storeProduct, extraParams)

        assertEquals("identifier", params["paywall_identifier"])
        assertEquals("https://example.com", params["paywall_url"])
        assertEquals("evt-001", params["presented_by_event_id"])
        assertEquals("2024-05-01T12:00:00Z", params["presented_by_event_timestamp"])
        assertEquals("register", params["presentation_source_type"])
        assertEquals("4.5.6", params["paywalljs_version"])
        assertEquals("store.product", params["product_id"])
        assertEquals("7", params["product_trial_period_days"])
        assertEquals("9.99", params["product_localized_price"])
        assertEquals("customValue", params["custom_key"])
        assertFalse(params.containsKey("null_key"))
        assertEquals("exp123", params["experiment_id"])
        assertEquals("variant", params["variant_id"])
        assertNotNull(params["paywall_response_load_start_time"])
    }

    private fun createProductItem(
        name: String,
        productIdentifier: String,
    ): ProductItem {
        val product =
            PlayStoreProduct(
                store = Store.PLAY_STORE,
                productIdentifier = productIdentifier,
                basePlanIdentifier = "base",
                offer = Offer.Automatic(),
            )
        return ProductItem(
            name = name,
            compositeId = "$productIdentifier:base:sw-auto",
            type = StoreProductType.PlayStore(product),
            entitlements = emptySet(),
        )
    }
}
