package com.superwall.sdk.debug

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.superwall.sdk.BuildConfig
import com.superwall.sdk.R
import com.superwall.sdk.Superwall
import com.superwall.sdk.debug.localizations.SWLocalizationActivity
import com.superwall.sdk.dependencies.RequestFactory
import com.superwall.sdk.dependencies.ViewFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.fold
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.network.Network
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.internallyPresent
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.request.PaywallRequestManager
import com.superwall.sdk.paywall.request.ResponseIdentifiers
import com.superwall.sdk.paywall.view.ActivityEncapsulatable
import com.superwall.sdk.paywall.view.PaywallViewState
import com.superwall.sdk.store.StoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

interface AppCompatActivityEncapsulatable {
    var encapsulatingActivity: AppCompatActivity?
}

class DebugView(
    private val context: Context,
    private val storeManager: StoreManager,
    private val network: Network,
    private val paywallRequestManager: PaywallRequestManager,
    private val paywallManager: PaywallManager,
    private val debugManager: DebugManager,
    private val factory: Factory,
) : ConstraintLayout(context),
    AppCompatActivityEncapsulatable {
    interface Factory :
        RequestFactory,
        ViewFactory

    data class AlertOption(
        val title: String? = "",
        val action: (suspend () -> Unit)? = null,
        val style: Int = AlertDialog.BUTTON_POSITIVE,
    )

    // The full screen activity instance if this view has been presented in one.
    override var encapsulatingActivity: AppCompatActivity? = null

    internal var paywallDatabaseId: String? = null
    private var paywallIdentifier: String? = null
    private var initialLocaleIdentifier: String? = null
    private var previewViewContent: View? = null
    private var paywalls: List<Paywall> = emptyList()
    private var paywall: Paywall? = null
    internal var isActive = false

    private val logoImageView: ImageView by lazy {
        ImageView(context).apply {
            id = View.generateViewId()
            val superwallLogo = ContextCompat.getDrawable(context, R.drawable.superwall_logo)
            setImageDrawable(superwallLogo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.TRANSPARENT)
            clipToOutline = true
            visibility = View.VISIBLE
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
    }
    private val exitButton: LinearLayout by lazy {
        LinearLayout(context).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )

            val imageView =
                ImageView(context).apply {
                    // Apply a color filter with full opacity to test visibility
                    val debuggerImage =
                        ContextCompat.getDrawable(context, R.drawable.exit)?.mutate()
                    debuggerImage?.colorFilter =
                        PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                    setImageDrawable(debuggerImage)
                    scaleType = ImageView.ScaleType.FIT_CENTER

                    // Apply alpha to the ImageView if needed
                    alpha = 0.5f

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        )
                }
            addView(imageView)

            setOnClickListener { pressedExitButton() }
        }
    }

    private val consoleButton: LinearLayout by lazy {
        LinearLayout(context).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )

            val imageView =
                ImageView(context).apply {
                    // Apply a color filter with full opacity to test visibility
                    val debuggerImage =
                        ContextCompat.getDrawable(context, R.drawable.debugger)?.mutate()
                    debuggerImage?.colorFilter =
                        PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                    setImageDrawable(debuggerImage)
                    scaleType = ImageView.ScaleType.FIT_CENTER

                    // Apply alpha to the ImageView if needed
                    alpha = 0.5f

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        )
                }
            addView(imageView)

            setOnClickListener { pressedConsoleButton() }
        }
    }

    private val activityIndicator: ProgressBar by lazy {
        ProgressBar(context).apply {
            id = View.generateViewId()
            isIndeterminate = true
            visibility = View.VISIBLE
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
    }
    private val primaryColor = Color.parseColor("#75FFF1")
    private val lightBackgroundColor = Color.parseColor("#181A1E")

    private val bottomButton: LinearLayout by lazy {
        id = View.generateViewId()
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            id = View.generateViewId()
            isFocusable = true
            val backgroundDrawable =
                GradientDrawable().apply {
                    setColor(Color.parseColor("#203133")) // Background color
                    cornerRadius = 70f // Set the corner radius for rounded corners
                }
            background = backgroundDrawable // Set the drawable as button background

            val playImageView =
                ImageView(context).apply {
                    id = View.generateViewId()
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(R.drawable.play_button)
                    setColorFilter(primaryColor) // Set the color of the play button
                    layoutParams =
                        LinearLayout
                            .LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                rightMargin =
                                    8 // You can adjust the margin to control the space between the image and the text
                            }
                }
            addView(playImageView)

            val previewTextView =
                TextView(context).apply {
                    id = View.generateViewId()
                    text = "Preview"
                    textSize = 17f // Set your text size
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(primaryColor) // Set your text color
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        )
                }
            addView(previewTextView)

            setPadding(32, 16, 32, 16) // Add padding as needed

            setOnClickListener {
                // Handle your click events here
                pressedBottomButton()
            }
        }
    }

    private lateinit var previewTextView: TextView
    private val previewPickerButton: LinearLayout by lazy {
        LinearLayout(context).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(lightBackgroundColor)
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )

            previewTextView =
                TextView(context).apply {
                    id = View.generateViewId()
                    text = ""
                    textSize = 14f // Set your text size
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(primaryColor)
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        )
                }
            addView(previewTextView)

            val arrowImageView =
                ImageView(context).apply {
                    id = View.generateViewId()
                    setImageResource(R.drawable.down_arrow)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setColorFilter(primaryColor) // Set the color of the play button
                    layoutParams =
                        LinearLayout
                            .LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                leftMargin =
                                    8 // You can adjust the margin to control the space between the text and the image
                            }
                }
            addView(arrowImageView)

            setPadding(10, 10, 10, 10) // Adjust padding as needed

            setOnClickListener { pressedPreview() }
        }
    }

    private val previewContainerView: ConstraintLayout by lazy {
        ConstraintLayout(context).apply {
            id = View.generateViewId()
            // shouldAnimateLightly = true
            isFocusable =
                true // Depending on the view's properties, you might need to set focusability
            layoutParams =
                LayoutParams(
                    LayoutParams.MATCH_CONSTRAINT,
                    LayoutParams.MATCH_CONSTRAINT,
                )
            setOnClickListener { pressedPreview() }
        }
    }

    init {
        initialLocaleIdentifier = Superwall.instance.options.localeIdentifier
        addSubviews()
        CoroutineScope(Dispatchers.IO).launch {
            loadPreview()
        }
    }

    fun viewDestroyed() {
        paywallManager.resetCache()
        debugManager.isDebuggerLaunched = false
        Superwall.instance.options.localeIdentifier = initialLocaleIdentifier
    }

    fun addSubviews() {
        val layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )

        // Adding views to the layout
        addView(activityIndicator, layoutParams)
        addView(logoImageView, layoutParams)
        addView(consoleButton, layoutParams)
        addView(exitButton, layoutParams)
        addView(bottomButton, layoutParams)
        addView(previewContainerView, layoutParams)
        addView(previewPickerButton, layoutParams)
        previewContainerView.clipToOutline = false

        // Setting background color
        setBackgroundColor(lightBackgroundColor)

        // Setting up the constraints
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        // Applying constraints to previewContainerView
        constraintSet.connect(
            previewContainerView.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
        )
        constraintSet.connect(
            previewContainerView.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
        )
        constraintSet.connect(
            previewContainerView.id,
            ConstraintSet.TOP,
            logoImageView.id,
            ConstraintSet.BOTTOM,
            dpToPx(5),
        )
        constraintSet.connect(
            previewContainerView.id,
            ConstraintSet.BOTTOM,
            bottomButton.id,
            ConstraintSet.TOP,
            dpToPx(5),
        )
        constraintSet.constrainHeight(previewContainerView.id, 0)

        // Constraints for logoImageView (Centered horizontally at the top)
        constraintSet.constrainWidth(logoImageView.id, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(logoImageView.id, dpToPx(20))
        constraintSet.connect(
            logoImageView.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            dpToPx(30),
        )
        constraintSet.connect(
            logoImageView.id,
            ConstraintSet.START,
            consoleButton.id,
            ConstraintSet.END,
        )
        constraintSet.connect(
            logoImageView.id,
            ConstraintSet.END,
            exitButton.id,
            ConstraintSet.START,
        )

        // Constraints for consoleButton (Top Left)
        constraintSet.connect(
            consoleButton.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            dpToPx(25),
        )
        constraintSet.connect(
            consoleButton.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            dpToPx(30),
        )
        constraintSet.constrainWidth(consoleButton.id, dpToPx(44))

        // Constraints for exitButton (Top Right)
        constraintSet.connect(
            exitButton.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            dpToPx(25),
        )
        constraintSet.connect(
            exitButton.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            dpToPx(30),
        )
        constraintSet.constrainWidth(exitButton.id, dpToPx(44))

        // Constraints for activityIndicator
        constraintSet.connect(
            activityIndicator.id,
            ConstraintSet.START,
            previewContainerView.id,
            ConstraintSet.START,
        )
        constraintSet.connect(
            activityIndicator.id,
            ConstraintSet.END,
            previewContainerView.id,
            ConstraintSet.END,
        )
        constraintSet.connect(
            activityIndicator.id,
            ConstraintSet.TOP,
            previewContainerView.id,
            ConstraintSet.TOP,
        )
        constraintSet.connect(
            activityIndicator.id,
            ConstraintSet.BOTTOM,
            previewContainerView.id,
            ConstraintSet.BOTTOM,
        )

        // Constraints for bottomButton
        constraintSet.connect(
            bottomButton.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            dpToPx(25),
        )
        constraintSet.connect(
            bottomButton.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            dpToPx(25),
        )
        constraintSet.constrainHeight(bottomButton.id, dpToPx(60))
        constraintSet.connect(
            bottomButton.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            dpToPx(30),
        )

        // Constraints for previewPickerButton
        constraintSet.connect(
            previewPickerButton.id,
            ConstraintSet.START,
            previewContainerView.id,
            ConstraintSet.START,
            dpToPx(25),
        )
        constraintSet.connect(
            previewPickerButton.id,
            ConstraintSet.END,
            previewContainerView.id,
            ConstraintSet.END,
            dpToPx(25),
        )
        constraintSet.constrainHeight(previewPickerButton.id, dpToPx(30))
        constraintSet.connect(
            previewPickerButton.id,
            ConstraintSet.BOTTOM,
            bottomButton.id,
            ConstraintSet.TOP,
            dpToPx(10),
        )

        // Apply all the constraints
        constraintSet.applyTo(this)
    }

    fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    suspend fun loadPreview() {
        withContext(Dispatchers.Main) {
            activityIndicator.visibility = View.VISIBLE

            previewViewContent?.let { view ->
                (view.parent as? ViewGroup)?.removeView(view)
            }
        }

        if (paywalls.isEmpty()) {
            network
                .getPaywalls(true)
                .fold({
                    paywalls = it
                    finishLoadingPreview()
                }, {
                    Logger.debug(
                        logLevel = LogLevel.error,
                        scope = LogScope.debugView,
                        message = "Failed to Fetch Paywalls",
                        error = it,
                    )
                })
        } else {
            finishLoadingPreview()
        }
    }

    suspend fun finishLoadingPreview() {
        var paywallId: String? =
            paywallIdentifier ?: paywallDatabaseId?.let { dbId ->
                paywalls.firstOrNull { it.databaseId == dbId }?.identifier?.also { identifier ->
                    paywallIdentifier = identifier
                }
            } ?: return

        try {
            val request =
                factory.makePaywallRequest(
                    eventData = null,
                    responseIdentifiers = ResponseIdentifiers(paywallId),
                    overrides = null,
                    isDebuggerLaunched = true,
                    presentationSourceType = null,
                )
            var paywall = paywallRequestManager.getPaywall(request).toResult().getOrThrow()

            val productVariables =
                storeManager.getProductVariables(
                    paywall,
                    request = request,
                )
            paywall.productVariables = productVariables

            this.paywall = paywall
            withContext(Dispatchers.Main) {
                previewTextView.text = paywall.name
                activityIndicator.visibility = View.INVISIBLE
                addPaywallPreview()
            }
        } catch (error: Throwable) {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.debugView,
                message = "No Paywall Response",
                error = error,
            )
        }
    }

    suspend fun addPaywallPreview() {
        val paywall = paywall ?: return

        val paywallVc =
            factory.makePaywallView(
                paywall = paywall,
                cache = null,
                delegate = null,
            )
        previewContainerView.addView(paywallVc)
        previewViewContent = paywallVc

        paywallVc.apply {
            layoutParams =
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                )
        }

        paywallVc.updateState(PaywallViewState.Updates.SetInterceptTouchEvents(true))

        val constraints =
            ConstraintSet().apply {
                clone(previewContainerView)
                connect(
                    paywallVc.id,
                    ConstraintSet.START,
                    previewContainerView.id,
                    ConstraintSet.START,
                )
                connect(paywallVc.id, ConstraintSet.END, previewContainerView.id, ConstraintSet.END)
                connect(paywallVc.id, ConstraintSet.TOP, previewContainerView.id, ConstraintSet.TOP)
                connect(
                    paywallVc.id,
                    ConstraintSet.BOTTOM,
                    previewContainerView.id,
                    ConstraintSet.BOTTOM,
                )
            }
        constraints.applyTo(previewContainerView)

        paywallVc.apply {
            clipToOutline = true
            alpha = 0.0f
        }
        val borderDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE) // Background color
                cornerRadius = 52f // Corner radius
                setStroke(2, Color.WHITE) // Border width and color
            }
        paywallVc.background = borderDrawable

        val ratio = previewContainerView.height.toFloat() / rootView.height.toFloat()

        paywallVc.apply {
            scaleX = ratio
            scaleY = ratio
        }

        paywallVc
            .animate()
            .alpha(1.0f)
            .setDuration(250)
            .setStartDelay(100)
            .start()
    }

    private fun pressedPreview() {
        val id = paywallDatabaseId ?: return

        val options =
            paywalls.map { paywall ->
                var name = paywall.name

                if (id == paywall.databaseId) {
                    name = "$name ✓"
                }

                val alert =
                    AlertOption(
                        title = name,
                        action = {
                            this.paywallDatabaseId = paywall.databaseId
                            this.paywallIdentifier = paywall.identifier
                            CoroutineScope(Dispatchers.IO).launch {
                                loadPreview()
                            }
                        },
                    )
                alert
            }

        presentAlert(
            title = "Your Paywalls",
            message = null,
            options = options,
        )
    }

    private fun pressedExitButton() {
        encapsulatingActivity?.finish()
    }

    fun pressedConsoleButton() {
        var releaseVersionNumber = ""
        var versionCode = ""
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            releaseVersionNumber = packageInfo.versionName ?: "Unknown"
            versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toString()
                } else {
                    packageInfo.versionCode.toLong().toString()
                }
        } catch (e: PackageManager.NameNotFoundException) {
        }
        val version = BuildConfig.SDK_VERSION

        val title = "Superwall v$version | App v$releaseVersionNumber ($versionCode)"

        // Assuming you have a showOptionsAlert function
        presentAlert(
            title = title,
            message = null,
            options =
                listOf(
                    AlertOption("Localization", ::showLocalizationPicker),
                    AlertOption("Templates", ::showConsole),
                ),
        )
    }

    suspend fun showConsole() {
        val paywall = this.paywall // Replace 'YourActivity' with the actual class name
        if (paywall == null) {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.debugView,
                message = "Paywall is nil",
            )
            return
        }

        try {
            val (productsById, _) =
                storeManager.getProducts(
                    paywall = paywall,
                )

            val products = paywall.productIds.mapNotNull { productsById[it] }
            SWConsoleActivity.products = products
            val activity = encapsulatingActivity.let { it } ?: return

            val intent =
                Intent(activity, SWConsoleActivity::class.java).apply {
                    putExtra("PARENT_ACTIVITY", this::class.java.simpleName)
                }
            activity.startActivity(intent)
        } catch (error: Throwable) {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.debugView,
                message = "Error retrieving products - ${error.message}",
            )
        }
    }

    private fun showLocalizationPicker() {
        // Set the completion callback
        SWLocalizationActivity.completion = { locale ->
            // Handle the locale identifier
            Superwall.instance.localeIdentifier = locale
            // Continue with any other operations after locale selection
            CoroutineScope(Dispatchers.IO).launch {
                loadPreview()
            }
        }
        val activity = encapsulatingActivity.let { it } ?: return

        // Start the localization activity
        val intent = Intent(activity, SWLocalizationActivity::class.java)
        activity.startActivity(intent)
    }

    fun pressedBottomButton() {
        val alertOptions =
            listOf(
                AlertOption(
                    title = "With Free Trial",
                    action = { loadAndShowPaywall(true) },
                    style = AlertDialog.BUTTON_POSITIVE,
                ),
                AlertOption(
                    title = "Without Free Trial",
                    action = { loadAndShowPaywall(false) },
                    style = AlertDialog.BUTTON_POSITIVE,
                ),
            )
        presentAlert("Which version?", null, alertOptions)
    }

    fun presentAlert(
        title: String?,
        message: String?,
        options: List<AlertOption> = emptyList(),
    ) {
        val activity = encapsulatingActivity.let { it } ?: return

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        builder.setMessage(message)

        val items = options.map { it.title }.toTypedArray()
        builder.setItems(items) { _, which ->
            val option = options[which]
            CoroutineScope(Dispatchers.IO).launch {
                option.action?.invoke()
            }
        }

        val dialog = builder.create()

        // Setting up for showing as a bottom sheet if possible
        dialog.window?.attributes?.gravity = Gravity.BOTTOM
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        dialog.show()
        dialog.window?.findViewById<View>(androidx.appcompat.R.id.contentPanel)?.post {
            val contentPanel =
                dialog.window?.findViewById<View>(androidx.appcompat.R.id.contentPanel)
            // remove dialog's default background to make it look more like an action sheet
            contentPanel?.background = null
        }
    }

    fun loadAndShowPaywall(freeTrialAvailable: Boolean = false) {
        val paywallIdentifier = paywallIdentifier ?: return

        // bottomButton.setImageDrawable(null)
        // bottomButton.showLoading = true

        val inactiveSubscriptionPublisher = MutableStateFlow(SubscriptionStatus.Inactive)

        val presentationRequest =
            factory.makePresentationRequest(
                PresentationInfo.FromIdentifier(
                    paywallIdentifier,
                    freeTrialOverride = freeTrialAvailable,
                ),
                paywallOverrides = null,
                presenter = encapsulatingActivity,
                isDebuggerLaunched = true,
                subscriptionStatus = inactiveSubscriptionPublisher,
                isPaywallPresented = Superwall.instance.isPaywallPresented,
                type = PresentationRequestType.Presentation,
            )

        val publisher = MutableSharedFlow<PaywallState>()

        CoroutineScope(Dispatchers.Main).launch {
            publisher.collect { state ->
                when (state) {
                    is PaywallState.Presented -> {
                        // bottomButton.showLoading = false
                        val playButton =
                            ResourcesCompat.getDrawable(resources, R.drawable.play_button, null)
                        // bottomButton.setImageDrawable(playButton)
                    }

                    is PaywallState.Skipped -> {
                        val errorMessage =
                            when (state.paywallSkippedReason) {
                                is PaywallSkippedReason.Holdout -> "The user was assigned to a holdout."
                                is PaywallSkippedReason.NoAudienceMatch -> "The user didn't match a rule."
                                is PaywallSkippedReason.PlacementNotFound -> "Couldn't find event."
                                is PaywallSkippedReason.UserIsSubscribed -> "The user is subscribed."
                            }
                        presentAlert(
                            title = "Paywall Skipped",
                            message = errorMessage,
                        )
                        // bottomButton.showLoading = false
                        val playButton =
                            ResourcesCompat.getDrawable(resources, R.drawable.play_button, null)
                        //   bottomButton.setImageDrawable(playButton)
                    }

                    is PaywallState.Dismissed -> {
                        // Handle dismissed state if needed
                    }

                    is PaywallState.PresentationError -> {
                        Logger.debug(
                            logLevel = LogLevel.error,
                            scope = LogScope.debugView,
                            message = "Failed to Show Paywall",
                        )
                        presentAlert(
                            title = "Presentation Error",
                            message = state.error.localizedMessage,
                        )
                        // bottomButton.showLoading = false
                        val playButton =
                            ResourcesCompat.getDrawable(resources, R.drawable.play_button, null)
                        // bottomButton.setImageDrawable(playButton)
                    }
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            Superwall.instance.internallyPresent(presentationRequest, publisher)
        }
    }
}

internal class DebugViewActivity : AppCompatActivity() {
    companion object {
        private const val VIEW_KEY = "debugViewKey"

        fun startWithView(
            context: Context,
            view: View,
        ) {
            val key = UUID.randomUUID().toString()
            Superwall.instance.dependencyContainer
                .makeViewStore()
                .storeView(key, view)

            val intent =
                Intent(context, DebugViewActivity::class.java).apply {
                    putExtra(VIEW_KEY, key)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

            context.startActivity(intent)
        }
    }

    private var contentView: View? = null

    override fun setContentView(view: View) {
        super.setContentView(view)
        contentView = view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        val key = intent.getStringExtra(VIEW_KEY)
        if (key == null) {
            finish() // Close the activity if there's no key
            return
        }
        val view =
            Superwall.instance.dependencyContainer
                .makeViewStore()
                .retrieveView(key) ?: run {
                finish() // Close the activity if the view associated with the key is not found
                return
            }

        if (view is AppCompatActivityEncapsulatable) {
            view.encapsulatingActivity = this
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )

        // Now add
        setContentView(view)
        supportActionBar?.hide()
    }

    override fun onStart() {
        super.onStart()

        val debugView = contentView as? DebugView ?: return

        debugView.isActive = true
    }

    override fun onStop() {
        super.onStop()

        val debugView = contentView as? DebugView ?: return

        debugView.isActive = false
    }

    override fun onDestroy() {
        super.onDestroy()

        val debugView = contentView as? DebugView ?: return

        CoroutineScope(Dispatchers.IO).launch {
            debugView.viewDestroyed()
        }

        // Clear reference to activity in the view
        (contentView as? ActivityEncapsulatable)?.encapsulatingActivity = null

        // Clear the reference to the contentView
        contentView = null
    }
}
