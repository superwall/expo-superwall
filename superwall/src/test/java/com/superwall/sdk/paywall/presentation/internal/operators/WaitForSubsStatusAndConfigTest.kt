package com.superwall.sdk.paywall.presentation.internal.operators

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.dependencies.DependencyContainer
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.state.PaywallState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class WaitForSubsStatusAndConfigTest {
    @After
    fun tearDown() {
        runCatching { unmockkStatic("com.superwall.sdk.analytics.internal.TrackingKt") }
    }

    @Test
    fun `waitForEntitlementsAndConfig returns when entitlements are set and config ready`() =
        runTest {
            Given("ready entitlements and retrieved config") {
                val superwall = mockk<com.superwall.sdk.Superwall>(relaxed = true)
                val dependencyContainer = mockk<DependencyContainer>(relaxed = true)
                every { superwall.dependencyContainer } returns dependencyContainer

                val configState = MutableStateFlow<ConfigState>(ConfigState.Retrieved(Config.stub()))
                every { dependencyContainer.configManager.configState } returns configState

                val identityManager = mockk<com.superwall.sdk.identity.IdentityManager>(relaxed = true)
                every { identityManager.hasIdentity } returns
                    MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1).also { it.tryEmit(true) }
                every { dependencyContainer.identityManager } returns identityManager

                val request =
                    PresentationRequest(
                        presentationInfo = PresentationInfo.ExplicitTrigger(EventData.stub()),
                        flags =
                            PresentationRequest.Flags(
                                isDebuggerLaunched = false,
                                entitlements = MutableStateFlow<SubscriptionStatus?>(SubscriptionStatus.Inactive),
                                isPaywallPresented = false,
                                type = PresentationRequestType.Presentation,
                            ),
                    )

                When("waitForEntitlementsAndConfig executes") {
                    waitForEntitlementsAndConfig(request, paywallStatePublisher = null, dependencyContainer = dependencyContainer)

                    Then("it completes without emitting errors") { }
                }
            }
        }

    @Test
    fun `waitForEntitlementsAndConfig emits error and throws when config is missing`() =
        runTest {
            Given("entitlements ready but config failed") {
                val superwall = mockk<com.superwall.sdk.Superwall>(relaxed = true)
                val dependencyContainer = mockk<DependencyContainer>(relaxed = true)
                every { superwall.dependencyContainer } returns dependencyContainer
                every { superwall.ioScope } returns
                    com.superwall.sdk.misc
                        .IOScope(coroutineContext)

                val configState = MutableStateFlow<ConfigState>(ConfigState.Failed(IllegalStateException("no config")))
                every { dependencyContainer.configManager.configState } returns configState

                val identityManager = mockk<com.superwall.sdk.identity.IdentityManager>(relaxed = true)
                every { identityManager.hasIdentity } returns
                    MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1).also { it.tryEmit(true) }
                every { dependencyContainer.identityManager } returns identityManager

                mockkStatic("com.superwall.sdk.analytics.internal.TrackingKt")
                coEvery { superwall.track(any()) } returns Result.success(mockk())

                val publisher = MutableSharedFlow<PaywallState>(replay = 1)
                val request =
                    PresentationRequest(
                        presentationInfo = PresentationInfo.ExplicitTrigger(EventData.stub()),
                        flags =
                            PresentationRequest.Flags(
                                isDebuggerLaunched = false,
                                entitlements = MutableStateFlow<SubscriptionStatus?>(SubscriptionStatus.Inactive),
                                isPaywallPresented = false,
                                type = PresentationRequestType.Presentation,
                            ),
                    )

                When("waitForEntitlementsAndConfig executes") {
                    kotlin.test.assertFailsWith<PaywallPresentationRequestStatusReason.NoConfig> {
                        waitForEntitlementsAndConfig(request, paywallStatePublisher = publisher, dependencyContainer = dependencyContainer)
                    }

                    Then("a presentation error is emitted") {
                        val emitted = publisher.replayCache.lastOrNull()
                        assert(emitted is PaywallState.PresentationError)
                    }
                }
            }
        }
}
