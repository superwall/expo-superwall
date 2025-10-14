package com.superwall.sdk.network

import com.superwall.sdk.misc.Either
import com.superwall.sdk.models.assignment.Assignment
import com.superwall.sdk.models.assignment.AssignmentPostback
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.enrichment.EnrichmentRequest
import com.superwall.sdk.models.entitlements.Redeemable
import com.superwall.sdk.models.entitlements.TransactionReceipt
import com.superwall.sdk.models.entitlements.WebEntitlements
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.events.EventsRequest
import com.superwall.sdk.models.internal.DeviceVendorId
import com.superwall.sdk.models.internal.UserId
import com.superwall.sdk.models.internal.WebRedemptionResponse
import com.superwall.sdk.models.paywall.Paywall
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration

class NetworkMock : SuperwallAPI {
    //    var sentSessionEvents: SessionEventsRequest? = null
    var getConfigCalled = false
    var assignmentsConfirmed = false
    var assignments: MutableList<Assignment> = mutableListOf()
    var configReturnValue: Config? = Config.stub()
    var configError: Exception? = null

    override suspend fun sendEvents(events: EventsRequest): Either<Unit, NetworkError> {
        TODO("Not yet implemented")
    }

//    suspend fun sendSessionEvents(session: SessionEventsRequest) {
//        sentSessionEvents = session
//    }

    @Throws(Exception::class)
    override suspend fun getConfig(isRetryingCallback: suspend () -> Unit): Either<Config, NetworkError> {
        getConfigCalled = true

        configReturnValue?.let {
            return Either.Success(it)
        } ?: throw configError ?: Exception("Config Error")
    }

    override suspend fun confirmAssignments(confirmableAssignments: AssignmentPostback): Either<Unit, NetworkError> {
        assignmentsConfirmed = true
        return Either.Success(Unit)
    }

    override suspend fun getPaywall(
        identifier: String?,
        event: EventData?,
    ): Either<Paywall, NetworkError> {
        TODO("Not yet implemented")
    }

    override suspend fun getPaywalls(isForDebugging: Boolean): Either<List<Paywall>, NetworkError> {
        TODO("Not yet implemented")
    }

    override suspend fun getEnrichment(
        enrichmentRequest: EnrichmentRequest,
        maxRetry: Int,
        timeout: Duration,
    ): Either<Enrichment, NetworkError> = Either.Success(Enrichment.stub())

    @Throws(Exception::class)
    override suspend fun getAssignments(): Either<List<Assignment>, NetworkError> = Either.Success(assignments)

    override suspend fun webEntitlementsByUserId(
        userId: UserId,
        deviceId: DeviceVendorId,
    ): Either<WebEntitlements, NetworkError> {
        TODO("Not yet implemented")
    }

    override suspend fun webEntitlementsByDeviceID(deviceId: DeviceVendorId): Either<WebEntitlements, NetworkError> {
        TODO("Not yet implemented")
    }

    override suspend fun redeemToken(
        token: List<Redeemable>,
        userId: UserId?,
        aliasId: String?,
        vendorId: DeviceVendorId,
        receipts: List<TransactionReceipt>,
        externalAccountId: String,
        attributionProps: Map<String, JsonElement>?,
    ): Either<WebRedemptionResponse, NetworkError> {
        TODO("Not yet implemented")
    }
}
