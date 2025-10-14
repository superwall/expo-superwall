package com.superwall.sdk.paywall.presentation.get_paywall

import android.app.Activity
import com.superwall.sdk.Superwall
import com.superwall.sdk.misc.Either
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.getPaywallComponents
import com.superwall.sdk.paywall.presentation.internal.operators.logErrors
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import com.superwall.sdk.paywall.view.PaywallView
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class PaywallComponents(
    val view: PaywallView,
    val presenter: Activity?,
    val rulesOutcome: RuleEvaluationOutcome,
    val debugInfo: Map<String, Any>,
)

/**
 * Gets a paywall to present, publishing [PaywallState] objects that provide updates on the lifecycle of the paywall.
 *
 * @param request A presentation request of type [PresentationRequest] to feed into a presentation pipeline.
 * @param publisher A [MutableSharedFlow] that emits [PaywallState] objects.
 * @return A [PaywallView] to present.
 * @throws Throwable if an error occurs during the process.
 */
@Throws(Throwable::class)
internal suspend fun Superwall.getPaywall(
    request: PresentationRequest,
    publisher: MutableSharedFlow<PaywallState> = MutableSharedFlow(),
): Either<PaywallView, Throwable> =
    getPaywallComponents(request, publisher).fold(onSuccess = {
        it.view.set(
            request = request,
            paywallStatePublisher = publisher,
            unsavedOccurrence = it.rulesOutcome.unsavedOccurrence,
        )
        Either.Success(it.view)
    }, onFailure = {
        logErrors(request, error = it)
        Either.Failure(it)
    })

/**
 * Gets a paywall to present synchronously, providing updates on the lifecycle of the paywall through a callback.
 * Warning: This blocks the calling thread until the paywall is returned.
 *
 * @param request A presentation request of type [PresentationRequest] to feed into a presentation pipeline.
 * @param onStateChanged A callback function that receives [PaywallState] updates.
 * @return A [PaywallView] to present.
 */
fun Superwall.getPaywallSync(
    request: PresentationRequest,
    onStateChanged: (PaywallState) -> Unit = {},
): Either<PaywallView, Throwable> {
    val scope = Superwall.instance.ioScope
    val publisher = MutableSharedFlow<PaywallState>()
    scope.launch {
        publisher.collectLatest {
            onStateChanged(it)
        }
    }
    return runBlocking {
        getPaywall(request, publisher)
    }
}
