package com.superwall.sdk.network

import Given
import Then
import When
import com.superwall.sdk.dependencies.ApiFactory
import com.superwall.sdk.misc.Either
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.events.EventsRequest
import com.superwall.sdk.models.events.EventsResponse
import com.superwall.sdk.network.session.CustomHttpUrlConnection
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Date
import kotlin.time.Duration.Companion.minutes

class CollectorServiceTest {
    private lateinit var customHttpUrlConnection: CustomHttpUrlConnection
    private lateinit var apiFactory: ApiFactory
    private lateinit var service: CollectorService
    private lateinit var executor: RequestExecutor

    @Before
    fun setup() {
        customHttpUrlConnection = mockk()
        apiFactory = mockk()
        executor = mockk()
        service =
            CollectorService(
                host = "test.com",
                version = "/v1/",
                factory = apiFactory,
                json = Json { ignoreUnknownKeys = true },
                customHttpUrlConnection =
                    CustomHttpUrlConnection(
                        Json { ignoreUnknownKeys = true },
                        executor,
                        emptyList(),
                    ),
            )
    }

    @Test
    fun test_events_success() =
        runTest(timeout = 5.minutes) {
            Given("a valid events request") {
                val eventsRequest =
                    EventsRequest(
                        listOf(
                            EventData(
                                name = "test_event",
                                parameters = emptyMap(),
                                createdAt =
                                    Date.from(
                                        Instant.now(),
                                    ),
                            ),
                        ),
                    )
                val mockResponse = EventsResponse(EventsResponse.Status.OK)
                coEvery { executor.execute(any()) } returns
                    Either.Success(
                        RequestResult(
                            requestId = "1",
                            responseCode = 200,
                            responseMessage = Json.encodeToString(mockResponse),
                            duration = 10.0,
                            headers = mapOf("Authorization " to "Bearer token"),
                        ),
                    )
                coEvery { apiFactory.makeHeaders(any(), any()) } returns mapOf("Authorization" to "Bearer token")

                When("sending events") {
                    val result = service.events(eventsRequest)

                    Then("the result should be a success with the expected response") {
                        assertTrue(result is Either.Success)
                        assertEquals(mockResponse, (result as Either.Success).value)
                    }
                }
            }
        }

    @Test
    fun test_events_partial_success() =
        runTest(timeout = 5.minutes) {
            Given("a partially valid events request") {
                val eventsRequest =
                    EventsRequest(
                        listOf(
                            EventData(
                                name = "test_event",
                                parameters = emptyMap(),
                                createdAt =
                                    Date.from(
                                        Instant.now(),
                                    ),
                            ),
                            EventData(
                                name = "invalid_event",
                                parameters = emptyMap(),
                                createdAt =
                                    Date.from(
                                        Instant.now(),
                                    ),
                            ),
                        ),
                    )
                val mockResponse = EventsResponse(EventsResponse.Status.PARTIAL_SUCCESS, listOf(1))
                coEvery { executor.execute(any()) } returns
                    Either.Success(
                        RequestResult(
                            requestId = "1",
                            responseCode = 200,
                            responseMessage = Json.encodeToString(mockResponse),
                            duration = 10.0,
                            headers = mapOf("Authorization" to "Bearer token"),
                        ),
                    )
                coEvery { apiFactory.makeHeaders(any(), any()) } returns mapOf("Authorization" to "Bearer token")

                When("sending events") {
                    val result = service.events(eventsRequest)

                    Then("the result should be a success with partial success status") {
                        assertTrue(result is Either.Success)
                        val response = (result as Either.Success).value
                        assertEquals(EventsResponse.Status.PARTIAL_SUCCESS, response.status)
                        assertEquals(listOf(1), response.invalidIndexes)
                    }
                }
            }
        }
}
