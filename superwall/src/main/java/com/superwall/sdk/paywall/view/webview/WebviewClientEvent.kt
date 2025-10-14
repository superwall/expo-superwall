package com.superwall.sdk.paywall.view.webview

sealed class WebviewClientEvent {
    data class OnPageFinished(
        val url: String,
    ) : WebviewClientEvent()

    data class OnError(
        val webviewError: WebviewError,
    ) : WebviewClientEvent()

    data class OnResourceError(
        val webviewError: WebviewError,
    ) : WebviewClientEvent()

    object LoadingFallback : WebviewClientEvent()
}

sealed class WebviewError {
    data class NetworkError(
        val code: Int,
        val description: String,
        val url: String,
    ) : WebviewError() {
        override fun toString(): String = "The network failed with error code: $code - $description - $url ."
    }

    object Timeout : WebviewError() {
        override fun toString(): String = "Paywall loading reached specified timeout"
    }

    data class MaxAttemptsReached(
        val urls: List<String>,
    ) : WebviewError() {
        override fun toString(): String = "The webview has attempted to load too many times."
    }

    object NoUrls : WebviewError() {
        override fun toString() = "There were no paywall URLs provided."
    }

    data class AllUrlsFailed(
        val urls: List<String>,
    ) : WebviewError() {
        override fun toString(): String = "All paywall URLs have failed to load."
    }
}
