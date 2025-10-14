package com.superwall.sdk.review

import android.app.Activity
import com.google.android.play.core.review.ReviewInfo

/**
 * Interface for managing in-app review functionality
 */
internal interface ReviewManager {
    /**
     * Requests an in-app review flow from Google Play
     * @return ReviewInfo on success or throws ReviewError on failure
     */
    suspend fun requestReviewFlow(): ReviewInfo

    /**
     * Launches the review flow with the provided ReviewInfo
     * @param activity The activity to launch the review flow from
     * @param reviewInfo The ReviewInfo obtained from requestReviewFlow
     * @throws ReviewError if the review flow fails to launch
     */
    suspend fun launchReviewFlow(
        activity: Activity,
        reviewInfo: ReviewInfo,
    )
}

/**
 * Represents errors that can occur during the review process
 */
sealed class ReviewError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * Error occurred while requesting the review flow
     */
    class RequestFlowError(
        val errorCode: Int,
        cause: Throwable? = null,
    ) : ReviewError("Failed to request review flow with error code: $errorCode", cause)

    /**
     * Error occurred while launching the review flow
     */
    class LaunchFlowError(
        cause: Throwable? = null,
    ) : ReviewError("Failed to launch review flow", cause)

    /**
     * Google Play Services not available
     */
    object PlayServicesUnavailable :
        ReviewError("Google Play Services not available")

    /**
     * Generic error
     */
    class GenericError(
        message: String,
        cause: Throwable? = null,
    ) : ReviewError(message, cause)
}
