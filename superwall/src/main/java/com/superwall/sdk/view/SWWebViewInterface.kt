package com.superwall.sdk.view

import android.content.Context
import android.webkit.JavascriptInterface
import com.superwall.sdk.deprecated.PaywallMessage
import com.superwall.sdk.deprecated.WrappedPaywallMessages
import com.superwall.sdk.deprecated.parseWrappedPaywallMessages
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger

interface PaywallMessageDelegate {
    fun didReceiveMessage(message: PaywallMessage)
}

class SWWebViewInterface(
    delegate: PaywallMessageDelegate,
    private val context: Context,
) {
    private val delegate: PaywallMessageDelegate = delegate

    @JavascriptInterface
    fun postMessage(message: String) {
        // Print out the message to the console using Log.d
        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "SWWebViewInterface: $message",
        )

        // Attempt to parse the message to json
        // and print out the version number
        val wrappedPaywallMessages: WrappedPaywallMessages
        try {
            wrappedPaywallMessages = parseWrappedPaywallMessages(message)
        } catch (e: Throwable) {
            e.printStackTrace()
            Logger.debug(
                LogLevel.error,
                LogScope.superwallCore,
                "SWWebViewInterface: Error parsing message$e - ${e.stackTraceToString()}",
            )
            return
        }

        // Loop through the messages and print out the event name
        for (paywallMessage in wrappedPaywallMessages.payload.messages) {
            Logger.debug(
                LogLevel.debug,
                LogScope.superwallCore,
                "SWWebViewInterface:" + paywallMessage.javaClass.simpleName,
            )
            delegate.didReceiveMessage(paywallMessage)
        }
    }
}
