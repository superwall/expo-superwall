package com.superwall.sdk.network

import Given
import Then
import When
import com.superwall.sdk.dependencies.ApiFactory
import com.superwall.sdk.misc.Either
import com.superwall.sdk.models.assignment.Assignment
import com.superwall.sdk.models.assignment.ConfirmedAssignmentResponse
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.paywall.Paywalls
import com.superwall.sdk.network.session.CustomHttpUrlConnection
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class BaseHostServiceTest {
    private lateinit var customHttpUrlConnection: CustomHttpUrlConnection
    private lateinit var apiFactory: ApiFactory
    private lateinit var service: BaseHostService
    private lateinit var executor: RequestExecutor

    @Before
    fun setup() {
        customHttpUrlConnection = mockk()
        apiFactory = mockk()
        executor = mockk()
        service =
            BaseHostService(
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
    fun test_config_success() =
        runTest(timeout = 5.minutes) {
            Given("a valid API key and request ID") {
                val apiKey = "test_api_key"
                val requestId = "test_request_id"
                coEvery { executor.execute(any()) } returns
                    Either.Success(
                        RequestResult(
                            requestId = "1",
                            responseCode = 200,
                            responseMessage = Json.encodeToString(Config.stub()),
                            duration = 10.0,
                            headers = mapOf("Authorization" to "Bearer token"),
                        ),
                    )
                coEvery { apiFactory.makeHeaders(any(), any()) } returns mapOf("Authorization" to "Bearer token")
                coEvery { apiFactory.storage.apiKey } returns apiKey

                When("requesting the config") {
                    val result = service.config(requestId)

                    Then("the result should be a success with the expected config") {
                        assertTrue(result is Either.Success)
                        assertEquals(Config.stub(), (result as Either.Success).value)
                    }
                }
            }
        }

    @Test
    fun test_config_failure() =
        runTest(timeout = 5.minutes) {
            val error = NetworkError.Unknown()
            Given("an invalid API key or network error") {
                val requestId = "test_request_id"
                coEvery { executor.execute(any()) } returns Either.Failure(error)
                coEvery { apiFactory.makeHeaders(any(), any()) } returns mapOf("Authorization" to "Bearer token")
                coEvery { apiFactory.storage.apiKey } returns "invalid_api_key"

                When("requesting the config") {
                    val result = service.config(requestId)

                    Then("the result should be a failure") {
                        assertTrue(result is Either.Failure)
                        assertEquals(error, (result as Either.Failure).error)
                    }
                }
            }
        }

    @Test
    fun test_assignments_success() =
        runTest(timeout = 5.minutes) {
            Given("a valid request for assignments") {
                val mockResponse = ConfirmedAssignmentResponse(mutableListOf(Assignment("exp1", "var1")))
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
                println("In given")
                coEvery { apiFactory.makeHeaders(any(), any()) } returns mapOf("Authorization" to "Bearer token")

                When("requesting assignments") {
                    println("In when")
                    val result = service.assignments()

                    Then("the result should be a success with the expected assignments") {
                        println("in then")
                        assertTrue(result is Either.Success)
                        assertEquals(mockResponse, (result as Either.Success).value)
                    }
                }
            }
        }

    @Test
    fun test_assignments_failure() =
        runTest(timeout = 5.minutes) {
            val error = NetworkError.Unknown()
            Given("a network error occurs") {
                coEvery { executor.execute(any()) } returns Either.Failure(error)
                coEvery { apiFactory.makeHeaders(any(), any()) } returns mapOf("Authorization" to "Bearer token")

                When("requesting assignments") {
                    val result = service.assignments()

                    Then("the result should be a failure") {
                        assertTrue(result is Either.Failure)
                        assertEquals(error, (result as Either.Failure).error)
                    }
                }
            }
        }

    @Test
    fun test_paywalls_success() =
        runTest(timeout = 5.minutes) {
            Given("a valid request for paywalls") {
                val mockResponse = Paywalls(listOf(Paywall.stub()))
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

                When("requesting paywalls") {
                    val result = service.paywalls()

                    Then("the result should be a success with the expected paywalls") {
                        assertTrue(result is Either.Success)
                        assertEquals(mockResponse, (result as Either.Success).value)
                    }
                }
            }
        }

    @Test
    fun test_paywalls_failure() =
        runTest(timeout = 5.minutes) {
            val error = NetworkError.Unknown()

            Given("a network error occurs") {
                coEvery { executor.execute(any()) } returns Either.Failure(error)
                coEvery { apiFactory.makeHeaders(any(), any()) } returns mapOf("Authorization" to "Bearer token")

                When("requesting paywalls") {
                    val result = service.paywalls()

                    Then("the result should be a failure") {
                        assertTrue(result is Either.Failure)
                        assertEquals(error, (result as Either.Failure).error)
                    }
                }
            }
        }

    @Test
    fun test_paywall_success() =
        runTest(timeout = 5.minutes) {
            Given("a valid request for a specific paywall") {
                val paywallId = "test_paywall_id"
                val mockResponse = Paywall.stub()
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
                coEvery { apiFactory.storage.apiKey } returns "test_api_key"
                coEvery { apiFactory.configManager.config } returns Config.stub()
                coEvery { apiFactory.deviceHelper.locale } returns "en_US"

                When("requesting a specific paywall") {
                    val result = service.paywall(paywallId)

                    Then("the result should be a success with the expected paywall") {
                        assertTrue(result is Either.Success)
                        assertEquals(mockResponse, (result as Either.Success).value)
                    }
                }
            }
        }

    @Test
    fun test_paywall_failure() =
        runTest(timeout = 5.minutes) {
            Given("an invalid paywall ID or network error") {
                val paywallId = "invalid_paywall_id"
                val error = NetworkError.Unknown()
                coEvery { executor.execute(any()) } returns Either.Failure(error)
                coEvery { apiFactory.makeHeaders(any(), any()) } returns mapOf("Authorization" to "Bearer token")
                coEvery { apiFactory.storage.apiKey } returns "test_api_key"
                coEvery { apiFactory.configManager.config } returns Config.stub()
                coEvery { apiFactory.deviceHelper.locale } returns "en_US"

                When("requesting a specific paywall") {
                    val result = service.paywall(paywallId)

                    Then("the result should be a failure") {
                        assertTrue(result is Either.Failure)
                        assertEquals(error, (result as Either.Failure).error)
                    }
                }
            }
        }
}
