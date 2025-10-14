package com.superwall.sdk.network

import com.superwall.sdk.dependencies.ApiFactory
import com.superwall.sdk.models.events.EventsRequest
import com.superwall.sdk.models.events.EventsResponse
import com.superwall.sdk.network.session.CustomHttpUrlConnection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CollectorService(
    override val host: String,
    override val version: String,
    val factory: ApiFactory,
    private val json: Json,
    override val customHttpUrlConnection: CustomHttpUrlConnection,
) : NetworkService() {
    override suspend fun makeHeaders(
        isForDebugging: Boolean,
        requestId: String,
    ): Map<String, String> = factory.makeHeaders(isForDebugging, requestId)

    suspend fun events(eventsRequest: EventsRequest) =
        post<EventsResponse>(
            "events",
            body = json.encodeToString(eventsRequest).toByteArray(),
        )
}
