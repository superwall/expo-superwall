package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.assertTrue
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

class PresentationGuardsTest {
    private fun request(
        isPresented: Boolean,
        isDebuggerLaunched: Boolean,
    ) = PresentationRequest(
        presentationInfo = PresentationInfo.ExplicitTrigger(EventData.stub()),
        flags =
            PresentationRequest.Flags(
                isDebuggerLaunched = isDebuggerLaunched,
                entitlements = MutableStateFlow<SubscriptionStatus?>(SubscriptionStatus.Inactive),
                isPaywallPresented = isPresented,
                type = PresentationRequestType.Presentation,
            ),
    )

    @Test
    fun throwsWhenPaywallAlreadyPresented() =
        runTest {
            Given("a request where a paywall is already presented") {
                val publisher = MutableSharedFlow<PaywallState>(replay = 1)

                val thrown =
                    When("the guard is checked") {
                        runCatching {
                            checkNoPaywallAlreadyPresented(request(isPresented = true, isDebuggerLaunched = false), publisher)
                        }.exceptionOrNull()
                    }

                Then("a paywall-already-presented error is thrown and state published") {
                    assertTrue(thrown is PaywallPresentationRequestStatusReason.PaywallAlreadyPresented)
                    assertNull(thrown?.cause)
                    val published = publisher.replayCache.firstOrNull()
                    assertTrue(published is PaywallState.PresentationError)
                }
            }
        }

    @Test
    fun doesNothingWhenPaywallNotPresented() =
        runTest {
            Given("a request without an active paywall") {
                val publisher = MutableSharedFlow<PaywallState>(replay = 1)

                When("the guard is evaluated") {
                    checkNoPaywallAlreadyPresented(request(isPresented = false, isDebuggerLaunched = false), publisher)
                }

                Then("no additional state is published") {
                    assertTrue(publisher.replayCache.isEmpty())
                }
            }
        }

    @Test
    fun throwsWhenDebuggerAlreadyLaunched() =
        runTest {
            Given("a request where debugger is already launched") {
                val publisher = MutableSharedFlow<PaywallState>(replay = 1)

                val thrown =
                    When("the debugger guard is executed") {
                        runCatching {
                            innerCheckDebuggerPresentation(request(isPresented = false, isDebuggerLaunched = true), publisher)
                        }.exceptionOrNull()
                    }

                Then("a debugger-presented error is thrown and state published") {
                    assertTrue(thrown is PaywallPresentationRequestStatusReason.DebuggerPresented)
                    assertNull(thrown?.cause)
                    val published = publisher.replayCache.firstOrNull()
                    assertTrue(published is PaywallState.PresentationError)
                }
            }
        }

    @Test
    fun skipsWhenPresenterIsDebugActivity() =
        runTest {
            Given("a request whose presenter is the debug activity") {
                val publisher = MutableSharedFlow<PaywallState>(replay = 1)
                val request = request(isPresented = false, isDebuggerLaunched = true)
                request.presenter = java.lang.ref.WeakReference(mockk<com.superwall.sdk.debug.DebugViewActivity>(relaxed = true))

                When("the debugger guard executes") {
                    innerCheckDebuggerPresentation(request, publisher)
                }

                Then("no error or state is emitted") {
                    assertTrue(publisher.replayCache.isEmpty())
                }
            }
        }
}
