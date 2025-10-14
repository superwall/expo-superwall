package com.superwall.sdk.paywall.presentation

@kotlinx.serialization.Serializable
sealed class PaywallCloseReason {
    // / The paywall was closed by system logic, either after a purchase, because
    // / a deeplink was presented, close button pressed, etc.
    object SystemLogic : PaywallCloseReason()

    // / The paywall was automatically closed becacuse another paywall will show.
    // /
    // / This prevents ``Superwall/register(placement:params:handler:feature:)`` `feature`
    // / block from executing on dismiss of the paywall, because another paywall is set to show
    object ForNextPaywall : PaywallCloseReason()

    // / The paywall was closed because the webview couldn't be loaded.
    // /
    // / If this happens for a gated paywall, the ``PaywallPresentationHandler/onError(_:)``
    // / handler will be called. If it's for a non-gated paywall, the feature block will be called.
    object WebViewFailedToLoad : PaywallCloseReason()

    // / The paywall was closed because the user tapped the close button or dragged to dismiss.
    object ManualClose : PaywallCloseReason()

    // / The paywall hasn't been closed yet.
    object None : PaywallCloseReason()

    val stateShouldComplete: Boolean
        get() =
            when (this) {
                is ForNextPaywall, None -> false
                else -> true
            }
}
