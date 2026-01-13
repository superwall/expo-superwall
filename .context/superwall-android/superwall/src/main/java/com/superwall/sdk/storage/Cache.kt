package com.superwall.sdk.storage

import android.content.Context
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.onError
import com.superwall.sdk.storage.memory.LRUCache
import com.superwall.sdk.storage.memory.PerpetualCache
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

class Cache(
    val context: Context,
    private val ioQueue: CoroutineContext = Dispatchers.IO.limitedParallelism(1),
    private val json: Json,
) : CoroutineScope by CoroutineScope(ioQueue) {
    companion object {
        private const val userSpecificDocumentDirectoryPrefix =
            "com.superwall.document.userSpecific.Store"
        private const val appSpecificDocumentDirectoryPrefix =
            "com.superwall.document.appSpecific.Store"
        private const val cacheDirectoryPrefix = "com.superwall.cache.Store"
        private const val ioQueuePrefix = "com.superwall.queue.Store"
        private const val defaultMaxCachePeriodInSecond: Long = 60 * 60 * 24 * 7 // a week
    }

    private val memCache: LRUCache<String, Any> =
        LRUCache(PerpetualCache<String, Any>(), 1000)

    // TODO: clear expired entries from disk cache when backgrounding/terminating

    fun <T> read(storable: Storable<T>): T? {
        var data = memCache[storable.key] as? T
        if (data == null) {
            val file = storable.file(context = context)
            if (file.exists()) {
                var jsonString = ""
                try {
                    jsonString = file.readText(Charsets.UTF_8)
                    data = json.decodeFromString(storable.serializer, jsonString)
                    data?.let {
                        memCache[storable.key] = it
                    }
                } catch (e: Throwable) {
                    Logger.debug(
                        logLevel = LogLevel.error,
                        LogScope.cache,
                        message = "Unable to read key: ${storable.key}, got $jsonString",
                        error = e,
                    )
                }
            }
        }

        return data
    }

    fun <T : Any> write(
        storable: Storable<T>,
        data: T,
    ) {
        memCache[storable.key] = data

        launch {
            try {
                val file = storable.file(context = context)
                val jsonString = json.encodeToString(storable.serializer, data)
                file.writeText(jsonString, Charsets.UTF_8)
            } catch (e: Throwable) {
                Logger.debug(
                    logLevel = LogLevel.error,
                    LogScope.cache,
                    message = "Unable to write key: ${storable.key}",
                    error = e,
                )
            }
        }
    }

    fun <T : Any> delete(storable: Storable<T>) {
        memCache.remove(storable.key)
        launch {
            withErrorTracking {
                try {
                    val file = storable.file(context = context)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    Logger.debug(
                        logLevel = LogLevel.error,
                        LogScope.cache,
                        message = "Unable to delete key: ${storable.key}",
                        error = e,
                    )
                }
            }
        }
    }

    //region Clean

    fun clean() {
        memCache.clear()
        cleanDiskCache()
    }

    private fun cleanDiskCache() {
        launch {
            withErrorTracking {
                val cacheDir = context.cacheDir
                if (cacheDir.exists()) {
                    cacheDir.listFiles()?.forEach { file ->
                        file.delete()
                    }
                }
            }.onError { error ->
                Logger.debug(
                    logLevel = LogLevel.error,
                    LogScope.cache,
                    message = "Unable to clean disk cache",
                    error = error,
                )
            }
        }
    }

    //endregion
}
