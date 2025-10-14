package com.superwall.sdk.network

import com.superwall.sdk.dependencies.ApiFactory
import com.superwall.sdk.models.entitlements.RedeemRequest
import com.superwall.sdk.models.entitlements.Redeemable
import com.superwall.sdk.models.entitlements.TransactionReceipt
import com.superwall.sdk.models.entitlements.WebEntitlements
import com.superwall.sdk.models.internal.DeviceVendorId
import com.superwall.sdk.models.internal.UserId
import com.superwall.sdk.models.internal.WebRedemptionResponse
import com.superwall.sdk.network.session.CustomHttpUrlConnection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class SubscriptionService(
    override val host: String,
    override val version: String,
    val factory: ApiFactory,
    json: Json,
    override val customHttpUrlConnection: CustomHttpUrlConnection,
) : NetworkService() {
    override suspend fun makeHeaders(
        isForDebugging: Boolean,
        requestId: String,
    ): Map<String, String> = factory.makeHeaders(isForDebugging, requestId)

    private val json =
        Json(json) {
            namingStrategy = null
            explicitNulls = false
            ignoreUnknownKeys = true
        }

    suspend fun redeemToken(
        codes: List<Redeemable>,
        userId: UserId?,
        aliasId: String?,
        vendorId: DeviceVendorId,
        transactionReceipt: List<TransactionReceipt>,
        externalAccountId: String,
        attributionProps: Map<String, JsonElement>?,
    ) = post<WebRedemptionResponse>(
        "redeem",
        retryCount = 0,
        body =
            json
                .encodeToString(
                    RedeemRequest(
                        vendorId.value,
                        userId?.value,
                        aliasId,
                        codes,
                        transactionReceipt,
                        externalAccountId,
                        attributionProps,
                    ),
                ).toByteArray(),
    )

    suspend fun webEntitlementsByUserId(
        userId: UserId?,
        deviceId: DeviceVendorId,
    ) = get<WebEntitlements>(
        "users/${userId?.value ?: deviceId.value}/entitlements",
        queryItems = listOf(URLQueryItem("deviceId", deviceId.value)),
    )

    suspend fun webEntitlementsByDeviceId(deviceId: DeviceVendorId) =
        get<WebEntitlements>(
            "users/${deviceId.value}/entitlements",
            queryItems = listOf(URLQueryItem("deviceId", deviceId.value)),
        )
}
