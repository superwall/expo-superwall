package com.superwall.sdk.paywall.presentation.internal

import com.superwall.sdk.models.triggers.Experiment

sealed class PaywallPresentationRequestStatus(
    val status: String,
) {
    object Presentation : PaywallPresentationRequestStatus("presentation")

    object NoPresentation : PaywallPresentationRequestStatus("no_presentation")

    object Timeout : PaywallPresentationRequestStatus("timeout")
}

/**
 * The reason to why the paywall couldn't present.
 */
sealed class PaywallPresentationRequestStatusReason(
    val description: String,
    val info: String,
) : Throwable() {
    /** Trying to present paywall when debugger is launched. */
    class DebuggerPresented :
        PaywallPresentationRequestStatusReason(
            "debugger_presented",
            "The paywall debugger is currently presented. Dismiss it before presenting a paywall.",
        )

    /** There's already a paywall presented. */
    class PaywallAlreadyPresented :
        PaywallPresentationRequestStatusReason(
            "paywall_already_presented",
            "A paywall is already being presented. Dismiss it before presenting another one.",
        )

    /** The user is in a holdout group. */
    data class Holdout(
        val experiment: Experiment,
    ) : PaywallPresentationRequestStatusReason(
            "holdout",
            "The user is in a holdout group for experiment '${experiment.id}'.",
        )

    /** No rules defined in the campaign for the event matched. */
    class NoAudienceMatch :
        PaywallPresentationRequestStatusReason(
            "no_rule_match",
            "The user did not match any rules configured for this placement.",
        )

    /** The event provided was not found in any campaign on the dashboard. */
    class PlacementNotFound :
        PaywallPresentationRequestStatusReason(
            "event_not_found",
            "The placement was not found in any campaign on the Superwall dashboard.",
        )

    /** There was an error getting the paywall view. */
    class NoPaywallView(
        reason: String = "The paywall view could not be created. Check that the Android System WebView is enabled on the device.",
    ) : PaywallPresentationRequestStatusReason(
            "no_paywall_view_controller",
            reason,
        )

    /** There isn't a view to present the paywall on. */
    class NoPresenter :
        PaywallPresentationRequestStatusReason(
            "no_presenter",
            "There is no Activity to present the paywall on. Make sure an Activity is visible before registering a placement.",
        )

    /** The config hasn't been retrieved from the server in time. */
    class NoConfig :
        PaywallPresentationRequestStatusReason(
            "no_config",
            "The Superwall config could not be retrieved in time. Check your network connection and that your API key is correct.",
        )

    /**
     * The entitlement status timed out or Google Play Billing is not available.
     * This happens when the entitlementStatus stays unknown for more than 5 seconds.
     */
    class SubscriptionStatusTimeout(
        reason: String = "The entitlement status stayed 'unknown' for over 5 seconds.  Ensure that Google Play Billing is available and if you're using a custom purchase controller, ensure you're setting the entitlement status on time.",
    ) : PaywallPresentationRequestStatusReason(
            "subscription_status_timeout",
            reason,
        )
}

typealias PresentationPipelineError = PaywallPresentationRequestStatusReason
