package com.superwall.sdk.web

import android.content.Context
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent.Redemptions
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent.Redemptions.RedemptionState
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.customer.CustomerInfoManager
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.asEither
import com.superwall.sdk.misc.fold
import com.superwall.sdk.models.customer.CustomerInfo
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
import com.superwall.sdk.storage.LastWebEntitlementsFetchDate
import com.superwall.sdk.storage.LatestRedemptionResponse
import com.superwall.sdk.storage.LatestWebCustomerInfo
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

@Suppress("EXPOSED_PARAMETER_TYPE")
class WebPaywallRedeemer(
    private val context: Context,
    private val ioScope: IOScope,
    internal val deepLinkReferrer: CheckForReferral,
    private val network: Network,
    private val storage: Storage,
    internal val customerInfoManager: CustomerInfoManager,
    private val factory: Factory,
) {
    interface Factory {
        fun willRedeemLink()

        fun didRedeemLink(redemptionResult: RedemptionResult)

        fun maxAge(): Long

        fun getActiveDeviceEntitlements(): Set<Entitlement>

        fun getUserId(): UserId?

        fun getDeviceId(): DeviceVendorId

        fun getAliasId(): String?

        suspend fun track(event: Trackable)

        fun internallySetSubscriptionStatus(status: SubscriptionStatus)

        suspend fun isPaywallVisible(): Boolean

        suspend fun triggerRestoreInPaywall()

        fun currentPaywallEntitlements(): Set<Entitlement>

        fun getPaywallInfo(): PaywallInfo

        fun trackRestorationFailed(message: String)

        fun isWebToAppEnabled(): Boolean

        suspend fun receipts(): List<TransactionReceipt>

        fun getExternalAccountId(): String

        fun getIntegrationProps(): Map<String, Any>

        fun closePaywallIfExists()

        fun isPaymentSheetOpen(): Boolean
    }

    private var pollingJob: Job? = null
    private var redemptionJob: Job? = null

    private suspend fun track(event: Trackable) = factory.track(event)

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
            if (factory.isWebToAppEnabled()) {
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
        //     delay(10.seconds)
        Logger.debug(
            LogLevel.error,
            LogScope.webEntitlements,
            "Starting redemption of type ${
                when (redemption) {
                    is RedeemType.Existing -> "Existing"
                    is RedeemType.Code -> "Code: ${redemption.code}"
                }
            }",
        )
        // We want to keep track of the codes that have been retrieved by the user
        val latestResponse = storage.read(LatestRedemptionResponse)
        val allCodes = latestResponse?.allCodes?.toMutableList() ?: mutableListOf()
        var isFirstRedemption = true
        if (redemption is RedeemType.Code) {
            factory.willRedeemLink()

            if (allCodes.isNotEmpty()) {
                isFirstRedemption = !allCodes.map { it.code }.contains(redemption.code)
            }
            allCodes.add(Redeemable(redemption.code, isFirstRedemption))

            if (factory.isPaywallVisible() && !factory.isPaymentSheetOpen()) {
                track(
                    InternalSuperwallEvent.Restore(
                        InternalSuperwallEvent.Restore.State.Start,
                        factory.getPaywallInfo(),
                    ),
                )
            }
        }
        track(Redemptions(RedemptionState.Start, redemption))
        network
            .redeemToken(
                allCodes,
                factory.getUserId(),
                factory.getAliasId(),
                factory.getDeviceId(),
                factory.receipts(),
                factory.getExternalAccountId(),
                factory.getIntegrationProps().takeIf { it.isNotEmpty() }?.let { props ->
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
                                if (factory.isPaywallVisible() && !factory.isPaymentSheetOpen()) {
                                    if (it.customerInfo?.entitlements?.containsAll(
                                            factory.currentPaywallEntitlements(),
                                        ) ?: false
                                    ) {
                                        factory.triggerRestoreInPaywall()
                                    } else {
                                        factory.trackRestorationFailed("Failed to restore subscriptions from the web")
                                    }
                                }
                            }
                        }

                        RedeemType.Existing -> {
                            // NO-OP
                        }
                    }
                    factory.internallySetSubscriptionStatus(
                        SubscriptionStatus.Active(
                            (
                                it.customerInfo
                                    ?.entitlements
                                    ?.filter { it.isActive }
                                    ?.toSet() ?: emptySet()
                            ) +
                                factory.getActiveDeviceEntitlements(),
                        ),
                    )
                    if (redemption is RedeemType.Code) {
                        factory.closePaywallIfExists()
                        val res = it.codes.first { it.code == redemption.code }
                        factory.didRedeemLink(
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
                        factory.didRedeemLink(
                            RedemptionResult.Error(
                                code = redemption.code ?: "",
                                error =
                                    ErrorInfo(
                                        errorMessage,
                                    ),
                            ),
                        )
                        if (factory.isPaywallVisible() && !factory.isPaymentSheetOpen()) {
                            factory.trackRestorationFailed(errorMessage)
                        }
                    }
                },
            )
        startPolling()
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
        val webEntitlementsResponse = webEntitlementsResult.getSuccess()

        // Extract CustomerInfo from response or construct from entitlements
        val webCustomerInfo =
            webEntitlementsResponse?.customerInfo ?: run {
                // Fallback for backward compatibility
                Logger.debug(
                    logLevel = LogLevel.debug,
                    scope = LogScope.customerInfo,
                    message = "Backend didn't return customerInfo, constructing from entitlements",
                )
                CustomerInfo(
                    subscriptions = emptyList(),
                    nonSubscriptions = emptyList(),
                    userId = userId?.value ?: "",
                    entitlements = emptyList(),
                    isPlaceholder = false,
                )
            }

        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.customerInfo,
            message =
                "Fetched web CustomerInfo: ${webCustomerInfo.subscriptions.size} subs, " +
                    "${webCustomerInfo.nonSubscriptions.size} non-subs",
        )

        // Store web CustomerInfo
        storage.write(LatestWebCustomerInfo, webCustomerInfo)
        storage.write(LastWebEntitlementsFetchDate, System.currentTimeMillis())

        // Return for compatibility with existing code
        webCustomerInfo.entitlements.toSet()
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
            )

        // Get active entitlements that remain after removing web sources or ones from the web
        if (withUserCodesRemoved != null) {
            storage.write(LatestRedemptionResponse, withUserCodesRemoved)
        }
        factory.internallySetSubscriptionStatus(
            SubscriptionStatus.Active(
                factory.getActiveDeviceEntitlements(),
            ),
        )
        redemptionJob?.cancel()
        redemptionJob =
            ioScope.launch {
                redeem(RedeemType.Existing)
            }
    }

    private fun startPolling(maxAge: Long = factory.maxAge()) {
        pollingJob?.cancel()
        pollingJob =
            (ioScope + Dispatchers.IO).launch {
                while (true) {
                    checkForWebEntitlements(factory.getUserId(), factory.getDeviceId())
                        .fold(
                            onFailure = {
                                it.printStackTrace()
                            },
                            onSuccess = { newEntitlements ->
                                val latestRedeemResponse =
                                    storage.read(LatestRedemptionResponse)
                                val existingWebEntitlements =
                                    latestRedeemResponse?.customerInfo?.entitlements?.toSet() ?: emptySet()

                                // Update customerInfo with new entitlements if response exists
                                if (latestRedeemResponse != null) {
                                    val updatedCustomerInfo =
                                        latestRedeemResponse.customerInfo?.copy(
                                            entitlements = newEntitlements.toList(),
                                        ) ?: CustomerInfo(
                                            subscriptions = emptyList(),
                                            nonSubscriptions = emptyList(),
                                            userId = "",
                                            entitlements = newEntitlements.toList(),
                                            isPlaceholder = false,
                                        )
                                    val updatedResponse =
                                        latestRedeemResponse.copy(
                                            customerInfo = updatedCustomerInfo,
                                        )
                                    storage.write(
                                        LatestRedemptionResponse,
                                        updatedResponse,
                                    )
                                }

                                // Trigger CustomerInfo merge
                                customerInfoManager.updateMergedCustomerInfo()

                                if (existingWebEntitlements.filter { it.isActive } != newEntitlements.filter { it.isActive }) {
                                    factory.internallySetSubscriptionStatus(
                                        SubscriptionStatus.Active(
                                            newEntitlements.filter { it.isActive }.toSet() + factory.getActiveDeviceEntitlements(),
                                        ),
                                    )
                                }
                            },
                        )
                    delay(maxAge)
                }
            }
    }
}
