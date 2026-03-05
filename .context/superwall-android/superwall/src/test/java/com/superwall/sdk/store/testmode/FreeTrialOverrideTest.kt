@file:Suppress("ktlint:standard:function-naming")

package com.superwall.sdk.store.testmode

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import org.junit.Assert.assertEquals
import org.junit.Test

class FreeTrialOverrideTest {
    @Test
    fun `UseDefault has correct display name`() {
        Given("a UseDefault override") {
            Then("displayName is 'Use Default'") {
                assertEquals("Use Default", FreeTrialOverride.UseDefault.displayName)
            }
        }
    }

    @Test
    fun `ForceAvailable has correct display name`() {
        Given("a ForceAvailable override") {
            Then("displayName is 'Force Available'") {
                assertEquals("Force Available", FreeTrialOverride.ForceAvailable.displayName)
            }
        }
    }

    @Test
    fun `ForceUnavailable has correct display name`() {
        Given("a ForceUnavailable override") {
            Then("displayName is 'Force Unavailable'") {
                assertEquals("Force Unavailable", FreeTrialOverride.ForceUnavailable.displayName)
            }
        }
    }

    @Test
    fun `enum has exactly three values`() {
        Given("the FreeTrialOverride enum") {
            Then("it has three values") {
                assertEquals(3, FreeTrialOverride.entries.size)
            }
        }
    }
}
