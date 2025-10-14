package com.superwall.sdk.web

import android.content.Context
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.newBuilder
import com.android.installreferrer.api.InstallReferrerStateListener
import com.superwall.sdk.deeplinks.redeemableCode
import com.superwall.sdk.misc.IOScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLDecoder
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface CheckForReferral {
    suspend fun checkForReferral(): Result<String>

    fun handleDeepLink(url: Uri): Result<String>
}

class DeepLinkReferrer(
    context: () -> Context,
    private val scope: IOScope,
) : CheckForReferral {
    private var referrerClient: InstallReferrerClient?
        get() {
            if (field?.isReady == true) {
                return field
            } else {
                return null
            }
        }

    init {
        referrerClient = newBuilder(context()).build()
        tryConnecting()
    }

    private class ConnectionListener(
        val finished: (responseCode: Int) -> Unit,
        val disconnected: () -> Unit,
    ) : InstallReferrerStateListener {
        override fun onInstallReferrerSetupFinished(responseCode: Int) {
            finished(responseCode)
        }

        override fun onInstallReferrerServiceDisconnected() {
            disconnected()
        }
    }

    fun tryConnecting(timeout: Int = 0) {
        try {
            val connect = {
                referrerClient?.startConnection(
                    ConnectionListener(
                        finished = {
                            when (it) {
                                InstallReferrerClient.InstallReferrerResponse.OK -> {
                                    referrerClient?.installReferrer?.installReferrer
                                }

                                else -> {
                                    referrerClient?.endConnection()
                                    referrerClient = null
                                }
                            }
                        },
                        disconnected = {
                            tryConnecting(timeout + 1000)
                        },
                    ),
                )
            }
            if (timeout == 0) {
                connect()
            } else {
                scope.launch {
                    withTimeout(timeout.milliseconds) {
                        connect()
                    }
                }
            }
        } catch (e: Throwable) {
            kotlin.runCatching {
                referrerClient?.endConnection()
                referrerClient = null
            }
        }
    }

    override suspend fun checkForReferral(): Result<String> =
        try {
            withTimeoutOrNull(30.seconds) {
                while (referrerClient?.isReady != true) {
                    // no-op
                }
                referrerClient?.installReferrer?.installReferrer?.toString()
            }.let {
                val query = it?.getUrlParams() ?: emptyMap()
                val code = query["code"]?.firstOrNull()
                referrerClient?.endConnection()
                referrerClient = null
                if (code == null) {
                    Result.failure(IllegalStateException("Play store cannot connect"))
                } else {
                    Result.success(code)
                }
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }

    override fun handleDeepLink(url: Uri): Result<String> = url.redeemableCode

    private fun String.getUrlParams(): Map<String, List<String>> {
        val urlParts = split("\\?".toRegex()).filter(String::isNotEmpty)
        if (urlParts.size < 2) {
            return emptyMap()
        }
        val query = urlParts[1]
        return listOf("item").associateWith { key ->
            query
                .split("&?$key=".toRegex())
                .filter(String::isNotEmpty)
                .map { URLDecoder.decode(it, "UTF-8") }
        }
    }
}
