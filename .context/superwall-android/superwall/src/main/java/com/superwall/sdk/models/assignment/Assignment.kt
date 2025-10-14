package com.superwall.sdk.models.assignment

// data class Assignment(var experimentId: String, var variantId: String) {
//
//    // Converts this Assignment object to a JSONObject.
//    fun toJson(): JSONObject {
//        val jsonObject = JSONObject()
//        jsonObject.put("experiment_Id", experimentId)
//        jsonObject.put("variant_id", variantId)
//        return jsonObject
//    }
//
//    // Creates an Assignment object from a JSONObject.
//    companion object {
//        fun fromJson(jsonObject: JSONObject): Assignment {
//            val experimentId = jsonObject.getString("experiment_id")
//            val variantId = jsonObject.getString("variant_id")
//            return Assignment(experimentId, variantId)
//        }
//    }
// }

import kotlinx.serialization.Serializable

@Serializable
data class Assignment(
    var experimentId: String,
    var variantId: String,
)
