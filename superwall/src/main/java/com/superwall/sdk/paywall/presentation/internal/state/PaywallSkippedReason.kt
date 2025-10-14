package com.superwall.sdk.paywall.presentation.internal.state

import com.superwall.sdk.models.triggers.Experiment

sealed class PaywallSkippedReason : Throwable() {
    data class Holdout(
        val experiment: Experiment,
    ) : PaywallSkippedReason()

    class NoAudienceMatch : PaywallSkippedReason()

    class PlacementNotFound : PaywallSkippedReason()

    class UserIsSubscribed : PaywallSkippedReason()

    override fun equals(other: Any?): Boolean =
        when {
            this is Holdout && other is Holdout -> this.experiment == other.experiment
            this is NoAudienceMatch && other is NoAudienceMatch -> true
            this is PlacementNotFound && other is PlacementNotFound -> true
            this is UserIsSubscribed && other is UserIsSubscribed -> true
            else -> false
        }
}
