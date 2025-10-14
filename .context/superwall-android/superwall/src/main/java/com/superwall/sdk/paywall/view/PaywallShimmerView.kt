package com.superwall.sdk.paywall.view

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout.LayoutParams
import androidx.appcompat.widget.AppCompatImageView.GONE
import androidx.appcompat.widget.AppCompatImageView.VISIBLE
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.superwall.sdk.misc.isDarkColor
import com.superwall.sdk.misc.readableOverlayColor
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState

interface PaywallShimmerView {
    fun hideShimmer()

    fun showShimmer()

    fun checkForOrientationChanges()
}

fun <T> T.setupFor(
    paywallView: PaywallView,
    loadingState: PaywallLoadingState,
) where T : PaywallShimmerView, T : View {
    (this.parent as? ViewGroup)?.removeView(this)
    if (this is ShimmerView && this.background != paywallView.backgroundColor) {
        background = paywallView.backgroundColor
        setBackgroundColor(background)
        isLightBackground = !background.isDarkColor()
        tintColor = background.readableOverlayColor()
        tintColorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                Color.argb(64, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)),
                BlendModeCompat.SRC_IN,
            )
    }

    visibility =
        when (loadingState) {
            is PaywallLoadingState.LoadingURL ->
                VISIBLE

            else -> GONE
        }
    paywallView.addView(this)
    layoutParams =
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    checkForOrientationChanges()
}
