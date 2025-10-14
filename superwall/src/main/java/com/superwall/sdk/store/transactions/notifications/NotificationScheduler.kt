package com.superwall.sdk.store.transactions.notifications

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.superwall.sdk.dependencies.DeviceHelperFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.paywall.LocalNotification
import com.superwall.sdk.paywall.view.SuperwallPaywallActivity
import java.util.concurrent.TimeUnit

internal class NotificationScheduler {
    companion object {
        fun scheduleNotifications(
            notifications: List<LocalNotification>,
            factory: DeviceHelperFactory,
            context: Context,
        ) {
            val workManager = WorkManager.getInstance(context)

            notifications.forEach { notification ->
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
                    return
                }

                val notificationWork =
                    OneTimeWorkRequestBuilder<NotificationWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data)
                        .addTag(SuperwallPaywallActivity.NOTIFICATION_CHANNEL_ID)
                        .build()

                workManager.enqueue(notificationWork)
            }
        }
    }
}
