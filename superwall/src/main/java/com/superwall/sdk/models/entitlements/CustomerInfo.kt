package com.superwall.sdk.models.entitlements

import com.superwall.sdk.models.internal.RedemptionResult

// Info about the customer such as active entitlements and redeemed codes.
data class CustomerInfo(
    // TODO remove netitlements
    // The active entitlements for the customer
    val entitlements: Set<Entitlement>,
    // An array of `RedemptionResult` objects, representing all the results of redeemed codes
    val redemptions: List<RedemptionResult>,
    // TODO add latest redemption info
    // Add customer info on demand - superwall.instance.customerInfo
)
