package com.superwall.sdk.permissions

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionStatusTest {
    @Test
    fun fromRaw_granted_returns_GRANTED() {
        Given("a raw status string 'granted'") {
            val raw = "granted"

            When("converting from raw") {
                val result = PermissionStatus.fromRaw(raw)

                Then("it returns GRANTED") {
                    assertEquals(PermissionStatus.GRANTED, result)
                }
            }
        }
    }

    @Test
    fun fromRaw_denied_returns_DENIED() {
        Given("a raw status string 'denied'") {
            val raw = "denied"

            When("converting from raw") {
                val result = PermissionStatus.fromRaw(raw)

                Then("it returns DENIED") {
                    assertEquals(PermissionStatus.DENIED, result)
                }
            }
        }
    }

    @Test
    fun fromRaw_unsupported_returns_UNSUPPORTED() {
        Given("a raw status string 'unsupported'") {
            val raw = "unsupported"

            When("converting from raw") {
                val result = PermissionStatus.fromRaw(raw)

                Then("it returns UNSUPPORTED") {
                    assertEquals(PermissionStatus.UNSUPPORTED, result)
                }
            }
        }
    }

    @Test
    fun fromRaw_unknown_returns_null() {
        Given("an unknown raw status string") {
            val raw = "unknown_status"

            When("converting from raw") {
                val result = PermissionStatus.fromRaw(raw)

                Then("it returns null") {
                    assertNull(result)
                }
            }
        }
    }

    @Test
    fun rawValue_GRANTED_is_correct() {
        Given("the GRANTED status") {
            val status = PermissionStatus.GRANTED

            When("getting the raw value") {
                val raw = status.rawValue

                Then("it returns 'granted'") {
                    assertEquals("granted", raw)
                }
            }
        }
    }

    @Test
    fun rawValue_DENIED_is_correct() {
        Given("the DENIED status") {
            val status = PermissionStatus.DENIED

            When("getting the raw value") {
                val raw = status.rawValue

                Then("it returns 'denied'") {
                    assertEquals("denied", raw)
                }
            }
        }
    }

    @Test
    fun rawValue_UNSUPPORTED_is_correct() {
        Given("the UNSUPPORTED status") {
            val status = PermissionStatus.UNSUPPORTED

            When("getting the raw value") {
                val raw = status.rawValue

                Then("it returns 'unsupported'") {
                    assertEquals("unsupported", raw)
                }
            }
        }
    }
}
