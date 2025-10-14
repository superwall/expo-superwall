package com.superwall.sdk.network

import kotlinx.serialization.Serializable
import java.net.URI
import java.util.*

data class URLQueryItem(
    val name: String,
    val value: String,
)

class NetworkRequestData<Response>(
    val components: Components? = null,
    val url: URI? = null,
    var method: HttpMethod = HttpMethod.GET,
    var requestId: String = UUID.randomUUID().toString(),
    var isForDebugging: Boolean = false,
    val factory: suspend (isForDebugging: Boolean, requestId: String) -> Map<String, String>,
) where Response : @Serializable Any {
    enum class HttpMethod(
        val method: String,
    ) {
        GET("GET"),
        POST("POST"),
    }

    data class Components(
        var scheme: String? = Api.scheme,
        val host: String? = null,
        val path: String,
        var queryItems: List<URLQueryItem>? = null,
        var bodyData: ByteArray? = null,
    )

    fun copyWithUrl(newUrl: URI) =
        NetworkRequestData<Response>(
            components =
                if (components == null) {
                    null
                } else {
                    components.copy(
                        scheme = newUrl.scheme,
                        host = newUrl.host,
                        path = newUrl.path,
                        queryItems = components.queryItems,
                        bodyData = components.bodyData,
                    )
                },
            url = if (url == null) null else newUrl,
            method = method,
            requestId = requestId,
            isForDebugging = isForDebugging,
            factory = factory,
        )
}
