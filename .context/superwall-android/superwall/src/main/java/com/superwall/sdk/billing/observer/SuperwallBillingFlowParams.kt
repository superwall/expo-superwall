package com.superwall.sdk.billing.observer

import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails

class SuperwallBillingFlowParams private constructor(
    internal val params: BillingFlowParams,
    internal val productDetailsParams: MutableList<ProductDetailsParams> = mutableListOf(),
) {
    companion object {
        fun newBuilder(): Builder = Builder()
    }

    fun toOriginal() = params

    class Builder {
        private val builder = BillingFlowParams.newBuilder()
        internal val productDetailsParams: MutableList<ProductDetailsParams> = mutableListOf()

        fun setIsOfferPersonalized(isOfferPersonalized: Boolean): Builder =
            apply {
                builder.setIsOfferPersonalized(isOfferPersonalized)
            }

        fun setObfuscatedAccountId(obfuscatedAccountId: String): Builder =
            apply {
                builder.setObfuscatedAccountId(obfuscatedAccountId)
            }

        fun setObfuscatedProfileId(obfuscatedProfileId: String): Builder =
            apply {
                builder.setObfuscatedProfileId(obfuscatedProfileId)
            }

        fun setProductDetailsParamsList(productDetailsParamsList: List<ProductDetailsParams>): Builder =
            apply {
                productDetailsParams.addAll(productDetailsParamsList)
                builder.setProductDetailsParamsList(productDetailsParamsList.map { it.toOriginal() })
            }

        @Deprecated("Use setProductDetailsParamsList instead")
        fun setSkuDetails(skuDetails: SkuDetails): Builder =
            apply {
                builder.setSkuDetails(skuDetails)
            }

        fun setSubscriptionUpdateParams(params: SubscriptionUpdateParams): Builder =
            apply {
                builder.setSubscriptionUpdateParams(params.toOriginal())
            }

        fun build(): SuperwallBillingFlowParams = SuperwallBillingFlowParams(builder.build(), productDetailsParams)
    }

    class ProductDetailsParams private constructor(
        private val params: BillingFlowParams.ProductDetailsParams,
        internal val details: ProductDetails,
    ) {
        fun toOriginal() = params

        companion object {
            fun newBuilder(): ProductDetailsParamsBuilder = ProductDetailsParamsBuilder()
        }

        class ProductDetailsParamsBuilder {
            private val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
            internal var details: ProductDetails? = null

            fun setOfferToken(offerToken: String): ProductDetailsParamsBuilder =
                apply {
                    builder.setOfferToken(offerToken)
                }

            fun setProductDetails(productDetails: ProductDetails): ProductDetailsParamsBuilder =
                apply {
                    details = productDetails
                    builder.setProductDetails(productDetails)
                }

            fun build(): ProductDetailsParams {
                val details =
                    details ?: throw IllegalArgumentException("ProductDetails are required")

                return ProductDetailsParams(
                    builder.build(),
                    details,
                )
            }
        }
    }

    class SubscriptionUpdateParams private constructor(
        private val params: BillingFlowParams.SubscriptionUpdateParams,
    ) {
        fun toOriginal() = params

        companion object {
            fun newBuilder(): SubscriptionUpdateParamsBuilder = SubscriptionUpdateParamsBuilder()
        }

        class SubscriptionUpdateParamsBuilder {
            private val builder = BillingFlowParams.SubscriptionUpdateParams.newBuilder()

            fun setOldPurchaseToken(purchaseToken: String): SubscriptionUpdateParamsBuilder =
                apply {
                    builder.setOldPurchaseToken(purchaseToken)
                }

            fun setOriginalExternalTransactionId(externalTransactionId: String): SubscriptionUpdateParamsBuilder =
                apply {
                    builder.setOriginalExternalTransactionId(externalTransactionId)
                }

            fun setSubscriptionReplacementMode(subscriptionReplacementMode: Int): SubscriptionUpdateParamsBuilder =
                apply {
                    builder.setSubscriptionReplacementMode(subscriptionReplacementMode)
                }

            fun build(): SubscriptionUpdateParams = SubscriptionUpdateParams(builder.build())
        }

        @Retention(AnnotationRetention.SOURCE)
        annotation class ReplacementMode {
            companion object {
                const val UNKNOWN_REPLACEMENT_MODE = 0
                const val WITH_TIME_PRORATION = 1
                const val CHARGE_PRORATED_PRICE = 2
                const val WITHOUT_PRORATION = 3
                const val CHARGE_FULL_PRICE = 5
                const val DEFERRED = 6
            }
        }
    }

    @Deprecated("Use SubscriptionUpdateParams.ReplacementMode instead")
    @Retention(AnnotationRetention.SOURCE)
    annotation class ProrationMode {
        companion object {
            const val UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY = 0
            const val IMMEDIATE_WITH_TIME_PRORATION = 1
            const val IMMEDIATE_AND_CHARGE_PRORATED_PRICE = 2
            const val IMMEDIATE_WITHOUT_PRORATION = 3
            const val DEFERRED = 4
            const val IMMEDIATE_AND_CHARGE_FULL_PRICE = 5
        }
    }
}
