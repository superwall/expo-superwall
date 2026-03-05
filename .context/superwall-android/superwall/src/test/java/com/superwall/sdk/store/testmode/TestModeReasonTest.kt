@file:Suppress("ktlint:standard:function-naming")

package com.superwall.sdk.store.testmode

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import org.junit.Assert.assertTrue
import org.junit.Test

class TestModeReasonTest {
    @Test
    fun `ConfigMatch description includes matched ID`() {
        Given("a ConfigMatch reason") {
            val reason = TestModeReason.ConfigMatch(matchedId = "test-user-123")

            Then("description includes the matched ID") {
                assertTrue(reason.description.contains("test-user-123"))
            }
        }
    }

    @Test
    fun `ApplicationIdMismatch description includes expected and actual`() {
        Given("a ApplicationIdMismatch reason") {
            val reason = TestModeReason.ApplicationIdMismatch(expected = "com.expected", actual = "com.actual")

            Then("description includes both bundle IDs") {
                assertTrue(reason.description.contains("com.expected"))
                assertTrue(reason.description.contains("com.actual"))
            }
        }
    }

    @Test
    fun `TestModeOption has descriptive text`() {
        Given("a TestModeOption reason") {
            val reason = TestModeReason.TestModeOption

            Then("description is not empty") {
                assertTrue(reason.description.isNotEmpty())
                assertTrue(reason.description.contains("SuperwallOptions"))
            }
        }
    }
}
