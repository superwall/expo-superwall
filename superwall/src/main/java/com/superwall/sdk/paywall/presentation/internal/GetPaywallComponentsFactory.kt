package com.superwall.sdk.paywall.presentation.internal

import com.superwall.sdk.Superwall
import com.superwall.sdk.paywall.presentation.internal.operators.attemptTriggerFire
import com.superwall.sdk.paywall.presentation.internal.operators.checkDebuggerPresentation
import com.superwall.sdk.paywall.presentation.internal.operators.confirmHoldoutAssignment
import com.superwall.sdk.paywall.presentation.internal.operators.confirmPaywallAssignment
import com.superwall.sdk.paywall.presentation.internal.operators.evaluateRules
import com.superwall.sdk.paywall.presentation.internal.operators.getPaywallView
import com.superwall.sdk.paywall.presentation.internal.operators.getPresenterIfNecessary
import com.superwall.sdk.paywall.presentation.internal.operators.waitForEntitlementsAndConfig
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.presentation.rule_logic.RuleEvaluationOutcome
import kotlinx.coroutines.flow.MutableSharedFlow

internal interface GetPaywallComponentsFactory {
    suspend fun waitForEntitlementsAndConfig(
        request: PresentationRequest,
        publisher: MutableSharedFlow<PaywallState>?,
    )

    suspend fun checkDebuggerPresentation(
        request: PresentationRequest,
        publisher: MutableSharedFlow<PaywallState>?,
    )

    suspend fun evaluateRules(request: PresentationRequest): Result<RuleEvaluationOutcome>

    suspend fun confirmHoldoutAssignment(
        request: PresentationRequest,
        rulesOutcome: RuleEvaluationOutcome,
    )

    suspend fun getPaywallView(
        request: PresentationRequest,
        rulesOutcome: RuleEvaluationOutcome,
        debugInfo: Map<String, Any>,
        publisher: MutableSharedFlow<PaywallState>?,
    ): Result<com.superwall.sdk.paywall.view.PaywallView>

    suspend fun getPresenterIfNecessary(
        paywallView: com.superwall.sdk.paywall.view.PaywallView,
        rulesOutcome: RuleEvaluationOutcome,
        request: PresentationRequest,
        publisher: MutableSharedFlow<PaywallState>?,
    ): android.app.Activity?

    suspend fun confirmPaywallAssignment(
        confirmableAssignment: com.superwall.sdk.models.assignment.ConfirmableAssignment?,
        request: PresentationRequest,
        isDebuggerLaunched: Boolean,
    )
}

internal class DefaultGetPaywallComponentsFactory(
    private val superwall: Superwall,
) : GetPaywallComponentsFactory {
    override suspend fun waitForEntitlementsAndConfig(
        request: PresentationRequest,
        publisher: MutableSharedFlow<PaywallState>?,
    ) = waitForEntitlementsAndConfig(request, publisher, superwall.dependencyContainer)

    override suspend fun checkDebuggerPresentation(
        request: PresentationRequest,
        publisher: MutableSharedFlow<PaywallState>?,
    ) = superwall.checkDebuggerPresentation(request, publisher)

    override suspend fun evaluateRules(request: PresentationRequest): Result<RuleEvaluationOutcome> = superwall.evaluateRules(request)

    override suspend fun confirmHoldoutAssignment(
        request: PresentationRequest,
        rulesOutcome: RuleEvaluationOutcome,
    ) = superwall.confirmHoldoutAssignment(request, rulesOutcome)

    override suspend fun getPaywallView(
        request: PresentationRequest,
        rulesOutcome: RuleEvaluationOutcome,
        debugInfo: Map<String, Any>,
        publisher: MutableSharedFlow<PaywallState>?,
    ): Result<com.superwall.sdk.paywall.view.PaywallView> =
        getPaywallView(request, rulesOutcome, debugInfo, publisher, superwall.dependencyContainer)

    override suspend fun getPresenterIfNecessary(
        paywallView: com.superwall.sdk.paywall.view.PaywallView,
        rulesOutcome: RuleEvaluationOutcome,
        request: PresentationRequest,
        publisher: MutableSharedFlow<PaywallState>?,
    ): android.app.Activity? =
        getPresenterIfNecessary(
            paywallView,
            rulesOutcome,
            request,
            publisher,
            attemptTriggerFire = { req, res -> superwall.attemptTriggerFire(req, res) },
            activity = { superwall.dependencyContainer.activityProvider?.getCurrentActivity() },
        )

    override suspend fun confirmPaywallAssignment(
        confirmableAssignment: com.superwall.sdk.models.assignment.ConfirmableAssignment?,
        request: PresentationRequest,
        isDebuggerLaunched: Boolean,
    ) = superwall.confirmPaywallAssignment(confirmableAssignment, request, isDebuggerLaunched)
}
