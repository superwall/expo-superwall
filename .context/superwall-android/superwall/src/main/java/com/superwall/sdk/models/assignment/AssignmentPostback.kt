package com.superwall.sdk.models.assignment

// import org.json.JSONArray
// import org.json.JSONObject
//
// data class AssignmentPostback(var assignments: MutableList<Assignment>) {
//
//    // Converts this AssignmentPostback object to a JSONObject.
//    fun toJson(): JSONObject {
//        val jsonObject = JSONObject()
//        val jsonArray = JSONArray()
//
//        for (assignment in assignments) {
//            jsonArray.put(assignment.toJson())
//        }
//
//        jsonObject.put("assignments", jsonArray)
//
//        return jsonObject
//    }
//
//    companion object {
//        // Creates an AssignmentPostback object from a ConfirmableAssignment.
//        fun create(confirmableAssignment: ConfirmableAssignment): AssignmentPostback {
//            val assignments = mutableListOf<Assignment>()
//
//            assignments.add(
//                Assignment(
//                    experimentId = confirmableAssignment.experimentId,
//                    variantId = confirmableAssignment.variant.id
//                )
//            )
//
//            return AssignmentPostback(assignments)
//        }
//    }
// }

import kotlinx.serialization.Serializable

@Serializable
data class AssignmentPostback(
    val assignments: MutableList<Assignment>,
) {
    companion object {
        fun create(confirmableAssignment: ConfirmableAssignment): AssignmentPostback {
            val assignments = mutableListOf<Assignment>()

            assignments.add(
                Assignment(
                    experimentId = confirmableAssignment.experimentId,
                    variantId = confirmableAssignment.variant.id,
                ),
            )

            return AssignmentPostback(assignments)
        }
    }
}
