package com.superwall.sdk.paywall.view

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.ComponentCaller
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.superwall.sdk.Superwall
import com.superwall.sdk.dependencies.DeviceHelperFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.isDarkColor
import com.superwall.sdk.misc.isLightColor
import com.superwall.sdk.misc.onError
import com.superwall.sdk.misc.readableOverlayColor
import com.superwall.sdk.models.paywall.LocalNotification
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.paywall.PaywallPresentationStyle
import com.superwall.sdk.network.JsonFactory
import com.superwall.sdk.paywall.presentation.PaywallCloseReason
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.view.webview.PaywallWebUI
import com.superwall.sdk.store.transactions.notifications.NotificationScheduler
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuperwallPaywallActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001
        const val NOTIFICATION_CHANNEL_ID = "com.superwall.android.notifications"
        private const val NOTIFICATION_CHANNEL_NAME = "Trial Reminder Notifications"
        private const val NOTIFICATION_CHANNEL_DESCRIPTION =
            "Notifications sent when a free trial is about to end."
        private const val VIEW_KEY = "viewKey"
        private const val PRESENTATION_STYLE_KEY = "presentationStyleKey"
        private const val IS_LIGHT_BACKGROUND_KEY = "isLightBackgroundKey"
        private const val ACTIVE_PAYWALL_TAG = "active_paywall"

        private const val DEFAULT_DELAY = 300L

        fun startWithView(
            context: Context,
            view: PaywallView,
            key: String = UUID.randomUUID().toString(),
            presentationStyleOverride: PaywallPresentationStyle? = null,
        ) {
            // We force this in main scope in case the user started it from a non-main thread
            CoroutineScope(Dispatchers.Main).launch {
                view.prepareViewForDisplay(key)
                val intent =
                    Intent(context, SuperwallPaywallActivity::class.java).apply {
                        putExtra(VIEW_KEY, key)
                        putExtra(
                            PRESENTATION_STYLE_KEY,
                            presentationStyleOverride?.toIntentString(
                                JsonFactory.JSON_POLYMORPHIC,
                            ),
                        )
                        putExtra(
                            IS_LIGHT_BACKGROUND_KEY,
                            view.state.paywall.backgroundColor
                                .isLightColor(),
                        )
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                context.startActivity(intent)
            }
        }

        private fun PaywallView.prepareViewForDisplay(key: String) {
            webView.enableBackgroundRendering()
            webView.attach(this)
            val viewStorageViewModel = Superwall.instance.dependencyContainer.makeViewStore()
            // If we started it directly and the view does not have shimmer and loading attached
            // We set them up for this PaywallView
            if (children.none { it is LoadingView || it is ShimmerView }) {
                val loading =
                    (viewStorageViewModel.retrieveView(LoadingView.TAG) as LoadingView)
                val style = state.paywall.presentation.style
                val shimmer =
                    if (style is PaywallPresentationStyle.Popup) {
                        ShimmerView(this@prepareViewForDisplay.context).apply {
                            layoutParams =
                                FrameLayout.LayoutParams(
                                    ((style.width * Resources.getSystem().displayMetrics.widthPixels) / 100).toInt(),
                                    ((style.height * Resources.getSystem().displayMetrics.heightPixels) / 100).toInt(),
                                )
                        }
                    } else {
                        (viewStorageViewModel.retrieveView(ShimmerView.TAG) as ShimmerView)
                    }

                setupWith(shimmer, loading)
            }
            viewStorageViewModel.storeView(key, this)
        }
    }

    private var contentView: View? = null
    private var notificationPermissionCallback: NotificationPermissionCallback? = null
    private val isBottomSheetView
        get() = contentView is CoordinatorLayout && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private val isPopupView
        get() = contentView is androidx.constraintlayout.widget.ConstraintLayout && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    override fun setContentView(view: View?) {
        super.setContentView(view)
        contentView = view
    }

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private fun paywallView(): PaywallView? = contentView?.findViewWithTag(ACTIVE_PAYWALL_TAG)

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        val presentationStyle =
            intent.getStringExtra(PRESENTATION_STYLE_KEY)?.let {
                PaywallPresentationStyle.fromIntentString(JsonFactory.JSON_POLYMORPHIC, it)
            }

        // Show content behind the status bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val isLightBackground = intent.getBooleanExtra(IS_LIGHT_BACKGROUND_KEY, false)
            if (isLightBackground) {
                window.insetsController?.let {
                    it.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    )
                }
            }
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        // Check savedInstanceState first (for activity recreation), then fallback to intent
        val key = savedInstanceState?.getString(VIEW_KEY) ?: intent.getStringExtra(VIEW_KEY)
        if (key == null) {
            Logger.debug(
                LogLevel.error,
                LogScope.paywallView,
                "No view key found in savedInstanceState or intent, finishing activity",
            )
            finish() // Close the activity if there's no key
            return
        }

        val viewStorageViewModel =
            try {
                Superwall.instance.dependencyContainer.makeViewStore()
            } catch (e: Exception) {
                Logger.debug(
                    LogLevel.error,
                    LogScope.paywallView,
                    "Cannot access viewStore or create view - has Superwall been initialised?",
                )
                return
            }

        val view =
            viewStorageViewModel.retrieveView(key) as? PaywallView ?: run {
                Logger.debug(
                    LogLevel.error,
                    LogScope.paywallView,
                    "PaywallView not found for key: $key. Activity may have been recreated after cleanup.",
                )
                finish() // Close the activity if the view associated with the key is not found
                return
            }
        val isRestoredButCleared = savedInstanceState != null && paywallView() == null
        if (isRestoredButCleared) {
            Logger.debug(
                LogLevel.debug,
                LogScope.paywallView,
                "Activity restored but PaywallView cleared - recreating with available view",
            )
            // The view was cleared during cleanup but activity is being recreated
            // Get the current paywall view from Superwall instance and recreate the activity
            val currentPaywallView = Superwall.instance.paywallView
            if (currentPaywallView != null) {
                // Check if the view has been cleaned up (no child views)
                if (currentPaywallView.childCount == 0) {
                    Logger.debug(
                        LogLevel.debug,
                        LogScope.paywallView,
                        "PaywallView was cleaned up, restoring child views",
                    )
                    // Restore the WebView and other child views
                    restorePaywallViewAfterCleanup(currentPaywallView, key)
                }

                // Store the view again with the same key for this activity
                viewStorageViewModel.storeView(key, currentPaywallView)
                // Continue with normal activity setup using the restored view
                setupActivityWithView(currentPaywallView, presentationStyle)
                return
            } else {
                Logger.debug(
                    LogLevel.error,
                    LogScope.paywallView,
                    "No PaywallView available in Superwall instance for recreation",
                )
                finish()
                return
            }
        }

        setupActivityWithView(view, presentationStyle)
    }

    override fun onNewIntent(
        intent: Intent,
        caller: ComponentCaller,
    ) {
        super.onNewIntent(intent, caller)
        intent.data?.let {
            Superwall.handleDeepLink(it)
        }
    }

    private fun setupActivityWithView(
        view: PaywallView,
        presentationStyle: PaywallPresentationStyle?,
    ) {
        window.decorView.setBackgroundColor(view.backgroundColor)
        view.registerIntent(this, this)

        val isBottomSheetStyle =
            presentationStyle is PaywallPresentationStyle.Drawer || presentationStyle is PaywallPresentationStyle.Modal
        val isPopupStyle = presentationStyle is PaywallPresentationStyle.Popup

        (view.parent as? ViewGroup)?.removeView(view)
        view.tag = ACTIVE_PAYWALL_TAG
        view.encapsulatingActivity = WeakReference(this)
        // If it's a bottom sheet or dialog, we set activity as transparent and show the UI in a container
        if (isBottomSheetStyle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setupBottomSheetLayout(
                view,
                presentationStyle is PaywallPresentationStyle.Modal,
                if (presentationStyle is PaywallPresentationStyle.Drawer) presentationStyle.height else 0.0,
                if (presentationStyle is PaywallPresentationStyle.Drawer) presentationStyle.cornerRadius else 0.0,
            )
        } else if (isPopupStyle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setupPopupLayout(
                view,
                presentationStyle.height,
                presentationStyle.width,
                presentationStyle.cornerRadius,
            )
        } else {
            setContentView(view)
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val shouldConsumeDismiss =
                        if (paywallView()?.state?.paywall?.rerouteBackButton == Paywall.ToggleMode.ENABLED) {
                            Superwall.instance.options.paywalls.onBackPressed
                                ?.let { it(paywallView()?.info) }
                                ?: false
                        } else {
                            false
                        }
                    if (!shouldConsumeDismiss) {
                        view.dismiss(
                            result = PaywallResult.Declined(),
                            closeReason = PaywallCloseReason.ManualClose,
                        )
                    }
                }
            },
        )
        try {
            supportActionBar?.hide()
        } catch (e: Throwable) {
        }
        window.navigationBarColor = view.backgroundColor
        // TODO: handle animation and style from `presentationStyleOverride`
        when (presentationStyle) {
            is PaywallPresentationStyle.Push -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(
                        OVERRIDE_TRANSITION_OPEN,
                        com.superwall.sdk.R.anim.slide_in_right,
                        com.superwall.sdk.R.anim.slide_out_left,
                    )
                } else {
                    overridePendingTransition(
                        com.superwall.sdk.R.anim.slide_in_right,
                        com.superwall.sdk.R.anim.slide_out_left,
                    )
                }
            }

            is PaywallPresentationStyle.Fullscreen -> {
                WindowCompat.setDecorFitsSystemWindows(window, true)

                // Set the navigation bar color to the paywall background color
                enableEdgeToEdge(
                    navigationBarStyle =
                        when (
                            view.state.paywall.backgroundColor
                                .isDarkColor()
                        ) {
                            true -> SystemBarStyle.dark(view.state.paywall.backgroundColor)
                            else ->
                                SystemBarStyle.light(
                                    scrim = view.state.paywall.backgroundColor,
                                    darkScrim =
                                        view.state.paywall.backgroundColor
                                            .readableOverlayColor(),
                                )
                        },
                )

                // Set the bottom margin of the webview to the height of the system bars
                view.webView.onView {
                    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
                        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            bottomMargin = insets.bottom
                        }

                        WindowInsetsCompat.CONSUMED
                    }
                }
            }

            is PaywallPresentationStyle.FullscreenNoAnimation -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
                    overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
                } else {
                    overridePendingTransition(0, 0)
                }
                enableEdgeToEdge(
                    navigationBarStyle =
                        when (
                            view.state.paywall.backgroundColor
                                .isDarkColor()
                        ) {
                            true -> SystemBarStyle.dark(view.state.paywall.backgroundColor)
                            else ->
                                SystemBarStyle.light(
                                    scrim = view.state.paywall.backgroundColor,
                                    darkScrim =
                                        view.state.paywall.backgroundColor
                                            .readableOverlayColor(),
                                )
                        },
                )
            }

            is PaywallPresentationStyle.Modal,
            is PaywallPresentationStyle.None,
            is PaywallPresentationStyle.Drawer,
            null,
            -> {
                // Do nothing
            }

            is PaywallPresentationStyle.Popup -> {
                // Disable activity transitions for popup - we handle animation ourselves
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
                    overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
                } else {
                    overridePendingTransition(0, 0)
                }
            }
        }
    }

    private fun setupBottomSheetLayout(
        paywallView: PaywallView,
        isModal: Boolean,
        height: Double = 0.0,
        cornerRadius: Double = 0.0,
    ) {
        val activityView =
            layoutInflater.inflate(com.superwall.sdk.R.layout.activity_bottom_sheet, null)
        setContentView(activityView)
        initBottomSheetBehavior(isModal, height)
        val container =
            activityView.findViewById<FrameLayout>(com.superwall.sdk.R.id.container)
        activityView.setOnClickListener { finish() }
        container.addView(paywallView)
        container.requestLayout()
        val radius =
            cornerRadius.toFloat() * Resources.getSystem().displayMetrics.density // Convert dp to px

        val drawable =
            roundedBackground(radius)

        container.background = drawable
    }

    private fun roundedBackground(radius: Float) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii =
                floatArrayOf(
                    radius,
                    radius, // top-left
                    radius,
                    radius, // top-right
                    radius,
                    radius, // top-left
                    radius,
                    radius, // top-right
                )
        }

    private fun setupPopupLayout(
        paywallView: PaywallView,
        height: Double = 0.0,
        width: Double = 0.0,
        cornerRadius: Double = 0.0,
    ) {
        val activityView =
            layoutInflater.inflate(com.superwall.sdk.R.layout.activity_popup, null)
        setContentView(activityView)
        val container =
            activityView.findViewById<FrameLayout>(com.superwall.sdk.R.id.container)

        paywallView.layoutParams =
            FrameLayout.LayoutParams(
                ((width * Resources.getSystem().displayMetrics.widthPixels) / 100).toInt(),
                ((height * Resources.getSystem().displayMetrics.heightPixels) / 100).toInt(),
            )
        val radius =
            cornerRadius.toFloat() * Resources.getSystem().displayMetrics.density // Convert dp to px

        container.background = roundedBackground(radius)

        container.setOnClickListener { /* consume click */ }
        container.addView(paywallView)
        container.requestLayout()

        // Animate popup entrance: scale and fade in from center
        container.scaleX = 0f
        container.scaleY = 0f
        container.alpha = 0f

        val scaleX = ObjectAnimator.ofFloat(container, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(container, "scaleY", 0f, 1f)
        val fadeIn = ObjectAnimator.ofFloat(container, "alpha", 0f, 1f)

        val animatorSet =
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, fadeIn)
                duration = paywallView()
                    ?.state
                    ?.paywall
                    ?.presentation
                    ?.delay ?: DEFAULT_DELAY
                interpolator = OvershootInterpolator(1.1f)
            }
        animatorSet.start()
    }

    private var bottomSheetCallback: BottomSheetCallback? = null

    private fun initBottomSheetBehavior(
        isModal: Boolean,
        height: Double,
    ) {
        val content = contentView as ViewGroup
        val bottomSheetBehavior = BottomSheetBehavior.from(content.getChildAt(0))
        if (!isModal) {
            val normalizedHeight = (if (height > 1.0) height / 100 else height).toFloat()
            // Clamp to (0, 1) since 0.0 = STATE_COLLAPSED and 1.0 = STATE_EXPANDED
            bottomSheetBehavior.halfExpandedRatio = normalizedHeight.coerceIn(0.01f, 0.99f)
        } else {
            // If it's a Modal, we want it to cover only 95% of the screen when expanded
            content.updateLayoutParams {
                (this as FrameLayout.LayoutParams).topMargin =
                    (Resources.getSystem().displayMetrics.heightPixels * 0.05).toInt()
            }
        }
        bottomSheetBehavior.skipCollapsed = true

        val setState = {
            if (!isModal) {
                // Expanded by default
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        // Check if we need to delay state change for Samsung devices on Android 14
        val isSamsungAndroid14 =
            Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                (
                    Build.MANUFACTURER.equals("samsung", ignoreCase = true) ||
                        Build.BRAND.equals("samsung", ignoreCase = true)
                )

        if (isSamsungAndroid14) {
            // Post state change to next frame after layout is complete
            // This fixes timing issues on Samsung devices with Android 14
            content.post { setState() }
        } else {
            setState()
        }
        content.invalidate()
        var currentWebViewScroll = 0
        if (isModal) {
            paywallView()?.webView?.onScrollChangeListener =
                object : PaywallWebUI.OnScrollChangeListener {
                    override fun onScrollChanged(
                        currentHorizontalScroll: Int,
                        currentVerticalScroll: Int,
                        oldHorizontalScroll: Int,
                        oldcurrentVerticalScroll: Int,
                    ) {
                        currentWebViewScroll = currentVerticalScroll
                    }
                }
        }

        bottomSheetCallback =
            object :
                BottomSheetCallback() {
                override fun onStateChanged(
                    bottomSheet: View,
                    newState: Int,
                ) {
                    // If it is an expanded modal and webview is scrolling, we do not allow dismissing
                    if (isModal && newState == BottomSheetBehavior.STATE_DRAGGING && currentWebViewScroll > 0) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        // If it is a modal, we skip the half-collapsed state when collapsing
                        if (isModal && newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            finish()
                        }
                    }
                }

                override fun onSlide(
                    bottomSheet: View,
                    slideOffset: Float,
                ) {
                    val webView = paywallView()?.webView
                    if (webView != null) {
                        webView.onView {
                            bottomSheetBehavior.isDraggable = !canScrollVertically(-1)
                        }
                    }
                }
            }
        bottomSheetCallback?.let {
            bottomSheetBehavior.addBottomSheetCallback(
                it,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val paywallVc = paywallView() ?: return

        if (paywallVc.state.isBrowserViewPresented) {
            paywallVc.updateState(PaywallViewState.Updates.SetBrowserPresented(true))
        }

        paywallVc.beforeViewCreated()
    }

    private fun setTransparentBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setTranslucent(true)
            val colorFrom = Color.argb(0, 0, 0, 0)
            val colorTo = Color.argb(200, 0, 0, 0)
            with(ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)) {
                setDuration(600) // milliseconds
                addUpdateListener { animator -> window.setBackgroundDrawable(ColorDrawable(animator.animatedValue as Int)) }
                start()
            }
        } else {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setTheme(android.R.style.Theme_Translucent_NoTitleBar)
        }
    }

    private fun hideBottomSheetAndFinish() {
        val colorFrom = Color.argb(200, 0, 0, 0)
        val colorTo = Color.argb(0, 0, 0, 0)

        // First animate the background dim, then call finish on the view
        with(ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)) {
            setDuration(DEFAULT_DELAY) // milliseconds
            addUpdateListener { animator ->
                val e = ((animator.animatedValue as Int) / colorFrom)
                if (e < 0.1) {
                    super.finish()
                }
                window.setBackgroundDrawable(ColorDrawable(animator.animatedValue as Int))
            }
            start()
        }
    }

    private fun hidePopupAndFinish() {
        super.finish()
    }

    override fun onResume() {
        super.onResume()
        val paywallVc = paywallView() ?: return
        if (isBottomSheetView || isPopupView) {
            setTransparentBackground()
        }
        paywallVc.onViewCreated()
        paywallVc.webView.onView {
            requestFocus()
        }
    }

    override fun onPause() {
        super.onPause()

        val paywallVc = paywallView() ?: return
        mainScope.launch {
            paywallVc.beforeOnDestroy()
        }
    }

    override fun onStop() {
        super.onStop()

        val paywallVc = paywallView() ?: return

        mainScope.launch {
            paywallVc.destroyed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the view key so we can restore it on recreation
        intent.getStringExtra(VIEW_KEY)?.let { key ->
            outState.putString(VIEW_KEY, key)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val content = contentView as? ViewGroup?
        if (content != null && content is CoordinatorLayout) {
            val bottomSheetBehavior = BottomSheetBehavior.from(content.getChildAt(0))
            bottomSheetCallback?.let {
                bottomSheetBehavior.removeBottomSheetCallback(it)
            }
        }
        val pv = intent.getStringExtra(VIEW_KEY)
        withErrorTracking {
            if (pv != null) {
                (
                    Superwall.instance.dependencyContainer
                        .makeViewStore()
                        .retrieveView(pv) as? PaywallView?
                )?.cleanup()
            }
        }.onError {
            Logger.debug(
                LogLevel.error,
                LogScope.paywallView,
                "Error cleaning up PaywallView: $it",
            )
        }
        paywallView()?.cleanup()
        content?.removeAllViews()
        // Clear reference to activity in the view
        (paywallView() as? ActivityEncapsulatable)?.encapsulatingActivity = null
        // Clear the reference to the contentView
        contentView = null
    }

    private fun restorePaywallViewAfterCleanup(
        paywallView: PaywallView,
        key: String,
    ) {
        withErrorTracking {
            paywallView.prepareViewForDisplay(key)
            Logger.debug(
                LogLevel.debug,
                LogScope.paywallView,
                "Successfully restored PaywallView child views after cleanup",
            )
        }.onError {
            Logger.debug(
                LogLevel.error,
                LogScope.paywallView,
                "Failed to restore PaywallView after cleanup: ${it.message}",
            )
        }
    }

    //region Notifications
    interface NotificationPermissionCallback {
        fun onPermissionResult(granted: Boolean)
    }

    override fun finish() {
        if (isBottomSheetView) {
            mainScope.launch {
                hideBottomSheetAndFinish()
            }
        } else if (isPopupView) {
            mainScope.launch {
                hidePopupAndFinish()
            }
        } else {
            super.finish()
        }
    }

    suspend fun attemptToScheduleNotifications(
        notifications: List<LocalNotification>,
        factory: DeviceHelperFactory,
        cancelExisting: Boolean = false,
    ) = suspendCoroutine { continuation ->
        if (notifications.isEmpty()) {
            continuation.resume(Unit) // Resume immediately as there's nothing to schedule
            return@suspendCoroutine
        }

        createNotificationChannel()

        notificationPermissionCallback =
            object : NotificationPermissionCallback {
                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        NotificationScheduler.scheduleNotifications(
                            notifications = notifications,
                            factory = factory,
                            context = this@SuperwallPaywallActivity,
                            cancelExisting = cancelExisting,
                        )
                    }
                    continuation.resume(Unit) // Resume coroutine after processing
                }
            }

        checkAndRequestNotificationPermissions(this, notificationPermissionCallback!!)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    importance,
                ).apply {
                    description = NOTIFICATION_CHANNEL_DESCRIPTION
                }
            channel.setShowBadge(false)
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkAndRequestNotificationPermissions(
        context: Context,
        callback: NotificationPermissionCallback,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                ) {
                    // First time asking or user previously denied without 'Don't ask again'
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_CODE_NOTIFICATION_PERMISSION,
                    )
                } else {
                    // Permission previously denied with 'Don't ask again'
                    callback.onPermissionResult(false)
                }
            } else {
                callback.onPermissionResult(true)
            }
        } else {
            callback.onPermissionResult(areNotificationsEnabled(context))
        }
    }

    private fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            // Invoke the callback here
            notificationPermissionCallback?.onPermissionResult(isGranted)
        }
    }
    //endregion
}
