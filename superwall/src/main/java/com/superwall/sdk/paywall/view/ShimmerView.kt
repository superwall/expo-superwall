package com.superwall.sdk.paywall.view

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.PathInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.superwall.sdk.R

class ShimmerView(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context.applicationContext, attrs),
    PaywallShimmerView {
    private var animator: ValueAnimator? = null
    private var vectorDrawable: VectorDrawable? = null

    companion object {
        internal const val TAG = "ShimmerView"
    }

    private var currentOrientation: Int = Configuration.ORIENTATION_UNDEFINED

    private val portraitDrawable: VectorDrawable? by lazy {
        ContextCompat.getDrawable(context.applicationContext, R.drawable.portrait_shimmer_skeleton) as? VectorDrawable
    }
    private val landscapeDrawable: VectorDrawable? by lazy {
        ContextCompat.getDrawable(context.applicationContext, R.drawable.landscape_shimmer_skeleton) as? VectorDrawable
    }
    var tintColorFilter: android.graphics.ColorFilter? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setTag(TAG)
        checkForOrientationChanges()
    }

    var background: Int = 0
    var isLightBackground: Boolean = false
    var tintColor: Int = 0

    override fun hideShimmer() {
        visibility = View.GONE
        stopShimmer()
    }

    override fun showShimmer() {
        visibility = View.VISIBLE
        startShimmer()
    }

    override fun checkForOrientationChanges() {
        val config = resources.configuration
        setDrawableBasedOnOrientation(config)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (currentOrientation == newConfig.orientation) {
            return
        }

        currentOrientation = newConfig.orientation

        setDrawableBasedOnOrientation(newConfig)

        // Redraw the view
        invalidate()
    }

    private fun setDrawableBasedOnOrientation(config: Configuration) {
        vectorDrawable =
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                landscapeDrawable
            } else {
                portraitDrawable
            }

        // Update the mask bitmap with the new drawable

        if (vectorDrawable != drawable) {
            setImageDrawable(vectorDrawable)
        }
        colorFilter = tintColorFilter
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        viewTreeObserver.addOnGlobalLayoutListener(
            object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // Ensure we only listen once to prevent multiple calls
                    viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // Now start the shimmer
                    startShimmer()
                }
            },
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopShimmer()
    }

    fun startShimmer() {
        stopShimmer()
        animator =
            ValueAnimator.ofFloat(1f, 0.5f, 1f).apply {
                duration = 3000
                repeatCount = ValueAnimator.INFINITE
                interpolator = PathInterpolator(0.4f, 0f, 0.6f, 1f)
                addUpdateListener { animation ->
                    drawable.alpha = (animation.animatedValue as Float * 255).toInt()
                }
                start()
            }
    }

    fun stopShimmer() {
        animator?.cancel()
    }
}
