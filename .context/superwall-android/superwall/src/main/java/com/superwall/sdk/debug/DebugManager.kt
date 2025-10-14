package com.superwall.sdk.debug

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import com.superwall.sdk.Superwall
import com.superwall.sdk.dependencies.ViewFactory
import com.superwall.sdk.paywall.presentation.dismiss
import com.superwall.sdk.storage.LocalStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DebugManager(
    private val context: Context,
    private val storage: LocalStorage,
    private val factory: ViewFactory,
) {
    var view: DebugView? = null
        @MainThread set
    var isDebuggerLaunched = false

    data class DeepLinkOutcome(
        val debugKey: String,
        val paywallId: String?,
    )

    companion object {
        fun outcomeForDeepLink(deepLinkUrl: Uri): Result<DeepLinkOutcome> {
            val launchDebugger =
                SWDebugManagerLogic.getQueryItemValue(
                    deepLinkUrl,
                    SWDebugManagerLogic.Parameter.SUPERWALL_DEBUG,
                ) ?: return Result.failure(IllegalArgumentException("Not debug link"))
            if (!launchDebugger.toBoolean()) return Result.failure(IllegalArgumentException("Not debug link"))

            val debugKey =
                SWDebugManagerLogic.getQueryItemValue(
                    deepLinkUrl,
                    SWDebugManagerLogic.Parameter.TOKEN,
                ) ?: return Result.failure(IllegalArgumentException("Not debug link"))

            val paywallId =
                SWDebugManagerLogic.getQueryItemValue(
                    deepLinkUrl,
                    SWDebugManagerLogic.Parameter.PAYWALL_ID,
                )

            return Result.success(DeepLinkOutcome(debugKey, paywallId))
        }
    }

    fun handle(deepLinkUrl: Uri): Boolean =
        outcomeForDeepLink(deepLinkUrl)
            .onSuccess {
                storage.debugKey = it.debugKey
            }.onSuccess {
                CoroutineScope(Dispatchers.IO).launch {
                    launchDebugger(it.paywallId)
                }
            }.isSuccess

    @MainThread
    suspend fun launchDebugger(paywallDatabaseId: String? = null) {
        if (Superwall.instance.isPaywallPresented) {
            Superwall.instance.dismiss()
            launchDebugger(paywallDatabaseId)
        } else {
            if (view == null) {
                delay(200)
                presentDebugger(paywallDatabaseId)
            } else {
                closeDebugger(true)
                launchDebugger(paywallDatabaseId)
            }
        }
    }

    @MainThread
    suspend fun presentDebugger(paywallDatabaseId: String? = null) {
        isDebuggerLaunched = true
        val currentView = view
        if (currentView != null) {
            if (currentView.isActive) return

            currentView.paywallDatabaseId = paywallDatabaseId
            currentView.loadPreview()

            DebugViewActivity.startWithView(
                context,
                currentView,
            )
        } else {
            val newView = factory.makeDebugView(paywallDatabaseId)
            DebugViewActivity.startWithView(
                context,
                newView,
            )
            view = newView
        }
    }

    @MainThread
    suspend fun closeDebugger(animated: Boolean) {
        view?.encapsulatingActivity?.finish()

        view = null
        isDebuggerLaunched = false
    }
}
