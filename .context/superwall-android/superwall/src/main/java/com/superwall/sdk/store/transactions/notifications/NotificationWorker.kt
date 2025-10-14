package com.superwall.sdk.store.transactions.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.superwall.sdk.paywall.view.SuperwallPaywallActivity

internal class NotificationWorker(
    val context: Context,
    workerParams: WorkerParameters,
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val notificationId = inputData.getInt("id", 0)
        val title = inputData.getString("title")
        val text = inputData.getString("body")

        val builder =
            NotificationCompat
                .Builder(applicationContext, SuperwallPaywallActivity.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(context.applicationInfo.icon)
                .setContentTitle(title)
                .let {
                    inputData.getString("subtitle")?.let { subtitle ->
                        it.setSubText(subtitle)
                    } ?: it
                }.setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(applicationContext)) {
            // This will always succeed, we just need to add this check for permissions before
            // continuing otherwise the compiler will mess up on the CI.
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return Result.failure()
            }
            notify(notificationId, builder.build())
        }

        return Result.success()
    }
}
