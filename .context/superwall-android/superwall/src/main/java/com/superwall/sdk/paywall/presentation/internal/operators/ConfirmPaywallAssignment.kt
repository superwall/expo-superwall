package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Superwall
import com.superwall.sdk.config.Assignments
import com.superwall.sdk.dependencies.DependencyContainer
import com.superwall.sdk.models.assignment.ConfirmableAssignment
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType

/**
 * Confirms the paywall assignment, if it exists.
 *
 * This is split from the holdout assignment because overrides can make the
 * paywall present even if the user is subscribed. We only know the overrides
 * at this point.
 */
fun Superwall.confirmPaywallAssignment(
    confirmableAssignment: ConfirmableAssignment?,
    request: PresentationRequest,
    isDebuggerLaunched: Boolean,
    dependencyContainer: DependencyContainer? = null,
) {
    val actualDependencyContainer = dependencyContainer ?: this.dependencyContainer
    confirmPaywallAssignment(
        confirmableAssignment,
        request,
        isDebuggerLaunched,
        actualDependencyContainer.assignments,
    )
}

internal fun confirmPaywallAssignment(
    confirmableAssignment: ConfirmableAssignment?,
    request: PresentationRequest,
    isDebuggerLaunched: Boolean,
    assignments: Assignments,
) {
    if (request.flags.type == PresentationRequestType.GetImplicitPresentationResult) {
        return
    }
    // Debuggers shouldn't confirm assignments.
    if (isDebuggerLaunched) {
        return
    }

    confirmableAssignment?.let {
        assignments.confirmAssignment(it)
    }
}
