package com.superwall.sdk.models.triggers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Experiment(
    @SerialName("experiment_id")
    val id: String,
    @SerialName("trigger_experiment_group_id")
    val groupId: String,
    @SerialName("variant")
    val variant: Variant,
) {
    @Serializable
    data class Variant(
        @SerialName("id")
        val id: String,
        @SerialName("type")
        val type: VariantType,
        @SerialName("paywall_identifier")
        val paywallId: String?,
    ) {
        @Serializable
        enum class VariantType {
            @SerialName("TREATMENT")
            TREATMENT,

            @SerialName("HOLDOUT")
            HOLDOUT,
        }
    }

    companion object {
        fun presentById(id: String) =
            Experiment(
                id = id,
                groupId = "",
                variant = Variant(id = "", type = Variant.VariantType.TREATMENT, paywallId = id),
            )
    }
}

typealias ExperimentID = String
