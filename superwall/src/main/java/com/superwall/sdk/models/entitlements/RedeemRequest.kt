package com.superwall.sdk.models.entitlements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RedeemRequest(
    @SerialName("deviceId")
    val deviceId: String,
    @SerialName("appUserId")
    val userId: String?,
    @SerialName("aliasId")
    val aliasId: String? = null,
    @SerialName("codes")
    val codes: List<Redeemable>,
    @SerialName("receipts")
    val receipts: List<TransactionReceipt>,
    @SerialName("externalAccountId")
    val externalAccountId: String,
    @SerialName("metadata")
    val metadata: Map<String, JsonElement>? = null,
)

@Serializable
data class Redeemable(
    @SerialName("code")
    val code: String,
    @SerialName("firstRedemption")
    val firstRedemption: Boolean? = false,
)

@Serializable
data class TransactionReceipt(
    @SerialName("jwsRepresentation")
    val purchaseToken: String,
    @SerialName("orderId")
    val orderId: String? = null,
    @SerialName("productId")
    val productId: String,
    @SerialName("productType")
    val productType: ProductType,
) {
    @Serializable
    enum class ProductType {
        @SerialName("iap")
        IAP,

        @SerialName("subscription")
        SUBSCRIPTION,
    }

    @SerialName("type")
    val type: String = "Android"
}
