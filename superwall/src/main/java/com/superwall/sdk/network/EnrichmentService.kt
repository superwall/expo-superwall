package com.superwall.sdk.network

import com.superwall.sdk.dependencies.ApiFactory
import com.superwall.sdk.misc.eitherWithTimeout
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.enrichment.EnrichmentRequest
import com.superwall.sdk.network.session.CustomHttpUrlConnection
import kotlinx.serialization.encodeToString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class EnrichmentService(
    override val host: String,
    override val version: String,
    val factory: ApiFactory,
    override val customHttpUrlConnection: CustomHttpUrlConnection,
) : NetworkService() {
    override suspend fun makeHeaders(
        isForDebugging: Boolean,
        requestId: String,
    ): Map<String, String> = factory.makeHeaders(isForDebugging, requestId)

    suspend fun enrichment(
        enrichmentRequest: EnrichmentRequest,
        maxRetry: Int = 0,
        timeout: Duration = 1.seconds,
    ) = eitherWithTimeout(timeout, error = { NetworkError.Timeout }) {
        post<Enrichment>(
            "enrich",
            retryCount = maxRetry,
            body = factory.json().encodeToString(enrichmentRequest).toByteArray(),
        )
    }
}
