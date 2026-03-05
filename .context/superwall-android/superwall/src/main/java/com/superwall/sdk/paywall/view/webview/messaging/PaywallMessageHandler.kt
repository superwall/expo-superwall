package com.superwall.sdk.paywall.view.webview.messaging

import TemplateLogic
import android.app.Activity
import android.os.Build
import android.view.HapticFeedbackConstants
import android.webkit.JavascriptInterface
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEvents
import com.superwall.sdk.dependencies.OptionsFactory
import com.superwall.sdk.dependencies.VariablesFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.MainScope
import com.superwall.sdk.models.paywall.LocalNotification
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.paywall.presentation.CustomCallback
import com.superwall.sdk.paywall.presentation.CustomCallbackRegistry
import com.superwall.sdk.paywall.presentation.CustomCallbackResult
import com.superwall.sdk.paywall.presentation.CustomCallbackResultStatus
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.PaywallViewState
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import com.superwall.sdk.paywall.view.webview.SendPaywallMessages
import com.superwall.sdk.permissions.PermissionStatus
import com.superwall.sdk.permissions.UserPermissions
import com.superwall.sdk.storage.core_data.convertFromJsonElement
import com.superwall.sdk.storage.core_data.convertToJsonElement
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.net.URI
import java.util.Date
import java.util.LinkedList
import java.util.Queue
import kotlin.coroutines.resume

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

    fun presentPaymentSheet(url: String)
}

class PaywallMessageHandler(
    private val factory: VariablesFactory,
    private val options: OptionsFactory,
    private val track: suspend (TrackableSuperwallEvent) -> Unit,
    private val setAttributes: (Map<String, Any>) -> Unit,
    private val getView: () -> PaywallView?,
    private val mainScope: MainScope,
    private val ioScope: CoroutineScope,
    private val json: Json = Json { encodeDefaults = true },
    private val encodeToB64: (String) -> String,
    private val userPermissions: UserPermissions,
    private val getActivity: () -> Activity?,
    private val customCallbackRegistry: CustomCallbackRegistry,
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
                it.printStackTrace()
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

            is PaywallMessage.OpenUrl ->
                openUrl(
                    message.url,
                    message.browserType == PaywallMessage.OpenUrl.BrowserType.PAYMENT_SHEET,
                )

            is PaywallMessage.OpenUrlInBrowser ->
                openUrlInBrowser(message.url)

            is PaywallMessage.OpenDeepLink -> openDeepLink(message.url.toString())
            is PaywallMessage.Restore -> restorePurchases()
            is PaywallMessage.Purchase ->
                purchaseProduct(
                    withId = message.productId,
                    shouldDismiss = message.shouldDismiss,
                )

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

            is PaywallMessage.TransactionStart -> {
                ioScope.launch {
                    pass(eventName = SuperwallEvents.TransactionStart.rawName, paywall = paywall)
                }
            }

            is PaywallMessage.UserAttributesUpdated -> {
                setAttributes(message.data)
            }

            is PaywallMessage.TransactionComplete -> {
                ioScope.launch {
                    pass(
                        SuperwallEvents.TransactionComplete.rawName,
                        paywall,
                        mapOf("product_identifier" to message.productIdentifier),
                    )
                }
            }

            is PaywallMessage.TrialStarted -> {
                ioScope.launch {
                    pass(
                        eventName = SuperwallEvents.FreeTrialStart.rawName,
                        paywall = paywall,
                        payload =
                            buildMap {
                                message.trialEndDate?.let { put("trial_end_date", it) }
                                put("product_identifier", message.productIdentifier)
                            },
                    )
                }
            }

            is PaywallMessage.ScheduleNotification ->
                messageHandler?.eventDidOccur(
                    PaywallWebEvent.ScheduleNotification(
                        LocalNotification(
                            id = message.id,
                            type = message.type,
                            title = message.title,
                            subtitle = message.subtitle,
                            body = message.body,
                            delay = message.delay,
                        ),
                    ),
                )

            is PaywallMessage.RequestPermission -> handleRequestPermission(message)

            is PaywallMessage.RequestCallback -> handleRequestCallback(message)

            is PaywallMessage.HapticFeedback -> triggerHapticFeedback(message.hapticType)

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
        payload: Map<String, Any> = emptyMap(),
    ) {
        val eventList =
            listOf(
                mapOf(
                    "event_name" to eventName,
                    "paywall_id" to paywall.databaseId,
                    "paywall_identifier" to paywall.identifier,
                ) + payload,
            )
        val jsonString =
            try {
                json.encodeToString(eventList.convertToJsonElement())
            } catch (e: Throwable) {
                e.printStackTrace()
                "{\"event_name\":\"$eventName\"}"
            }
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
                    flushPendingMessagesInternal()
                    messageHandler?.updateState(
                        PaywallViewState.Updates.SetLoadingState(
                            PaywallLoadingState.Ready,
                        ),
                    )
                }
            }
        }
    }

    fun flushPendingMessages() {
        ioScope.launch {
            mainScope.launch {
                flushPendingMessagesInternal()
            }
        }
    }

    private fun flushPendingMessagesInternal() {
        if (queue.isEmpty()) return

        val pending = queue.toList()
        queue.clear()
        pending.forEach { handle(it) }
    }

    private fun openUrl(
        url: URI,
        isPaymentSheet: Boolean,
    ) {
        detectHiddenPaywallEvent(
            "openUrl",
            mapOf("url" to url.toString()),
        )
        hapticFeedback()
        messageHandler?.eventDidOccur(PaywallWebEvent.OpenedURL(url))
        if (isPaymentSheet) {
            messageHandler?.presentPaymentSheet(url.toString())
        } else {
            messageHandler?.presentBrowserInApp(url.toString())
        }
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

    private fun purchaseProduct(
        withId: String,
        shouldDismiss: Boolean,
    ) {
        detectHiddenPaywallEvent("purchase")
        hapticFeedback()
        messageHandler?.eventDidOccur(PaywallWebEvent.InitiatePurchase(withId, shouldDismiss))
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
        params: JsonObject,
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

    private fun handleRequestPermission(request: PaywallMessage.RequestPermission) {
        val activity = getActivity()
        val paywallIdentifier = messageHandler?.state?.paywall?.identifier ?: ""
        val permissionName = request.permissionType.rawValue

        messageHandler?.eventDidOccur(
            PaywallWebEvent.RequestPermission(
                permissionType = request.permissionType,
                requestId = request.requestId,
            ),
        )

        // Track permission requested event
        ioScope.launch {
            track(
                InternalSuperwallEvent.Permission(
                    state = InternalSuperwallEvent.Permission.State.Requested,
                    permissionName = permissionName,
                    paywallIdentifier = paywallIdentifier,
                ),
            )
        }

        if (activity == null) {
            Logger.debug(
                LogLevel.error,
                LogScope.superwallCore,
                "Cannot request permission - no activity available",
            )
            // Send unsupported status back to webview since we can't request
            ioScope.launch {
                sendPermissionResult(
                    requestId = request.requestId,
                    permissionType = request.permissionType,
                    status = PermissionStatus.UNSUPPORTED,
                )
            }
            return
        }

        ioScope.launch {
            val status =
                try {
                    userPermissions.requestPermission(activity, request.permissionType)
                } catch (e: Exception) {
                    Logger.debug(
                        LogLevel.error,
                        LogScope.superwallCore,
                        "Error requesting permission: ${e.message}",
                        error = e,
                    )
                    PermissionStatus.UNSUPPORTED
                }

            // Track permission result event
            when (status) {
                PermissionStatus.GRANTED -> {
                    track(
                        InternalSuperwallEvent.Permission(
                            state = InternalSuperwallEvent.Permission.State.Granted,
                            permissionName = permissionName,
                            paywallIdentifier = paywallIdentifier,
                        ),
                    )
                }

                PermissionStatus.DENIED, PermissionStatus.UNSUPPORTED -> {
                    track(
                        InternalSuperwallEvent.Permission(
                            state = InternalSuperwallEvent.Permission.State.Denied,
                            permissionName = permissionName,
                            paywallIdentifier = paywallIdentifier,
                        ),
                    )
                }
            }

            sendPermissionResult(
                requestId = request.requestId,
                permissionType = request.permissionType,
                status = status,
            )
        }
    }

    /**
     * Send a permission_result message back to the webview
     */
    private suspend fun sendPermissionResult(
        requestId: String,
        permissionType: com.superwall.sdk.permissions.PermissionType,
        status: PermissionStatus,
    ) {
        val eventList =
            listOf(
                mapOf(
                    "event_name" to "permission_result",
                    "permission_type" to permissionType.rawValue,
                    "request_id" to requestId,
                    "status" to status.rawValue,
                ),
            )

        val jsonString =
            try {
                json.encodeToString(eventList.convertToJsonElement())
            } catch (e: Throwable) {
                Logger.debug(
                    LogLevel.error,
                    LogScope.superwallCore,
                    "Error encoding permission result: ${e.message}",
                    error = e,
                )
                return
            }

        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "Sending permission_result: $jsonString",
        )

        passMessageToWebView(base64String = encodeToB64(jsonString))
    }

    private fun handleRequestCallback(request: PaywallMessage.RequestCallback) {
        val paywallIdentifier = messageHandler?.state?.paywall?.identifier ?: ""

        // Emit event to listeners
        messageHandler?.eventDidOccur(
            PaywallWebEvent.RequestCallback(
                name = request.name,
                behavior = request.behavior,
                requestId = request.requestId,
                variables = request.variables,
            ),
        )

        // Get the callback handler from the registry
        val callbackHandler = customCallbackRegistry.getHandler(paywallIdentifier)

        if (callbackHandler == null) {
            Logger.debug(
                LogLevel.warn,
                LogScope.superwallCore,
                "No custom callback handler registered for callback: ${request.name}",
            )
            // Send failure response if no handler is registered
            ioScope.launch {
                sendCallbackResult(
                    requestId = request.requestId,
                    name = request.name,
                    status = CustomCallbackResultStatus.FAILURE,
                    data = null,
                )
            }
            return
        }

        ioScope.launch {
            val result =
                try {
                    val callback =
                        CustomCallback(
                            name = request.name,
                            variables = request.variables,
                        )
                    callbackHandler(callback)
                } catch (e: Exception) {
                    Logger.debug(
                        LogLevel.error,
                        LogScope.superwallCore,
                        "Error executing custom callback: ${e.message}",
                        error = e,
                    )
                    CustomCallbackResult.failure()
                }

            sendCallbackResult(
                requestId = request.requestId,
                name = request.name,
                status = result.status,
                data = result.data,
            )
        }
    }

    /**
     * Send a callback_result message back to the webview
     */
    private suspend fun sendCallbackResult(
        requestId: String,
        name: String,
        status: CustomCallbackResultStatus,
        data: Map<String, Any>?,
    ) {
        val eventMap =
            mutableMapOf<String, Any>(
                "event_name" to "callback_result",
                "request_id" to requestId,
                "name" to name,
                "status" to status.rawValue,
            )

        if (data != null) {
            eventMap["data"] = data
        }

        val eventList = listOf(eventMap)

        val jsonString =
            try {
                json.encodeToString(eventList.convertToJsonElement())
            } catch (e: Throwable) {
                Logger.debug(
                    LogLevel.error,
                    LogScope.superwallCore,
                    "Error encoding callback result: ${e.message}",
                    error = e,
                )
                return
            }

        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "Sending callback_result: $jsonString",
        )

        passMessageToWebView(base64String = encodeToB64(jsonString))
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

        if (!isHapticFeedbackEnabled || isGameControllerEnabled) {
            return
        }

        mainScope.launch {
            getView()?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun triggerHapticFeedback(hapticType: PaywallMessage.HapticFeedback.HapticType) {
        withErrorTracking {
            val options = options.makeSuperwallOptions()
            if (!options.paywalls.isHapticFeedbackEnabled || options.isGameControllerEnabled) {
                return
            }

            val feedbackConstant =
                when (hapticType) {
                    PaywallMessage.HapticFeedback.HapticType.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
                    PaywallMessage.HapticFeedback.HapticType.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
                    PaywallMessage.HapticFeedback.HapticType.HEAVY -> HapticFeedbackConstants.LONG_PRESS
                    PaywallMessage.HapticFeedback.HapticType.SUCCESS ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HapticFeedbackConstants.CONFIRM
                        } else {
                            HapticFeedbackConstants.VIRTUAL_KEY
                        }

                    PaywallMessage.HapticFeedback.HapticType.WARNING -> HapticFeedbackConstants.LONG_PRESS
                    PaywallMessage.HapticFeedback.HapticType.ERROR ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HapticFeedbackConstants.REJECT
                        } else {
                            HapticFeedbackConstants.LONG_PRESS
                        }

                    PaywallMessage.HapticFeedback.HapticType.SELECTION -> HapticFeedbackConstants.CLOCK_TICK
                }

            mainScope.launch {
                getView()?.performHapticFeedback(feedbackConstant)
            }
        }
    }

    /**
     * Gets the current state from the paywall webview by evaluating JavaScript.
     * @return A map containing the paywall state, or an empty map if evaluation fails.
     */
    suspend fun getState(): Map<String, Any> {
        val messageScript = "window.app.getAllState();"

        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.paywallView,
            message = "Getting state",
            info = mapOf("message" to messageScript),
        )

        return withTimeoutOrNull(1000) {
            suspendCancellableCoroutine { continuation ->
                mainScope.launch {
                    messageHandler?.evaluate(messageScript) { result ->
                        if (result != null) {
                            try {
                                val parsed = json.parseToJsonElement(result)
                                val converted = parsed.convertFromJsonElement()
                                val stateMap =
                                    replaceNullsWithEmpty(converted) as? Map<String, Any>
                                        ?: emptyMap()
                                continuation.resume(stateMap)
                            } catch (e: Exception) {
                                Logger.debug(
                                    logLevel = LogLevel.error,
                                    scope = LogScope.paywallView,
                                    message = "Error parsing state JSON",
                                    info = mapOf("message" to messageScript, "result" to result),
                                    error = e,
                                )
                                continuation.resume(emptyMap())
                            }
                        } else {
                            continuation.resume(emptyMap())
                        }
                    } ?: run {
                        continuation.resume(emptyMap())
                    }
                }
            }
        } ?: emptyMap()
    }

    private fun replaceNullsWithEmpty(value: Any?): Any =
        when (value) {
            null -> ""
            is Map<*, *> -> value.mapValues { (_, v) -> replaceNullsWithEmpty(v) }
            is List<*> -> value.map { replaceNullsWithEmpty(it) }
            else -> value
        }
}
