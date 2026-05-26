package com.superwall.sdk.config

import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.models.config.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for [ConfigState.Updates]. Reducers are `(ConfigState) -> ConfigState`
 * with no side effects, so they can be exercised without an actor, scope, or context.
 *
 * These guard the trivial state shape rather than behavior — if someone adds a field
 * to ConfigState or subtly changes the phase model, they'll fail fast here instead of
 * waiting for an integration test to catch it.
 */
class ConfigStateReducerTest {
    private val stubConfig = Config.stub()
    private val stubError = RuntimeException("boom")

    @Test
    fun `SetRetrieving replaces any prior state with Retrieving`() {
        val inputs: List<ConfigState> =
            listOf(
                ConfigState.None,
                ConfigState.Retrieving,
                ConfigState.Retrying,
                ConfigState.Retrieved(stubConfig),
                ConfigState.Failed(stubError),
            )
        inputs.forEach { input ->
            val out = ConfigState.Updates.SetRetrieving.reduce(input)
            assertSame("SetRetrieving from $input", ConfigState.Retrieving, out)
        }
    }

    @Test
    fun `SetRetrying replaces any prior state with Retrying`() {
        val inputs: List<ConfigState> =
            listOf(
                ConfigState.None,
                ConfigState.Retrieving,
                ConfigState.Retrying,
                ConfigState.Retrieved(stubConfig),
                ConfigState.Failed(stubError),
            )
        inputs.forEach { input ->
            val out = ConfigState.Updates.SetRetrying.reduce(input)
            assertSame("SetRetrying from $input", ConfigState.Retrying, out)
        }
    }

    @Test
    fun `SetRetrieved carries the config payload and overwrites any prior state`() {
        val next = ConfigState.Updates.SetRetrieved(stubConfig).reduce(ConfigState.Retrieving)
        assertTrue(next is ConfigState.Retrieved)
        assertEquals(stubConfig, (next as ConfigState.Retrieved).config)

        // Also from Failed (cold-start recovery path).
        val next2 = ConfigState.Updates.SetRetrieved(stubConfig).reduce(ConfigState.Failed(stubError))
        assertTrue(next2 is ConfigState.Retrieved)
        assertEquals(stubConfig, (next2 as ConfigState.Retrieved).config)
    }

    @Test
    fun `SetFailed carries the throwable payload`() {
        val err = IllegalStateException("oops")
        val next = ConfigState.Updates.SetFailed(err).reduce(ConfigState.Retrieving)
        assertTrue(next is ConfigState.Failed)
        assertEquals(err, (next as ConfigState.Failed).throwable)
    }

    @Test
    fun `Set forces any prior state to the supplied state — test-only escape hatch`() {
        val target = ConfigState.Retrieved(stubConfig)
        val out = ConfigState.Updates.Set(target).reduce(ConfigState.Failed(stubError))
        assertSame(target, out)
    }

    @Test
    fun `Updates are pure — invoking twice on the same input yields the same output`() {
        val input = ConfigState.None
        val a = ConfigState.Updates.SetRetrieving.reduce(input)
        val b = ConfigState.Updates.SetRetrieving.reduce(input)
        assertSame(a, b)

        val c = ConfigState.Updates.SetRetrieved(stubConfig).reduce(input)
        val d = ConfigState.Updates.SetRetrieved(stubConfig).reduce(input)
        // Retrieved uses data class equality, not identity — assertEquals is the right check.
        assertEquals(c, d)
    }
}
