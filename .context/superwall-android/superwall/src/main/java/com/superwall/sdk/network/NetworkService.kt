package com.superwall.sdk.network

import com.superwall.sdk.misc.Either
import com.superwall.sdk.network.NetworkRequestData.HttpMethod
import com.superwall.sdk.network.session.CustomHttpUrlConnection
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Duration

abstract class NetworkService {
    abstract val customHttpUrlConnection: CustomHttpUrlConnection

    abstract suspend fun makeHeaders(
        isForDebugging: Boolean,
        requestId: String,
    ): Map<String, String>

    abstract val host: String
    abstract val version: String

    suspend inline fun <reified T> get(
        path: String,
        queryItems: List<URLQueryItem>? = null,
        isForDebugging: Boolean = false,
        requestId: String = UUID.randomUUID().toString(),
        retryCount: Int = NetworkConsts.retryCount(),
        noinline isRetryingCallback: (suspend () -> Unit)? = null,
        timeout: Duration? = null
    ): Either<T, NetworkError> where T : @Serializable Any =
        customHttpUrlConnection.request(
            buildRequestData = {
                NetworkRequestData<T>(
                    isForDebugging = isForDebugging,
                    requestId = requestId,
                    components =
                        NetworkRequestData.Components(
                            host = host,
                            path = version + path,
                            queryItems = queryItems,
                        ),
                    method = HttpMethod.GET,
                    factory = this::makeHeaders,
                    timeout = timeout
                )
            },
            retryCount = retryCount,
            isRetryingCallback = isRetryingCallback,
        )

    suspend inline fun <reified T> post(
        path: String,
        isForDebugging: Boolean = false,
        body: ByteArray? = null,
        requestId: String = UUID.randomUUID().toString(),
        retryCount: Int = 6,
        timeout: Duration? = null
    ): Either<T, NetworkError> where T : @Serializable Any =
        customHttpUrlConnection.request<T>(
            buildRequestData = {
                NetworkRequestData<T>(
                    isForDebugging = isForDebugging,
                    requestId = requestId,
                    components =
                        NetworkRequestData.Components(
                            host = host,
                            path = version + path,
                            bodyData = body,
                        ),
                    method = HttpMethod.POST,
                    factory = this::makeHeaders,
                    timeout = timeout
                )
            },
            retryCount = retryCount,
        )
}
