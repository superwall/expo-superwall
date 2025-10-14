package com.superwall.sdk.paywall.presentation.internal

import com.superwall.sdk.Superwall
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.models.assignment.ConfirmedAssignment
import com.superwall.sdk.paywall.presentation.get_paywall.PaywallComponents
import com.superwall.sdk.paywall.presentation.internal.operators.checkDebuggerPresentation
import com.superwall.sdk.paywall.presentation.internal.operators.confirmHoldoutAssignment
import com.superwall.sdk.paywall.presentation.internal.operators.confirmPaywallAssignment
import com.superwall.sdk.paywall.presentation.internal.operators.evaluateRules
import com.superwall.sdk.paywall.presentation.internal.operators.getPaywallView
import com.superwall.sdk.paywall.presentation.internal.operators.getPresenterIfNecessary
import com.superwall.sdk.paywall.presentation.internal.operators.waitForEntitlementsAndConfig
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

/**
 * Runs a pipeline of operations to get a paywall to present and associated components.
 *
 * @param request The presentation request.
 * @param publisher A `MutableStateFlow` that gets sent `PaywallState` objects.
 * @return A `PaywallComponents` object that contains objects associated with the
 * paywall view.
 * @throws PresentationPipelineError object associated with stages of the pipeline.
 */
@Throws(Throwable::class)
suspend fun Superwall.getPaywallComponents(
    request: PresentationRequest,
    publisher: MutableSharedFlow<PaywallState>? = null,
): Result<PaywallComponents> =
    withErrorTracking {
        waitForEntitlementsAndConfig(request, publisher)
        // TODO:
//    val debugInfo = logPresentation(request)
        val debugInfo = emptyMap<String, Any>()

        checkDebuggerPresentation(request, publisher)

        val rulesOutcome = evaluateRules(request)
        val outcome = rulesOutcome.getOrThrow()

        confirmHoldoutAssignment(request = request, rulesOutcome = outcome)

        val paywallView =
            getPaywallView(request, outcome, debugInfo, publisher, dependencyContainer).getOrThrow()

        val presenter = getPresenterIfNecessary(paywallView, outcome, request, publisher)

        confirmPaywallAssignment(
            outcome.confirmableAssignment,
            request,
            request.flags.isDebuggerLaunched,
        )

        PaywallComponents(
            view = paywallView,
            presenter = presenter,
            rulesOutcome = outcome,
            debugInfo = debugInfo,
        )
    }.toResult()

internal suspend fun Superwall.confirmAssignment(request: PresentationRequest): Either<ConfirmedAssignment?, Throwable> {
    return withErrorTracking {
        waitForEntitlementsAndConfig(request)
        val rules = evaluateRules(request)
        if (rules.isFailure) {
            throw rules.exceptionOrNull()!!
        }
        confirmHoldoutAssignment(request, rules.getOrThrow())
        val confirmableAssignment = rules.getOrThrow().confirmableAssignment
        confirmPaywallAssignment(confirmableAssignment, request, request.flags.isDebuggerLaunched)

        return@withErrorTracking confirmableAssignment?.let {
            ConfirmedAssignment(
                experimentId = it.experimentId,
                variant = it.variant,
            )
        }
    }
}

/**
 * Synchronously runs a pipeline of operations to get a paywall to present and associated components.
 *
 * @param request The presentation request.
 * @param publisher A `MutableStateFlow` that gets sent `PaywallState` objects.
 * @return A `PaywallComponents` object that contains objects associated with the
 * paywall view controller.
 * @throws PresentationPipelineError object associated with stages of the pipeline.
 */

fun Superwall.getPaywallComponentsSync(
    request: PresentationRequest,
    publisher: MutableSharedFlow<PaywallState>? = null,
): Result<PaywallComponents> =
    runBlocking {
        getPaywallComponents(request, publisher)
    }
