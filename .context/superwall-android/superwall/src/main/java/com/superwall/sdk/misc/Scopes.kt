package com.superwall.sdk.misc

import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal interface SuperwallScope : CoroutineScope {
    val exceptionHandler: CoroutineExceptionHandler
        get() =
            CoroutineExceptionHandler { _, throwable ->
                withErrorTracking {
                    throw throwable
                }
            }
}

class IOScope(
    overrideWithContext: CoroutineContext = Dispatchers.IO,
) : CoroutineScope,
    SuperwallScope {
    override val coroutineContext: CoroutineContext = overrideWithContext + exceptionHandler
}

class MainScope(
    overrideWithContext: CoroutineContext = Dispatchers.Main,
) : CoroutineScope,
    SuperwallScope {
    override val coroutineContext: CoroutineContext = overrideWithContext + exceptionHandler
}

internal fun SuperwallScope.launchWithTracking(block: suspend CoroutineScope.() -> Unit): Job =
    launch {
        withErrorTracking {
            block()
        }
    }

internal suspend fun <T> SuperwallScope.asyncWithTracking(block: suspend CoroutineScope.() -> T): Deferred<Either<T, in Throwable>> =
    (this as SuperwallScope).async(exceptionHandler) {
        withErrorTracking {
            block()
        }
    }
