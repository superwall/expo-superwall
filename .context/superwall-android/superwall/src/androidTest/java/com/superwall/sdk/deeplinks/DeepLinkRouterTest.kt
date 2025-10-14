package com.superwall.sdk.deeplinks

import Given
import Then
import When
import android.net.Uri
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.debug.DebugManager
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.web.WebPaywallRedeemer
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class DeepLinkRouterTest {
    private lateinit var mockRedeemer: WebPaywallRedeemer
    private lateinit var mockDebugManager: DebugManager
    private lateinit var mockTrackFunction: (TrackableSuperwallEvent) -> Unit

    @Before
    fun setup() {
        mockRedeemer = mockk()
        mockDebugManager = mockk()
        mockTrackFunction = mockk()

        every { mockTrackFunction(any()) } just Runs

        mockkObject(Superwall)
        every { Superwall.hasInitialized } returns flowOf(true)
        every { Superwall.instance.configurationStateListener } returns flowOf(ConfigurationStatus.Configured)
    }

    private fun TestScope.setupDeepLinkRouter(): DeepLinkRouter {
        val ioScope = IOScope(this.coroutineContext)
        return DeepLinkRouter(
            redeemer = mockRedeemer,
            ioScope = ioScope,
            debugManager = mockDebugManager,
            track = mockTrackFunction,
        )
    }

    @Test
    fun test_handleDeepLink_valid_superwall_redemption_code() =
        runTest(timeout = 5.minutes) {
            Given("a valid superwall redemption deep link") {
                val deepLinkRouter = setupDeepLinkRouter()
                val uri = Uri.parse("https://superwall.com/redeem?code=TEST123")
                val mockDeepLinkReferrer = mockk<com.superwall.sdk.web.DeepLinkReferrer>()
                every { mockRedeemer.deepLinkReferrer } returns mockDeepLinkReferrer
                every { mockDeepLinkReferrer.handleDeepLink(uri) } returns Result.success("TEST123")
                coEvery { mockRedeemer.redeem(any()) } just Runs

                When("handling the deep link") {
                    val result = deepLinkRouter.handleDeepLink(uri)
                    advanceUntilIdle()

                    Then("the result should be success and redemption should be triggered") {
                        assertTrue(result.isSuccess)
                        assertTrue(result.getOrNull() == true)
                        coVerify { mockRedeemer.redeem(WebPaywallRedeemer.RedeemType.Code("TEST123")) }
                        verify { mockTrackFunction(any<InternalSuperwallEvent.DeepLink>()) }
                    }
                }
            }
        }

    @Test
    fun test_handleDeepLink_invalid_redemption_code_handled_by_debug() =
        runTest(timeout = 5.minutes) {
            Given("a deep link that is not a valid redemption code but can be handled by debug manager") {
                val deepLinkRouter = setupDeepLinkRouter()
                val uri = Uri.parse("https://superwall.com/debug?key=value")
                val mockDeepLinkReferrer = mockk<com.superwall.sdk.web.DeepLinkReferrer>()
                every { mockRedeemer.deepLinkReferrer } returns mockDeepLinkReferrer
                every { mockDeepLinkReferrer.handleDeepLink(uri) } returns Result.failure(Exception("Not a redemption code"))
                every { mockDebugManager.handle(deepLinkUrl = uri) } returns true

                When("handling the deep link") {
                    val result = deepLinkRouter.handleDeepLink(uri)
                    advanceUntilIdle()

                    Then("the result should be success and debug manager should handle it") {
                        assertTrue(result.isSuccess)
                        assertTrue(result.getOrNull() == true)
                        verify { mockDebugManager.handle(deepLinkUrl = uri) }
                        verify { mockTrackFunction(any<InternalSuperwallEvent.DeepLink>()) }
                        coVerify(exactly = 0) { mockRedeemer.redeem(any()) }
                    }
                }
            }
        }

    @Test
    fun test_handleDeepLink_unhandled_link() =
        runTest(timeout = 5.minutes) {
            Given("a deep link that cannot be handled by redemption or debug") {
                val deepLinkRouter = setupDeepLinkRouter()
                val uri = Uri.parse("https://example.com/some/path")
                val mockDeepLinkReferrer = mockk<com.superwall.sdk.web.DeepLinkReferrer>()
                every { mockRedeemer.deepLinkReferrer } returns mockDeepLinkReferrer
                every { mockDeepLinkReferrer.handleDeepLink(uri) } returns Result.failure(Exception("Not a redemption code"))
                every { mockDebugManager.handle(deepLinkUrl = uri) } returns false

                When("handling the deep link") {
                    val result = deepLinkRouter.handleDeepLink(uri)
                    advanceUntilIdle()

                    Then("the result should be success but indicate the link was not handled") {
                        assertTrue(result.isSuccess)
                        assertFalse(result.getOrNull() == true)
                        verify { mockDebugManager.handle(deepLinkUrl = uri) }
                        verify { mockTrackFunction(any<InternalSuperwallEvent.DeepLink>()) }
                        coVerify(exactly = 0) { mockRedeemer.redeem(any()) }
                    }
                }
            }
        }

    @Test
    fun test_companion_handleDeepLink_valid_superwall_redemption_code() =
        runTest(timeout = 5.minutes) {
            Given("a valid superwall redemption deep link") {
                val uri = Uri.parse("https://superwall.com/redeem?code=TEST123")

                mockkObject(DebugManager)
                every { DebugManager.outcomeForDeepLink(uri) } returns Result.failure(Exception("Not a debug link"))

                When("handling the deep link via companion object") {
                    val result = DeepLinkRouter.handleDeepLink(uri)

                    Then("the result should be success and link should be queued") {
                        assertTrue(result.isSuccess)
                        assertTrue(result.getOrNull() == true)
                    }
                }
            }
        }

    @Test
    fun test_companion_handleDeepLink_valid_debug_link() =
        runTest(timeout = 5.minutes) {
            Given("a valid debug deep link") {
                val uri = Uri.parse("https://superwall.com/debug?key=value")

                mockkObject(DebugManager)
                every { DebugManager.outcomeForDeepLink(uri) } returns Result.success(DebugManager.DeepLinkOutcome("value", null))

                When("handling the deep link via companion object") {
                    val result = DeepLinkRouter.handleDeepLink(uri)

                    Then("the result should be success and link should be queued") {
                        assertTrue(result.isSuccess)
                        assertTrue(result.getOrNull() == true)
                    }
                }
            }
        }

    @Test
    fun test_companion_handleDeepLink_invalid_link() =
        runTest(timeout = 5.minutes) {
            Given("an invalid deep link that is not a superwall link") {
                val uri = Uri.parse("https://example.com/some/path")

                mockkObject(DebugManager)
                every { DebugManager.outcomeForDeepLink(uri) } returns Result.failure(Exception("Not a debug link"))

                When("handling the deep link via companion object") {
                    val result = DeepLinkRouter.handleDeepLink(uri)

                    Then("the result should be failure") {
                        assertTrue(result.isFailure)
                        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
                        assertEquals("Not a superwall link", result.exceptionOrNull()?.message)
                    }
                }
            }
        }

    @Test
    fun test_uri_redeemableCode_extension_valid_redemption_link() =
        runTest(timeout = 5.minutes) {
            Given("a valid superwall redemption URI") {
                val uri = Uri.parse("https://superwall.com/redeem?code=ABC123")

                When("extracting the redeemable code") {
                    val result = uri.redeemableCode

                    Then("the result should be success with the correct code") {
                        assertTrue(result.isSuccess)
                        assertEquals("ABC123", result.getOrNull())
                    }
                }
            }
        }

    @Test
    fun test_uri_redeemableCode_extension_invalid_host() =
        runTest(timeout = 5.minutes) {
            Given("a URI with invalid host") {
                val uri = Uri.parse("https://example.com/redeem?code=ABC123")

                When("extracting the redeemable code") {
                    val result = uri.redeemableCode

                    Then("the result should be failure") {
                        assertTrue(result.isFailure)
                        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
                        assertEquals("Link not valid for redemption", result.exceptionOrNull()?.message)
                    }
                }
            }
        }

    @Test
    fun test_uri_redeemableCode_extension_invalid_path() =
        runTest(timeout = 5.minutes) {
            Given("a superwall URI with invalid path") {
                val uri = Uri.parse("https://superwall.com/invalid?code=ABC123")

                When("extracting the redeemable code") {
                    val result = uri.redeemableCode

                    Then("the result should be failure") {
                        assertTrue(result.isFailure)
                        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
                        assertEquals("Link not valid for redemption", result.exceptionOrNull()?.message)
                    }
                }
            }
        }

    @Test
    fun test_uri_redeemableCode_extension_missing_code_parameter() =
        runTest(timeout = 5.minutes) {
            Given("a superwall redemption URI without code parameter") {
                val uri = Uri.parse("https://superwall.com/redeem")

                When("extracting the redeemable code") {
                    val result = uri.redeemableCode

                    Then("the result should be failure") {
                        assertTrue(result.isFailure)
                        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
                        assertEquals("Link not valid for redemption", result.exceptionOrNull()?.message)
                    }
                }
            }
        }

    @Test
    fun test_handleDeepLink_with_exception_in_redeemer() =
        runTest(timeout = 5.minutes) {
            Given("a deep link that causes an exception in the redeemer") {
                val deepLinkRouter = setupDeepLinkRouter()
                val uri = Uri.parse("https://superwall.com/redeem?code=TEST123")
                val mockDeepLinkReferrer = mockk<com.superwall.sdk.web.DeepLinkReferrer>()
                every { mockRedeemer.deepLinkReferrer } returns mockDeepLinkReferrer
                every { mockDeepLinkReferrer.handleDeepLink(uri) } throws RuntimeException("Network error")
                every { mockDebugManager.handle(deepLinkUrl = uri) } returns false

                When("handling the deep link") {
                    val result = deepLinkRouter.handleDeepLink(uri)
                    advanceUntilIdle()

                    Then("the result should handle the exception gracefully") {
                        assertTrue(result.isFailure)
                        verify { mockTrackFunction(any<InternalSuperwallEvent.DeepLink>()) }
                        coVerify(exactly = 0) { mockRedeemer.redeem(any()) }
                    }
                }
            }
        }
}
