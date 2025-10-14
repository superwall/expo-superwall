package com.superwall.sdk.review

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.testing.FakeReviewManager
import com.superwall.sdk.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Mock ReviewManager for testing purposes that shows a custom bottom sheet
 */
internal class MockReviewManager(
    private val context: Context,
) : ReviewManager {
    private val fakeReviewManager = FakeReviewManager(context)

    override suspend fun requestReviewFlow(): ReviewInfo {
        // Use FakeReviewManager to create a valid ReviewInfo
        return suspendCancellableCoroutine { continuation ->
            val request = fakeReviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resume(task.result) // FakeReviewManager should always succeed
                }
            }
        }
    }

    override suspend fun launchReviewFlow(
        activity: Activity,
        reviewInfo: ReviewInfo,
    ) = suspendCancellableCoroutine { continuation ->
        showMockReviewBottomSheet(activity) { rating ->
            // Simulate review completion
            continuation.resume(Unit)
        }
    }

    private fun showMockReviewBottomSheet(
        activity: Activity,
        onComplete: (Int) -> Unit,
    ) {
        MainScope().launch {
            val dialog =
                AlertDialog
                    .Builder(activity, android.R.style.Theme_DeviceDefault_Light_Dialog)
                    .setCancelable(true)
                    .create()

            val dialogView =
                createMockReviewView(activity) { rating ->
                    dialog.dismiss()
                    onComplete(rating)
                }

            dialog.setView(dialogView)

            dialog.window?.let { window ->
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                window.setGravity(Gravity.BOTTOM)
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }

            dialog.show()
        }
    }

    private fun createMockReviewView(
        activity: Activity,
        onComplete: (Int) -> Unit,
    ): View {
        val inflater = LayoutInflater.from(activity)
        val view = inflater.inflate(R.layout.mock_review_layout, null)

        // Set up app icon
        val appIcon = view.findViewById<ImageView>(R.id.app_icon)
        val packageManager = activity.packageManager
        val applicationInfo =
            try {
                packageManager.getApplicationInfo(activity.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

        val drawable =
            applicationInfo?.let {
                packageManager.getApplicationIcon(it)
            } ?: ContextCompat.getDrawable(activity, android.R.drawable.sym_def_app_icon)

        appIcon.setImageDrawable(drawable)

        // Set up app name
        val appName = view.findViewById<TextView>(R.id.app_name)
        appName.text = applicationInfo?.let {
            packageManager.getApplicationLabel(it).toString()
        } ?: "App Name"

        // Set up rating bar
        val ratingBar = view.findViewById<RatingBar>(R.id.rating_bar)

        // Set up buttons
        val notNowButton = view.findViewById<Button>(R.id.not_now_button)
        val submitButton = view.findViewById<Button>(R.id.submit_button)

        notNowButton.setOnClickListener {
            onComplete(0) // Rating 0 means "not now"
        }

        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            onComplete(rating)
        }

        return view
    }
}
