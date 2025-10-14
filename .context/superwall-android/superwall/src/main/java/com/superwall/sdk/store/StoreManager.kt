package com.superwall.sdk.store

import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.billing.Billing
import com.superwall.sdk.billing.BillingError
import com.superwall.sdk.billing.DecomposedProductIds
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.Offer
import com.superwall.sdk.models.product.PlayStoreProduct
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.paywall.request.PaywallRequest
import com.superwall.sdk.store.abstractions.product.OfferType
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.abstractions.product.receipt.ReceiptManager
import com.superwall.sdk.store.coordinator.ProductsFetcher
import java.util.Date

class StoreManager(
    val purchaseController: InternalPurchaseController,
    val billing: Billing,
    private val track: suspend (InternalSuperwallEvent) -> Unit = {
        Superwall.instance.track(it)
    },
) : ProductsFetcher,
    StoreKit {
    val receiptManager by lazy { ReceiptManager(delegate = this, billing) }

    var productsByFullId: MutableMap<String, StoreProduct> = mutableMapOf()

    private data class ProductProcessingResult(
        val fullProductIdsToLoad: Set<String>,
        val substituteProductsById: Map<String, StoreProduct>,
        val productItems: List<ProductItem>,
    )

    override suspend fun getProductVariables(
        paywall: Paywall,
        request: PaywallRequest,
    ): List<ProductVariable> {
        val output =
            getProducts(
                paywall = paywall,
                request = request,
            )

        val productAttributes =
            paywall.productItems.mapNotNull { productItem ->
                output.productsByFullId[productItem.fullProductId]?.let { storeProduct ->
                    ProductVariable(
                        name = productItem.name,
                        attributes = storeProduct.attributes,
                    )
                }
            }

        return productAttributes
    }

    override suspend fun getProductsWithoutPaywall(
        productIds: List<String>,
        substituteProducts: Map<String, StoreProduct>?,
    ): Map<String, StoreProduct> {
        val processingResult =
            removeAndStore(
                substituteProductsByName = substituteProducts,
                fullProductIds = productIds,
                productItems = emptyList(),
            )

        val products: Set<StoreProduct>
        try {
            products = billing.awaitGetProducts(processingResult.fullProductIdsToLoad)
        } catch (error: Throwable) {
            throw error
        }

        val productsById = processingResult.substituteProductsById.toMutableMap()

        for (product in products) {
            val fullProductIdentifier = product.fullIdentifier
            productsById[fullProductIdentifier] = product
            this.productsByFullId[fullProductIdentifier] = product
        }

        return products.map { it.fullIdentifier to it }.toMap()
    }

    override suspend fun getProducts(
        substituteProducts: Map<String, StoreProduct>?,
        paywall: Paywall,
        request: PaywallRequest?,
    ): GetProductsResponse {
        val processingResult =
            removeAndStore(
                substituteProductsByName = substituteProducts,
                fullProductIds = paywall.productIds,
                productItems = paywall.productItems,
            )

        var products: Set<StoreProduct> = setOf()
        try {
            products = billing.awaitGetProducts(processingResult.fullProductIdsToLoad)
        } catch (error: Throwable) {
            paywall.productsLoadingInfo.failAt = Date()
            val paywallInfo = paywall.getInfo(request?.eventData)
            val productLoadEvent =
                InternalSuperwallEvent.PaywallProductsLoad(
                    state = InternalSuperwallEvent.PaywallProductsLoad.State.Fail(error.message),
                    paywallInfo = paywallInfo,
                    eventData = request?.eventData,
                )
            track(productLoadEvent)

            // If billing isn't available, make it call the onError handler when requesting
            // a paywall.
            if (error is BillingError.BillingNotAvailable) {
                throw error
            }
        }

        val productsById = processingResult.substituteProductsById.toMutableMap()

        for (product in products) {
            val fullProductIdentifier = product.fullIdentifier
            productsById[fullProductIdentifier] = product
            this.productsByFullId[fullProductIdentifier] = product
        }

        return GetProductsResponse(
            productsByFullId = productsById,
            productItems = processingResult.productItems,
            paywall = paywall,
        )
    }

    private fun removeAndStore(
        substituteProductsByName: Map<String, StoreProduct>?,
        fullProductIds: List<String>,
        productItems: List<ProductItem>,
    ): ProductProcessingResult {
        val fullProductIdsToLoad = fullProductIds.toMutableList()
        val substituteProductsByFullId: MutableMap<String, StoreProduct> = mutableMapOf()
        val productItems: MutableList<ProductItem> = productItems.toMutableList()

        substituteProductsByName?.let { substituteProducts ->
            // Otherwise, iterate over each substitute product
            for ((name, product) in substituteProducts) {
                val fullProductId = product.fullIdentifier

                // Map substitute product by its ID.
                substituteProductsByFullId[fullProductId] = product

                // Store the substitute product by id in the class' dictionary
                this.productsByFullId[fullProductId] = product
                val decomposedProductIds = DecomposedProductIds.from(product.fullIdentifier)

                // Search for an existing product with specified name
                productItems.indexOfFirst { it.name == name }.takeIf { it >= 0 }?.let { index ->
                    // Update the product ID at the found index
                    productItems[index] =
                        ProductItem(
                            name = productItems[index].name,
                            entitlements = productItems[index].entitlements,
                            type =
                                ProductItem.StoreProductType.PlayStore(
                                    PlayStoreProduct(
                                        productIdentifier = decomposedProductIds.subscriptionId,
                                        basePlanIdentifier = decomposedProductIds.basePlanId ?: "",
                                        offer =
                                            decomposedProductIds.offerType.let { offerType ->
                                                when (offerType) {
                                                    is OfferType.Offer -> Offer.Specified(offerIdentifier = offerType.id)
                                                    is OfferType.Auto -> Offer.Automatic()
                                                }
                                            },
                                    ),
                                ),
                        )
                } ?: run {
                    // If no existing product found, just append to the list.
                    productItems.add(
                        ProductItem(
                            name = name,
                            entitlements = emptySet(),
                            type =
                                ProductItem.StoreProductType.PlayStore(
                                    PlayStoreProduct(
                                        productIdentifier = decomposedProductIds.subscriptionId,
                                        basePlanIdentifier = decomposedProductIds.basePlanId ?: "",
                                        offer =
                                            decomposedProductIds.offerType.let { offerType ->
                                                when (offerType) {
                                                    is OfferType.Offer -> Offer.Specified(offerIdentifier = offerType.id)
                                                    is OfferType.Auto -> Offer.Automatic()
                                                }
                                            },
                                    ),
                                ),
                        ),
                    )
                }

                // Make sure we don't load the substitute product id
                fullProductIdsToLoad.removeAll { it == fullProductId }
            }
        }

        return ProductProcessingResult(
            fullProductIdsToLoad = fullProductIdsToLoad.toSet(),
            substituteProductsById = substituteProductsByFullId,
            productItems = productItems,
        )
    }

    override fun refreshReceipt() {
        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.storeKitManager, // Rename this scope to reflect Billing Manager
            message = "Refreshing Google Play receipt.",
        )
        receiptManager.refreshReceipt()
    }

    override suspend fun loadPurchasedProducts() {
        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.storeKitManager, // Rename this scope to reflect Billing Manager
            message = "Loading purchased products from the Google Play receipt.",
        )
        receiptManager.loadPurchasedProducts()
    }

    @Throws(Throwable::class)
    override suspend fun products(identifiers: Set<String>): Set<StoreProduct> = billing.awaitGetProducts(identifiers)
}
