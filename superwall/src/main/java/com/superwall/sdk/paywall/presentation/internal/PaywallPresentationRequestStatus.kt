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
) : Throwable() {
    /** Trying to present paywall when debugger is launched. */
    class DebuggerPresented : PaywallPresentationRequestStatusReason("debugger_presented")

    /** There's already a paywall presented. */
    class PaywallAlreadyPresented : PaywallPresentationRequestStatusReason("paywall_already_presented")

    /** The user is in a holdout group. */
    data class Holdout(
        val experiment: Experiment,
    ) : PaywallPresentationRequestStatusReason("holdout")

    /** No rules defined in the campaign for the event matched. */
    class NoAudienceMatch : PaywallPresentationRequestStatusReason("no_rule_match")

    /** The event provided was not found in any campaign on the dashboard. */
    class PlacementNotFound : PaywallPresentationRequestStatusReason("event_not_found")

    /** There was an error getting the paywall view. */
    class NoPaywallView : PaywallPresentationRequestStatusReason("no_paywall_view_controller")

    /** There isn't a view to present the paywall on. */
    class NoPresenter : PaywallPresentationRequestStatusReason("no_presenter")

    /** The config hasn't been retrieved from the server in time. */
    class NoConfig : PaywallPresentationRequestStatusReason("no_config")

    /**
     * The entitlement status timed out.
     * This happens when the entitlementStatus stays unknown for more than 5 seconds.
     */
    class SubscriptionStatusTimeout : PaywallPresentationRequestStatusReason("subscription_status_timeout")
}

typealias PresentationPipelineError = PaywallPresentationRequestStatusReason
