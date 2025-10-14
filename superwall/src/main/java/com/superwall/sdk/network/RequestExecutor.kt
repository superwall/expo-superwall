package com.superwall.sdk.network

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.Either
import com.superwall.sdk.network.NetworkRequestData.HttpMethod
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class RequestExecutor(
    val buildHeaders: suspend (isForDebugging: Boolean, requestId: String) -> Map<String, String>,
) {
    suspend fun execute(requestData: NetworkRequestData<*>): Either<RequestResult, NetworkError> {
        try {
            val headers = buildHeaders(requestData.isForDebugging, requestData.requestId)
            val request =
                try {
                    requestData.buildRequest(headers)
                } catch (e: Throwable) {
                    return Either.Failure(NetworkError.Unknown(e))
                }
            val auth =
                request?.getRequestProperty("Authorization")
                    ?: return Either.Failure(NetworkError.NotAuthenticated())

            Logger.debug(
                LogLevel.debug,
                LogScope.network,
                "Request Started",
                mapOf(
                    "url" to (request.url?.toString() ?: "unknown"),
                ),
            )

            val startTime = System.currentTimeMillis()
            val responseCode: Int =
                try {
                    request.responseCode
                } catch (e: Throwable) {
                    e.printStackTrace()
                    return Either.Failure(NetworkError.Unknown(e))
                }

            var responseMessage: String? = null

            when (responseCode) {
                in 200..299 -> {
                    responseMessage = request.inputStream.bufferedReader().use { it.readText() }
                    request.disconnect()
                }
                HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER -> {
                    val location = request.getHeaderField("Location")
                    request.disconnect()
                    return execute(requestData.copyWithUrl(URI(location)))
                }
                else -> {
                    Logger.debug(
                        LogLevel.error,
                        LogScope.network,
                        "Request failed : ${request.responseCode}",
                        mapOf(
                            "request" to request.toString(),
                            "api_key" to auth,
                            "url" to (request.url?.toString() ?: "unknown"),
                        ),
                    )
                    request.disconnect()
                    return Either.Failure(NetworkError.Unknown(Exception("Failed, response code  $responseCode")))
                }
            }

            val requestDuration = (System.currentTimeMillis() - startTime) / 1000.0
            val requestId =
                try {
                    request.getRequestId(auth, requestDuration)
                } catch (e: Throwable) {
                    return Either.Failure(
                        if (e is NetworkError) {
                            e
                        } else {
                            NetworkError.Unknown(
                                Exception("Failed to get request id. ${e.message}"),
                            )
                        },
                    )
                }

            Logger.debug(
                LogLevel.debug,
                LogScope.network,
                "Request Completed",
                mapOf(
                    "request" to request.toString(),
                    "api_key" to auth,
                    "url" to (request.url?.toString() ?: "unknown"),
                    "request_id" to requestId,
                    "request_duration" to requestDuration,
                ),
            )
            return Either.Success(
                RequestResult(
                    requestId,
                    responseCode,
                    responseMessage,
                    requestDuration,
                    headers,
                ),
            )
        } catch (e: Throwable) {
            return Either.Failure(NetworkError.Unknown(e))
        }
    }

    private fun <T : @Serializable Any> NetworkRequestData<T>.buildRequest(headers: Map<String, String>): HttpURLConnection? {
        val url: URL

        if (components != null) {
            val query = components.queryItems?.joinToString("&") { "${it.name}=${it.value}" }
            val urlString =
                "${components.scheme}://${components.host}${components.path}?${query ?: ""}"
            url = URL(urlString)
        } else if (this.url != null) {
            url = this.url.toURL()
        } else {
            return null
        }

        val connection = url.openConnection() as HttpURLConnection
        headers.forEach { header ->
            connection.setRequestProperty(header.key, header.value)
        }

        connection.doOutput = method.method == HttpMethod.POST.method
        if (components?.bodyData != null) {
            connection.doInput = true
        }

        if (components?.bodyData != null) {
            val outputStream = connection.outputStream
            outputStream.write(components.bodyData)
            outputStream.close()
        }

        connection.requestMethod = method.method

        return connection
    }

    @Throws(NetworkError::class)
    private fun HttpURLConnection.getRequestId(
        auth: String,
        requestDuration: Double,
    ): String {
        var requestId = "unknown"
        val request = this

        val id = request.getHeaderField("x-request-id")
        if (id != null) {
            requestId = id
        }

        when (request.responseCode) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> {
                Logger.debug(
                    LogLevel.error,
                    LogScope.network,
                    "Unable to Authenticate",
                    mapOf(
                        "request" to request.toString(),
                        "api_key" to auth,
                        "url" to request.url.toString(),
                        "request_id" to requestId,
                        "request_duration" to requestDuration,
                    ),
                )
                throw NetworkError.NotAuthenticated()
            }

            HttpURLConnection.HTTP_NOT_FOUND -> {
                Logger.debug(
                    LogLevel.error,
                    LogScope.network,
                    "Not Found",
                    mapOf(
                        "request" to request.toString(),
                        "api_key" to auth,
                        "url" to request.url.toString(),
                        "request_id" to requestId,
                        "request_duration" to requestDuration,
                    ),
                )
                throw NetworkError.NotFound()
            }
        }
        return requestId
    }
}
