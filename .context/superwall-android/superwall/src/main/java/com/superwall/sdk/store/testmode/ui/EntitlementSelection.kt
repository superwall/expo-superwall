package com.superwall.sdk.store.testmode.ui

import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.product.Store
import com.superwall.sdk.store.abstractions.product.receipt.LatestPeriodType
import com.superwall.sdk.store.abstractions.product.receipt.LatestSubscriptionState
import kotlinx.serialization.Serializable

@Serializable
enum class EntitlementStateOption(
    val displayName: String,
) {
    Inactive("Inactive"),
    Subscribed("Subscribed"),
    InGracePeriod("In Grace Period"),
    BillingRetry("Billing Retry"),
    Expired("Expired"),
    Revoked("Revoked"),
    ;

    val isActive: Boolean
        get() = this == Subscribed || this == InGracePeriod || this == BillingRetry

    fun toSubscriptionState(): LatestSubscriptionState? =
        when (this) {
            Inactive -> null
            Subscribed -> LatestSubscriptionState.SUBSCRIBED
            InGracePeriod -> LatestSubscriptionState.GRACE_PERIOD
            BillingRetry -> LatestSubscriptionState.BILLING_RETRY
            Expired -> LatestSubscriptionState.EXPIRED
            Revoked -> LatestSubscriptionState.REVOKED
        }
}

@Serializable
enum class OfferTypeOption(
    val displayName: String,
) {
    None("None"),
    Trial("Trial"),
    Promotional("Promotional"),
    ;

    fun toPeriodType(): LatestPeriodType? =
        when (this) {
            None -> null
            Trial -> LatestPeriodType.TRIAL
            Promotional -> LatestPeriodType.PROMOTIONAL
        }
}

@Serializable
data class EntitlementSelection(
    val identifier: String,
    val state: EntitlementStateOption = EntitlementStateOption.Inactive,
    val offerType: OfferTypeOption = OfferTypeOption.None,
) {
    fun toEntitlement(): Entitlement =
        Entitlement(
            id = identifier,
            type = Entitlement.Type.SERVICE_LEVEL,
            isActive = state.isActive,
            state = state.toSubscriptionState(),
            offerType = offerType.toPeriodType(),
            store = Store.PLAY_STORE,
        )
}
