package com.superwall.sdk.models.assignment

import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.ExperimentID

class ConfirmedAssignment(
    val experimentId: ExperimentID,
    val variant: Experiment.Variant,
)
