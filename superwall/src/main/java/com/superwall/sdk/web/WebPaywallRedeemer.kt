package com.superwall.sdk.web

import android.content.Context
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent.Redemptions
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent.Redemptions.RedemptionState
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.asEither
import com.superwall.sdk.misc.fold
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.Redeemable
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.entitlements.TransactionReceipt
import com.superwall.sdk.models.internal.DeviceVendorId
import com.superwall.sdk.models.internal.ErrorInfo
import com.superwall.sdk.models.internal.RedemptionOwnership
import com.superwall.sdk.models.internal.RedemptionOwnershipType
import com.superwall.sdk.models.internal.RedemptionResult
import com.superwall.sdk.models.internal.UserId
import com.superwall.sdk.network.Network
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.storage.LatestRedemptionResponse
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class WebPaywallRedeemer(
    val context: Context,
    val ioScope: IOScope,
    val deepLinkReferrer: CheckForReferral,
    val network: Network,
    val storage: Storage,
    val willRedeemLink: () -> Unit,
    val didRedeemLink: (
        redemptionResult: RedemptionResult,
    ) -> Unit,
    val maxAge: () -> Long,
    val getActiveDeviceEntitlements: () -> Set<Entitlement> = {
        Superwall.instance.entitlements.activeDeviceEntitlements
    },
    val getUserId: () -> UserId? = {
        Superwall.instance.dependencyContainer.identityManager.appUserId?.let {
            UserId(
                it,
            )
        }
    },
    val getDeviceId: () -> DeviceVendorId = { DeviceVendorId(Superwall.instance.vendorId) },
    val getAliasId: () -> String? = { Superwall.instance.dependencyContainer.identityManager.aliasId },
    val track: suspend (Trackable) -> Unit = {
        Superwall.instance.track(it)
    },
    val internallySetSubscriptionStatus: (SubscriptionStatus) -> Unit,
    val isPaywallVisible: suspend () -> Boolean = { Superwall.instance.isPaywallPresented },
    val triggerRestoreInPaywall: suspend () -> Unit = { },
    val currentPaywallEntitlements: () -> Set<Entitlement>,
    val getPaywallInfo: () -> PaywallInfo,
    val trackRestorationFailed: (message: String) -> Unit,
    val isWebToAppEnabled: () -> Boolean,
    val receipts: suspend () -> List<TransactionReceipt>,
    val getExternalAccountId: () -> String,
    val getIntegrationProps: () -> Map<String, Any> = {
        Superwall.instance.dependencyContainer.attributionManager
            .getFullAttributionIds()
    },
) {
    private var pollingJob: Job? = null

    private fun convertToJsonElement(value: Any?): JsonElement? =
        when (value) {
            null -> null
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> ->
                buildJsonObject {
                    value.forEach { (k, v) ->
                        if (k is String && v != null) {
                            val jsonElement = convertToJsonElement(v)
                            if (jsonElement != null) {
                                put(k, jsonElement)
                            }
                        }
                    }
                }

            is Collection<*> ->
                JsonArray(
                    value.mapNotNull { convertToJsonElement(it) },
                )

            else -> JsonPrimitive(value.toString())
        }

    init {
        ioScope.launch {
            if (isWebToAppEnabled()) {
                checkForRefferal()
            }
        }
    }

    suspend fun checkForRefferal() =
        asEither {
            deepLinkReferrer
                .checkForReferral()
                .fold(
                    onSuccess = {
                        redeem(RedeemType.Code(it))
                    },
                    onFailure = { throw it },
                )
        }

    sealed interface RedeemType {
        val description: String
        val code: String?

        data class Code(
            override val code: String,
        ) : RedeemType {
            override val description: String = "CODE"
        }

        object Existing : RedeemType {
            override val description: String = "EXISTING_CODES"
            override val code: String? = null
        }
    }

    suspend fun redeem(redemption: RedeemType) {
        if (!isWebToAppEnabled()) {
            return
        } else {
            // We want to keep track of the codes that have been retrieved by the user
            val latestResponse = storage.read(LatestRedemptionResponse)
            val allCodes = latestResponse?.allCodes?.toMutableList() ?: mutableListOf()
            var isFirstRedemption = true
            if (redemption is RedeemType.Code) {
                willRedeemLink()

                if (allCodes.isNotEmpty()) {
                    isFirstRedemption = !allCodes.map { it.code }.contains(redemption.code)
                }
                allCodes.add(Redeemable(redemption.code, isFirstRedemption))

                if (isPaywallVisible()) {
                    track(
                        InternalSuperwallEvent.Restore(
                            InternalSuperwallEvent.Restore.State.Start,
                            getPaywallInfo(),
                        ),
                    )
                }
            }
            track(Redemptions(RedemptionState.Start, redemption))
            network
                .redeemToken(
                    allCodes,
                    getUserId(),
                    getAliasId(),
                    getDeviceId(),
                    receipts(),
                    getExternalAccountId(),
                    getIntegrationProps().takeIf { it.isNotEmpty() }?.let { props ->
                        props
                            .mapValues { (_, value) -> convertToJsonElement(value) }
                            .filterValues { it != null }
                            .mapValues { (_, value) -> value!! }
                    },
                ).fold(
                    onSuccess = {
                        storage.write(LatestRedemptionResponse, it)
                        track(
                            Redemptions(
                                RedemptionState.Complete,
                                redemption,
                            ),
                        )
                        when (redemption) {
                            is RedeemType.Code -> {
                                Logger.debug(
                                    logLevel = LogLevel.debug,
                                    scope = LogScope.webEntitlements,
                                    message = "Entitlement redemption done",
                                    info =
                                        mapOf(
                                            "code" to (
                                                (redemption as? RedeemType.Code)?.code
                                                    ?: ""
                                            ),
                                        ),
                                )

                                val result =
                                    if (it.codes.any { it.code == redemption.code }) {
                                        it.codes
                                    } else {
                                        listOf(
                                            RedemptionResult.Error(
                                                code =
                                                    (redemption as? RedeemType.Code?)?.code
                                                        ?: "",
                                                error = ErrorInfo("Redemption failed, code not returned"),
                                            ),
                                        )
                                    }
                                val redemptionResultForCode =
                                    result.firstOrNull { it.code == redemption.code }
                                if (redemptionResultForCode != null) {
                                    if (isPaywallVisible()) {
                                        if (it.entitlements.containsAll(
                                                currentPaywallEntitlements(),
                                            )
                                        ) {
                                            triggerRestoreInPaywall()
                                        } else {
                                            trackRestorationFailed("Failed to restore subscriptions from the web")
                                        }
                                    }
                                }
                            }

                            RedeemType.Existing -> {
                                // NO-OP
                            }
                        }

                        internallySetSubscriptionStatus(SubscriptionStatus.Active(it.entitlements.toSet() + getActiveDeviceEntitlements()))
                        if (redemption is RedeemType.Code) {
                            val res = it.codes.first { it.code == redemption.code }
                            didRedeemLink(
                                res,
                            )
                        }

                        // Notify the delegate that the redemption succeeded, unless the code has not been redeemed
                    },
                    onFailure = {
                        track(
                            Redemptions(
                                RedemptionState.Fail,
                                redemption,
                            ),
                        )
                        if (redemption is RedeemType.Code) {
                            val errorMessage =
                                it.localizedMessage ?: it.message
                                    ?: "Redemption failed, error unknown"
                            Logger.debug(
                                logLevel = LogLevel.error,
                                scope = LogScope.webEntitlements,
                                message = "Failed to redeem purchase token",
                                info = mapOf("code" to redemption.code),
                                error = it,
                            )
                            // Notify the delegate that the redemption failed
                            didRedeemLink(
                                RedemptionResult.Error(
                                    code = redemption.code ?: "",
                                    error =
                                        ErrorInfo(
                                            errorMessage,
                                        ),
                                ),
                            )
                            if (isPaywallVisible()) {
                                trackRestorationFailed(errorMessage)
                            }
                        }
                    },
                )
            startPolling()
        }
    }

    suspend fun checkForWebEntitlements(
        userId: UserId?,
        deviceId: DeviceVendorId,
    ) = withErrorTracking {
        val webEntitlementsResult =
            if (userId == null) {
                network.webEntitlementsByDeviceID(deviceId)
            } else {
                network.webEntitlementsByUserId(userId, deviceId)
            }
        val entitlements =
            (webEntitlementsResult.getSuccess()?.entitlements ?: listOf())
        entitlements
            .toSet()
    }

    fun clear(ownership: RedemptionOwnershipType) {
        val latestResponse = storage.read(LatestRedemptionResponse)
        // User has not redeemed anything yet
        // Find success codes that belong to the user
        val userCodesToRemove =
            latestResponse
                ?.codes
                ?.filterIsInstance<RedemptionResult.Success>()
                ?.filter {
                    when (ownership) {
                        RedemptionOwnershipType.AppUser -> it.redemptionInfo.ownership is RedemptionOwnership.AppUser
                        RedemptionOwnershipType.Device -> it.redemptionInfo.ownership is RedemptionOwnership.Device
                    }
                }

        // Create a new response with the filtered codes and entitlements
        // (we do not need to check source here as they are all web)

        val withUserCodesRemoved =
            latestResponse?.copy(
                codes = latestResponse.codes.filterNot { it in userCodesToRemove.orEmpty() },
                entitlements = emptyList(),
            )

        // Get active entitlements that remain after removing web sources or ones from the web
        if (withUserCodesRemoved != null) {
            storage.write(LatestRedemptionResponse, withUserCodesRemoved)
        }
        internallySetSubscriptionStatus(
            SubscriptionStatus.Active(
                getActiveDeviceEntitlements(),
            ),
        )
        ioScope.launch {
            redeem(RedeemType.Existing)
        }
    }

    private fun startPolling(maxAge: Long = maxAge()) {
        if (isWebToAppEnabled()) {
            pollingJob?.cancel()
            pollingJob =
                (ioScope + Dispatchers.IO).launch {
                    while (true) {
                        delay(maxAge)
                        checkForWebEntitlements(getUserId(), getDeviceId())
                            .fold(
                                onFailure = {
                                    it.printStackTrace()
                                },
                                onSuccess = {
                                    val newEntitlements = it
                                    var latestRedeemResponse =
                                        storage.read(LatestRedemptionResponse)
                                    val existingWebEntitlements =
                                        latestRedeemResponse?.entitlements ?: emptySet()
                                    latestRedeemResponse =
                                        latestRedeemResponse?.copy(entitlements = newEntitlements.toList())
                                    if (latestRedeemResponse != null) {
                                        storage.write(
                                            LatestRedemptionResponse,
                                            latestRedeemResponse,
                                        )
                                    }
                                    if (existingWebEntitlements.toSet() != newEntitlements) {
                                        internallySetSubscriptionStatus(SubscriptionStatus.Active(it + getActiveDeviceEntitlements()))
                                    }
                                },
                            )
                    }
                }
        }
    }
}
