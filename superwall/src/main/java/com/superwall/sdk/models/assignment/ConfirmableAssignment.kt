package com.superwall.sdk.models.assignment

import com.superwall.sdk.models.triggers.Experiment

data class ConfirmableAssignment(
    val experimentId: String, // Replace with the correct type
    val variant: Experiment.Variant, // Replace with the correct type
)
