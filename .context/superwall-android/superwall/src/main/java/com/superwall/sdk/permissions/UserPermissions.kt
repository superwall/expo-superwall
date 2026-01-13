package com.superwall.sdk.permissions

import android.app.Activity

/**
 * Interface for managing user permissions in a testable way.
 * Handles permission requests from paywalls and returns results to be sent back.
 */
interface UserPermissions {
    /**
     * Check if a specific permission is granted
     * @param permission The permission to check
     * @return The current status of the permission
     */
    fun hasPermission(permission: PermissionType): PermissionStatus

    /**
     * Request a specific permission from the user
     * @param activity The activity to use for the permission request
     * @param permission The permission to request
     * @return The result of the permission request
     */
    suspend fun requestPermission(
        activity: Activity,
        permission: PermissionType,
    ): PermissionStatus
}
