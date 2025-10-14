package com.superwall.sdk.misc

import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.network.session.TaskRetryLogic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

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
