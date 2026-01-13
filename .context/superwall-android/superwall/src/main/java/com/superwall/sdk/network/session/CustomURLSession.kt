package com.superwall.sdk.network.session

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.flatMap
import com.superwall.sdk.misc.map
import com.superwall.sdk.misc.retrying
import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.network.NetworkRequestData
import com.superwall.sdk.network.RequestExecutor
import com.superwall.sdk.network.RequestResult
import com.superwall.sdk.network.authHeader
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CustomHttpUrlConnection(
    val json: Json,
    val requestExecutor: RequestExecutor,
    val interceptors: List<(RequestResult) -> RequestResult> = emptyList(),
) {
    @Throws(NetworkError::class)
    suspend inline fun <reified Response : @Serializable Any> request(
        crossinline buildRequestData: suspend () -> NetworkRequestData<Response>,
        retryCount: Int,
        noinline isRetryingCallback: (suspend () -> Unit)? = null,
    ): Either<Response, NetworkError> =
        retrying(
            maxRetryCount = retryCount,
            isRetryingCallback = isRetryingCallback,
        ) {
            val requestData = buildRequestData()
            requestExecutor
                .execute(requestData)
                .map {
                    if (interceptors.isNotEmpty()) {
                        interceptors.fold(it, { res, interceptor -> interceptor(res) })
                    } else {
                        it
                    }
                }.flatMap {
                    try {
                        Either.Success(
                            this.json.decodeFromString<Response>(
                                it.responseMessage,
                            ),
                        )
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        Logger.debug(
                            LogLevel.error,
                            LogScope.network,
                            "Request Error",
                            mapOf(
                                "request" to it.toString(),
                                "api_key" to it.authHeader(),
                                "url" to (requestData.url?.toString() ?: "unknown"),
                                "message" to "Unable to decode response to type ${Response::class.simpleName} - ${e.message}",
                                "info" to it.responseMessage,
                                "request_duration" to it.duration,
                            ),
                        )
                        Either.Failure(NetworkError.Decoding(e))
                    }
                }
        }
}
