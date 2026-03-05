package com.superwall.sdk.billing

import com.superwall.sdk.store.abstractions.product.BasePlanType
import com.superwall.sdk.store.abstractions.product.OfferType

/**
 * Represents a decomposed product ID in the format: `productId:basePlan:offer`
 *
 * [basePlanType] uses [BasePlanType]:
 * - [BasePlanType.Auto] when the value is "sw-auto", null, or empty
 * - [BasePlanType.Specific] when a specific ID is provided
 *
 * [offerType] uses [OfferType]:
 * - [OfferType.Auto] when the value is "sw-auto", null, or empty
 * - [OfferType.None] when the value is "sw-none" (no offer)
 * - [OfferType.Specific] when a specific ID is provided
 */
data class DecomposedProductIds(
    val subscriptionId: String,
    val basePlanType: BasePlanType,
    val offerType: OfferType,
    val fullId: String,
) {
    /** Returns the base plan ID if specific, null if auto */
    val basePlanId: String? get() = basePlanType.specificId

    companion object {
        fun from(productId: String): DecomposedProductIds {
            val components = productId.split(":")
            val subscriptionId = components.getOrNull(0) ?: ""
            val rawBasePlan = components.getOrNull(1)
            val rawOffer = components.getOrNull(2)

            return DecomposedProductIds(
                subscriptionId = subscriptionId,
                basePlanType = BasePlanType.from(rawBasePlan),
                offerType = OfferType.from(rawOffer),
                fullId = productId,
            )
        }
    }
}
