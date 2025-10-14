package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Superwall
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest

// File.kt

fun Superwall.logPresentation(
    request: PresentationRequest,
    message: String,
): Map<String, Any> {
    val eventData = request.presentationInfo.eventData
    val debugInfo: Map<String, Any> =
        mapOf(
            "on" to request.presenter.toString(),
            "fromEvent" to (eventData?.toString() ?: ""),
        )
    Logger.debug(
        logLevel = LogLevel.debug,
        scope = LogScope.paywallPresentation,
        message = message,
        info = debugInfo,
    )
    return debugInfo
}
