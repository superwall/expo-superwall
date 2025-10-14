package com.superwall.sdk.paywall.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEvents
import com.superwall.sdk.config.models.OnDeviceCaching
import com.superwall.sdk.config.options.PaywallOptions
import com.superwall.sdk.dependencies.AttributesFactory
import com.superwall.sdk.dependencies.EnrichmentFactory
import com.superwall.sdk.dependencies.OptionsFactory
import com.superwall.sdk.dependencies.TrackingFactory
import com.superwall.sdk.dependencies.TriggerFactory
import com.superwall.sdk.game.GameControllerDelegate
import com.superwall.sdk.game.GameControllerEvent
import com.superwall.sdk.game.GameControllerManager
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.AlertControllerFactory
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.MainScope
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.models.paywall.PaywallPresentationStyle
import com.superwall.sdk.models.triggers.TriggerRuleOccurrence
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.manager.PaywallViewCache
import com.superwall.sdk.paywall.presentation.PaywallCloseReason
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.paywall.presentation.get_presentation_result.internallyGetPresentationResult
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.operators.storePresentationObjects
import com.superwall.sdk.paywall.presentation.internal.state.PaywallErrors
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.presentation.result.PresentationResult
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import com.superwall.sdk.paywall.view.delegate.PaywallViewDelegateAdapter
import com.superwall.sdk.paywall.view.delegate.PaywallViewEventCallback
import com.superwall.sdk.paywall.view.survey.SurveyManager
import com.superwall.sdk.paywall.view.webview.PaywallMessage
import com.superwall.sdk.paywall.view.webview.SWWebView
import com.superwall.sdk.paywall.view.webview.SWWebViewDelegate
import com.superwall.sdk.paywall.view.webview.SendPaywallMessages
import com.superwall.sdk.paywall.view.webview.messaging.PaywallMessageHandlerDelegate
import com.superwall.sdk.paywall.view.webview.messaging.PaywallStateDelegate
import com.superwall.sdk.paywall.view.webview.messaging.PaywallWebEvent
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.net.URI
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

class PaywallView(
    context: Context,
    val eventCallback: PaywallViewEventCallback? = null,
    var callback: PaywallViewDelegateAdapter? = null,
    val deviceHelper: DeviceHelper,
    val factory: Factory,
    val storage: LocalStorage,
    webView: SWWebView,
    private val cache: PaywallViewCache?,
    private val useMultipleUrls: Boolean,
    val controller: PaywallController,
) : FrameLayout(context),
    PaywallMessageHandlerDelegate,
    SWWebViewDelegate,
    PaywallStateDelegate by controller,
    ActivityEncapsulatable,
    GameControllerDelegate,
    SendPaywallMessages by webView.messageHandler {
    class PaywallController(
        initialState: PaywallViewState,
    ) : PaywallStateDelegate {
        private var _state = MutableStateFlow(initialState)
        val currentState = _state.asStateFlow()
        override val state: PaywallViewState
            get() = _state.value

        override fun updateState(update: PaywallViewState.Updates) {
            _state.update { currentState ->
                // Apply the transform to get new state
                val newState = update.transform(currentState)
                newState
            }
        }
    }

    private companion object {
        private val mainScope: MainScope = MainScope()
        private val ioScope: IOScope = IOScope()
        private val gameControllerJson by lazy {
            Json {
                encodeDefaults = true
                namingStrategy = JsonNamingStrategy.SnakeCase
            }
        }
    }

    interface Factory :
        TriggerFactory,
        OptionsFactory,
        AttributesFactory,
        EnrichmentFactory,
        TrackingFactory
    //region Public properties

    // We use a local webview so we can handle cases where webview process crashes
    internal var webView: SWWebView = webView
        private set

    fun scrollBy(y: Int) {
        webView.scrollBy(0, y)
    }

    fun scrollTo(y: Int) {
        webView.scrollTo(0, y)
    }

    //endregion

    //region Presentation properties

    // The full screen activity instance if this view has been presented in one.
    override var encapsulatingActivity: WeakReference<Activity>? = null
    private var shimmerView: PaywallShimmerView? = null

    private var loadingView: PaywallPurchaseLoadingView? = null

    //endregion
    // region State properties

    // / The paywall info
    override val info: PaywallInfo
        get() = controller.state.info

    // / The loading state of the paywall.
    var loadingState: PaywallLoadingState
        get() = state.loadingState
        private set(value) {

            controller.updateState(PaywallViewState.Updates.SetLoadingState(value))
        }

    val backgroundColor: Int
        get() {

            val style =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                } else {
                    Configuration.UI_MODE_NIGHT_UNDEFINED
                }
            return when (style) {
                Configuration.UI_MODE_NIGHT_YES ->
                    state.paywall.darkBackgroundColor
                        ?: state.paywall.backgroundColor

                else -> state.paywall.backgroundColor
            }
        }
    //endregion

    //region Initialization

    private var stateListener: Job? = null

    private fun stopStateListener() {
        stateListener?.cancel()
        stateListener = null
    }

    private fun startStateListener() {
        // Ensure any previous listener is cancelled before starting a new one
        stopStateListener()
        stateListener =
            ioScope.launch {
                controller.currentState
                    .map { it.loadingState }
                    .distinctUntilChanged { old, new -> old::class == new::class }
                    .collectLatest { loadingStateDidChange() }
            }
    }

    init {
        id = View.generateViewId()
        setBackgroundColor(backgroundColor)
    }

    //endregion

    internal fun set(
        request: PresentationRequest,
        paywallStatePublisher: MutableSharedFlow<PaywallState>,
        unsavedOccurrence: TriggerRuleOccurrence?,
    ) {
        controller.updateState(
            PaywallViewState.Updates.SetRequest(
                request,
                paywallStatePublisher,
                unsavedOccurrence,
            ),
        )
    }

    internal fun setupShimmer(shimmerView: PaywallShimmerView) {
        this.shimmerView = shimmerView
        if (shimmerView is View) {
            // Note: This always _is_ true, but the compiler doesn't know that
            shimmerView.setupFor(this, loadingState)
        }
    }

    internal fun setupLoading(loadingView: PaywallPurchaseLoadingView) {
        this.loadingView = loadingView
        if (loadingView is View) {
            loadingView.setupFor(this, loadingState)
        }
    }
    //region Public functions

    fun setupWith(
        shimmerView: PaywallShimmerView,
        loadingView: PaywallPurchaseLoadingView,
    ) {
        if (webView.parent == null) {
            addView(webView)
            webView.layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        this.shimmerView = shimmerView
        this.loadingView = loadingView
    }

    fun present(
        presenter: Activity,
        request: PresentationRequest,
        unsavedOccurrence: TriggerRuleOccurrence?,
        presentationStyleOverride: PaywallPresentationStyle?,
        paywallStatePublisher: MutableSharedFlow<PaywallState>,
        completion: (Boolean) -> Unit,
    ) {
        if (webView.parent == null) addView(webView)
        cache?.acquireLoadingView()?.let {
            setupLoading(it)
        }

        cache?.acquireShimmerView()?.let {
            setupShimmer(it)
        }
        set(request, paywallStatePublisher, unsavedOccurrence)
        controller.updateState(
            PaywallViewState.Updates.SetPresentationConfig(
                presentationStyleOverride,
                completion,
            ),
        )

        SuperwallPaywallActivity.startWithView(
            presenter,
            this,
            state.cacheKey,
            state.presentationStyle,
        )
        startStateListener()
    }

    override fun updateState(update: PaywallViewState.Updates) {
        controller.updateState(update)
    }

    override val state: PaywallViewState
        get() = controller.state

    fun beforeViewCreated() {
        if (state.isBrowserViewPresented) {
            return
        }
        shimmerView?.checkForOrientationChanges()
        presentationWillBegin()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = state.interceptTouchEvents

    private fun presentationWillBegin() {
        if (!state.presentationWillPrepare || state.presentationDidFinishPrepare) {
            return
        }
        controller.updateState(PaywallViewState.Updates.PresentationWillBegin)

        Superwall.instance.dependencyContainer.delegateAdapter
            .willPresentPaywall(info)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Temporary disabled
                // webView.setRendererPriorityPolicy(RENDERER_PRIORITY_IMPORTANT, true)
            } catch (e: Throwable) {
                Logger.debug(
                    LogLevel.info,
                    LogScope.paywallView,
                    "Cannot set webview priority when beginning presentation",
                    error = e,
                )
            }
        }
        webView.scrollTo(0, 0)
        if (loadingState is PaywallLoadingState.Ready) {
            webView.messageHandler.handle(PaywallMessage.TemplateParamsAndUserAttributes)
        }
        controller.updateState(PaywallViewState.Updates.ShimmerStarted)
        trackShimmerStart()
    }

    fun beforeOnDestroy() {
        if (state.isBrowserViewPresented) {
            return
        }
        Superwall.instance.presentationItems.paywallInfo = info
        Superwall.instance.dependencyContainer.delegateAdapter
            .willDismissPaywall(info)
    }

    suspend fun destroyed() {
        if (state.isBrowserViewPresented) {
            return
        }

        // Ensure we stop listening to state changes when being destroyed
        stopStateListener()

        ioScope.launch {
            trackClose()
        }

        // Reset spinner
        val isShowingSpinner =
            loadingState is PaywallLoadingState.LoadingPurchase || loadingState is PaywallLoadingState.ManualLoading
        if (isShowingSpinner) {
            controller.updateState(PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.Ready))
        }

        Superwall.instance.dependencyContainer.delegateAdapter
            .didDismissPaywall(info)

        val result = state.paywallResult ?: PaywallResult.Declined()

        state.paywallStatePublisher?.emit(PaywallState.Dismissed(info, result))

        if (!state.callbackInvoked) {
            callback?.onFinished(
                paywall = this,
                result = result,
                shouldDismiss = false,
            )
        }

        if (state.paywall.closeReason.stateShouldComplete) {
            controller.updateState(PaywallViewState.Updates.ClearStatePublisher)
        }

        GameControllerManager.shared.clearDelegate(this)
        resetPresentationPreparations()

        controller.updateState(PaywallViewState.Updates.CleanupAfterDestroy)
        cache?.activePaywallVcKey = null
    }

    private fun resetPresentationPreparations() {
        controller.updateState(PaywallViewState.Updates.ResetPresentationPreparations)
    }

    internal fun dismiss(
        result: PaywallResult,
        closeReason: PaywallCloseReason,
        completion: (() -> Unit)? = null,
    ) {
        controller.updateState(
            PaywallViewState.Updates.InitiateDismiss(
                result,
                closeReason,
                completion,
            ),
        )

        val isDeclined = state.paywallResult is PaywallResult.Declined
        val isManualClose = closeReason is PaywallCloseReason.ManualClose

        suspend fun dismissView() {
            if (isDeclined && isManualClose) {
                val trackedEvent = InternalSuperwallEvent.PaywallDecline(paywallInfo = info)

                val presentationResult =
                    withErrorTracking {
                        Superwall.instance.internallyGetPresentationResult(
                            event = trackedEvent,
                            isImplicit = true,
                        )
                    }.toResult()
                val presentingPlacement = info.presentedByEventWithName
                val presentedByPaywallDecline =
                    presentingPlacement == SuperwallEvents.PaywallDecline.rawName
                val presentedByTransactionAbandon =
                    presentingPlacement == SuperwallEvents.TransactionAbandon.rawName
                val presentedByTransactionFail =
                    presentingPlacement == SuperwallEvents.TransactionFail.rawName

                factory.track(trackedEvent).getOrNull()
                val capturedResult = presentationResult.getOrNull()
                if (capturedResult != null &&
                    capturedResult is PresentationResult.Paywall &&
                    !presentedByPaywallDecline &&
                    !presentedByTransactionAbandon &&
                    !presentedByTransactionFail
                ) {
                    // If a paywall_decline trigger is active and the current paywall wasn't presented
                    // by paywall_decline, transaction_abandon, or transaction_fail, it lands here so
                    // as not to dismiss the paywall. track() will do that before presenting the next paywall.
                    return
                }
            }

            callback?.let {
                controller.updateState(PaywallViewState.Updates.CallbackInvoked)
                it.onFinished(
                    paywall = this,
                    result = result,
                    shouldDismiss = true,
                )
            } ?: run {
                dismiss(presentationIsAnimated = false)
            }
            stopStateListener()
        }

        val finalDismiss = {
            ioScope.launch {
                dismissView()
            }
        }

        SurveyManager.presentSurveyIfAvailable(
            state.paywall.surveys,
            paywallResult = result,
            paywallCloseReason = closeReason,
            activity =
                encapsulatingActivity?.get() ?: run {
                    finalDismiss()
                    return
                },
            paywallView = this,
            loadingState = loadingState,
            isDebuggerLaunched = state.request?.flags?.isDebuggerLaunched == true,
            paywallInfo = info,
            storage = storage,
            factory = factory,
        ) { res ->
            controller.updateState(PaywallViewState.Updates.UpdateSurveyState(res))
            finalDismiss()
        }
    }

    //endregion

    //region Lifecycle

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Assert if no `request`
        // fatalAssert(request != null, "Must be presenting a Paywallview with a `request` instance.")

        if (loadingState is PaywallLoadingState.Unknown) {
            loadWebView()
        }
    }

    // Lets the view know that presentation has finished.
    // Only called once per presentation.
    fun onViewCreated() {
        state.viewCreatedCompletion?.invoke(true)
        controller.updateState(PaywallViewState.Updates.ClearViewCreatedCompletion)

        if (state.presentationDidFinishPrepare) {
            return
        }
        ioScope.launch {
            state.paywallStatePublisher?.let {
                Superwall.instance.storePresentationObjects(state.request, it)
            }
        }
        state.unsavedOccurrence?.let {
            storage.coreDataManager.save(triggerRuleOccurrence = it)
            controller.updateState(PaywallViewState.Updates.ClearUnsavedOccurrence)
        }

        controller.updateState(PaywallViewState.Updates.SetPresentedAndFinished)
        Superwall.instance.dependencyContainer.delegateAdapter
            .didPresentPaywall(info)
        ioScope.launch {
            trackOpen()
        }
        GameControllerManager.shared.setDelegate(this@PaywallView)

        val currentOrientation = resources.configuration.orientation
        initialOrientation = currentOrientation
    }

    private var initialOrientation: Int? = null

    private suspend fun trackOpen() {
        storage.trackPaywallOpen()
        webView.messageHandler.handle(PaywallMessage.PaywallOpen)
        val trackedEvent =
            InternalSuperwallEvent.PaywallOpen(
                info,
                factory.getCurrentUserAttributes(),
                demandTier = factory.demandTier(),
                demandScore = factory.demandScore(),
            )
        factory.track(trackedEvent)
    }

    private suspend fun trackClose() {
        val trackedEvent =
            InternalSuperwallEvent.PaywallClose(
                info,
                state.surveyPresentationResult,
            )
        factory.track(trackedEvent)
    }

    override fun eventDidOccur(paywallWebEvent: PaywallWebEvent) {
        ioScope.launch {
            eventCallback?.eventDidOccur(paywallWebEvent, this@PaywallView)
        }
    }

    //endregion

    //region Presentation

    private fun dismiss(presentationIsAnimated: Boolean) {
        // TODO: SW-2162 Implement animation support
        // https://linear.app/superwall/issue/SW-2162/%5Bandroid%5D-%5Bv1%5D-get-animated-presentation-working

        encapsulatingActivity?.get()?.finish()
    }

    private fun showLoadingView() {
        val transactionBackgroundView =
            Superwall.instance.options.paywalls.transactionBackgroundView
        if (transactionBackgroundView != PaywallOptions.TransactionBackgroundView.SPINNER) {
            return
        }
        loadingView?.let {
            mainScope.launch {
                it.showLoading()
            }
        }
    }

    private fun hideLoadingView() {
        loadingView?.let {
            mainScope.launch {
                it.hideLoading()
            }
        }
    }

    private fun trackShimmerStart() {
        val trackedEvent =
            InternalSuperwallEvent.ShimmerLoad(
                state = InternalSuperwallEvent.ShimmerLoad.State.Started,
                paywallId = state.paywall.identifier,
                visibleDuration = null,
                preloadingEnabled = factory.makeSuperwallOptions().paywalls.shouldPreload,
                delay =
                    state.paywall.presentation.delay
                        .toDouble(),
            )
        ioScope.launch {
            factory.track(trackedEvent)
        }
    }

    fun onThemeChanged() {
        webView.messageHandler.handle(PaywallMessage.TemplateParamsAndUserAttributes)
    }

    private fun showShimmerView() {
        shimmerView?.let {
            mainScope.launch {
                it.showShimmer()
            }
        }
    }

    private fun hideShimmerView() {
        shimmerView?.let {
            mainScope.launch {
                it.hideShimmer()
            }
        }
        val visible = state.paywall.shimmerLoadingInfo.startAt
        val now = Date()
        controller.updateState(PaywallViewState.Updates.ShimmerEnded)
        ioScope.launch {
            val trackedEvent =
                InternalSuperwallEvent.ShimmerLoad(
                    state = InternalSuperwallEvent.ShimmerLoad.State.Complete,
                    paywallId = state.paywall.identifier,
                    visibleDuration =
                        if (visible != null) {
                            (now.time - visible.time).milliseconds.toDouble(
                                DurationUnit.MILLISECONDS,
                            )
                        } else {
                            0.0
                        },
                    delay =
                        state.paywall.presentation.delay
                            .toDouble(),
                    preloadingEnabled = factory.makeSuperwallOptions().paywalls.shouldPreload,
                )
            factory.track(trackedEvent)
        }
    }

    fun showRefreshButtonAfterTimeout(isVisible: Boolean) {
        // TODO: Implement this
    }

    fun showAlert(
        title: String? = null,
        message: String? = null,
        actionTitle: String? = null,
        closeActionTitle: String = "Done",
        action: (() -> Unit)? = null,
        onClose: (() -> Unit)? = null,
    ) {
        val activity =
            encapsulatingActivity?.get() ?: kotlin.run {
                return
            }

        mainScope.launch {
            val alertController =
                AlertControllerFactory.make(
                    context = activity,
                    title = title,
                    message = message,
                    actionTitle = actionTitle,
                    closeActionTitle = closeActionTitle,
                    action = {
                        action?.invoke()
                    },
                    onClose = {
                        onClose?.invoke()
                    },
                )
            alertController.show()

            controller.updateState(PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.Ready))
        }
    }

    //endregion

    //region State

    internal fun loadingStateDidChange() {
        if (state.isPresented) {
            mainScope.launch {
                when (loadingState) {
                    is PaywallLoadingState.Unknown -> {
                    }

                    is PaywallLoadingState.LoadingPurchase, is PaywallLoadingState.ManualLoading -> {
                        // Add Loading View
                        showLoadingView()
                    }

                    is PaywallLoadingState.LoadingURL -> {
                        showShimmerView()
                        showRefreshButtonAfterTimeout(isVisible = true)
                    }

                    is PaywallLoadingState.Ready -> {
                        ioScope.launch {
                            delay(state.paywall.presentation.delay)
                            mainScope.launch {
                                showRefreshButtonAfterTimeout(false)
                                hideLoadingView()
                                hideShimmerView()
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadWebView() {
        ioScope.launch {
            val url = state.paywall.url
            controller.updateState(PaywallViewState.Updates.WebLoadingStarted)

            launch {
                val trackedEvent =
                    InternalSuperwallEvent.PaywallWebviewLoad(
                        state = InternalSuperwallEvent.PaywallWebviewLoad.State.Start(),
                        paywallInfo = this@PaywallView.info,
                    )
                factory.track(trackedEvent)
            }

            webView.onRenderProcessCrashed = {
                val isOverO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.paywallView,
                    message =
                        "Webview Process has crashed for paywall with identifier: ${state.paywall.identifier}.\n" +
                            "Crashed by the system: ${
                                if (isOverO) it.didCrash() else "Unknown"
                            } - priority ${
                                if (isOverO) it.rendererPriorityAtExit() else "Unknown"
                            }",
                )
                recreateWebview()
            }
            if (factory.makeSuperwallOptions().paywalls.timeoutAfter != null) {
                webView.onTimeout = {
                    ioScope.launch {
                        state.paywallStatePublisher?.emit(
                            PaywallState.PresentationError(
                                PaywallErrors.Timeout(it.toString()),
                            ),
                        )
                    }
                }
            }

            webView.scrollEnabled = state.paywall.isScrollEnabled ?: true
            mainScope.launch {
                if (state.paywall.onDeviceCache is OnDeviceCaching.Enabled) {
                    webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                } else {
                    webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
                }
                if (useMultipleUrls) {
                    webView.loadPaywallWithFallbackUrl(state.paywall)
                } else {
                    webView.loadUrl(url.value)
                }
            }
            controller.updateState(PaywallViewState.Updates.SetLoadingState(PaywallLoadingState.LoadingURL))
        }
    }

    private fun recreateWebview() {
        removeView(webView)
        webView =
            SWWebView(context, webView.messageHandler, options = {
                factory.makeSuperwallOptions().paywalls
            })
        addView(webView)
        webView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        webView.messageHandler.handle(PaywallMessage.PaywallOpen)
        loadWebView()
    }

//endregion

//region Deep linking

    override fun presentBrowserInApp(url: String) {
        try {
            val parsedUrl = URI(url)
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            customTabsIntent.launchUrl(context, parsedUrl.toString().toUri())
            controller.updateState(PaywallViewState.Updates.SetBrowserPresented(true))
        } catch (e: MalformedURLException) {
            Logger.debug(
                logLevel = LogLevel.debug,
                scope = LogScope.paywallView,
                message = "Invalid URL provided for \"Open In-App URL\" click behavior.",
            )
        } catch (e: Throwable) {
            Logger.debug(
                logLevel = LogLevel.debug,
                scope = LogScope.paywallView,
                message = "Exception thrown for \"Open In-App URL\" click behavior.",
            )
        }
    }

    override fun presentBrowserExternal(url: String) {
        try {
            val parsedUrl = URI(url)
            val intent = Intent(Intent.ACTION_VIEW, parsedUrl.toString().toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: MalformedURLException) {
            Logger.debug(
                logLevel = LogLevel.debug,
                scope = LogScope.paywallView,
                message = "Invalid URL provided for \"Open External URL\" click behavior.",
            )
        } catch (e: Throwable) {
            Logger.debug(
                logLevel = LogLevel.debug,
                scope = LogScope.paywallView,
                message = "Exception thrown for \"Open External URL\" click behavior.",
            )
        }
    }

    override fun evaluate(
        code: String,
        resultCallback: ((String?) -> Unit)?,
    ) {
        webView.evaluateJavascript(code, resultCallback)
    }

    override fun openDeepLink(url: String) {
        var uri = url.toUri()
        eventDidOccur(PaywallWebEvent.OpenedDeepLink(uri))
        val context = encapsulatingActivity?.get()
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, uri)
        context?.startActivity(deepLinkIntent)
    }

    //region GameController
    override fun gameControllerEventOccured(event: GameControllerEvent) {
        val payload =
            try {
                gameControllerJson.encodeToString(event)
            } catch (e: Throwable) {
                null
            }
        webView.evaluateJavascript("window.paywall.accept([$payload])", null)
        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.paywallView,
            message = "Game controller event occurred: $payload",
        )
    }

//endregion

//region Misc

    // Android-specific
    fun prepareToDisplay() {
        // Check if the view already has a parent
        val parentViewGroup = this@PaywallView.parent as? ViewGroup
        parentViewGroup?.removeView(this@PaywallView)
        cache?.activePaywallVcKey = state.cacheKey
    }

    fun cleanup() {
        encapsulatingActivity?.clear()
        callback = null
        webView.onScrollChangeListener = null
        (parent as? ViewGroup)?.removeAllViews()
        removeAllViews()
        detachAllViewsFromParent()
        // Cancel any active state listener to avoid leaks
        stopStateListener()
    }

    internal fun destroyWebview() {
        webView.destroy()
    }

//endregion
}

interface ActivityEncapsulatable {
    var encapsulatingActivity: WeakReference<Activity>?
}
