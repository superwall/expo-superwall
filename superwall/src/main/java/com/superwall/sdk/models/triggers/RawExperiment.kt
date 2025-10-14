package com.superwall.sdk.models.triggers

import kotlinx.serialization.Serializable

@Serializable
data class RawExperiment(
    var id: String,
    var groupId: String,
    var variants: List<VariantOption>,
) {
    companion object {
        fun stub() =
            RawExperiment(
                id = "abc",
                groupId = "def",
                variants = listOf(VariantOption.stub()),
            )
    }
}
