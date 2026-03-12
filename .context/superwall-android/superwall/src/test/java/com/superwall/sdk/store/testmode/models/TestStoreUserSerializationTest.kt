@file:Suppress("ktlint:standard:function-naming")

package com.superwall.sdk.store.testmode.models

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TestStoreUserSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `deserializes userId type`() {
        Given("a JSON test user with userId type") {
            val jsonString = """{ "type": "userId", "value": "user-123" }"""

            When("it is deserialized") {
                val user = json.decodeFromString<TestStoreUser>(jsonString)

                Then("type is UserId and value matches") {
                    assertEquals(TestStoreUserType.UserId, user.type)
                    assertEquals("user-123", user.value)
                }
            }
        }
    }

    @Test
    fun `deserializes aliasId type`() {
        Given("a JSON test user with aliasId type") {
            val jsonString = """{ "type": "aliasId", "value": "alias-456" }"""

            When("it is deserialized") {
                val user = json.decodeFromString<TestStoreUser>(jsonString)

                Then("type is AliasId and value matches") {
                    assertEquals(TestStoreUserType.AliasId, user.type)
                    assertEquals("alias-456", user.value)
                }
            }
        }
    }

    @Test
    fun `deserializes list of test users`() {
        Given("a JSON array of test users") {
            val jsonString =
                """
                [
                  { "type": "userId", "value": "user-A" },
                  { "type": "aliasId", "value": "alias-B" }
                ]
                """.trimIndent()

            When("the list is deserialized") {
                val users = json.decodeFromString<List<TestStoreUser>>(jsonString)

                Then("both users are parsed correctly") {
                    assertEquals(2, users.size)
                    assertEquals(TestStoreUserType.UserId, users[0].type)
                    assertEquals("user-A", users[0].value)
                    assertEquals(TestStoreUserType.AliasId, users[1].type)
                    assertEquals("alias-B", users[1].value)
                }
            }
        }
    }
}
