package com.superwall.sdk.models.assignment

import com.superwall.sdk.models.SerializableEntity
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmedAssignmentResponse(
    val assignments: MutableList<Assignment>,
) : SerializableEntity
