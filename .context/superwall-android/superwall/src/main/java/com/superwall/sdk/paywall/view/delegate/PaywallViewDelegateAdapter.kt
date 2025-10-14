package com.superwall.sdk.paywall.view.delegate

import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.view.PaywallView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class PaywallViewDelegateAdapter(
    kotlinDelegate: PaywallViewCallback?,
) {
    val kotlinDelegate = WeakReference(kotlinDelegate)

    val hasJavaDelegate: Boolean
        get() = false

    suspend fun onFinished(
        paywall: PaywallView,
        result: PaywallResult,
        shouldDismiss: Boolean,
    ) = withContext(Dispatchers.Main) {
        kotlinDelegate?.get()?.onFinished(paywall, result, shouldDismiss)
    }
}
