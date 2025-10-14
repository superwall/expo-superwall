package com.superwall.sdk.network

import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.dependencies.ApiFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.map
import com.superwall.sdk.misc.onError
import com.superwall.sdk.misc.onErrorAsync
import com.superwall.sdk.misc.then
import com.superwall.sdk.models.assignment.Assignment
import com.superwall.sdk.models.assignment.AssignmentPostback
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.enrichment.EnrichmentRequest
import com.superwall.sdk.models.entitlements.Redeemable
import com.superwall.sdk.models.entitlements.TransactionReceipt
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.events.EventsRequest
import com.superwall.sdk.models.events.EventsResponse
import com.superwall.sdk.models.internal.DeviceVendorId
import com.superwall.sdk.models.internal.UserId
import com.superwall.sdk.models.internal.WebRedemptionResponse
import com.superwall.sdk.models.paywall.Paywall
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import java.util.UUID
import kotlin.time.Duration

open class Network(
    private val baseHostService: BaseHostService,
    private val collectorService: CollectorService,
    private val enrichmentService: EnrichmentService,
    private val factory: ApiFactory,
    private val subscriptionService: SubscriptionService,
) : SuperwallAPI {
    override suspend fun sendEvents(events: EventsRequest): Either<Unit, NetworkError> =
        collectorService
            .events(
                events,
            ).map {
                if (it.status == EventsResponse.Status.PARTIAL_SUCCESS) {
                    Logger.debug(
                        logLevel = LogLevel.warn,
                        scope = LogScope.network,
                        message = "Request had partial success: /events",
                    )
                }
                Unit
            }.logError("/events", mapOf("payload" to events))

    override suspend fun getConfig(isRetryingCallback: suspend () -> Unit): Either<Config, NetworkError> {
        awaitUntilAppInForeground()

        val requestId = UUID.randomUUID().toString()

        return baseHostService
            .config(
                requestId,
            ).map { config ->
                config.requestId = requestId
                config
            }.logError("/static_config")
    }

    override suspend fun confirmAssignments(confirmableAssignments: AssignmentPostback) =
        baseHostService
            .confirmAssignments(confirmableAssignments)
            .map { }
            .logError("/confirm_assignments", mapOf("assignments" to confirmableAssignments))

    override suspend fun getPaywall(
        identifier: String?,
        event: EventData?,
    ): Either<Paywall, NetworkError> =
        baseHostService
            .paywall(identifier)
            .logError(
                "/paywall${identifier?.let { "/:$it" } ?: ""}",
                if (identifier == null) {
                    mapOf(
                        "identifier" to (identifier ?: "none"),
                        "event" to (event?.name ?: ""),
                    )
                } else {
                    null
                },
            )

    override suspend fun getPaywalls(isForDebugging: Boolean): Either<List<Paywall>, NetworkError> =
        baseHostService
            .paywalls(isForDebugging)
            .map {
                it.paywalls
            }.logError("/paywalls")

    override suspend fun getEnrichment(
        enrichmentRequest: EnrichmentRequest,
        maxRetry: Int,
        timeout: Duration,
    ): Either<Enrichment, NetworkError> {
        awaitUntilAppInForeground()
        factory.track(InternalSuperwallEvent.EnrichmentLoad(InternalSuperwallEvent.EnrichmentLoad.State.Start))
        return enrichmentService
            .enrichment(enrichmentRequest, maxRetry, timeout)
            .then {
                factory.track(
                    InternalSuperwallEvent.EnrichmentLoad(
                        InternalSuperwallEvent.EnrichmentLoad.State.Complete(
                            it,
                        ),
                    ),
                )
            }.onErrorAsync {
                factory.track(InternalSuperwallEvent.EnrichmentLoad(InternalSuperwallEvent.EnrichmentLoad.State.Fail))
            }.logError("/enrich")
    }

    override suspend fun getAssignments(): Either<List<Assignment>, NetworkError> =
        baseHostService
            .assignments()
            .map {
                it.assignments
            }.logError("/assignments")

    override suspend fun redeemToken(
        codes: List<Redeemable>,
        userId: UserId?,
        aliasId: String?,
        vendorId: DeviceVendorId,
        receipts: List<TransactionReceipt>,
        externalAccountId: String,
        attributionProps: Map<String, JsonElement>?,
    ): Either<WebRedemptionResponse, NetworkError> =
        subscriptionService
            .redeemToken(codes, userId, aliasId, vendorId, receipts, externalAccountId, attributionProps)
            .logError("/redeem")

    override suspend fun webEntitlementsByUserId(
        userId: UserId,
        deviceId: DeviceVendorId,
    ) = subscriptionService
        .webEntitlementsByUserId(userId, deviceId)
        .logError("/redeem")

    override suspend fun webEntitlementsByDeviceID(deviceId: DeviceVendorId) =
        subscriptionService
            .webEntitlementsByDeviceId(deviceId)
            .logError("/redeem")

    private suspend fun awaitUntilAppInForeground() {
        // Wait until the app is not in the background.
        factory.appLifecycleObserver
            .isInBackground
            .filter { !it }
            .first()
    }

    private fun <T> Either<T, NetworkError>.logError(
        url: String,
        data: Map<String, Any>? = null,
    ) = onError {
        Logger.debug(
            logLevel = LogLevel.error,
            scope = LogScope.network,
            message = "Request Failed: $url",
            info = data,
            error = it,
        )
    }
}
