package com.superwall.sdk.paywall.view

import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.paywall.PaywallPresentationStyle
import com.superwall.sdk.models.triggers.TriggerRuleOccurrence
import com.superwall.sdk.paywall.manager.PaywallCacheLogic
import com.superwall.sdk.paywall.presentation.PaywallCloseReason
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import com.superwall.sdk.paywall.view.survey.SurveyPresentationResult
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.Date

data class PaywallViewState(
    val paywall: Paywall,
    val locale: String,
    val request: PresentationRequest? = null,
    val presentationStyle: PaywallPresentationStyle = paywall.presentation.style,
    val paywallStatePublisher: MutableSharedFlow<PaywallState>? = null,
    val paywallResult: PaywallResult? = null,
    // / Stores the completion block when calling dismiss.
    val dismissCompletionBlock: (() -> Unit)? = null,
    val callbackInvoked: Boolean = false,
    val viewCreatedCompletion: ((Boolean) -> Unit)? = null,
    // / Defines when Browser is presenting in app.
    val isBrowserViewPresented: Boolean = false,
    val interceptTouchEvents: Boolean = false,
    // / Whether the survey was shown, not shown, or in a holdout. Defaults to not shown.
    val surveyPresentationResult: SurveyPresentationResult = SurveyPresentationResult.NOSHOW,
    val loadingState: PaywallLoadingState = PaywallLoadingState.Unknown,
    // / Defines whether the view is being presented or not.
    val isPresented: Boolean = false,
    val presentationWillPrepare: Boolean = true,
    val presentationDidFinishPrepare: Boolean = false,
    // / `true` if there's a survey to complete and the paywall is displayed in a modal style.
    val didDisableSwipeForSurvey: Boolean = false,
    // / If the user match a rule with an occurrence, this needs to be saved on paywall presentation.
    val unsavedOccurrence: TriggerRuleOccurrence? = null,
    val useMultipleUrls: Boolean = false,
    val crashRetries: Int = 0,
) {
    val info: PaywallInfo
        get() = paywall.getInfo(request?.presentationInfo?.eventData)

    internal val cacheKey: String = PaywallCacheLogic.key(paywall.identifier, locale)

    override fun toString(): String =
        """PaywallViewState(
            |  paywallId: ${paywall.identifier}
            |  presentationStyle: $presentationStyle
            |  loadingState: $loadingState
            |  isPresented: $isPresented
            |  presentationWillPrepare: $presentationWillPrepare
            |  presentationDidFinishPrepare: $presentationDidFinishPrepare
            |  callbackInvoked: $callbackInvoked
            |  isBrowserViewPresented: $isBrowserViewPresented
            |  interceptTouchEvents: $interceptTouchEvents
            |  surveyPresentationResult: $surveyPresentationResult
            |  paywallResult: $paywallResult
            |  hasRequest: ${request != null}
            |  hasStatePublisher: ${paywallStatePublisher != null}
            |  hasUnsavedOccurrence: ${unsavedOccurrence != null}
            |  hasViewCreatedCompletion: ${viewCreatedCompletion != null}
            |  hasDismissCompletionBlock: ${dismissCompletionBlock != null}
            |  useMultipleUrls: $useMultipleUrls
            |)
        """.trimMargin()

    sealed class Updates(
        val transform: (PaywallViewState) -> PaywallViewState,
    ) {
        class MergePaywall(
            val from: Paywall,
        ) : Updates({ state ->
                val base = state.paywall
                val merged =
                    base.copy(
                        _productItemsV3 = from._productItemsV3,
                        productVariables = from.productVariables,
                        swProductVariablesTemplate = from.swProductVariablesTemplate,
                        isFreeTrialAvailable = from.isFreeTrialAvailable,
                        productsLoadingInfo = from.productsLoadingInfo,
                        presentationSourceType = from.presentationSourceType,
                        experiment = from.experiment,
                    )
                // Update productItems via setter to also refresh related fields.
                merged.productItems = from.productItems
                state.copy(paywall = merged)
            })

        class SetRequest(
            val req: PresentationRequest,
            val publisher: MutableSharedFlow<PaywallState>?,
            val occurrence: TriggerRuleOccurrence?,
        ) : Updates({ state ->
                state.copy(
                    request = req,
                    paywallStatePublisher = publisher,
                    unsavedOccurrence = occurrence,
                )
            })

        class SetPresentationConfig(
            val styleOverride: PaywallPresentationStyle?,
            val completion: ((Boolean) -> Unit)?,
        ) : Updates({ state ->
                state.copy(
                    presentationStyle =
                        if (styleOverride != null && styleOverride !is PaywallPresentationStyle.None) {
                            styleOverride
                        } else {
                            state.paywall.presentation.style
                        },
                    viewCreatedCompletion = completion,
                )
            })

        object PresentationWillBegin : Updates({ state ->
            state.copy(
                paywall = state.paywall.copy(closeReason = PaywallCloseReason.None),
                callbackInvoked = false,
            )
        })

        object ShimmerStarted : Updates({ state ->
            state.copy(
                paywall =
                    state.paywall.copy(
                        shimmerLoadingInfo =
                            state.paywall.shimmerLoadingInfo.copy(startAt = Date()),
                    ),
                presentationWillPrepare = false,
            )
        })

        object ResetPresentationPreparations : Updates({ state ->
            state.copy(
                presentationWillPrepare = false,
                presentationDidFinishPrepare = true,
            )
        })

        class InitiateDismiss(
            val result: PaywallResult,
            val closeReason: PaywallCloseReason,
            val completion: (() -> Unit)?,
        ) : Updates({ state ->
                state.copy(
                    paywall = state.paywall.copy(closeReason = closeReason),
                    dismissCompletionBlock = completion,
                    paywallResult = result,
                )
            })

        object CallbackInvoked : Updates({ state ->
            state.copy(callbackInvoked = true)
        })

        class UpdateSurveyState(
            val result: SurveyPresentationResult,
        ) : Updates({ state ->
                state.copy(surveyPresentationResult = result)
            })

        object CleanupAfterDestroy : Updates({ state ->
            state.dismissCompletionBlock?.invoke()
            state.copy(
                paywallResult = null,
                isPresented = false,
                dismissCompletionBlock = null,
            )
        })

        object ClearViewCreatedCompletion : Updates({ state ->
            state.copy(viewCreatedCompletion = null)
        })

        object ClearUnsavedOccurrence : Updates({ state ->
            state.copy(unsavedOccurrence = null)
        })

        object SetPresentedAndFinished : Updates({ state ->
            state.copy(
                isPresented = true,
                presentationDidFinishPrepare = true,
            )
        })

        object ShimmerEnded : Updates({ state ->
            state.copy(
                paywall =
                    state.paywall.copy(
                        shimmerLoadingInfo =
                            state.paywall.shimmerLoadingInfo.copy(endAt = Date()),
                    ),
            )
        })

        object WebLoadingStarted : Updates({ state ->
            val current = state.paywall.webviewLoadingInfo
            val updated = if (current.startAt == null) current.copy(startAt = Date()) else current
            state.copy(paywall = state.paywall.copy(webviewLoadingInfo = updated))
        })

        object WebLoadingFailed : Updates({ state ->
            val current = state.paywall.webviewLoadingInfo
            val updated = if (current.failAt == null) current.copy(failAt = Date()) else current
            state.copy(paywall = state.paywall.copy(webviewLoadingInfo = updated))
        })

        class WebLoadingEnded(
            val endAt: Date,
        ) : Updates({ state ->
                state.copy(
                    paywall =
                        state.paywall.copy(
                            webviewLoadingInfo =
                                state.paywall.webviewLoadingInfo.copy(endAt = endAt),
                        ),
                )
            })

        class SetPaywallJsVersion(
            val version: String?,
        ) : Updates({ state ->
                state.copy(paywall = state.paywall.copy(paywalljsVersion = version))
            })

        class SetInterceptTouchEvents(
            val intercept: Boolean,
        ) : Updates({ state ->
                state.copy(interceptTouchEvents = intercept)
            })

        class SetBrowserPresented(
            val presented: Boolean,
        ) : Updates({ state ->
                state.copy(isBrowserViewPresented = presented)
            })

        class SetLoadingState(
            val newState: PaywallLoadingState,
        ) : Updates({ state ->
                state.copy(loadingState = newState)
            })

        object ClearStatePublisher : Updates({
            it.copy(paywallStatePublisher = null)
        })

        object CrashRetry : Updates({
            it.copy(crashRetries = it.crashRetries + 1)
        })

        object ResetCrashRetry : Updates({
            it.copy(crashRetries = 0)
        })

        /**
         * Hides or displays the paywall spinner.
         *
         * @param isHidden A Boolean indicating whether to show or hide the spinner.
         */

        data class ToggleSpinner(
            val hidden: Boolean,
        ) : Updates({ state ->
                when {
                    hidden -> {
                        if (state.loadingState is PaywallLoadingState.ManualLoading ||
                            state.loadingState is PaywallLoadingState.LoadingPurchase
                        ) {
                            state.copy(loadingState = PaywallLoadingState.Ready)
                        } else {
                            state
                        }
                    }

                    else -> {
                        if (state.loadingState is PaywallLoadingState.Ready) {
                            state.copy(
                                loadingState =
                                    PaywallLoadingState.ManualLoading,
                            )
                        } else {
                            state
                        }
                    }
                }
            })
    }
}
