package com.superwall.sdk.deeplinks

import android.net.Uri
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.debug.DebugManager
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.utilities.withErrorTracking
import com.superwall.sdk.web.WebPaywallRedeemer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque

class DeepLinkRouter(
    val redeemer: WebPaywallRedeemer,
    val ioScope: IOScope = IOScope(),
    val debugManager: DebugManager,
    val track: suspend (TrackableSuperwallEvent) -> Unit,
) {
    companion object {
        private var unhandledDeepLinks = ConcurrentLinkedDeque<Uri>()

        fun handleDeepLink(uri: Uri): Result<Boolean> =
            if (uri.redeemableCode.isSuccess || DebugManager.outcomeForDeepLink(uri).isSuccess) {
                if (Superwall.initialized && Superwall.instance.configurationState is ConfigurationStatus.Configured) {
                    Superwall.instance.dependencyContainer.deepLinkRouter
                        .handleDeepLink(uri)
                } else {
                    unhandledDeepLinks.add(uri)
                }
                Result.success(true)
            } else {
                Result.failure(IllegalArgumentException("Not a superwall link"))
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
                track(InternalSuperwallEvent.DeepLink(uri = uri))
            }
            val handledAsRedemption =
                redeemer.deepLinkReferrer
                    .handleDeepLink(uri)
                    .onSuccess {
                        ioScope.launch {
                            redeemer.redeem(WebPaywallRedeemer.RedeemType.Code(it))
                        }
                        return Result.success(true)
                    }.isSuccess

            val result =
                if (!handledAsRedemption) {
                    debugManager.handle(deepLinkUrl = uri)
                } else {
                    handledAsRedemption
                }
            return Result.success(result)
        }.toResult()
}

internal val Uri.redeemableCode: Result<String>
    get() {
        val failure =
            Result.failure<String>(UnsupportedOperationException("Link not valid for redemption"))
        return if (host?.contains("superwall") == true && lastPathSegment.equals("redeem")) {
            getQueryParameter("code")?.let {
                Result.success(it)
            } ?: failure
        } else {
            failure
        }
    }
