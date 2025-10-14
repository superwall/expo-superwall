package com.superwall.sdk.web

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import com.superwall.sdk.Superwall
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger

fun Superwall.openRestoreOnWeb() {
    val config =
        Superwall.instance.dependencyContainer.configManager.config
            ?.webToAppConfig ?: run {
            Logger.debug(
                LogLevel.error,
                LogScope.webEntitlements,
                "Configuration not available - cannot restore purchases",
            )
            return
        }

    Superwall.instance.dependencyContainer.activityProvider?.getCurrentActivity()?.let {
        it.startActivity(
            Intent(
                ACTION_VIEW,
                Uri.parse(
                    config.restoreAccesUrl,
                ),
            ),
        )
    }
}
