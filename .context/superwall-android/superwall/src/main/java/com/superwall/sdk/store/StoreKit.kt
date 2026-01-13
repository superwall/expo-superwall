package com.superwall.sdk.store

import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.paywall.request.PaywallRequest
import com.superwall.sdk.store.abstractions.product.StoreProduct

interface StoreKit {
    suspend fun getProductVariables(
        paywall: Paywall,
        request: PaywallRequest,
    ): List<ProductVariable>

    suspend fun getProducts(
        substituteProducts: Map<String, StoreProduct>? = null,
        paywall: Paywall,
        request: PaywallRequest? = null,
    ): GetProductsResponse

    suspend fun getProductsWithoutPaywall(
        productIds: List<String>,
        substituteProducts: Map<String, StoreProduct>? = null,
    ): Map<String, StoreProduct>

    fun refreshReceipt()

    suspend fun loadPurchasedProducts(serverEntitlementsByProductId: Map<String, Set<Entitlement>>)

    fun cacheProduct(
        fullProductIdentifier: String,
        storeProduct: StoreProduct,
    )

    fun getProductFromCache(productId: String): StoreProduct?

    fun hasCached(productId: String): Boolean

    suspend fun consume(purchaseToken: String): Result<String>
}
