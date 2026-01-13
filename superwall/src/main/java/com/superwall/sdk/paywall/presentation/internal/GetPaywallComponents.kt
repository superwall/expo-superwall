package com.superwall.sdk.paywall.presentation.internal

import com.superwall.sdk.Superwall
import com.superwall.sdk.misc.Either
import com.superwall.sdk.misc.toResult
import com.superwall.sdk.models.assignment.ConfirmedAssignment
import com.superwall.sdk.paywall.presentation.get_paywall.PaywallComponents
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

@Throws(Throwable::class)
suspend fun Superwall.getPaywallComponents(
    request: PresentationRequest,
    publisher: MutableSharedFlow<PaywallState>? = null,
): Result<PaywallComponents> =
    runGetPaywallComponents(
        factory = dependencyContainer.getPaywallComponentsFactory,
        request = request,
        publisher = publisher,
    )

internal suspend fun runGetPaywallComponents(
    factory: GetPaywallComponentsFactory,
    request: PresentationRequest,
    publisher: MutableSharedFlow<PaywallState>? = null,
): Result<PaywallComponents> =
    withErrorTracking {
        factory.waitForEntitlementsAndConfig(request, publisher)
        val debugInfo = emptyMap<String, Any>() // TODO: logPresentation

        factory.checkDebuggerPresentation(request, publisher)

        val rulesOutcome = factory.evaluateRules(request)
        val outcome = rulesOutcome.getOrThrow()

        factory.confirmHoldoutAssignment(request, outcome)

        val paywallView =
            factory.getPaywallView(request, outcome, debugInfo, publisher).getOrThrow()

        val presenter =
            factory.getPresenterIfNecessary(
                paywallView,
                outcome,
                request,
                publisher,
            )

        factory.confirmPaywallAssignment(
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

internal suspend fun confirmAssignment(
    factory: GetPaywallComponentsFactory,
    request: PresentationRequest,
): Either<ConfirmedAssignment?, Throwable> {
    return withErrorTracking {
        factory.waitForEntitlementsAndConfig(request, publisher = null)
        val rules = factory.evaluateRules(request)
        if (rules.isFailure) {
            throw rules.exceptionOrNull()!!
        }
        factory.confirmHoldoutAssignment(request, rules.getOrThrow())
        val confirmableAssignment = rules.getOrThrow().confirmableAssignment
        factory.confirmPaywallAssignment(confirmableAssignment, request, request.flags.isDebuggerLaunched)

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
        runGetPaywallComponents(dependencyContainer.getPaywallComponentsFactory, request, publisher)
    }
