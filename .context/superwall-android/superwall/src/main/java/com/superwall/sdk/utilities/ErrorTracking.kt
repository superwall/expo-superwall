package com.superwall.sdk.utilities

import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.billing.BillingError
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.Either
import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationPipelineError
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason
import com.superwall.sdk.storage.ErrorLog
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.store.transactions.TransactionError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal interface ErrorTracking {
    fun trackError(throwable: Throwable)

    @Serializable
    data class ErrorOccurence(
        @SerialName("type")
        val type: String,
        @SerialName("message")
        val message: String,
        @SerialName("stacktrace")
        val stacktrace: String,
        @SerialName("timestamp")
        val timestamp: Long,
        @SerialName("isFatal")
        val isFatal: Boolean,
    )
}

/**
 * Used to track errors that occur in the SDK.
 * When an error occurs, it is stored in the cache and sent to the server when the SDK is initialized.
 * This ensures the error is logged even with the crash occurring. We only save a single error
 * in cache at a time, to ensure only the crashing error is logged. This also helps us avoid logging the
 * same error multiple times without relying on hashcodes or unique identifier.
 **/
internal class ErrorTracker(
    scope: CoroutineScope,
    private val cache: LocalStorage,
    private val track: suspend (InternalSuperwallEvent.ErrorThrown) -> Unit = {
        Superwall.instance.track(
            it,
        )
    },
) : ErrorTracking {
    init {
        val exists = cache.read(ErrorLog)
        if (exists != null) {
            scope.launch {
                track(
                    InternalSuperwallEvent.ErrorThrown(
                        exists.message,
                        exists.stacktrace,
                        exists.timestamp,
                        exists.type,
                        exists.isFatal,
                    ),
                )
                cache.delete(ErrorLog)
            }
        }
    }

    fun String.replaceNonSuperwallPackages() =
        lines()
            .map {
                if (it.containsAny(
                        "com.superwall.sdk",
                        "com.superwall.supercel",
                        "java.lang",
                        "net.java.dev.jna",
                        "kotlin.",
                        "kotlinx.",
                        "android.os",
                        "androidx.os",
                        "com.android.",
                        "com.google.",
                        "org.threeten.",
                        "com.revenuecat.purchases",
                    )
                ) {
                    it
                } else {
                    it.map { if (it.isLetter()) "*" else it }
                }
            }.joinToString("\n")

    fun String.containsAny(vararg strings: String) = strings.any { this.contains(it) }

    override fun trackError(throwable: Throwable) {
        val errorOccurence =
            ErrorTracking.ErrorOccurence(
                message = throwable.message ?: "",
                stacktrace = throwable.stackTraceToString().replaceNonSuperwallPackages(),
                timestamp = System.currentTimeMillis(),
                isFatal = throwable.isFatal(),
                type = throwable.javaClass.simpleName,
            )
        cache.write(ErrorLog, errorOccurence)
    }
}

internal fun Throwable.isFatal() =
    this is RuntimeException ||
        this is StackOverflowError ||
        this is OutOfMemoryError ||
        this is ClassNotFoundException ||
        this is NoClassDefFoundError

// Utility methods and closures for error tracking

internal fun Superwall.trackError(e: Throwable) {
    try {
        dependencyContainer.errorTracker.trackError(e)
    } catch (_e: Exception) {
        Logger.debug(
            com.superwall.sdk.logger.LogLevel.error,
            com.superwall.sdk.logger.LogScope.all,
            "Error tracking failed for ${e.message}. Cause ${_e.message}} ",
        )
    }
}

internal inline fun <T> withErrorTracking(block: () -> T): Either<T, Throwable> =
    try {
        Either.Success(block())
    } catch (e: Throwable) {
        if (e.shouldLog()) {
            if (Superwall.initialized) {
                Superwall.instance.trackError(e)
            }
        }
        Either.Failure(e)
    }

private fun Throwable.shouldLog() =
    this !is CancellationException &&
        this !is InterruptedException &&
        this !is PresentationPipelineError &&
        this !is TransactionError &&
        this !is PaywallSkippedReason &&
        (this is NetworkError.Decoding || this !is NetworkError) &&
        this !is BillingError ||
        this !is PaywallPresentationRequestStatusReason
