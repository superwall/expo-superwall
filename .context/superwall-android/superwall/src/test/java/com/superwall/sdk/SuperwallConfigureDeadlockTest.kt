package com.superwall.sdk

import android.content.Context
import com.superwall.sdk.dependencies.DependencyContainer
import com.superwall.sdk.store.Entitlements
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression guard for the AB-BA deadlock that caused the production ANR
 * tracked in expo-superwall#194 / SW-5092.
 *
 * Original cycle, present before the fix:
 *
 *   Lock A — the Superwall singleton's intrinsic monitor, taken by both
 *            setup() and the dependencyContainer getter.
 *
 *   Lock B — the SynchronizedLazyImpl monitor backing the `entitlements`
 *            property. Its initializer body `{ dependencyContainer.entitlements }`
 *            re-entered Lock A.
 *
 * Production trace (before the fix):
 *   worker-1: holds A inside setup()                    -> wants B
 *   worker-2: holds B inside lazy initializer           -> wants A
 *   main:     wants A from identify() / setUserAttrs()  -> ANR
 *
 * This test arms the exact interleaving that previously deadlocked:
 *   1. Thread X holds the Superwall singleton monitor (Lock A) and then
 *      reads `entitlements` — the pattern setup() uses when it calls
 *      setSubscriptionStatus while still inside `synchronized(this@Superwall)`.
 *   2. Thread Y reads `entitlements` from outside the singleton monitor —
 *      the pattern AppSessionManager.detectNewSession -> DeviceHelper takes
 *      from a worker. This forces Y through the lazy initializer (Lock B).
 *
 * Under the previous code Thread Y's initializer would block on Lock A
 * while Thread X blocked on Lock B, and both threads would stay BLOCKED
 * indefinitely. Under the fix, the lazy initializer does not re-enter
 * the singleton monitor, so both threads complete promptly.
 *
 * The guard asserts that both threads finish within a short window. If
 * anyone reintroduces a synchronized hop into the `entitlements` /
 * `subscriptionStatus` lazy initializers (or anything else they call
 * that takes the Superwall singleton monitor), this test will fail by
 * timing out.
 *
 * java.lang.management is unavailable on the Android unit-test runtime,
 * so completion is observed via Thread.join with a timeout.
 */
class SuperwallConfigureDeadlockTest {
    @Test(timeout = 15_000)
    fun entitlements_lazy_initializer_does_not_reenter_singleton_monitor() {
        val context = mockk<Context>(relaxed = true)
        val sw =
            Superwall(
                context = context,
                apiKey = "test",
                purchaseController = null,
                options = null,
                activityProvider = null,
                completion = null,
            )

        // Skip setup() but plant a usable _dependencyContainer so the
        // entitlements lazy initializer can return without throwing
        // UninitializedPropertyAccessException.
        val fakeDc = mockk<DependencyContainer>(relaxed = true)
        every { fakeDc.entitlements } returns mockk<Entitlements>(relaxed = true)
        val dcField = Superwall::class.java.getDeclaredField("_dependencyContainer")
        dcField.isAccessible = true
        dcField.set(sw, fakeDc)

        val xHasLockA = CountDownLatch(1)
        val yFinishedLazy = CountDownLatch(1)

        // Thread Y: read `entitlements` from outside the singleton monitor.
        // This goes through SynchronizedLazyImpl.getValue (Lock B). For Y to
        // complete while X holds Lock A, the lazy initializer must NOT take
        // the singleton monitor.
        val threadY =
            Thread({
                xHasLockA.await()
                sw.entitlements
                yFinishedLazy.countDown()
            }, "deadlock-guard-Y").apply { isDaemon = true }

        // Thread X: hold the singleton monitor (Lock A), then read
        // `entitlements`. Mirrors setup() calling setSubscriptionStatus
        // while inside `synchronized(this@Superwall)`. Waits until Y has
        // finished its lazy access so we know Y did not deadlock.
        val threadX =
            Thread({
                synchronized(sw) {
                    xHasLockA.countDown()
                    yFinishedLazy.await(5, TimeUnit.SECONDS)
                    sw.entitlements
                }
            }, "deadlock-guard-X").apply { isDaemon = true }

        threadX.start()
        threadY.start()

        threadY.join(5_000)
        threadX.join(5_000)

        if (threadY.isAlive || threadX.isAlive) {
            val xFrames = threadX.stackTrace.take(8).joinToString("\n") { "  at $it" }
            val yFrames = threadY.stackTrace.take(8).joinToString("\n") { "  at $it" }
            val msg =
                buildString {
                    appendLine("AB-BA deadlock regression: the entitlements lazy initializer")
                    appendLine("appears to re-enter a synchronized scope on the Superwall singleton.")
                    appendLine("This is the cycle that produced the production ANR in SW-5092.")
                    appendLine()
                    appendLine("Thread X (held singleton monitor, then read entitlements) state=${threadX.state}:")
                    appendLine(xFrames)
                    appendLine()
                    appendLine("Thread Y (read entitlements from outside singleton monitor) state=${threadY.state}:")
                    appendLine(yFrames)
                }
            // Daemon threads will be cleaned up on JVM exit; we just need them out of the way.
            assertTrue(msg, false)
        }
    }
}
