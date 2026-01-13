package com.superwall.sdk.permissions

import android.Manifest
import android.os.Build

/**
 * Permission types that can be requested from the host app.
 * Maps to the paywall schema permission_type values.
 */
enum class PermissionType(
    val rawValue: String,
) {
    NOTIFICATION("notification"),
    LOCATION("location"),
    BACKGROUND_LOCATION("background_location"),
    READ_IMAGES("read_images"),
    CONTACTS("contacts"),
    READ_VIDEO("read_video"),
    CAMERA("camera"),
    ;

    /**
     * Get the Android manifest permission string for this permission type.
     * Returns null if the permission is not available on the current API level.
     */
    fun toManifestPermission(): String? =
        when (this) {
            NOTIFICATION ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    null // Notifications don't require runtime permission before API 33
                }
            LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
            BACKGROUND_LOCATION ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                } else {
                    null // Background location not available before API 29
                }
            READ_IMAGES ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            CONTACTS -> Manifest.permission.READ_CONTACTS
            READ_VIDEO ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_VIDEO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            CAMERA -> Manifest.permission.CAMERA
        }

    companion object {
        /**
         * Find a PermissionType by its raw value from the paywall schema
         * @param raw The permission type string (e.g., "notification")
         * @return The corresponding PermissionType or null if not found
         */
        fun fromRaw(raw: String): PermissionType? = values().find { it.rawValue == raw }
    }
}
