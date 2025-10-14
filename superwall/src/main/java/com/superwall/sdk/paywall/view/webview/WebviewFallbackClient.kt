package com.superwall.sdk.paywall.view.webview

import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.superwall.sdk.models.paywall.PaywallWebviewUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/*
* An implementation of a FallbackHandler and a WebViewClient that will attempt to load a PaywallWebviewUrl from
* a list of URLs with a weighted probability. If the loading fails, it will try to load another URL from the list.
* */
internal class WebviewFallbackClient(
    private val config: PaywallWebviewUrl.Config,
    private val ioScope: CoroutineScope,
    private val mainScope: CoroutineScope,
    private val loadUrl: (PaywallWebviewUrl) -> Unit,
    private val stopLoading: () -> Unit,
    private val onCrashed: (RenderProcessGoneDetail) -> Unit,
) : DefaultWebviewClient("", ioScope, onCrashed) {
    private class MaxAttemptsReachedException : Exception("Max attempts reached")

    private var failureCount = 0

    private val urls = config.endpoints

    private val untriedUrls = urls.toMutableSet()

    /*
     * The state of currently Loading URL, reset to None when URL is loaded
     * */

    private sealed interface UrlState {
        object None : UrlState

        object Loading : UrlState

        object PageStarted : UrlState

        object PageError : UrlState

        object Timeout : UrlState
    }

    private val timeoutFlow = MutableStateFlow<UrlState>(UrlState.None)

    /*
     * When page starts Loading, we wait for N miliseconds until we consider it timed out.
     * If it is timedOut, we update the timeoutFlow so the client knows it can start loading
     * next fallback URL
     * */
    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?,
    ) {
        super.onPageStarted(view, url, favicon)
        val timeoutForUrl =
            urls.find { url?.contains(it.url) ?: false }?.timeout
                ?: error("No such URL($url in list of - ${urls.joinToString()} ")
        ioScope.launch {
            timeoutFlow.update { UrlState.Loading }
            try {
                withTimeout(timeoutForUrl) {
                    timeoutFlow.first { it is UrlState.PageStarted || it is UrlState.PageError }
                }
            } catch (e: TimeoutCancellationException) {
                mainScope.launch {
                    stopLoading()
                }
                timeoutFlow.update { UrlState.Timeout }
            }
        }
    }

    /*
     * Indicates the page has started loading resources, meaning we won't need to
     * fallback to another URL so we can stop the timeout handler.
     * */

    override fun onLoadResource(
        view: WebView?,
        url: String?,
    ) {
        ioScope.launch {
            if (timeoutFlow.value == UrlState.Loading) {
                timeoutFlow.emit(UrlState.PageStarted)
            }
        }
        super.onLoadResource(view, url)
    }

    internal fun nextUrl(): PaywallWebviewUrl {
        if (failureCount < config.maxAttempts) {
            failureCount += 1
            return if (untriedUrls.all { it.score == 0 }) {
                nextWeightedUrl(untriedUrls)
            } else {
                nextWeightedUrl(untriedUrls.filter { it.score != 0 })
            }
        } else {
            throw MaxAttemptsReachedException()
        }
    }

    // HTML has loaded and started rendering
    override fun onPageCommitVisible(
        view: WebView?,
        url: String?,
    ) {
        super.onPageCommitVisible(view, url)
        failureCount = 0
    }

    private tailrec fun evaluateNext(
        chosenNumber: Int,
        untriedUrls: Collection<PaywallWebviewUrl>,
        accumulatedScore: Int = 0,
    ): PaywallWebviewUrl {
        val toTry =
            untriedUrls.firstOrNull() ?: throw NoSuchElementException("No more URLs to evaluate")
        val accScore = accumulatedScore + toTry.score
        return if (chosenNumber < accScore) {
            toTry
        } else {
            evaluateNext(chosenNumber, untriedUrls.drop(1), accScore)
        }
    }

    private fun nextWeightedUrl(fromUrls: Collection<PaywallWebviewUrl>): PaywallWebviewUrl {
        val totalScore = fromUrls.sumOf { it.score }

        if (totalScore == 0) {
            val url = untriedUrls.random()
            untriedUrls.remove(url)
            return url
        }

        val random = (0 until totalScore).random()
        val next = evaluateNext(random, untriedUrls, 0)
        untriedUrls.remove(next)
        return next
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        timeoutFlow.update { UrlState.PageError }
        super.onReceivedHttpError(view, request, errorResponse)
    }

    internal fun loadWithFallback() {
        // If we have no URLs to try, we emit an error
        if (urls.isEmpty()) {
            ioScope.launch {
                webviewClientEvents.emit(WebviewClientEvent.OnError(WebviewError.NoUrls))
            }
            return
        }

        // We try to get the next URL
        val url =
            try {
                nextUrl()
            } catch (e: NoSuchElementException) {
                // If there is no more URLS, we let the client know
                ioScope.launch {
                    webviewClientEvents.emit(
                        WebviewClientEvent.OnError(
                            WebviewError.AllUrlsFailed(
                                urls.map { it.url },
                            ),
                        ),
                    )
                }
                return
            } catch (e: MaxAttemptsReachedException) {
                // If we reached the max attempts, we let the client know
                ioScope.launch {
                    webviewClientEvents.emit(
                        WebviewClientEvent.OnError(
                            WebviewError.MaxAttemptsReached(
                                urls.subtract(untriedUrls).map { it.url },
                            ),
                        ),
                    )
                }
                return
            }

        if (failureCount > 0) {
            ioScope.launch {
                webviewClientEvents.emit(WebviewClientEvent.LoadingFallback)
            }
        }

        mainScope.launch {
            loadUrl(url)
            ioScope.launch {
                val nextEvent =
                    timeoutFlow.first { it is UrlState.PageStarted || it is UrlState.Timeout || it is UrlState.PageError }
                if (nextEvent is UrlState.Timeout) {
                    loadWithFallback()
                } else {
                    timeoutFlow.update { UrlState.None }
                }
            }
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError,
    ) {
        timeoutFlow.update { UrlState.PageError }
        super.onReceivedError(view, request, error)
        loadWithFallback()
    }
}
