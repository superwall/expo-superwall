package com.superwall.sdk.paywall.request

import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.TrackingResult
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.customer.CustomerInfo
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.IntroOfferEligibility
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.models.product.Store
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.store.abstractions.product.StoreProduct

data class ResponseIdentifiers(
    val paywallId: String?,
    var experiment: Experiment? = null,
) {
    companion object {
        val none: ResponseIdentifiers
            get() = ResponseIdentifiers(paywallId = null, experiment = null)
    }
}

data class ProductProcessingOutcome(
    var productVariables: List<ProductVariable>,
//    var swProductVariablesTemplate: List<ProductVariable>,
//    var orderedSwProducts: List<SWProduct>,
    var isFreeTrialAvailable: Boolean,
)

object PaywallLogic {
    fun requestHash(
        identifier: String? = null,
        event: EventData? = null,
        locale: String,
        joinedSubstituteProductIds: String?,
    ): String {
        val id = identifier ?: event?.name ?: "\$called_manually"

        var substitutions = ""
        joinedSubstituteProductIds?.let {
            substitutions = it
        }

        return "${id}_${locale}_$substitutions"
    }

    fun handlePaywallError(
        error: Throwable,
        event: EventData?,
        trackEvent: (suspend (event: Trackable) -> Result<TrackingResult>)? = null,
    ): Throwable {
        var _trackEvent: (suspend (event: Trackable) -> Result<TrackingResult>)? = trackEvent
        if (_trackEvent == null) {
            _trackEvent = { event: Trackable ->
                Superwall.instance.track(event)
            }
        }

//        if (error is CustomURLSession.NetworkError && error == .notFound) {
//        if (error is)
//            val trackedEvent = InternalSuperwallEvent.PaywallLoad(
//                state = .notFound,
//                eventData = event
//            )
//            GlobalScope.launch {
//                trackEvent(trackedEvent)
//            }
//        } else {
//            val trackedEvent = InternalSuperwallEvent.PaywallLoad(
//                state = .fail,
//            eventData = event
//            )
//            GlobalScope.launch {
//                trackEvent(trackedEvent)
//            }
//        }
//
//        val userInfo = mapOf(
//            NSLocalizedDescriptionKey to NSLocalizedString(
//                key = "Not Found",
//                value = "There isn't a paywall configured to show in this context",
//                comment = ""
//            )
//        )
//        return NSError(
//            domain = "SWPaywallNotFound",
//            code = 404,
//            userInfo = userInfo
//        )
        return java.lang.Exception("Not Found")
    }

    fun getVariablesAndFreeTrial(
        productItems: List<ProductItem>,
        productsByFullId: Map<String, StoreProduct>,
        isFreeTrialAvailableOverride: Boolean?,
        customerInfo: CustomerInfo,
        introOfferEligibility: IntroOfferEligibility = IntroOfferEligibility.AUTOMATIC,
    ): ProductProcessingOutcome {
        val productVariables = mutableListOf<ProductVariable>()

        for (productItem in productItems) {
            val storeProduct = productsByFullId[productItem.fullProductId]
            if (storeProduct != null) {
                productVariables.add(
                    ProductVariable(
                        name = productItem.name,
                        attributes = storeProduct.attributes,
                    ),
                )
            }
        }

        val hasFreeTrial =
            if (isFreeTrialAvailableOverride != null) {
                isFreeTrialAvailableOverride
            } else {
                computeHasFreeTrial(productItems, productsByFullId, customerInfo, introOfferEligibility)
            }

        return ProductProcessingOutcome(
            productVariables = productVariables,
            isFreeTrialAvailable = hasFreeTrial,
        )
    }

    private fun computeHasFreeTrial(
        productItems: List<ProductItem>,
        productsByFullId: Map<String, StoreProduct>,
        customerInfo: CustomerInfo,
        introOfferEligibility: IntroOfferEligibility,
    ): Boolean =
        productItems.any { productItem ->
            when (val type = productItem.type) {
                is ProductItem.StoreProductType.PlayStore,
                is ProductItem.StoreProductType.AppStore,
                is ProductItem.StoreProductType.Other,
                -> productsByFullId[productItem.fullProductId]?.hasFreeTrial == true

                is ProductItem.StoreProductType.Stripe ->
                    isWebTrialAvailable(
                        name = productItem.name,
                        trialDays = type.product.trialDays,
                        entitlements = productItem.entitlements,
                        customerInfo = customerInfo,
                        introOfferEligibility = introOfferEligibility,
                    )

                is ProductItem.StoreProductType.Paddle ->
                    isWebTrialAvailable(
                        name = productItem.name,
                        trialDays = type.product.trialDays,
                        entitlements = productItem.entitlements,
                        customerInfo = customerInfo,
                        introOfferEligibility = introOfferEligibility,
                    )
            }
        }

    private fun isWebTrialAvailable(
        name: String,
        trialDays: Int?,
        entitlements: Set<Entitlement>,
        customerInfo: CustomerInfo,
        introOfferEligibility: IntroOfferEligibility,
    ): Boolean {
        if ((trialDays ?: 0) <= 0) return false

        when (introOfferEligibility) {
            IntroOfferEligibility.INELIGIBLE -> return false
            IntroOfferEligibility.ELIGIBLE -> return true
            IntroOfferEligibility.AUTOMATIC -> Unit
        }

        if (entitlements.isEmpty()) {
            Logger.debug(
                logLevel = LogLevel.warn,
                scope = LogScope.paywallPresentation,
                message = "$name has trialDays > 0 but no entitlements — skipping trial eligibility check",
            )
            return false
        }

        return !hasEverHadEntitlement(entitlements, customerInfo)
    }

    private fun hasEverHadEntitlement(
        productEntitlements: Set<Entitlement>,
        customerInfo: CustomerInfo,
    ): Boolean {
        // Placeholder guard: if data hasn't loaded yet, assume the trial was consumed
        // to avoid falsely offering a trial.
        if (customerInfo.isPlaceholder) return true

        val relevantCustomerEntitlementIds =
            customerInfo.entitlements
                .asSequence()
                .filter { it.latestProductId != null || it.store == Store.SUPERWALL || it.isActive }
                .map { it.id }
                .toSet()

        return productEntitlements.any { it.id in relevantCustomerEntitlementIds }
    }
}
