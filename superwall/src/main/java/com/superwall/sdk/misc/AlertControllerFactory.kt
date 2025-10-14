package com.superwall.sdk.misc

import android.app.AlertDialog
import android.content.Context

object AlertControllerFactory {
    data class AlertProps(
        val title: String? = null,
        val message: String? = null,
        val actionTitle: String? = null,
        val closeActionTitle: String = "Done",
        val action: (() -> Unit)? = null,
        val onClose: (() -> Unit)? = null,
    )

    fun make(
        context: Context,
        title: String? = null,
        message: String? = null,
        actionTitle: String? = null,
        closeActionTitle: String = "Done",
        action: (() -> Unit)? = null,
        onClose: (() -> Unit)? = null,
    ): AlertDialog {
        val builder =
            AlertDialog
                .Builder(context)
                .setTitle(title)
                .setMessage(message)

        if (actionTitle != null) {
            builder.setPositiveButton(actionTitle) { _, _ ->
                action?.invoke()
            }
        }

        builder.setNegativeButton(closeActionTitle) { _, _ ->
            onClose?.invoke()
        }

        return builder.create()
    }
}
