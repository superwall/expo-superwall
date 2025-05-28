package expo.modules.superwallexpo.json


import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.assignment.ConfirmedAssignment

fun ConfirmedAssignment.toJson(): Map<String, Any> {
    val assignmentMap = mutableMapOf<String, Any>()
    assignmentMap["experimentId"] = this.experimentId
    assignmentMap["variant"] = this.variant.toJson()
    return assignmentMap
}
