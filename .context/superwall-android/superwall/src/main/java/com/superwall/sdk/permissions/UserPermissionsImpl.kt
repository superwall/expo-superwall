package com.superwall.sdk.permissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementation of UserPermissions using Android's permission system
 */
internal class UserPermissionsImpl(
    private val context: Context,
) : UserPermissions {
    override fun hasPermission(permission: PermissionType): PermissionStatus =
        when (permission) {
            PermissionType.NOTIFICATION -> checkNotificationPermission()
            PermissionType.LOCATION,
            PermissionType.READ_IMAGES,
            PermissionType.CONTACTS,
            PermissionType.READ_VIDEO,
            PermissionType.CAMERA,
            -> checkRuntimePermission(permission)
            PermissionType.BACKGROUND_LOCATION -> checkBackgroundLocationPermission()
        }

    private fun checkNotificationPermission(): PermissionStatus {
        // On API 33+, check the runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            return if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
        }

        // On older APIs, check if notifications are enabled in system settings
        val notificationManager = NotificationManagerCompat.from(context)
        return if (notificationManager.areNotificationsEnabled()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    private fun checkRuntimePermission(permission: PermissionType): PermissionStatus {
        val manifestPermission =
            permission.toManifestPermission()
                ?: return PermissionStatus.UNSUPPORTED

        val granted =
            ContextCompat.checkSelfPermission(
                context,
                manifestPermission,
            ) == PackageManager.PERMISSION_GRANTED
        return if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    private fun checkBackgroundLocationPermission(): PermissionStatus {
        // Background location requires API 29+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return PermissionStatus.UNSUPPORTED
        }

        // First check if foreground location is granted
        val foregroundGranted =
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        if (!foregroundGranted) {
            return PermissionStatus.DENIED
        }

        // Then check background location
        val backgroundGranted =
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        return if (backgroundGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    override suspend fun requestPermission(
        activity: Activity,
        permission: PermissionType,
    ): PermissionStatus =
        when (permission) {
            PermissionType.NOTIFICATION -> requestNotificationPermission(activity)
            PermissionType.BACKGROUND_LOCATION -> requestBackgroundLocationPermission(activity)
            PermissionType.LOCATION,
            PermissionType.READ_IMAGES,
            PermissionType.CONTACTS,
            PermissionType.READ_VIDEO,
            PermissionType.CAMERA,
            -> requestStandardPermission(activity, permission)
        }

    private suspend fun requestNotificationPermission(activity: Activity): PermissionStatus {
        // Check current status first
        val currentStatus = hasPermission(PermissionType.NOTIFICATION)
        if (currentStatus == PermissionStatus.GRANTED) {
            return PermissionStatus.GRANTED
        }

        // On API < 33, we can't request notification permission at runtime
        // The user needs to enable it in system settings
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Can't request at runtime - return current status
            return currentStatus
        }

        // On API 33+, request the POST_NOTIFICATIONS permission
        val manifestPermission =
            PermissionType.NOTIFICATION.toManifestPermission()
                ?: return PermissionStatus.UNSUPPORTED

        return requestRuntimePermission(activity, manifestPermission)
    }

    private suspend fun requestStandardPermission(
        activity: Activity,
        permission: PermissionType,
    ): PermissionStatus {
        // Check current status first
        val currentStatus = hasPermission(permission)
        if (currentStatus == PermissionStatus.GRANTED) {
            return PermissionStatus.GRANTED
        }

        val manifestPermission =
            permission.toManifestPermission()
                ?: return PermissionStatus.UNSUPPORTED

        return requestRuntimePermission(activity, manifestPermission)
    }

    private suspend fun requestBackgroundLocationPermission(activity: Activity): PermissionStatus {
        // Background location requires API 29+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return PermissionStatus.UNSUPPORTED
        }

        // Check if already granted
        val currentStatus = hasPermission(PermissionType.BACKGROUND_LOCATION)
        if (currentStatus == PermissionStatus.GRANTED) {
            return PermissionStatus.GRANTED
        }

        // First ensure foreground location is granted
        val foregroundStatus = hasPermission(PermissionType.LOCATION)
        if (foregroundStatus != PermissionStatus.GRANTED) {
            // Request foreground location first
            val foregroundResult = requestStandardPermission(activity, PermissionType.LOCATION)
            if (foregroundResult != PermissionStatus.GRANTED) {
                return PermissionStatus.DENIED
            }
        }

        // Now request background location
        val manifestPermission =
            PermissionType.BACKGROUND_LOCATION.toManifestPermission()
                ?: return PermissionStatus.UNSUPPORTED

        return requestRuntimePermission(activity, manifestPermission)
    }

    private suspend fun requestRuntimePermission(
        activity: Activity,
        manifestPermission: String,
    ): PermissionStatus =
        suspendCancellableCoroutine { continuation ->
            // Check if we can use the modern ActivityResult API
            if (activity is ComponentActivity) {
                // Use a one-shot launcher pattern
                var launcher: ActivityResultLauncher<String>? = null
                launcher =
                    activity.activityResultRegistry.register(
                        "permission_request_${System.currentTimeMillis()}",
                        ActivityResultContracts.RequestPermission(),
                    ) { isGranted ->
                        launcher?.unregister()
                        val status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
                        if (continuation.isActive) {
                            continuation.resume(status)
                        }
                    }

                continuation.invokeOnCancellation {
                    launcher.unregister()
                }

                launcher.launch(manifestPermission)
            } else {
                // Fallback for non-ComponentActivity (legacy approach)
                // This won't wait for result, so we can only check current state
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(manifestPermission),
                    PERMISSION_REQUEST_CODE,
                )

                // Since we can't wait for result with legacy API, check after a delay
                // This is not ideal but provides fallback compatibility
                val isGranted =
                    ContextCompat.checkSelfPermission(
                        activity,
                        manifestPermission,
                    ) == PackageManager.PERMISSION_GRANTED

                if (continuation.isActive) {
                    continuation.resume(if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED)
                }
            }
        }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 10001
    }
}
