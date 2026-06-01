package com.superwall.sdk.identity

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.And
import com.superwall.sdk.identity.IdentityState.Pending
import com.superwall.sdk.identity.IdentityState.Phase
import com.superwall.sdk.identity.IdentityState.Updates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure reducer tests — no coroutines, no mocks, no actor pipeline.
 * Each test applies a single Updates reducer to a known state and
 * asserts the output.
 */
class IdentityStateReducerTest {

    private fun readyState(
        appUserId: String? = null,
        aliasId: String = "alias-1",
        seed: Int = 42,
        userAttributes: Map<String, Any> = emptyMap(),
        appInstalledAtString: String = "2024-01-01",
    ) = IdentityState(
        appUserId = appUserId,
        aliasId = aliasId,
        seed = seed,
        userAttributes = userAttributes,
        phase = Phase.Ready,
        appInstalledAtString = appInstalledAtString,
    )

    private fun pendingState(
        vararg items: Pending,
        appUserId: String? = null,
        aliasId: String = "alias-1",
        seed: Int = 42,
        userAttributes: Map<String, Any> = emptyMap(),
    ) = IdentityState(
        appUserId = appUserId,
        aliasId = aliasId,
        seed = seed,
        userAttributes = userAttributes,
        phase = Phase.Pending(items.toSet()),
        appInstalledAtString = "2024-01-01",
    )

    // -----------------------------------------------------------------------
    // Updates.Identify
    // -----------------------------------------------------------------------

    @Test
    fun `Identify - first login sets appUserId and Pending Seed`() =
        Given("an anonymous ready state") {
            val state = readyState()

            val result = When("Identify is applied with a new userId") {
                Updates.Identify("user-1", restoreAssignments = false).reduce(state)
            }

            Then("appUserId is set") {
                assertEquals("user-1", result.appUserId)
            }
            And("phase is Pending with Seed") {
                assertEquals(Phase.Pending(setOf(Pending.Seed)), result.phase)
            }
            And("userAttributes contain the userId") {
                assertEquals("user-1", result.userAttributes[Keys.APP_USER_ID])
            }
        }

    @Test
    fun `Identify - with restoreAssignments adds Assignments to pending`() =
        Given("an anonymous ready state") {
            val state = readyState()

            val result = When("Identify is applied with restoreAssignments = true") {
                Updates.Identify("user-1", restoreAssignments = true).reduce(state)
            }

            Then("phase has both Seed and Assignments pending") {
                assertEquals(
                    Phase.Pending(setOf(Pending.Seed, Pending.Assignments)),
                    result.phase,
                )
            }
        }

    @Test
    fun `Identify - same userId is a no-op`() =
        Given("a logged-in state") {
            val state = readyState(appUserId = "user-1")

            val result = When("Identify is applied with the same userId") {
                Updates.Identify("user-1", restoreAssignments = false).reduce(state)
            }

            Then("state is unchanged") {
                assertEquals(state, result)
            }
        }

    @Test
    fun `Identify - switching users resets to fresh identity`() =
        Given("a logged-in state with attributes") {
            val state = readyState(
                appUserId = "user-1",
                aliasId = "old-alias",
                seed = 42,
                userAttributes = mapOf("custom" to "value"),
            )

            val result = When("Identify is applied with a different userId") {
                Updates.Identify("user-2", restoreAssignments = false).reduce(state)
            }

            Then("appUserId is updated") {
                assertEquals("user-2", result.appUserId)
            }
            And("aliasId is regenerated (different from old)") {
                assertNotEquals("old-alias", result.aliasId)
            }
            And("old custom attributes are dropped") {
                assertNull(result.userAttributes["custom"])
            }
            And("new identity attributes are present") {
                assertEquals("user-2", result.userAttributes[Keys.APP_USER_ID])
            }
        }

    @Test
    fun `Identify - preserves appInstalledAtString across user switch`() =
        Given("a state with a specific install date") {
            val state = readyState(
                appUserId = "old-user",
                appInstalledAtString = "2023-06-15",
            )

            val result = When("switching users") {
                Updates.Identify("new-user", restoreAssignments = false).reduce(state)
            }

            Then("appInstalledAtString is preserved") {
                assertEquals("2023-06-15", result.appInstalledAtString)
            }
        }

    // -----------------------------------------------------------------------
    // Updates.SeedResolved
    // -----------------------------------------------------------------------

    @Test
    fun `SeedResolved - updates seed and resolves Pending Seed`() =
        Given("a state with Seed pending") {
            val state = pendingState(Pending.Seed, appUserId = "user-1", seed = 42)

            val result = When("SeedResolved is applied") {
                Updates.SeedResolved(seed = 77).reduce(state)
            }

            Then("seed is updated") {
                assertEquals(77, result.seed)
            }
            And("state is Ready (no other pending items)") {
                assertTrue(result.isReady)
            }
            And("seed is in userAttributes") {
                assertEquals(77, result.userAttributes[Keys.SEED])
            }
        }

    @Test
    fun `SeedResolved - preserves other pending items`() =
        Given("a state with Seed and Assignments pending") {
            val state = pendingState(Pending.Seed, Pending.Assignments, appUserId = "user-1")

            val result = When("SeedResolved is applied") {
                Updates.SeedResolved(seed = 55).reduce(state)
            }

            Then("Seed is resolved but Assignments remains") {
                assertEquals(Phase.Pending(setOf(Pending.Assignments)), result.phase)
            }
            And("state is not ready") {
                assertFalse(result.isReady)
            }
        }

    // -----------------------------------------------------------------------
    // Updates.SeedSkipped
    // -----------------------------------------------------------------------

    @Test
    fun `SeedSkipped - resolves Pending Seed without changing seed value`() =
        Given("a state with Seed pending") {
            val state = pendingState(Pending.Seed, seed = 42)

            val result = When("SeedSkipped is applied") {
                Updates.SeedSkipped.reduce(state)
            }

            Then("seed is unchanged") {
                assertEquals(42, result.seed)
            }
            And("state is Ready") {
                assertTrue(result.isReady)
            }
        }

    @Test
    fun `SeedSkipped - preserves other pending items`() =
        Given("a state with Seed and Assignments pending") {
            val state = pendingState(Pending.Seed, Pending.Assignments)

            val result = When("SeedSkipped is applied") {
                Updates.SeedSkipped.reduce(state)
            }

            Then("only Assignments remains pending") {
                assertEquals(Phase.Pending(setOf(Pending.Assignments)), result.phase)
            }
        }

    // -----------------------------------------------------------------------
    // Updates.AttributesMerged
    // -----------------------------------------------------------------------

    @Test
    fun `AttributesMerged - adds new attributes`() =
        Given("a state with existing attributes") {
            val state = readyState(userAttributes = mapOf("existing" to "value"))

            val result = When("new attributes are merged") {
                Updates.AttributesMerged(mapOf("new_key" to "new_value")).reduce(state)
            }

            Then("both old and new attributes are present") {
                assertEquals("value", result.userAttributes["existing"])
                assertEquals("new_value", result.userAttributes["new_key"])
            }
        }

    @Test
    fun `AttributesMerged - null removes attribute`() =
        Given("a state with an attribute") {
            val state = readyState(userAttributes = mapOf("name" to "John", "age" to 30))

            val result = When("attribute is set to null") {
                Updates.AttributesMerged(mapOf("name" to null)).reduce(state)
            }

            Then("attribute is removed") {
                assertFalse(result.userAttributes.containsKey("name"))
            }
            And("other attributes remain") {
                assertEquals(30, result.userAttributes["age"])
            }
        }

    @Test
    fun `AttributesMerged - does not change phase`() =
        Given("a pending state") {
            val state = pendingState(Pending.Configuration)

            val result = When("attributes are merged") {
                Updates.AttributesMerged(mapOf("key" to "val")).reduce(state)
            }

            Then("phase is unchanged") {
                assertEquals(Phase.Pending(setOf(Pending.Configuration)), result.phase)
            }
        }

    // -----------------------------------------------------------------------
    // Updates.AssignmentsCompleted
    // -----------------------------------------------------------------------

    @Test
    fun `AssignmentsCompleted - resolves Pending Assignments`() =
        Given("a state with only Assignments pending") {
            val state = pendingState(Pending.Assignments)

            val result = When("AssignmentsCompleted is applied") {
                Updates.AssignmentsCompleted.reduce(state)
            }

            Then("state is Ready") {
                assertTrue(result.isReady)
            }
        }

    @Test
    fun `AssignmentsCompleted - no-op when Assignments not pending`() =
        Given("a Ready state") {
            val state = readyState()

            val result = When("AssignmentsCompleted is applied") {
                Updates.AssignmentsCompleted.reduce(state)
            }

            Then("state is unchanged") {
                assertEquals(state, result)
            }
        }

    @Test
    fun `AssignmentsCompleted - preserves other pending items`() =
        Given("a state with Seed and Assignments pending") {
            val state = pendingState(Pending.Seed, Pending.Assignments)

            val result = When("AssignmentsCompleted is applied") {
                Updates.AssignmentsCompleted.reduce(state)
            }

            Then("only Seed remains pending") {
                assertEquals(Phase.Pending(setOf(Pending.Seed)), result.phase)
            }
        }

    // -----------------------------------------------------------------------
    // Updates.Configure
    // -----------------------------------------------------------------------

    @Test
    fun `Configure - resolves Configuration when no assignments needed`() =
        Given("initial state with Configuration pending") {
            val state = pendingState(Pending.Configuration)

            val result = When("Configure is applied with needsAssignments = false") {
                Updates.Configure(needsAssignments = false).reduce(state)
            }

            Then("state is Ready") {
                assertTrue(result.isReady)
            }
        }

    @Test
    fun `Configure - adds Assignments when needed`() =
        Given("initial state with Configuration pending") {
            val state = pendingState(Pending.Configuration)

            val result = When("Configure is applied with needsAssignments = true") {
                Updates.Configure(needsAssignments = true).reduce(state)
            }

            Then("Configuration is resolved and Assignments is added") {
                assertEquals(Phase.Pending(setOf(Pending.Assignments)), result.phase)
            }
        }

    @Test
    fun `Configure - preserves existing Seed pending from concurrent identify`() =
        Given("a state with both Configuration and Seed pending (identify ran before config)") {
            val state = pendingState(Pending.Configuration, Pending.Seed)

            val result = When("Configure is applied with needsAssignments = false") {
                Updates.Configure(needsAssignments = false).reduce(state)
            }

            Then("Configuration is resolved but Seed remains") {
                assertEquals(Phase.Pending(setOf(Pending.Seed)), result.phase)
            }
            And("state is not ready") {
                assertFalse(result.isReady)
            }
        }

    @Test
    fun `Configure - preserves Seed and adds Assignments`() =
        Given("a state with Configuration and Seed pending") {
            val state = pendingState(Pending.Configuration, Pending.Seed)

            val result = When("Configure is applied with needsAssignments = true") {
                Updates.Configure(needsAssignments = true).reduce(state)
            }

            Then("both Seed and Assignments are pending") {
                assertEquals(
                    Phase.Pending(setOf(Pending.Seed, Pending.Assignments)),
                    result.phase,
                )
            }
        }

    @Test
    fun `Configure - on already Ready state with needsAssignments adds Assignments`() =
        Given("a Ready state (Configure already ran or not applicable)") {
            val state = readyState()

            val result = When("Configure is applied with needsAssignments = true") {
                Updates.Configure(needsAssignments = true).reduce(state)
            }

            Then("Assignments is pending") {
                assertEquals(Phase.Pending(setOf(Pending.Assignments)), result.phase)
            }
        }

    @Test
    fun `Configure - on already Ready state without assignments is no-op`() =
        Given("a Ready state") {
            val state = readyState()

            val result = When("Configure is applied with needsAssignments = false") {
                Updates.Configure(needsAssignments = false).reduce(state)
            }

            Then("state remains Ready") {
                assertTrue(result.isReady)
            }
        }

    // -----------------------------------------------------------------------
    // Updates.Reset
    // -----------------------------------------------------------------------

    @Test
    fun `Reset - creates fresh identity and preserves readiness`() =
        Given("a logged-in state with attributes") {
            val state = readyState(
                appUserId = "user-1",
                aliasId = "alias-1",
                seed = 42,
                userAttributes = mapOf("custom" to "value"),
            )

            val result = When("Reset is applied") {
                Updates.Reset.reduce(state)
            }

            Then("appUserId is cleared") {
                assertNull(result.appUserId)
            }
            And("aliasId is regenerated") {
                assertNotEquals("alias-1", result.aliasId)
            }
            And("seed is regenerated") {
                // Can't assert exact value since it's random, but it exists
                assertTrue(result.seed in 0..99)
            }
            And("custom attributes are cleared") {
                assertNull(result.userAttributes["custom"])
            }
            And("phase stays Ready") {
                assertEquals(Phase.Ready, result.phase)
                assertTrue(result.isReady)
            }
            And("appInstalledAtString is preserved") {
                assertEquals("2024-01-01", result.appInstalledAtString)
            }
            And("aliasId is in userAttributes") {
                assertEquals(result.aliasId, result.userAttributes[Keys.ALIAS_ID])
            }
        }

    @Test
    fun `Reset - from anonymous state also generates fresh identity`() =
        Given("an anonymous state") {
            val state = readyState(aliasId = "old-alias")

            val result = When("Reset is applied") {
                Updates.Reset.reduce(state)
            }

            Then("aliasId is regenerated") {
                assertNotEquals("old-alias", result.aliasId)
            }
            And("state remains ready") {
                assertTrue(result.isReady)
            }
        }

    // -----------------------------------------------------------------------
    // IdentityState helpers
    // -----------------------------------------------------------------------

    @Test
    fun `resolve - removes item and transitions to Ready when last item`() =
        Given("a state with a single pending item") {
            val state = pendingState(Pending.Seed)

            val result = When("that item is resolved") {
                state.resolve(Pending.Seed)
            }

            Then("state is Ready") {
                assertTrue(result.isReady)
            }
        }

    @Test
    fun `resolve - removes item but stays Pending when others remain`() =
        Given("a state with multiple pending items") {
            val state = pendingState(Pending.Seed, Pending.Assignments, Pending.Configuration)

            val result = When("one item is resolved") {
                state.resolve(Pending.Seed)
            }

            Then("remaining items are still pending") {
                assertEquals(
                    Phase.Pending(setOf(Pending.Assignments, Pending.Configuration)),
                    result.phase,
                )
            }
        }

    @Test
    fun `resolve - no-op when item not in pending set`() =
        Given("a state without Assignments pending") {
            val state = pendingState(Pending.Seed)

            val result = When("Assignments is resolved") {
                state.resolve(Pending.Assignments)
            }

            Then("state is unchanged (Seed still pending)") {
                assertEquals(Phase.Pending(setOf(Pending.Seed)), result.phase)
            }
        }

    @Test
    fun `resolve - no-op on Ready state`() =
        Given("a Ready state") {
            val state = readyState()

            val result = When("any item is resolved") {
                state.resolve(Pending.Seed)
            }

            Then("state is unchanged") {
                assertEquals(state, result)
            }
        }

    @Test
    fun `enrichedAttributes - always includes userId and aliasId`() =
        Given("a state with minimal attributes") {
            val state = readyState(
                appUserId = "user-1",
                aliasId = "alias-1",
                userAttributes = mapOf("custom" to "value"),
            )

            val enriched = When("enrichedAttributes is read") {
                state.enrichedAttributes
            }

            Then("it contains custom attributes") {
                assertEquals("value", enriched["custom"])
            }
            And("it contains appUserId") {
                assertEquals("user-1", enriched[Keys.APP_USER_ID])
            }
            And("it contains aliasId") {
                assertEquals("alias-1", enriched[Keys.ALIAS_ID])
            }
        }

    @Test
    fun `enrichedAttributes - omits appUserId when anonymous`() =
        Given("an anonymous state") {
            val state = readyState(appUserId = null, aliasId = "alias-1")

            val enriched = When("enrichedAttributes is read") {
                state.enrichedAttributes
            }

            Then("appUserId key is absent (matches iOS behavior)") {
                assertFalse(
                    "anonymous user_attributes must not inject appUserId fallback",
                    enriched.containsKey(Keys.APP_USER_ID),
                )
            }
            And("aliasId is still present") {
                assertEquals("alias-1", enriched[Keys.ALIAS_ID])
            }
        }

    // -----------------------------------------------------------------------
    // Composition: multi-step reducer sequences
    // -----------------------------------------------------------------------

    @Test
    fun `full identify flow - Identify then SeedResolved reaches Ready`() =
        Given("an anonymous ready state") {
            val initial = readyState()

            val afterIdentify = When("Identify is applied") {
                Updates.Identify("user-1", restoreAssignments = false).reduce(initial)
            }

            Then("state is Pending Seed") {
                assertEquals(Phase.Pending(setOf(Pending.Seed)), afterIdentify.phase)
            }

            val afterSeed = When("SeedResolved is applied") {
                Updates.SeedResolved(seed = 77).reduce(afterIdentify)
            }

            Then("state is Ready") {
                assertTrue(afterSeed.isReady)
            }
            And("seed is updated") {
                assertEquals(77, afterSeed.seed)
            }
        }

    @Test
    fun `full identify flow with restore - needs both Seed and Assignments`() =
        Given("an anonymous ready state") {
            val initial = readyState()

            val afterIdentify = When("Identify with restoreAssignments is applied") {
                Updates.Identify("user-1", restoreAssignments = true).reduce(initial)
            }

            Then("both Seed and Assignments are pending") {
                assertEquals(
                    Phase.Pending(setOf(Pending.Seed, Pending.Assignments)),
                    afterIdentify.phase,
                )
            }

            val afterSeed = When("SeedResolved is applied") {
                Updates.SeedResolved(seed = 50).reduce(afterIdentify)
            }

            Then("still not ready — Assignments pending") {
                assertFalse(afterSeed.isReady)
                assertEquals(Phase.Pending(setOf(Pending.Assignments)), afterSeed.phase)
            }

            val afterAssignments = When("AssignmentsCompleted is applied") {
                Updates.AssignmentsCompleted.reduce(afterSeed)
            }

            Then("now Ready") {
                assertTrue(afterAssignments.isReady)
            }
        }

    @Test
    fun `configure then identify race - Configure preserves Seed from concurrent identify`() =
        Given("initial Pending Configuration state") {
            val initial = pendingState(Pending.Configuration)

            // Simulate: identify runs before configure
            val afterIdentify = When("Identify runs (adding Seed) while Configuration still pending") {
                // This simulates the state after identify runs but before configure
                initial.copy(
                    phase = Phase.Pending(setOf(Pending.Configuration, Pending.Seed)),
                    appUserId = "user-1",
                )
            }

            val afterConfigure = When("Configure runs") {
                Updates.Configure(needsAssignments = false).reduce(afterIdentify)
            }

            Then("Seed is preserved, Configuration is resolved") {
                assertEquals(Phase.Pending(setOf(Pending.Seed)), afterConfigure.phase)
                assertFalse(afterConfigure.isReady)
            }
        }

    @Test
    fun `reset preserves existing pending markers`() =
        Given("a logged-in ready state") {
            val initial =
                readyState(appUserId = "user-1").copy(
                    phase = Phase.Pending(setOf(Pending.Reset)),
                )

            val afterReset = When("Reset is applied") {
                Updates.Reset.reduce(initial)
            }

            Then("reset pending is preserved") {
                assertEquals(Phase.Pending(setOf(Pending.Reset)), afterReset.phase)
            }
            And("user is cleared") {
                assertNull(afterReset.appUserId)
            }
        }
}
