package com.superwall.sdk.store.testmode.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuperwallProductsResponse(
    val data: List<SuperwallProduct>,
)

@Serializable
data class SuperwallProduct(
    @SerialName("object")
    val objectType: String? = null,
    val identifier: String,
    val platform: SuperwallProductPlatform,
    val price: SuperwallProductPrice? = null,
    val subscription: SuperwallProductSubscription? = null,
    val entitlements: List<SuperwallEntitlementRef> = emptyList(),
    val storefront: String? = null,
)

@Serializable
data class SuperwallProductPrice(
    val amount: Int,
    val currency: String,
)

@Serializable
data class SuperwallProductSubscription(
    val period: SuperwallSubscriptionPeriod,
    @SerialName("period_count")
    val periodCount: Int = 1,
    @SerialName("trial_period_days")
    val trialPeriodDays: Int? = null,
)

@Serializable
enum class SuperwallSubscriptionPeriod {
    @SerialName("day")
    DAY,

    @SerialName("week")
    WEEK,

    @SerialName("month")
    MONTH,

    @SerialName("year")
    YEAR,
}

@Serializable
data class SuperwallEntitlementRef(
    val identifier: String,
    val type: String? = null,
)

@Serializable
enum class SuperwallProductPlatform {
    @SerialName("ios")
    IOS,

    @SerialName("android")
    ANDROID,

    @SerialName("stripe")
    STRIPE,

    @SerialName("paddle")
    PADDLE,

    @SerialName("superwall")
    SUPERWALL,
}
