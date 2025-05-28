package expo.modules.superwallexpo.json


import com.superwall.sdk.models.triggers.Experiment

fun Experiment.Variant.toJson(): Map<String, Any?> {
    return mapOf(
        "id" to this.id,
        "type" to this.type.toString(),
        "paywallId" to this.paywallId
    )
}
