package com.superwall.sdk.config.models

import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.storage.SurveyAssignmentKey
import kotlinx.serialization.Serializable

@Serializable
data class Survey(
    val id: String,
    val assignmentKey: String,
    val title: String,
    val message: String,
    val options: List<SurveyOption>,
    val presentationCondition: SurveyShowCondition,
    val presentationProbability: Double,
    val includeOtherOption: Boolean,
    val includeCloseOption: Boolean,
) {
    fun shouldAssignHoldout(isDebuggerLaunched: Boolean): Boolean {
        if (isDebuggerLaunched) {
            return false
        }
        // Return immediately if no chance to present.
        if (presentationProbability == 0.0) {
            return true
        }

        // Choose random number to present the survey with
        // the probability of presentationProbability.
        val randomNumber = Math.random()
        return randomNumber >= presentationProbability
    }

    fun hasSeenSurvey(storage: LocalStorage): Boolean {
        val existingAssignmentKey = storage.read(SurveyAssignmentKey) ?: return false

        return existingAssignmentKey == assignmentKey
    }
}
