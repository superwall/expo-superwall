package com.superwall.sdk.misc

import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.network.session.TaskRetryLogic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

suspend fun <T> retryOrNull(
    maxRetries: Int,
    delayMs: (attempt: Int) -> Long = { (it + 1) * 1000L },
    operation: suspend () -> T,
): T? {
    repeat(maxRetries) { attempt ->
        try {
            return operation()
        } catch (_: Throwable) {
            if (attempt < maxRetries - 1) delay(delayMs(attempt))
        }
    }
    return null
}

suspend fun <T> retrying(
    maxRetryCount: Int,
    isRetryingCallback: (suspend () -> Unit)?,
    operation: suspend () -> Either<T, NetworkError>,
): Either<T, NetworkError> =
    run {
        val job = Job()
        for (attempt in 0 until maxRetryCount) {
            try {
                return@run withContext(job) {
                    when (val result = operation()) {
                        is Either.Success -> return@withContext result
                        is Either.Failure -> throw result.error
                    }
                }
            } catch (e: Throwable) {
                isRetryingCallback?.invoke()
                val delayTime = TaskRetryLogic.delay(attempt, maxRetryCount) ?: break
                delay(delayTime)
            }
        }
        job.ensureActive()
        operation()
    }
