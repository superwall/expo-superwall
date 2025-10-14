package com.superwall.sdk.models.triggers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class VariantOption(
    @SerialName("variant_type")
    var type: Experiment.Variant.VariantType,
    @SerialName("variant_id")
    var id: String,
    @SerialName("percentage")
    var percentage: Int,
    @SerialName("paywall_identifier")
    var paywallId: String? = null,
) {
    @Transient
    val variant = toVariant()

    fun toVariant() =
        Experiment.Variant(
            id = id,
            type = type,
            paywallId = paywallId,
        )

    companion object {
        fun stub() =
            VariantOption(
                type = Experiment.Variant.VariantType.TREATMENT,
                id = UUID.randomUUID().toString(),
                percentage = 100,
                paywallId = UUID.randomUUID().toString(),
            )
    }
}
