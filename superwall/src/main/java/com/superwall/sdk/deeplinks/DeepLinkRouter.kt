package com.superwall.sdk.deeplinks

import android.net.Uri
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.debug.DebugManager
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.utilities.withErrorTracking
import com.superwall.sdk.web.WebPaywallRedeemer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URI
import java.util.concurrent.ConcurrentLinkedDeque

class DeepLinkRouter(
    val redeemer: WebPaywallRedeemer,
    val ioScope: IOScope = IOScope(),
    val debugManager: DebugManager,
    val track: suspend (TrackableSuperwallEvent) -> Unit,
) {
    companion object {
        private var unhandledDeepLinks = ConcurrentLinkedDeque<Uri>()

        fun handleDeepLink(uri: Uri): Result<Boolean> {
            if (Superwall.initialized && Superwall.instance.configurationState is ConfigurationStatus.Configured) {
                Superwall.instance.dependencyContainer.deepLinkRouter
                    .handleDeepLink(uri)
            } else {
                unhandledDeepLinks.add(uri)
            }
            Logger.debug(
                LogLevel.info,
                LogScope.deepLinks,
                "Superwall handling provided deep link",
            )

            val handled =
                uri.redeemableCode.isSuccess || DebugManager.outcomeForDeepLink(uri).isSuccess

            if (handled) {
                return Result.success(true)
            } else {
                Logger.debug(
                    LogLevel.info,
                    LogScope.deepLinks,
                    "Superwall not handling the provided deep link",
                )
                return Result.failure(IllegalArgumentException("Not a superwall link"))
            }
        }
    }

    init {
        ioScope.launch {
            Superwall.hasInitialized.first { it }
            Superwall.instance.configurationStateListener.first { it is ConfigurationStatus.Configured }
            while (unhandledDeepLinks.isNotEmpty()) {
                val next = unhandledDeepLinks.pop()
                this@DeepLinkRouter.handleDeepLink(next)
                unhandledDeepLinks.remove(next)
            }
        }
    }

    fun handleDeepLink(uri: Uri): Result<Boolean> =
        withErrorTracking<Boolean> {
            ioScope.launch {
                track(InternalSuperwallEvent.DeepLink(uri = URI.create(uri.toString())))
            }
            val handledAsRedemption =
                redeemer.deepLinkReferrer
                    .handleDeepLink(uri)
                    .onSuccess {
                        ioScope.launch {
                            redeemer.redeem(WebPaywallRedeemer.RedeemType.Code(it))
                        }
                    }.isSuccess

            val result: Boolean =
                if (handledAsRedemption) {
                    true
                } else {
                    debugManager.handle(deepLinkUrl = uri)
                }
            result
        }.toResult()
}

internal val Uri.redeemableCode: Result<String>
    get() {
        val failure =
            Result.failure<String>(UnsupportedOperationException("Link not valid for redemption"))
        return if (host?.contains("superwall") == true && lastPathSegment.equals("redeem")) {
            (getQueryParameter("code") ?: getQueryParameter("codes"))?.let {
                Result.success(it)
            } ?: failure
        } else {
            failure
        }
    }
