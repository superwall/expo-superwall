package com.superwall.sdk.misc

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class SerialTaskManager(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    private val channel = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        coroutineScope.launch {
            for (task in channel) {
                withErrorTracking {
                    task()
                }
            }
        }
    }

    fun addTask(task: suspend () -> Unit) {
        val result = channel.trySend(task)
        if (result.isFailure) {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.superwallCore,
                message = "SerialTaskManager: failed to enqueue task — channel closed",
            )
        }
    }
}
