package com.superwall.sdk

import com.superwall.sdk.config.ConfigManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Smoke tests for [SdkContextImpl] — the cross-slice bridge used by the identity
 * actor to reach into [ConfigManager]. Thin delegates, but since they're the only
 * bridge between the two actors, a missing forward would silently break identity
 * flows in production.
 */
class SdkContextImplTest {
    @Test
    fun `reevaluateTestMode forwards appUserId and aliasId to ConfigManager`() =
        runTest {
            val manager =
                mockk<ConfigManager>(relaxed = true) {
                    coEvery { reevaluateTestMode(any(), any(), any()) } just Runs
                }
            val ctx = SdkContextImpl(configManager = { manager })

            ctx.reevaluateTestMode(appUserId = "user-1", aliasId = "alias-1")

            coVerify(exactly = 1) {
                manager.reevaluateTestMode(
                    config = any(),
                    appUserId = "user-1",
                    aliasId = "alias-1",
                )
            }
        }

    @Test
    fun `fetchAssignments delegates to ConfigManager_getAssignments`() =
        runTest {
            val manager =
                mockk<ConfigManager> {
                    coEvery { getAssignments() } just Runs
                }
            val ctx = SdkContextImpl(configManager = { manager })

            ctx.fetchAssignments()

            coVerify(exactly = 1) { manager.getAssignments() }
        }

    @Test
    fun `configManager factory is invoked lazily so teardown-reconfigure swaps are observable`() =
        runTest {
            val first =
                mockk<ConfigManager>(relaxed = true) {
                    coEvery { reevaluateTestMode(any(), any(), any()) } just Runs
                }
            val second =
                mockk<ConfigManager>(relaxed = true) {
                    coEvery { reevaluateTestMode(any(), any(), any()) } just Runs
                }
            var current: ConfigManager = first
            val ctx = SdkContextImpl(configManager = { current })

            ctx.reevaluateTestMode(null, null)
            coVerify(exactly = 1) { first.reevaluateTestMode(any(), any(), any()) }

            current = second
            ctx.reevaluateTestMode(null, null)
            coVerify(exactly = 1) { second.reevaluateTestMode(any(), any(), any()) }
        }
}
