package com.superwall.sdk.config

import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.then
import com.superwall.sdk.models.assignment.Assignment
import com.superwall.sdk.models.assignment.AssignmentPostback
import com.superwall.sdk.models.assignment.ConfirmableAssignment
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.ExperimentID
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.network.NetworkError
import com.superwall.sdk.network.SuperwallAPI
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class Assignments(
    private val storage: LocalStorage,
    private val network: SuperwallAPI,
    private val ioScope: CoroutineScope,
    unconfirmedAssignments: Map<ExperimentID, Experiment.Variant> = emptyMap(),
) {
    // A memory store of assignments that are yet to be confirmed.
    private var _unconfirmedAssignments = unconfirmedAssignments.toMutableMap()
    val unconfirmedAssignments: Map<ExperimentID, Experiment.Variant>
        get() = _unconfirmedAssignments

    fun choosePaywallVariants(triggers: Set<Trigger>) {
        updateAssignments { confirmedAssignments ->
            ConfigLogic.chooseAssignments(
                fromTriggers = triggers,
                confirmedAssignments = confirmedAssignments,
            )
        }
    }

    suspend fun getAssignments(triggers: Set<Trigger>): Either<List<Assignment>, NetworkError> =
        network
            .getAssignments()
            .then {
                updateAssignments { confirmedAssignments ->
                    ConfigLogic.transferAssignmentsFromServerToDisk(
                        assignments = it,
                        triggers = triggers,
                        confirmedAssignments = confirmedAssignments,
                        unconfirmedAssignments = unconfirmedAssignments,
                    )
                }
            }

    fun confirmAssignment(assignment: ConfirmableAssignment) {
        withErrorTracking {
            val postback: AssignmentPostback = AssignmentPostback.create(assignment)
            ioScope.launch { network.confirmAssignments(postback) }

            updateAssignments { confirmedAssignments ->
                ConfigLogic.move(
                    assignment,
                    unconfirmedAssignments,
                    confirmedAssignments,
                )
            }
        }
    }

    private fun updateAssignments(operation: (Map<ExperimentID, Experiment.Variant>) -> ConfigLogic.AssignmentOutcome) {
        withErrorTracking {
            var confirmedAssignments = storage.getConfirmedAssignments()

            val updatedAssignments = operation(confirmedAssignments)
            _unconfirmedAssignments = updatedAssignments.unconfirmed.toMutableMap()
            confirmedAssignments = updatedAssignments.confirmed.toMutableMap()

            storage.saveConfirmedAssignments(confirmedAssignments)
        }
    }

    fun reset() {
        _unconfirmedAssignments.clear()
    }
}
