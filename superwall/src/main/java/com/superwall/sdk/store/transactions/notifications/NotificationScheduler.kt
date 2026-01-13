package com.superwall.sdk.store.transactions.notifications

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.superwall.sdk.dependencies.DeviceHelperFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.paywall.LocalNotification
import com.superwall.sdk.paywall.view.SuperwallPaywallActivity
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

internal class NotificationScheduler {
    companion object {
        private const val TAG_PREFIX = "superwall_notification_"

        /**
         * Cancels any pending notification with the given ID.
         */
        fun cancelNotificationById(
            id: String,
            context: Context,
        ) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(id)
            Logger.debug(
                logLevel = LogLevel.debug,
                scope = LogScope.paywallView,
                message = "Cancelled pending notification with tag: $id",
            )
        }

        suspend fun Context.existsWithTag(id: String): Boolean {
            val workManager = WorkManager.getInstance(this)
            return workManager.getWorkInfosByTag(id).await().isNotEmpty()
        }

        fun scheduleNotifications(
            notifications: List<LocalNotification>,
            factory: DeviceHelperFactory,
            context: Context,
            cancelExisting: Boolean = false,
        ) {
            val workManager = WorkManager.getInstance(context)
            IOScope().launch {
                notifications.forEach { notification ->
                    // Cancel existing notification with the same ID if requested
                    if (cancelExisting) {
                        cancelNotificationById(notification.id, context)
                    } else if (context.existsWithTag(notification.id)) {
                        return@forEach
                    }

                    val data =
                        workDataOf(
                            "id" to notification.id,
                            "title" to notification.title,
                            "body" to notification.body,
                            "subtitle" to notification.subtitle,
                        )

                    var delay = notification.delay // delay in milliseconds

                    val isSandbox = factory.makeIsSandbox()
                    if (isSandbox) {
                        delay = delay / 24 / 60
                    }

                    if (delay <= 0) {
                        Logger.debug(
                            logLevel = LogLevel.error,
                            scope = LogScope.paywallView,
                            message =
                                "Notification delay isn't greater than 0 milliseconds. " +
                                    "Notifications will not be scheduled.",
                        )
                        return@forEach
                    }

                    val notificationWork =
                        OneTimeWorkRequestBuilder<NotificationWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(data)
                            .addTag(SuperwallPaywallActivity.NOTIFICATION_CHANNEL_ID)
                            .addTag(notification.id) // Add ID-specific tag for cancellation
                            .build()

                    // Try canceling again just in case it was made
                    if (cancelExisting) {
                        cancelNotificationById(notification.id, context)
                    }
                    workManager.enqueue(notificationWork)

                    Logger.debug(
                        logLevel = LogLevel.debug,
                        scope = LogScope.paywallView,
                        message = "Scheduled notification with tag: ${notification.id}, delay: ${delay}ms",
                    )
                }
            }
        }
    }
}
