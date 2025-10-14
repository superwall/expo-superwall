package com.superwall.sdk.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementation of ReviewManager using Google Play Core Review API
 */
class ReviewManagerImpl(
    private val context: Context,
    val isDebug: () -> Boolean,
) : ReviewManager {
    private val playReviewManager by lazy {
        if (isDebug()) {
            FakeReviewManager(context)
        } else {
            ReviewManagerFactory.create(context)
        }
    }

    override suspend fun requestReviewFlow(): ReviewInfo =
        suspendCancellableCoroutine { continuation ->
            val request = playReviewManager.requestReviewFlow()

            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    continuation.resume(reviewInfo)
                } else {
                    val exception = task.exception
                    val error =
                        when (exception) {
                            is ReviewException -> {
                                ReviewError.RequestFlowError(exception.errorCode, exception)
                            }
                            else -> {
                                ReviewError.GenericError(
                                    "Failed to request review flow: ${exception?.message}",
                                    exception,
                                )
                            }
                        }
                    continuation.resumeWith(kotlin.Result.failure(error))
                }
            }
        }

    override suspend fun launchReviewFlow(
        activity: Activity,
        reviewInfo: ReviewInfo,
    ) = suspendCancellableCoroutine { continuation ->
        val flow = playReviewManager.launchReviewFlow(activity, reviewInfo)

        flow.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // The flow has finished. The API does not indicate whether the user
                // reviewed or not, or even whether the review dialog was shown.
                continuation.resume(Unit)
            } else {
                val exception = task.exception
                val error = ReviewError.LaunchFlowError(exception)
                continuation.resumeWith(kotlin.Result.failure(error))
            }
        }
    }
}
