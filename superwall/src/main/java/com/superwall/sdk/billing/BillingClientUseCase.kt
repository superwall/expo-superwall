package com.superwall.sdk.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.min

internal typealias ExecuteRequestOnUIThreadFunction = (delayInMillis: Long, onError: (BillingError?) -> Unit) -> Unit

private const val MAX_RETRIES_DEFAULT = 3
private const val RETRY_TIMER_START_MILLISECONDS = 878L // So it gets close to 15 minutes in last retry
internal const val RETRY_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes

internal interface UseCaseParams {
    val appInBackground: Boolean
}

/**
 * A superclass that is used to interact with the billing client. It coordinates the request on the
 * UI thread. Deals with error handling such as retries and returning errors.
 */
internal abstract class BillingClientUseCase<T>(
    private val useCaseParams: UseCaseParams,
    private val onError: (BillingError) -> Unit,
    val executeRequestOnUIThread: ExecuteRequestOnUIThreadFunction,
) {
    protected open val backoffForNetworkErrors = false

    private val maxRetries: Int = MAX_RETRIES_DEFAULT
    private var retryAttempt: Int = 0
    private var retryBackoffMilliseconds = RETRY_TIMER_START_MILLISECONDS

    fun run(delayMilliseconds: Long = 0) {
        executeRequestOnUIThread(delayMilliseconds) { connectionError ->
            if (connectionError == null) {
                this.executeAsync()
            } else {
                onError(connectionError)
            }
        }
    }

    abstract fun executeAsync()

    abstract fun onOk(received: T)

    fun processResult(
        billingResult: BillingResult,
        response: T,
        onSuccess: (T) -> Unit = ::onOk,
        onError: (BillingResult) -> Unit = ::forwardError,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                retryBackoffMilliseconds = RETRY_TIMER_START_MILLISECONDS
                onSuccess(response)
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.productsManager,
                    message = "Billing Service disconnected.",
                )
                run()
            }

            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                backoffOrErrorIfUseInSession(onError, billingResult)
            }

            BillingClient.BillingResponseCode.NETWORK_ERROR,
            BillingClient.BillingResponseCode.ERROR,
            -> {
                backoffOrRetryNetworkError(onError, billingResult)
            }

            else -> {
                onError(billingResult)
            }
        }
    }

    protected fun BillingClient?.withConnectedClient(receivingFunction: BillingClient.() -> Unit) {
        this?.takeIf { it.isReady }?.let {
            it.receivingFunction()
        } ?: Logger.debug(
            logLevel = LogLevel.warn,
            scope = LogScope.productsManager,
            message = "Billing Service disconnected.  Stack trace: ${getStackTrace()}",
        )
    }

    private fun getStackTrace(): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        Throwable().printStackTrace(printWriter)
        return stringWriter.toString()
    }

    private fun forwardError(billingResult: BillingResult) {
        val underlyingErrorMessage =
            "Error loading products - DebugMessage: ${billingResult.debugMessage} " +
                "ErrorCode: ${billingResult.responseCode}."
        val error = BillingError.BillingNotAvailable(underlyingErrorMessage)
        Logger.debug(
            logLevel = LogLevel.error,
            scope = LogScope.productsManager,
            message = underlyingErrorMessage,
        )
        onError(error)
    }

    private fun backoffOrRetryNetworkError(
        onError: (BillingResult) -> Unit,
        billingResult: BillingResult,
    ) {
        if (backoffForNetworkErrors && retryBackoffMilliseconds < RETRY_TIMER_MAX_TIME_MILLISECONDS) {
            retryWithBackoff()
        } else if (!backoffForNetworkErrors && retryAttempt < maxRetries) {
            retryAttempt++
            executeAsync()
        } else {
            onError(billingResult)
        }
    }

    private fun backoffOrErrorIfUseInSession(
        onError: (BillingResult) -> Unit,
        billingResult: BillingResult,
    ) {
        if (useCaseParams.appInBackground) {
            Logger.debug(
                logLevel = LogLevel.warn,
                scope = LogScope.productsManager,
                message = "Billing is unavailable. App is in background, will retry with backoff.",
            )
            if (retryBackoffMilliseconds < RETRY_TIMER_MAX_TIME_MILLISECONDS) {
                retryWithBackoff()
            } else {
                onError(billingResult)
            }
        } else {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.productsManager,
                message = "Billing is unavailable. App is in foreground. Won't retry.",
            )
            onError(billingResult)
        }
    }

    private fun retryWithBackoff() {
        retryBackoffMilliseconds.let { currentDelayMilliseconds ->
            retryBackoffMilliseconds =
                min(
                    retryBackoffMilliseconds * 2,
                    RETRY_TIMER_MAX_TIME_MILLISECONDS,
                )
            run(currentDelayMilliseconds)
        }
    }
}
