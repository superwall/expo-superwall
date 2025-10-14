package com.superwall.sdk.paywall.presentation.result

import com.superwall.sdk.models.triggers.Experiment

// The result of tracking an event.
//
// Contains the possible cases resulting from tracking an event.
sealed class PresentationResult {
    // This event was not found on the dashboard.
    //
    // Please make sure you have added the event to a campaign on the dashboard and
    // double check its spelling.
    class PlacementNotFound : PresentationResult()

    // No matching rule was found for this trigger so no paywall will be shown.
    class NoAudienceMatch : PresentationResult()

    // A matching rule was found and this user will be shown a paywall.
    //
    // - Parameters:
    //   - experiment: The experiment associated with the trigger.
    data class Paywall(
        val experiment: Experiment,
    ) : PresentationResult()

    // A matching rule was found and this user was assigned to a holdout group so will not be shown a paywall.
    //
    // - Parameters:
    //   - experiment: The experiment  associated with the trigger.
    data class Holdout(
        val experiment: Experiment,
    ) : PresentationResult()

    // No view controller could be found to present on.
    class PaywallNotAvailable : PresentationResult()
}
