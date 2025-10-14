package com.superwall.sdk.store

import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.paywall.PaywallProducts
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.store.abstractions.product.StoreProduct

data class GetProductsResponse(
    val productsByFullId: Map<String, StoreProduct>,
    val productItems: List<ProductItem>,
    val paywall: Paywall? = null,
)

interface StoreKitManagerInterface {
    val productsById: Map<String, StoreProduct>

    suspend fun getProductVariables(paywall: Paywall): List<ProductVariable>

    suspend fun getProducts(
        responseProductIds: List<String>,
        paywallName: String? = null,
        responseProducts: List<ProductItem> = listOf(),
        substituteProducts: PaywallProducts? = null,
    ): GetProductsResponse

    suspend fun tryToRestore(paywallView: PaywallView)

    suspend fun processRestoration(
        restorationResult: RestorationResult,
        paywallView: PaywallView,
    )

    suspend fun refreshReceipt()

    suspend fun loadPurchasedProducts()

    suspend fun isFreeTrialAvailable(product: StoreProduct): Boolean

    suspend fun products(
        identifiers: Set<String>,
        paywallName: String? = null,
    ): Set<StoreProduct>
}
