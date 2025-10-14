package com.superwall.sdk.paywall.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.GONE
import android.widget.FrameLayout.VISIBLE
import android.widget.ProgressBar
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState

class LoadingView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : FrameLayout(context.applicationContext),
        PaywallPurchaseLoadingView {
        companion object {
            internal const val TAG = "LoadingView"
        }

        init {
            setTag(TAG)
            setBackgroundColor(Color.TRANSPARENT)

            // Create a ProgressBar with the default spinner style
            val progressBar =
                ProgressBar(context.applicationContext).apply {
                    // Ensure it's centered in the LoadingViewController
                    val params =
                        LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER,
                        )
                    layoutParams = params
                }

            // Add the ProgressBar to this view
            addView(progressBar)

            // Set an OnTouchListener that does nothing but consume touch events
            // to prevent taps from reaching the view this loading indicator obstructs
            setOnTouchListener { _, _ -> true }
        }

        override fun showLoading() {
            visibility = VISIBLE
        }

        override fun hideLoading() {
            visibility = GONE
        }
    }

fun <T> T.setupFor(
    paywallView: PaywallView,
    loadingState: PaywallLoadingState,
) where T : View, T : PaywallPurchaseLoadingView {
    (this.parent as? ViewGroup)?.removeView(this)
    paywallView.addView(this)
    visibility =
        when (loadingState) {
            is PaywallLoadingState.LoadingPurchase, is PaywallLoadingState.ManualLoading ->
                VISIBLE

            else -> GONE
        }
}
