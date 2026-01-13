package com.superwall.sdk.models.entitlements

import com.superwall.sdk.models.customer.CustomerInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebEntitlements(
    @SerialName("customerInfo")
    val customerInfo: CustomerInfo? = null,
)
