package com.superwall.sdk.paywall.view.webview

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.config.models.OnDeviceCaching
import com.superwall.sdk.config.options.PaywallOptions
import com.superwall.sdk.game.dispatchKeyEvent
import com.superwall.sdk.game.dispatchMotionEvent
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.MainScope
import com.superwall.sdk.misc.asEither
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.paywall.PaywallURL
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.paywall.view.PaywallViewState
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import com.superwall.sdk.paywall.view.webview.messaging.PaywallMessage
import com.superwall.sdk.paywall.view.webview.messaging.PaywallMessageHandler
import com.superwall.sdk.paywall.view.webview.messaging.PaywallMessageHandlerDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:class-naming")
interface PaywallInfoDelegate {
    val info: PaywallInfo
}

interface PaywallUIDelegate :
    PaywallInfoDelegate,
    PaywallMessageHandlerDelegate

interface SendPaywallMessages {
    fun handle(message: PaywallMessage)
}

class SWWebView(
    context: Context,
    override val messageHandler: PaywallMessageHandler,
    private val onFinishedLoading: ((url: String) -> Unit)? = null,
    private val options: () -> PaywallOptions,
    private val track: suspend (TrackableSuperwallEvent) -> Unit = { Superwall.instance.track(it) },
) : WebView(context),
    PaywallWebUI {
    override var delegate: PaywallUIDelegate? = null
    private val mainScope = MainScope()
    private val ioScope = IOScope()
    val activity: Activity?
        get() =
            Superwall.instance.dependencyContainer.activityProvider
                ?.getCurrentActivity()
    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(
            context,
            ScrollDisabledListener(),
        )
    }
    override var onScrollChangeListener: PaywallWebUI.OnScrollChangeListener? = null

    override fun detach(fromView: ViewGroup) {
        fromView.removeView(this)
    }

    override fun attach(toView: ViewGroup) {
        if (parent == null) {
            toView.addView(this)
        }
        if (layoutParams.height != LayoutParams.MATCH_PARENT &&
            layoutParams.width != LayoutParams.MATCH_PARENT
        ) {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
    }

    var scrollEnabled = true

    var onRenderCrashed: (didCrash: Boolean, priority: Int) -> Unit =
        { i, e -> }

    private companion object ChromeClient : WebChromeClient() {
        private class ChromeClient(
            val activity: () -> Activity?,
        ) : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                // Don't log anything
                return true
            }

            override fun onJsAlert(
                view: WebView,
                url: String,
                message: String,
                result: JsResult,
            ): Boolean {
                AlertDialog
                    .Builder(activity() ?: return false)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(
                view: WebView,
                url: String,
                message: String,
                result: JsResult,
            ): Boolean {
                AlertDialog
                    .Builder(activity() ?: return false)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                    .show()
                return true
            }

            override fun onJsPrompt(
                view: WebView,
                url: String,
                message: String,
                defaultValue: String?,
                result: JsPromptResult,
            ): Boolean {
                val input = EditText(activity()).apply { setText(defaultValue ?: "") }
                AlertDialog
                    .Builder(activity() ?: return false)
                    .setMessage(message)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm(input.text.toString()) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                    .show()
                return true
            }
        }
    }

    private var lastWebViewClient: WebViewClient? = null
    private var lastLoadedUrl: String? = null

    internal fun prepareWebview() {
        addJavascriptInterface(messageHandler, "SWAndroid")

        val webSettings = this.settings
        setWebContentsDebuggingEnabled(true)
        webSettings.javaScriptEnabled = true
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        webSettings.allowFileAccess = false
        webSettings.allowContentAccess = false
        webSettings.textZoom = 100
        // Enable inline media playback, requires API level 17
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.mediaPlaybackRequiresUserGesture = false
        }
        this.setBackgroundColor(Color.TRANSPARENT)
        this.webChromeClient = ChromeClient({ activity })
    }

    internal fun loadPaywallWithFallbackUrl(paywall: Paywall) {
        prepareWebview()
        val client =
            WebviewFallbackClient(
                config =
                    paywall.urlConfig
                        ?: run {
                            Logger.debug(
                                LogLevel.error,
                                LogScope.paywallView,
                                "Tried to start a paywall with multiple URLS but without URL config",
                            )
                            return
                        },
                mainScope = mainScope,
                ioScope = ioScope,
                loadUrl = {
                    lastLoadedUrl = it.url
                    super.loadUrl(transformUri(it.url))
                },
                stopLoading = {
                    stopLoading()
                },
                onCrashed = { view, detail ->
                    if (view == this) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            onRenderCrashed(
                                detail.didCrash(),
                                detail.rendererPriorityAtExit(),
                            )
                        } else {
                            onRenderCrashed(true, -1)
                        }
                    }
                },
            )
        this.webViewClient = client
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            lastWebViewClient = client
        }
        listenToWebviewClientEvents(client)
        client.loadWithFallback()
    }

    override fun enableBackgroundRendering() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Temporary disabled
            //     settings.offscreenPreRaster = true
        }
    }

    // ???
    // https://stackoverflow.com/questions/20968707/capturing-keypress-events-in-android-webview
    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection = BaseInputConnection(this, false)

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || !Superwall.instance.options.isGameControllerEnabled) {
            return super.dispatchKeyEvent(event)
        }
        Superwall.instance.dispatchKeyEvent(event)
        return true
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event == null || !Superwall.instance.options.isGameControllerEnabled) {
            return super.dispatchGenericMotionEvent(event)
        }
        Superwall.instance.dispatchMotionEvent(event)
        return true
    }

    override fun loadUrl(url: String) {
        lastLoadedUrl = url
        prepareWebview()
        val client =
            DefaultWebviewClient(
                forUrl = url,
                ioScope = CoroutineScope(Dispatchers.IO),
                onWebViewCrash = { v, detail ->
                    if (v == this) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            onRenderCrashed(
                                detail.didCrash(),
                                detail.rendererPriorityAtExit(),
                            )
                        } else {
                            onRenderCrashed(true, -1)
                        }
                    }
                },
            )
        this.webViewClient = client

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            lastWebViewClient = client
        }

        listenToWebviewClientEvents(client)
        super.loadUrl(transformUri(url))
    }

    private fun transformUri(url: String): String {
        // Parse the url and add the query parameter
        val uri = Uri.parse(url)

        // Add a query parameter
        val builder = uri.buildUpon()
        builder.appendQueryParameter("platform", "android")
        builder.appendQueryParameter("transport", "android")
        builder.appendQueryParameter("debug", "true")
//        builder.appendQueryParameter("pjs", "http://10.0.2.2:4444/runtime/dev/entrypoint.js")
        val newUri = builder.build()

        // Use the new URL
        val urlString = newUri.toString()
        Logger.debug(
            LogLevel.debug,
            LogScope.paywallView,
            "SWWebView.loadUrl: $urlString",
        )
        return urlString
    }

    private fun listenToWebviewClientEvents(client: DefaultWebviewClient) {
        ioScope.launch {
            client.webviewClientEvents
                .takeWhile {
                    mainScope
                        .async {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                webViewClient == client
                            } else {
                                lastWebViewClient == client
                            }
                        }.await()
                }.collect {
                    fun log(
                        message: String,
                        info: Map<String, Any> = emptyMap(),
                    ) {
                        Logger.debug(
                            LogLevel.error,
                            LogScope.paywallView,
                            message,
                            info = info,
                        )
                    }
                    mainScope.launch {
                        when (it) {
                            is WebviewClientEvent.OnError -> {
                                trackPaywallError(
                                    it.webviewError,
                                    when (val e = it.webviewError) {
                                        is WebviewError.NetworkError -> {
                                            log(
                                                "Paywall loading failed due to network error - ${e.code} - ${e.description}",
                                            )
                                            listOf(e.url)
                                        }

                                        is WebviewError.NoUrls -> {
                                            log(
                                                "Paywall loading failed - no URL's to load",
                                            )
                                            emptyList()
                                        }

                                        is WebviewError.MaxAttemptsReached -> {
                                            log(
                                                "Paywall loading failed - maximum attempts reached.",
                                            )
                                            e.urls
                                        }

                                        is WebviewError.AllUrlsFailed -> {
                                            log(
                                                "Paywall loading failed - ${e.urls.size}/${e.urls.size} tried",
                                            )
                                            e.urls
                                        }

                                        is WebviewError.Timeout -> {
                                            log(
                                                "Paywall loading reached user defined timeout - the paywall might still display",
                                            )
                                            listOfNotNull(lastLoadedUrl)
                                        }
                                    },
                                )
                                if (lastLoadedUrl != null) {
                                    when (lastWebViewClient) {
                                        is WebviewFallbackClient -> {}
                                        is DefaultWebviewClient -> {
                                            log(
                                                "Paywall loading failed - retrying $lastLoadedUrl",
                                            )
                                            loadUrl(lastLoadedUrl!!)
                                        }

                                        else -> {
                                            // NO-OP as it has internal fallback
                                        }
                                    }
                                }
                            }

                            is WebviewClientEvent.OnResourceError -> {
                                trackPaywallResourceError(
                                    it.webviewError,
                                    when (val e = it.webviewError) {
                                        is WebviewError.NetworkError ->
                                            e.url

                                        is WebviewError.NoUrls ->
                                            ""

                                        is WebviewError.MaxAttemptsReached ->
                                            e.urls.first()

                                        is WebviewError.AllUrlsFailed -> e.urls.first()
                                        is WebviewError.Timeout -> lastLoadedUrl ?: ""
                                    },
                                )
                            }

                            is WebviewClientEvent.OnPageFinished -> {
                                asEither {
                                    mainScope.launch {
                                        this@SWWebView.scrollTo(0, 0)
                                    }
                                }
                                if (options().optimisticLoading) {
                                    delegate?.updateState(
                                        PaywallViewState.Updates.SetLoadingState(
                                            PaywallLoadingState.Ready,
                                        ),
                                    )
                                }
                                onFinishedLoading?.invoke(it.url)
                            }

                            is WebviewClientEvent.LoadingFallback -> {
                                trackLoadFallback()
                            }
                        }
                    }
                }
        }
    }

    private fun trackLoadFallback() {
        mainScope.launch {
            delegate?.updateState(PaywallViewState.Updates.WebLoadingFailed)

            val paywallInfo = delegate?.info ?: return@launch

            val trackedEvent =
                InternalSuperwallEvent.PaywallWebviewLoad(
                    state =
                        InternalSuperwallEvent.PaywallWebviewLoad.State.Fallback,
                    paywallInfo = paywallInfo,
                )
            track(trackedEvent)
        }
    }

    private fun trackPaywallError(
        error: WebviewError,
        urls: List<String>,
    ) {
        mainScope.launch {
            delegate?.updateState(PaywallViewState.Updates.WebLoadingFailed)

            val paywallInfo = delegate?.info ?: return@launch

            val trackedEvent =
                InternalSuperwallEvent.PaywallWebviewLoad(
                    state =
                        InternalSuperwallEvent.PaywallWebviewLoad.State.Fail(
                            error,
                            urls,
                        ),
                    paywallInfo = paywallInfo,
                )
            track(trackedEvent)
        }
    }

    private fun trackPaywallResourceError(
        error: WebviewError,
        url: String,
    ) {
        mainScope.launch {
            val trackedEvent =
                InternalSuperwallEvent.PaywallResourceLoadFail(
                    url = url,
                    error = error.toString(),
                )
            track(trackedEvent)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean =
        if (scrollEnabled || event.action != MotionEvent.ACTION_MOVE) {
            super.onTouchEvent(event)
        } else {
            val gesture = gestureDetector.onTouchEvent(event)
            if (!gesture) {
                super.onTouchEvent(event)
            } else {
                gesture
            }
        }

    override fun onScrollChanged(
        horizontalOrigin: Int,
        verticalOrigin: Int,
        oldHorizontal: Int,
        oldVertical: Int,
    ) {
        super.onScrollChanged(horizontalOrigin, verticalOrigin, oldHorizontal, oldVertical)
        onScrollChangeListener?.onScrollChanged(
            horizontalOrigin,
            verticalOrigin,
            oldHorizontal,
            oldVertical,
        )
    }

    override fun destroyView() {
        onScrollChangeListener = null
        destroy()
    }

    override fun destroy() {
        onScrollChangeListener = null
        super.destroy()
    }

    override fun onView(perform: View.() -> Unit) = perform(this)

    override fun evaluate(
        code: String,
        resultCallback: ((String?) -> Unit)?,
    ) {
        evaluateJavascript(code, resultCallback)
    }

    override fun setup(
        url: PaywallURL,
        onRenderCrashed: (Boolean, Int) -> Unit,
    ) {
        this.onRenderCrashed = onRenderCrashed
        scrollEnabled = delegate?.state?.paywall?.isScrollEnabled ?: true
        mainScope.launch {
            val state = delegate?.state ?: return@launch
            if (state.paywall.onDeviceCache is OnDeviceCaching.Enabled) {
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            } else {
                settings.cacheMode = WebSettings.LOAD_DEFAULT
            }
            if (state.useMultipleUrls) {
                loadPaywallWithFallbackUrl(state.paywall)
            } else {
                loadUrl(url.value)
            }
        }
    }
}

internal fun webViewExists(): Boolean =
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WebView.getCurrentWebViewPackage() != null
        } else {
            runCatching { CookieManager.getInstance() }.isSuccess
        }
    } catch (e: Throwable) {
        false
    }
