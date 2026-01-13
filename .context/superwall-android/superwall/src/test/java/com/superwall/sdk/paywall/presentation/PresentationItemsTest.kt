package com.superwall.sdk.paywall.presentation

import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PresentationItemsTest {
    @Test
    fun lastTracksLatestRequest() =
        runBlocking {
            val items = PresentationItems()
            val expected =
                LastPresentationItems(
                    request = mockk<PresentationRequest>(),
                    statePublisher = MutableSharedFlow<PaywallState>(),
                )

            items.last = expected

            waitUntil {
                items.last?.request === expected.request
            }

            assertSame(expected, items.last)
        }

    @Test
    fun paywallInfoTracksLatestInfo() =
        runBlocking {
            val items = PresentationItems()
            val info = PaywallInfo.empty()

            items.paywallInfo = info

            waitUntil {
                items.paywallInfo === info
            }

            assertSame(info, items.paywallInfo)
        }

    @Test
    fun resetClearsStoredState() =
        runBlocking {
            val items = PresentationItems()

            items.last =
                LastPresentationItems(
                    request = mockk<PresentationRequest>(),
                    statePublisher = MutableSharedFlow<PaywallState>(),
                )
            items.paywallInfo = PaywallInfo.empty()

            waitUntil { items.last != null && items.paywallInfo != null }

            items.reset()

            waitUntil { items.last == null && items.paywallInfo == null }

            assertNull(items.last)
            assertNull(items.paywallInfo)
        }

    private suspend fun waitUntil(predicate: () -> Boolean) {
        withTimeout(1_000) {
            while (!predicate()) {
                delay(10)
            }
        }
    }
}
