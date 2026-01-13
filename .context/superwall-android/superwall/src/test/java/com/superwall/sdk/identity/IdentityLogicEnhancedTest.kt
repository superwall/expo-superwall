package com.superwall.sdk.identity

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityLogicEnhancedTest {
    @Test
    fun `generateAlias creates unique alias with correct format`() =
        Given("the identity logic") {
            When("generating an alias") {
                val alias = IdentityLogic.generateAlias()

                Then("it should start with dollar SuperwallAlias prefix") {
                    assertTrue(alias.startsWith("\$SuperwallAlias:"))
                }

                Then("it should contain a UUID") {
                    val uuidPart = alias.substringAfter("\$SuperwallAlias:")
                    assertTrue(uuidPart.isNotEmpty())
                    assertTrue(uuidPart.contains("-"))
                }
            }
        }

    @Test
    fun `generateAlias creates unique values each time`() {
        val alias1 = IdentityLogic.generateAlias()
        val alias2 = IdentityLogic.generateAlias()

        assertFalse(alias1 == alias2)
    }

    @Test
    fun `generateSeed returns value between 0 and 99`() =
        Given("the identity logic") {
            When("generating seeds multiple times") {
                val seeds = (1..100).map { IdentityLogic.generateSeed() }

                Then("all seeds should be in range 0-99") {
                    seeds.forEach { seed ->
                        assertTrue(seed >= 0)
                        assertTrue(seed < 100)
                    }
                }
            }
        }

    @Test
    fun `mergeAttributes merges new attributes with old`() =
        Given("old attributes") {
            val oldAttributes =
                mapOf(
                    "name" to "John",
                    "age" to 30,
                )

            When("merging new attributes") {
                val newAttributes =
                    mapOf(
                        "age" to 31,
                        "city" to "New York",
                    )

                val result =
                    IdentityLogic.mergeAttributes(
                        newAttributes,
                        oldAttributes,
                        "2024-01-01",
                    )

                Then("old attribute is preserved") {
                    assertEquals("John", result["name"])
                }

                Then("updated attribute has new value") {
                    assertEquals(31, result["age"])
                }

                Then("new attribute is added") {
                    assertEquals("New York", result["city"])
                }

                Then("applicationInstalledAt is added") {
                    assertEquals("2024-01-01", result["applicationInstalledAt"])
                }
            }
        }

    @Test
    fun `mergeAttributes removes dollar signs from keys`() {
        val newAttributes = mapOf("\$customAttribute" to "value")

        val result =
            IdentityLogic.mergeAttributes(
                newAttributes,
                emptyMap(),
                "2024-01-01",
            )

        assertTrue(result.containsKey("customAttribute"))
        assertFalse(result.containsKey("\$customAttribute"))
    }

    @Test
    fun `mergeAttributes filters null values from lists`() {
        val newAttributes = mapOf("items" to listOf("a", null, "b", null, "c"))

        val result =
            IdentityLogic.mergeAttributes(
                newAttributes,
                emptyMap(),
                "2024-01-01",
            )

        val items = result["items"] as List<*>
        assertEquals(listOf("a", "b", "c"), items)
    }

    @Test
    fun `mergeAttributes filters null values from maps`() {
        val newAttributes =
            mapOf(
                "metadata" to
                    mapOf(
                        "key1" to "value1",
                        "key2" to null,
                        "key3" to "value3",
                    ),
            )

        val result =
            IdentityLogic.mergeAttributes(
                newAttributes,
                emptyMap(),
                "2024-01-01",
            )

        @Suppress("UNCHECKED_CAST")
        val metadata = result["metadata"] as Map<*, *>
        assertTrue(metadata.containsKey("key1"))
        assertFalse(metadata.containsKey("key2"))
        assertTrue(metadata.containsKey("key3"))
    }

    @Test
    fun `mergeAttributes removes attribute when value is null`() {
        val oldAttributes = mapOf("name" to "John", "age" to 30)
        val newAttributes = mapOf("name" to null)

        val result =
            IdentityLogic.mergeAttributes(
                newAttributes,
                oldAttributes,
                "2024-01-01",
            )

        assertFalse(result.containsKey("name"))
        assertTrue(result.containsKey("age"))
    }

    @Test
    fun `mergeAttributes ignores is_standard_event key`() {
        val newAttributes = mapOf("\$is_standard_event" to true, "custom" to "value")

        val result =
            IdentityLogic.mergeAttributes(
                newAttributes,
                emptyMap(),
                "2024-01-01",
            )

        assertFalse(result.containsKey("\$is_standard_event"))
        assertFalse(result.containsKey("is_standard_event"))
        assertTrue(result.containsKey("custom"))
    }

    @Test
    fun `mergeAttributes ignores application_installed_at from new attributes`() {
        val newAttributes = mapOf("\$application_installed_at" to "2024-01-01", "custom" to "value")

        val result =
            IdentityLogic.mergeAttributes(
                newAttributes,
                emptyMap(),
                "2024-06-01",
            )

        // Should use the appInstalledAtString parameter, not the one from attributes
        assertEquals("2024-06-01", result["applicationInstalledAt"])
        assertFalse(result.containsKey("\$application_installed_at"))
    }

    @Test
    fun `sanitize removes whitespace and newlines`() {
        val userId = "  user123  \n\t"

        val result = IdentityLogic.sanitize(userId)

        assertEquals("user123", result)
    }

    @Test
    fun `sanitize returns null for empty string after trimming`() {
        val userId = "   \n\t   "

        val result = IdentityLogic.sanitize(userId)

        assertNull(result)
    }

    @Test
    fun `sanitize returns null for empty string`() {
        val result = IdentityLogic.sanitize("")

        assertNull(result)
    }

    @Test
    fun `sanitize preserves valid userId`() {
        val userId = "user@example.com"

        val result = IdentityLogic.sanitize(userId)

        assertEquals("user@example.com", result)
    }

    @Test
    fun `shouldGetAssignments returns false for anonymous first app open post static config`() {
        val result =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = false,
                neverCalledStaticConfig = false,
                isFirstAppOpen = true,
            )

        assertFalse(result)
    }

    @Test
    fun `shouldGetAssignments returns false for logged in post static config`() {
        val result =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = true,
                neverCalledStaticConfig = false,
                isFirstAppOpen = false,
            )

        assertFalse(result)
    }

    @Test
    fun `shouldGetAssignments returns true for logged in pre static config`() {
        val result =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = true,
                neverCalledStaticConfig = true,
                isFirstAppOpen = false,
            )

        assertTrue(result)
    }

    @Test
    fun `shouldGetAssignments returns true for anonymous not first open pre static config`() {
        val result =
            IdentityLogic.shouldGetAssignments(
                isLoggedIn = false,
                neverCalledStaticConfig = true,
                isFirstAppOpen = false,
            )

        assertTrue(result)
    }
}
