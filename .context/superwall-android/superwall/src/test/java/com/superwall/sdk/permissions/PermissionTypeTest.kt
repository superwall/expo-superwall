package com.superwall.sdk.permissions

import android.Manifest
import android.os.Build
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionTypeTest {
    @Test
    fun fromRaw_notification_returns_NOTIFICATION() {
        Given("a raw permission type string 'notification'") {
            val raw = "notification"

            When("converting from raw") {
                val result = PermissionType.fromRaw(raw)

                Then("it returns NOTIFICATION") {
                    assertEquals(PermissionType.NOTIFICATION, result)
                }
            }
        }
    }

    @Test
    fun fromRaw_unknown_returns_null() {
        Given("an unknown raw permission type string") {
            val raw = "unknown_permission"

            When("converting from raw") {
                val result = PermissionType.fromRaw(raw)

                Then("it returns null") {
                    assertNull(result)
                }
            }
        }
    }

    @Test
    fun rawValue_notification_is_correct() {
        Given("the NOTIFICATION permission type") {
            val permissionType = PermissionType.NOTIFICATION

            When("getting the raw value") {
                val raw = permissionType.rawValue

                Then("it returns 'notification'") {
                    assertEquals("notification", raw)
                }
            }
        }
    }

    @Test
    fun toManifestPermission_notification_on_api33_plus_returns_POST_NOTIFICATIONS() {
        Given("the NOTIFICATION permission type") {
            val permissionType = PermissionType.NOTIFICATION

            When("getting the manifest permission") {
                val manifestPermission = permissionType.toManifestPermission()

                Then("on API 33+ it should return POST_NOTIFICATIONS or null on older APIs") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        assertEquals(Manifest.permission.POST_NOTIFICATIONS, manifestPermission)
                    } else {
                        assertNull(manifestPermission)
                    }
                }
            }
        }
    }
}
