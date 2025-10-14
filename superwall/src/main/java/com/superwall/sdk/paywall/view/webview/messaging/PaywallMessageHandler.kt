package com.superwall.sdk.paywall.view.webview.messaging

import TemplateLogic
import android.webkit.JavascriptInterface
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEvents
import com.superwall.sdk.dependencies.OptionsFactory
import com.superwall.sdk.dependencies.VariablesFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.MainScope
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.PaywallViewState
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import com.superwall.sdk.paywall.view.webview.PaywallMessage
import com.superwall.sdk.paywall.view.webview.SendPaywallMessages
import com.superwall.sdk.paywall.view.webview.parseWrappedPaywallMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.URI
import java.util.Date
import java.util.LinkedList
import java.util.Queue

interface PaywallStateDelegate {
    val state: PaywallViewState

    fun updateState(update: PaywallViewState.Updates)
}

interface PaywallMessageHandlerDelegate : PaywallStateDelegate {
    fun eventDidOccur(paywallWebEvent: PaywallWebEvent)

    fun openDeepLink(url: String)

    fun presentBrowserInApp(url: String)

    fun presentBrowserExternal(url: String)

    fun evaluate(
        code: String,
        resultCallback: ((String?) -> Unit)?,
    )
}

class PaywallMessageHandler(
    private val factory: VariablesFactory,
    private val options: OptionsFactory,
    private val track: suspend (TrackableSuperwallEvent) -> Unit,
    private val getView: () -> PaywallView?,
    private val mainScope: MainScope,
    private val ioScope: CoroutineScope,
    private val json: Json = Json { encodeDefaults = true },
    private val encodeToB64: (String) -> String,
) : SendPaywallMessages {
    private companion object {
        val selectionString =
            """var css = '*{-webkit-touch-callout:none;-webkit-user-select:none} .w-webflow-badge { display: none !important; }';
                    var head = document.head || document.getElementsByTagName('head')[0];
                    var style = document.createElement('style'); style.type = 'text/css';
                    style.appendChild(document.createTextNode(css)); head.appendChild(style); """
        val preventZoom =
            """var meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                        var head = document.getElementsByTagName('head')[0];
                        head.appendChild(meta);"""
    }

    var messageHandler: PaywallMessageHandlerDelegate? = null
    private val queue: Queue<PaywallMessage> = LinkedList()

    @JavascriptInterface
    fun postMessage(message: String) {
        // Print out the message to the console using Log.d
        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "SWWebViewInterface: $message",
        )

        // Attempt to parse the message to json
        parseWrappedPaywallMessages(message)
            .fold({
                for (paywallMessage in it.payload.messages) {
                    Logger.debug(
                        LogLevel.debug,
                        LogScope.superwallCore,
                        "SWWebViewInterface: ${paywallMessage.javaClass.simpleName}",
                    )
                    handle(paywallMessage)
                }
            }, {
                Logger.debug(
                    LogLevel.debug,
                    LogScope.superwallCore,
                    "SWWebViewInterface: Error parsing message - $it",
                )
                return@fold
            })
    }

    override fun handle(message: PaywallMessage) {
        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "!! PaywallMessageHandler: Handling message: $message ${messageHandler?.state?.paywall}, delegeate: $messageHandler",
        )
        val paywall = messageHandler?.state?.paywall ?: return
        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "!! PaywallMessageHandler: Paywall: $paywall, delegeate: $messageHandler",
        )
        when (message) {
            is PaywallMessage.TemplateParamsAndUserAttributes ->
                ioScope.launch { passTemplatesToWebView(paywall) }

            is PaywallMessage.OnReady -> {
                messageHandler?.updateState(
                    PaywallViewState.Updates.SetPaywallJsVersion(message.paywallJsVersion),
                )
                val loadedAt = Date()
                Logger.debug(
                    LogLevel.debug,
                    LogScope.superwallCore,
                    "!! PaywallMessageHandler: Ready !!",
                )
                ioScope.launch { didLoadWebView(paywall, loadedAt) }
            }

            is PaywallMessage.Close -> {
                hapticFeedback()
                messageHandler?.eventDidOccur(PaywallWebEvent.Closed)
            }

            is PaywallMessage.OpenUrl -> openUrl(message.url)
            is PaywallMessage.OpenUrlInBrowser -> openUrlInBrowser(message.url)
            is PaywallMessage.OpenDeepLink -> openDeepLink(message.url.toString())
            is PaywallMessage.Restore -> restorePurchases()
            is PaywallMessage.Purchase -> purchaseProduct(withId = message.productId)
            is PaywallMessage.PaywallOpen -> {
                if (messageHandler?.state?.paywall?.paywalljsVersion == null) {
                    queue.offer(message)
                } else {
                    ioScope.launch {
                        pass(eventName = SuperwallEvents.PaywallOpen.rawName, paywall = paywall)
                    }
                }
            }

            is PaywallMessage.PaywallClose -> {
                if (messageHandler?.state?.paywall?.paywalljsVersion == null) {
                    queue.offer(message)
                } else {
                    ioScope.launch {
                        val eventName = SuperwallEvents.PaywallClose.rawName
                        pass(eventName = eventName, paywall = paywall)
                    }
                }
            }

            is PaywallMessage.Custom -> handleCustomEvent(message.data)
            is PaywallMessage.CustomPlacement -> handleCustomPlacement(message.name, message.params)
            is PaywallMessage.RestoreFailed ->
                ioScope.launch {
                    pass(SuperwallEvents.RestoreFail.rawName, paywall)
                }

            is PaywallMessage.RequestReview -> handleRequestReview(message)

            else -> {
                Logger.debug(
                    LogLevel.error,
                    LogScope.superwallCore,
                    "!! PaywallMessageHandler: Unknown message type: $message",
                )
            }
        }
    }

    // Serialize the event to JSON and pass as B64 encoded string
    private suspend fun pass(
        eventName: String,
        paywall: Paywall,
    ) {
        val eventList =
            listOf(
                mapOf(
                    "event_name" to eventName,
                    "paywall_id" to paywall.databaseId,
                    "paywall_identifier" to paywall.identifier,
                ),
            )
        val jsonString = json.encodeToString(eventList)
        passMessageToWebView(base64String = encodeToB64(jsonString))
    }

    // Passes the templated variables and params to the webview.
    // This is called every paywall open incase variables like user attributes have changed.
    private suspend fun passTemplatesToWebView(paywall: Paywall) {
        val eventData =
            messageHandler
                ?.state
                ?.request
                ?.presentationInfo
                ?.eventData
        val templates =
            TemplateLogic.getBase64EncodedTemplates(
                paywall = paywall,
                event = eventData,
                factory = factory,
                json = json,
                encodeToBase64 = encodeToB64,
            )
        passMessageToWebView(base64String = templates)
    }

    private suspend fun passMessageToWebView(base64String: String) {
        val templateScript = """
      window.paywall.accept64('$base64String');
    """

        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.paywallView,
            message = "Posting Message",
            info = mapOf("message" to templateScript),
        )

        withContext(Dispatchers.Main) {
            messageHandler?.evaluate(templateScript) { error ->
                if (error != null) {
                    Logger.debug(
                        logLevel = LogLevel.error,
                        scope = LogScope.paywallView,
                        message = "Error Evaluating JS",
                        info = mapOf("message" to templateScript),
                        error = java.lang.Exception(error),
                    )
                }
            }
        }
    }

    // Passes in the HTML substitutions, templates and other scripts to make the webview feel native.
    private suspend fun didLoadWebView(
        paywall: Paywall,
        loadedAt: Date,
    ) {
        ioScope.launch {
            val delegate = this@PaywallMessageHandler.messageHandler
            if (delegate != null) {
                delegate.updateState(PaywallViewState.Updates.WebLoadingEnded(loadedAt))

                val paywallInfo = delegate.state.info
                val trackedEvent =
                    InternalSuperwallEvent.PaywallWebviewLoad(
                        state = InternalSuperwallEvent.PaywallWebviewLoad.State.Complete(),
                        paywallInfo = paywallInfo,
                    )
                track(trackedEvent)
            }
        }

        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "!! PaywallMessageHandler: didLoadWebView",
        )

        val htmlSubstitutions = paywall.htmlSubstitutions
        val eventData =
            messageHandler
                ?.state
                ?.request
                ?.presentationInfo
                ?.eventData
        val templates =
            TemplateLogic.getBase64EncodedTemplates(
                paywall = paywall,
                event = eventData,
                factory = factory,
                json = json,
                encodeToBase64 = encodeToB64,
            )
        val scriptSrc = """
      window.paywall.accept64('$templates');
      window.paywall.accept64('$htmlSubstitutions');
    """

        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "!! PaywallMessageHandler: $scriptSrc",
        )

        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.paywallView,
            message = "Posting Message",
            info = mapOf("message" to scriptSrc),
        )

        mainScope.launch {
            messageHandler?.evaluate(scriptSrc) { error ->
                if (error != null) {
                    Logger.debug(
                        logLevel = LogLevel.error,
                        scope = LogScope.paywallView,
                        message = "Error Evaluating JS",
                        info = mapOf("message" to scriptSrc),
                        error = java.lang.Exception(error),
                    )
                }
            }

            // block selection
            messageHandler?.evaluate(selectionString, null)
            messageHandler?.evaluate(preventZoom, null)
            ioScope.launch {
                mainScope.launch {
                    while (queue.isNotEmpty()) {
                        val item = queue.remove()
                        handle(item)
                    }
                    messageHandler?.updateState(
                        PaywallViewState.Updates.SetLoadingState(
                            PaywallLoadingState.Ready,
                        ),
                    )
                }
            }
        }
    }

    private fun openUrl(url: URI) {
        detectHiddenPaywallEvent(
            "openUrl",
            mapOf("url" to url.toString()),
        )
        hapticFeedback()
        messageHandler?.eventDidOccur(PaywallWebEvent.OpenedURL(url))
        messageHandler?.presentBrowserInApp(url.toString())
    }

    private fun openUrlInBrowser(url: URI) {
        detectHiddenPaywallEvent(
            "openUrlInSafari",
            mapOf("url" to url),
        )
        hapticFeedback()
        messageHandler?.eventDidOccur(PaywallWebEvent.OpenedUrlInChrome(url))
        messageHandler?.presentBrowserExternal(url.toString())
    }

    private fun openDeepLink(url: String) {
        detectHiddenPaywallEvent(
            "openDeepLink",
            mapOf("url" to url),
        )
        hapticFeedback()
        messageHandler?.openDeepLink(url)
    }

    private fun restorePurchases() {
        detectHiddenPaywallEvent("restore")
        hapticFeedback()
        messageHandler?.eventDidOccur(PaywallWebEvent.InitiateRestore)
    }

    private fun purchaseProduct(withId: String) {
        detectHiddenPaywallEvent("purchase")
        hapticFeedback()
        messageHandler?.eventDidOccur(PaywallWebEvent.InitiatePurchase(withId))
    }

    private fun handleCustomEvent(customEvent: String) {
        detectHiddenPaywallEvent(
            "custom",
            mapOf("custom_event" to customEvent),
        )
        messageHandler?.eventDidOccur(PaywallWebEvent.Custom(customEvent))
    }

    private fun handleCustomPlacement(
        name: String,
        params: JSONObject,
    ) {
        messageHandler?.eventDidOccur(PaywallWebEvent.CustomPlacement(name, params))
    }

    private fun handleRequestReview(request: PaywallMessage.RequestReview) {
        hapticFeedback()
        messageHandler?.eventDidOccur(
            PaywallWebEvent.RequestReview(
                when (request.type) {
                    PaywallMessage.RequestReview.Type.EXTERNAL -> PaywallWebEvent.RequestReview.Type.EXTERNAL

                    else -> PaywallWebEvent.RequestReview.Type.INAPP
                },
            ),
        )
    }

    private fun detectHiddenPaywallEvent(
        eventName: String,
        userInfo: Map<String, Any>? = null,
    ) {
        if (messageHandler?.state?.isPresented == true) {
            return
        }

        val paywallDebugDescription = getView().toString()

        var info: MutableMap<String, Any> =
            mutableMapOf(
                "self" to this,
                "Superwall.instance.paywallViewController" to paywallDebugDescription,
                "event" to eventName,
            )
        userInfo?.let {
            info.putAll(userInfo)
        }

        Logger.debug(
            logLevel = LogLevel.error,
            scope = LogScope.paywallView,
            message = "Received Event on Hidden Superwall",
            info = info,
        )
    }

    private fun hapticFeedback() {
        val options = options.makeSuperwallOptions()
        val isHapticFeedbackEnabled = options.paywalls.isHapticFeedbackEnabled
        val isGameControllerEnabled = options.isGameControllerEnabled

        if (isHapticFeedbackEnabled == false || isGameControllerEnabled == true) {
            return
        }

        // Replace this with your platform-specific implementation for haptic feedback
        // Android doesn't have a direct equivalent to UIImpactFeedbackGenerator
        // TODO: Implement haptic feedback
    }
}
