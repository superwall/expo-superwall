package com.superwall.sdk.analytics.internal

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.analytics.internal.trackable.TrackableSuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEvents
import com.superwall.sdk.config.models.Survey
import com.superwall.sdk.config.models.SurveyOption
import com.superwall.sdk.config.models.SurveyShowCondition
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.paywall.presentation.PaywallInfo.Companion.empty
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.webview.WebviewError
import com.superwall.sdk.store.transactions.TransactionError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Month
import java.net.URI

class TrackingLogicTest {
    @Test
    fun processParameters_filtersUnsupportedValuesAndPrefixesKeys() =
        runBlocking {
            val now = LocalDateTime.of(2024, Month.JANUARY, 1, 10, 0)
            val paywallUrl = PaywallInfo.empty().url
            val trackable =
                TrackableTestUtils.fakeTrackable(
                    rawName = "event_a",
                    audienceParams =
                        mapOf(
                            "custom_int" to 3,
                            "custom_list" to listOf("ignore"),
                            "custom_json" to JsonPrimitive("json"),
                        ),
                    superwallParams =
                        mapOf(
                            "string_value" to "value",
                            "list_value" to listOf("skip"),
                            "map_value" to mapOf("nested" to "ok"),
                            "json_value" to Json.parseToJsonElement("{\"foo\":1}"),
                            "date_value" to now,
                            "uri_value" to URI("https://example.com"),
                            "paywall_url" to paywallUrl,
                        ),
                    canImplicitlyTrigger = false,
                )

            val parameters = TrackingLogic.processParameters(trackable, appSessionId = "session-123")

            assertEquals("value", parameters.delegateParams["string_value"])
            assertFalse(parameters.delegateParams.containsKey("list_value"))
            assertEquals(mapOf("nested" to "ok"), parameters.delegateParams["map_value"])
            assertEquals("json", parameters.delegateParams["custom_json"])
            assertTrue(parameters.audienceFilterParams.containsKey("\$string_value"))
            assertEquals("event_a", parameters.audienceFilterParams["event_name"])
            assertEquals("session-123", parameters.delegateParams["app_session_id"])
            assertEquals(false, parameters.audienceFilterParams["\$is_standard_event"])
            assertEquals(
                now.atZone(org.threeten.bp.ZoneOffset.UTC).toInstant().toEpochMilli(),
                parameters.delegateParams["date_value"],
            )
            assertEquals("https://example.com", parameters.delegateParams["uri_value"])
        }

    @Test
    fun isNotDisabledVerboseEvent_handlesVariousEventTypes() {
        val paywallInfo = PaywallInfo.empty()
        val presentationRequest =
            InternalSuperwallEvent.PresentationRequest(
                eventData = EventData.stub(),
                type = com.superwall.sdk.paywall.presentation.internal.PresentationRequestType.Presentation,
                status = com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatus.Presentation,
                statusReason = null,
                factory = TrackingLogicTestFactory,
            )

        assertFalse(
            TrackingLogic.isNotDisabledVerboseEvent(
                presentationRequest,
                disableVerboseEvents = true,
                isSandbox = false,
            ),
        )

        assertTrue(
            TrackingLogic.isNotDisabledVerboseEvent(
                presentationRequest,
                disableVerboseEvents = true,
                isSandbox = true,
            ),
        )

        val startEvent =
            InternalSuperwallEvent.PaywallLoad(
                state = InternalSuperwallEvent.PaywallLoad.State.Start(),
                eventData = EventData.stub(),
            )

        assertFalse(
            TrackingLogic.isNotDisabledVerboseEvent(
                startEvent,
                disableVerboseEvents = true,
                isSandbox = false,
            ),
        )

        val failEvent =
            InternalSuperwallEvent.PaywallLoad(
                state = InternalSuperwallEvent.PaywallLoad.State.Fail(),
                eventData = EventData.stub(),
            )
        assertTrue(
            TrackingLogic.isNotDisabledVerboseEvent(
                failEvent,
                disableVerboseEvents = true,
                isSandbox = false,
            ),
        )

        val shimmer =
            InternalSuperwallEvent.ShimmerLoad(
                state = InternalSuperwallEvent.ShimmerLoad.State.Started,
                paywallId = "pw",
                visibleDuration = null,
                delay = 0.0,
                preloadingEnabled = false,
            )
        assertFalse(
            TrackingLogic.isNotDisabledVerboseEvent(
                shimmer,
                disableVerboseEvents = true,
                isSandbox = false,
            ),
        )
    }

    @Test
    fun checkNotSuperwallEvent_throwsForReservedNames() {
        assertThrows(Exception::class.java) {
            TrackingLogic.checkNotSuperwallEvent(SuperwallEvent.AppOpen().rawName)
        }

        TrackingLogic.checkNotSuperwallEvent("my_custom_event")
    }

    @Test
    fun canTriggerPaywall_handlesDifferentOutcomes() {
        val triggers = setOf("event_a")

        val deepLinkEvent = InternalSuperwallEvent.DeepLink(uri = URI("https://example.com"))
        assertEquals(
            TrackingLogic.ImplicitTriggerOutcome.DeepLinkTrigger,
            TrackingLogic.canTriggerPaywall(deepLinkEvent, triggers, null),
        )

        val nonTriggerEvent =
            TrackableTestUtils.fakeTrackable(rawName = "other", superwallParams = emptyMap())
        assertEquals(
            TrackingLogic.ImplicitTriggerOutcome.DontTriggerPaywall,
            TrackingLogic.canTriggerPaywall(nonTriggerEvent, triggers, null),
        )

        val paywallInfo =
            PaywallInfo.empty().copy(
                presentedByEventWithName =
                    SuperwallEvent
                        .TransactionFail(
                            TransactionError.Pending(""),
                            PaywallInfo.empty(),
                        ).rawName,
            )
        val paywallView =
            mockk<PaywallView> {
                every { info } returns paywallInfo
            }
        val triggerEvent =
            TrackableTestUtils.fakeTrackable(
                rawName = "event_a",
                superwallParams = emptyMap(),
                canImplicitlyTrigger = false,
            )
        assertEquals(
            TrackingLogic.ImplicitTriggerOutcome.DontTriggerPaywall,
            TrackingLogic.canTriggerPaywall(triggerEvent, triggers, paywallView),
        )

        val trackableSuperwallEvent =
            object : TrackableSuperwallEvent {
                override val superwallPlacement: SuperwallEvent =
                    SuperwallEvent.PaywallDecline(paywallInfo = paywallInfo)
                override val rawName: String = superwallPlacement.rawName
                override val audienceFilterParams: Map<String, Any> = emptyMap()
                override val canImplicitlyTriggerPaywall: Boolean = true

                override suspend fun getSuperwallParameters(): Map<String, Any> = emptyMap()
            }
        assertEquals(
            TrackingLogic.ImplicitTriggerOutcome.DontTriggerPaywall,
            TrackingLogic.canTriggerPaywall(trackableSuperwallEvent, triggers, null),
        )

        val allowedReferrerInfo = paywallInfo.copy(presentedByEventWithName = null)
        val openPaywallView =
            mockk<PaywallView> {
                every { info } returns allowedReferrerInfo
            }
        assertEquals(
            TrackingLogic.ImplicitTriggerOutcome.DontTriggerPaywall,
            TrackingLogic.canTriggerPaywall(triggerEvent, triggers, openPaywallView),
        )

        assertEquals(
            TrackingLogic.ImplicitTriggerOutcome.TriggerPaywall,
            TrackingLogic.canTriggerPaywall(triggerEvent, triggers, null),
        )
    }

    // ========== Given/When/Then Tests ==========

    // ========== isNotDisabledVerboseEvent Tests ==========

    @Test
    fun `isNotDisabledVerboseEvent returns true when disableVerboseEvents is null`() =
        Given("disableVerboseEvents is null") {
            val event =
                InternalSuperwallEvent.PresentationRequest(
                    eventData = EventData.stub(),
                    type = com.superwall.sdk.paywall.presentation.internal.PresentationRequestType.Presentation,
                    status = com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatus.Presentation,
                    statusReason = null,
                    factory = TrackingLogicTestFactory,
                )

            When("checking if event is not disabled verbose event") {
                val result =
                    TrackingLogic.isNotDisabledVerboseEvent(
                        event,
                        disableVerboseEvents = null,
                        isSandbox = false,
                    )

                Then("it should return true") {
                    assertTrue(result)
                }
            }
        }

    @Test
    fun `isNotDisabledVerboseEvent returns true in sandbox mode even when verbose events disabled`() =
        Given("sandbox mode is enabled and verbose events are disabled") {
            val event =
                InternalSuperwallEvent.ShimmerLoad(
                    state = InternalSuperwallEvent.ShimmerLoad.State.Started,
                    paywallId = "pw_123",
                    visibleDuration = null,
                    delay = 0.0,
                    preloadingEnabled = false,
                )

            When("checking if event is not disabled verbose event") {
                val result =
                    TrackingLogic.isNotDisabledVerboseEvent(
                        event,
                        disableVerboseEvents = true,
                        isSandbox = true,
                    )

                Then("it should return true because we're in sandbox mode") {
                    assertTrue(result)
                }
            }
        }

    @Test
    fun `isNotDisabledVerboseEvent filters PaywallProductsLoad Start when verbose events disabled`() =
        Given("PaywallProductsLoad with Start state and verbose events disabled") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.PaywallProductsLoad(
                    state = InternalSuperwallEvent.PaywallProductsLoad.State.Start(),
                    paywallInfo = paywallInfo,
                    eventData = null,
                )

            When("checking if event is not disabled verbose event") {
                val result =
                    TrackingLogic.isNotDisabledVerboseEvent(
                        event,
                        disableVerboseEvents = true,
                        isSandbox = false,
                    )

                Then("it should return false because Start is a verbose event") {
                    assertFalse(result)
                }
            }
        }

    @Test
    fun `isNotDisabledVerboseEvent filters PaywallProductsLoad Complete when verbose events disabled`() =
        Given("PaywallProductsLoad with Complete state and verbose events disabled") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.PaywallProductsLoad(
                    state = InternalSuperwallEvent.PaywallProductsLoad.State.Complete(),
                    paywallInfo = paywallInfo,
                    eventData = null,
                )

            When("checking if event is not disabled verbose event") {
                val result =
                    TrackingLogic.isNotDisabledVerboseEvent(
                        event,
                        disableVerboseEvents = true,
                        isSandbox = false,
                    )

                Then("it should return false because Complete is a verbose event") {
                    assertFalse(result)
                }
            }
        }

    @Test
    fun `isNotDisabledVerboseEvent allows PaywallProductsLoad Fail even when verbose events disabled`() =
        Given("PaywallProductsLoad with Fail state and verbose events disabled") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.PaywallProductsLoad(
                    state = InternalSuperwallEvent.PaywallProductsLoad.State.Fail("error"),
                    paywallInfo = paywallInfo,
                    eventData = null,
                )

            When("checking if event is not disabled verbose event") {
                val result =
                    TrackingLogic.isNotDisabledVerboseEvent(
                        event,
                        disableVerboseEvents = true,
                        isSandbox = false,
                    )

                Then("it should return true because Fail events are always allowed") {
                    assertTrue(result)
                }
            }
        }

    @Test
    fun `isNotDisabledVerboseEvent filters PaywallWebviewLoad Start when verbose events disabled`() =
        Given("PaywallWebviewLoad with Start state and verbose events disabled") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.PaywallWebviewLoad(
                    state = InternalSuperwallEvent.PaywallWebviewLoad.State.Start(),
                    paywallInfo = paywallInfo,
                )

            When("checking if event is not disabled verbose event") {
                val result =
                    TrackingLogic.isNotDisabledVerboseEvent(
                        event,
                        disableVerboseEvents = true,
                        isSandbox = false,
                    )

                Then("it should return false because Start is a verbose event") {
                    assertFalse(result)
                }
            }
        }

    @Test
    fun `isNotDisabledVerboseEvent filters PaywallWebviewLoad Complete when verbose events disabled`() =
        Given("PaywallWebviewLoad with Complete state and verbose events disabled") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.PaywallWebviewLoad(
                    state = InternalSuperwallEvent.PaywallWebviewLoad.State.Complete(),
                    paywallInfo = paywallInfo,
                )

            When("checking if event is not disabled verbose event") {
                val result =
                    TrackingLogic.isNotDisabledVerboseEvent(
                        event,
                        disableVerboseEvents = true,
                        isSandbox = false,
                    )

                Then("it should return false because Complete is a verbose event") {
                    assertFalse(result)
                }
            }
        }

    @Test
    fun `isNotDisabledVerboseEvent allows PaywallWebviewLoad Fail even when verbose events disabled`() =
        Given("PaywallWebviewLoad with Fail state and verbose events disabled") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.PaywallWebviewLoad(
                    state =
                        InternalSuperwallEvent.PaywallWebviewLoad.State.Fail(
                            error = WebviewError.NoUrls,
                            urls = listOf("https://example.com"),
                        ),
                    paywallInfo = paywallInfo,
                )

            When("checking if event is not disabled verbose event") {
                val result =
                    TrackingLogic.isNotDisabledVerboseEvent(
                        event,
                        disableVerboseEvents = true,
                        isSandbox = false,
                    )

                Then("it should return true because Fail events are always allowed") {
                    assertTrue(result)
                }
            }
        }

    @Test
    fun `isNotDisabledVerboseEvent allows PaywallWebviewLoad Timeout even when verbose events disabled`() =
        Given("PaywallWebviewLoad with Timeout state and verbose events disabled") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.PaywallWebviewLoad(
                    state = InternalSuperwallEvent.PaywallWebviewLoad.State.Timeout("timeout"),
                    paywallInfo = paywallInfo,
                )

            When("checking if event is not disabled verbose event") {
                val result =
                    TrackingLogic.isNotDisabledVerboseEvent(
                        event,
                        disableVerboseEvents = true,
                        isSandbox = false,
                    )

                Then("it should return true because Timeout events are always allowed") {
                    assertTrue(result)
                }
            }
        }

    // ========== canTriggerPaywall Tests ==========

    @Test
    fun `canTriggerPaywall returns DeepLinkTrigger for DeepLink events`() =
        Given("a DeepLink event") {
            val event = InternalSuperwallEvent.DeepLink(uri = URI("https://example.com/deeplink"))
            val triggers = setOf("some_trigger")

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, null)

                Then("it should return DeepLinkTrigger") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.DeepLinkTrigger, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall returns DontTriggerPaywall when event not in triggers`() =
        Given("an event that is not in the triggers set") {
            val event =
                TrackableTestUtils.fakeTrackable(
                    rawName = "unregistered_event",
                    superwallParams = emptyMap(),
                )
            val triggers = setOf("registered_event")

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, null)

                Then("it should return DontTriggerPaywall") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.DontTriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall returns TriggerPaywall for regular event in triggers with no open paywall`() =
        Given("an event in triggers with no paywall currently open") {
            val event =
                TrackableTestUtils.fakeTrackable(
                    rawName = "my_event",
                    superwallParams = emptyMap(),
                    canImplicitlyTrigger = true,
                )
            val triggers = setOf("my_event")

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, null)

                Then("it should return TriggerPaywall") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.TriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall returns DontTriggerPaywall when paywall is already showing for non-superwall event`() =
        Given("a regular event while a paywall is already open") {
            val event =
                TrackableTestUtils.fakeTrackable(
                    rawName = "my_event",
                    superwallParams = emptyMap(),
                    canImplicitlyTrigger = true,
                )
            val triggers = setOf("my_event")
            val paywallInfo = PaywallInfo.empty()
            val paywallView =
                mockk<PaywallView> {
                    every { info } returns paywallInfo
                }

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, paywallView)

                Then("it should return DontTriggerPaywall") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.DontTriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall returns ClosePaywallThenTriggerPaywall for TransactionAbandon event`() =
        Given("a TransactionAbandon event that is in triggers") {
            val paywallInfo = PaywallInfo.empty()
            val mockProduct =
                mockk<com.superwall.sdk.store.abstractions.product.StoreProductType> {
                    every { fullIdentifier } returns "product_id"
                    every { productIdentifier } returns "product_id"
                }
            val event =
                InternalSuperwallEvent.Transaction(
                    state = InternalSuperwallEvent.Transaction.State.Abandon(product = mockProduct),
                    paywallInfo = paywallInfo,
                    product = null,
                    model = null,
                    source = InternalSuperwallEvent.Transaction.TransactionSource.INTERNAL,
                    isObserved = false,
                    demandScore = null,
                    demandTier = null,
                )
            val triggers = setOf(SuperwallEvents.TransactionAbandon.rawName)

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, null)

                Then("it should return ClosePaywallThenTriggerPaywall") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.ClosePaywallThenTriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall returns ClosePaywallThenTriggerPaywall for PaywallDecline event`() =
        Given("a PaywallDecline event that is in triggers") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.PaywallDecline(
                    paywallInfo = paywallInfo,
                )
            val triggers = setOf(SuperwallEvents.PaywallDecline.rawName)

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, null)

                Then("it should return ClosePaywallThenTriggerPaywall") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.ClosePaywallThenTriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall returns ClosePaywallThenTriggerPaywall for SurveyResponse event`() =
        Given("a SurveyResponse event that is in triggers") {
            val paywallInfo = PaywallInfo.empty()
            val survey =
                Survey(
                    id = "survey_1",
                    assignmentKey = "key",
                    title = "Survey",
                    message = "message",
                    options = listOf(SurveyOption("opt_1", "Option 1")),
                    includeOtherOption = false,
                    includeCloseOption = false,
                    presentationCondition = SurveyShowCondition.ON_MANUAL_CLOSE,
                    presentationProbability = 1.0,
                )
            val event =
                InternalSuperwallEvent.SurveyResponse(
                    survey = survey,
                    selectedOption = SurveyOption("opt_1", "Option 1"),
                    customResponse = null,
                    paywallInfo = paywallInfo,
                )
            val triggers = setOf(SuperwallEvents.SurveyResponse.rawName)

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, null)

                Then("it should return ClosePaywallThenTriggerPaywall") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.ClosePaywallThenTriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall blocks duplicate exit offer event from same type`() =
        Given("a PaywallDecline event while paywall was already presented by PaywallDecline") {
            val paywallInfo =
                PaywallInfo.empty().copy(
                    presentedByEventWithName = SuperwallEvents.PaywallDecline.rawName,
                )
            val paywallView =
                mockk<PaywallView> {
                    every { info } returns paywallInfo
                }
            val event = InternalSuperwallEvent.PaywallDecline(paywallInfo = paywallInfo)
            val triggers = setOf(SuperwallEvents.PaywallDecline.rawName)

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, paywallView)

                Then("it should return DontTriggerPaywall to prevent infinite loop") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.DontTriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall allows chaining different exit offer events`() =
        Given("a PaywallDecline event while paywall was presented by TransactionAbandon") {
            val paywallInfo =
                PaywallInfo.empty().copy(
                    presentedByEventWithName = SuperwallEvents.TransactionAbandon.rawName,
                )
            val paywallView =
                mockk<PaywallView> {
                    every { info } returns paywallInfo
                }
            val declinePaywallInfo = PaywallInfo.empty()
            val event = InternalSuperwallEvent.PaywallDecline(paywallInfo = declinePaywallInfo)
            val triggers = setOf(SuperwallEvents.PaywallDecline.rawName)

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, paywallView)

                Then("it should return ClosePaywallThenTriggerPaywall allowing event chaining") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.ClosePaywallThenTriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall blocks duplicate TransactionFail exit offer`() =
        Given("a TransactionFail event while paywall was already presented by TransactionFail") {
            val paywallInfo =
                PaywallInfo.empty().copy(
                    presentedByEventWithName = SuperwallEvents.TransactionFail.rawName,
                )
            val paywallView =
                mockk<PaywallView> {
                    every { info } returns paywallInfo
                }
            val event =
                InternalSuperwallEvent.Transaction(
                    state =
                        InternalSuperwallEvent.Transaction.State.Fail(
                            error = TransactionError.Pending("pending"),
                        ),
                    paywallInfo = paywallInfo,
                    product = null,
                    model = null,
                    source = InternalSuperwallEvent.Transaction.TransactionSource.INTERNAL,
                    isObserved = false,
                    demandScore = null,
                    demandTier = null,
                )
            val triggers = setOf(SuperwallEvents.TransactionFail.rawName)

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, paywallView)

                Then("it should return DontTriggerPaywall to prevent infinite loop") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.DontTriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall returns ClosePaywallThenTriggerPaywall for CustomPlacement event`() =
        Given("a CustomPlacement event that is in triggers") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.CustomPlacement(
                    placementName = "custom_placement",
                    paywallInfo = paywallInfo,
                    params = mapOf("key" to "value"),
                )
            val triggers = setOf(SuperwallEvents.CustomPlacement.rawName)

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, null)

                Then("it should return ClosePaywallThenTriggerPaywall") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.ClosePaywallThenTriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall returns TriggerPaywall for standard Superwall events that are not exit offers`() =
        Given("a PaywallOpen event that is in triggers") {
            val paywallInfo = PaywallInfo.empty()
            val event =
                InternalSuperwallEvent.PaywallOpen(
                    paywallInfo = paywallInfo,
                    userAttributes = emptyMap(),
                    demandTier = null,
                    demandScore = null,
                )
            val triggers = setOf(SuperwallEvents.PaywallOpen.rawName)

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, null)

                Then("it should return TriggerPaywall (not ClosePaywallThenTriggerPaywall)") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.TriggerPaywall, result)
                }
            }
        }

    @Test
    fun `canTriggerPaywall blocks duplicate CustomPlacement exit offer`() =
        Given("a CustomPlacement event while paywall was already presented by CustomPlacement") {
            val paywallInfo =
                PaywallInfo.empty().copy(
                    presentedByEventWithName = SuperwallEvents.CustomPlacement.rawName,
                )
            val paywallView =
                mockk<PaywallView> {
                    every { info } returns paywallInfo
                }
            val event =
                InternalSuperwallEvent.CustomPlacement(
                    placementName = "custom_placement",
                    paywallInfo = paywallInfo,
                    params = emptyMap(),
                )
            val triggers = setOf(SuperwallEvents.CustomPlacement.rawName)

            When("checking if can trigger paywall") {
                val result = TrackingLogic.canTriggerPaywall(event, triggers, paywallView)

                Then("it should return DontTriggerPaywall to prevent infinite loop") {
                    assertEquals(TrackingLogic.ImplicitTriggerOutcome.DontTriggerPaywall, result)
                }
            }
        }

    // ========== processParameters Tests ==========

    @Test
    fun `processParameters drops custom parameters starting with dollar sign`() =
        runBlocking {
            Given("a trackable event with custom params that start with dollar sign") {
                val trackable =
                    TrackableTestUtils.fakeTrackable(
                        rawName = "test_event",
                        audienceParams =
                            mapOf(
                                "\$invalid_param" to "should_be_dropped",
                                "valid_param" to "should_be_kept",
                            ),
                        superwallParams = emptyMap(),
                    )

                When("processing the parameters") {
                    val result = TrackingLogic.processParameters(trackable, "session-123")

                    Then("dollar sign prefixed custom params should be dropped") {
                        assertFalse(result.audienceFilterParams.containsKey("\$invalid_param"))
                        assertFalse(result.delegateParams.containsKey("\$invalid_param"))
                    }

                    Then("valid params should be kept") {
                        assertEquals("should_be_kept", result.audienceFilterParams["valid_param"])
                        assertEquals("should_be_kept", result.delegateParams["valid_param"])
                    }
                }
            }
        }

    @Test
    fun `processParameters adds app_session_id to superwall params`() =
        runBlocking {
            Given("a trackable event") {
                val trackable =
                    TrackableTestUtils.fakeTrackable(
                        rawName = "test_event",
                        superwallParams = emptyMap(),
                    )
                val appSessionId = "unique-session-id"

                When("processing the parameters") {
                    val result = TrackingLogic.processParameters(trackable, appSessionId)

                    Then("app_session_id should be added to delegate params") {
                        assertEquals(appSessionId, result.delegateParams["app_session_id"])
                    }

                    Then("app_session_id should be added to audience filter params with $ prefix") {
                        assertEquals(appSessionId, result.audienceFilterParams["\$app_session_id"])
                    }
                }
            }
        }

    @Test
    fun `processParameters marks standard events correctly`() =
        runBlocking {
            Given("a TrackableSuperwallEvent") {
                val paywallInfo = PaywallInfo.empty()
                val trackable = InternalSuperwallEvent.PaywallDecline(paywallInfo = paywallInfo)

                When("processing the parameters") {
                    val result = TrackingLogic.processParameters(trackable, "session-123")

                    Then("is_standard_event should be true") {
                        assertEquals(true, result.audienceFilterParams["\$is_standard_event"])
                    }
                }
            }

            Given("a non-TrackableSuperwallEvent") {
                val trackable =
                    TrackableTestUtils.fakeTrackable(
                        rawName = "custom_event",
                        superwallParams = emptyMap(),
                    )

                When("processing the parameters") {
                    val result = TrackingLogic.processParameters(trackable, "session-123")

                    Then("is_standard_event should be false") {
                        assertEquals(false, result.audienceFilterParams["\$is_standard_event"])
                    }
                }
            }
        }

    @Test
    fun `processParameters includes event_name in both param maps`() =
        runBlocking {
            Given("a trackable event with specific name") {
                val eventName = "my_custom_event"
                val trackable =
                    TrackableTestUtils.fakeTrackable(
                        rawName = eventName,
                        superwallParams = emptyMap(),
                    )

                When("processing the parameters") {
                    val result = TrackingLogic.processParameters(trackable, "session-123")

                    Then("event_name should be in audience filter params") {
                        assertEquals(eventName, result.audienceFilterParams["event_name"])
                        assertEquals(eventName, result.audienceFilterParams["\$event_name"])
                    }
                }
            }
        }

    // ========== checkNotSuperwallEvent Tests ==========

    @Test
    fun `checkNotSuperwallEvent throws for all reserved Superwall event names`() =
        Given("reserved Superwall event names") {
            val reservedNames =
                listOf(
                    SuperwallEvents.AppOpen.rawName,
                    SuperwallEvents.PaywallOpen.rawName,
                    SuperwallEvents.PaywallClose.rawName,
                    SuperwallEvents.TransactionStart.rawName,
                    SuperwallEvents.TransactionComplete.rawName,
                    SuperwallEvents.SubscriptionStart.rawName,
                )

            reservedNames.forEach { name ->
                When("checking $name") {
                    var exceptionThrown = false
                    try {
                        TrackingLogic.checkNotSuperwallEvent(name)
                    } catch (e: Exception) {
                        exceptionThrown = true
                    }

                    Then("it should throw an exception") {
                        assertTrue("Expected exception for $name", exceptionThrown)
                    }
                }
            }
        }

    @Test
    fun `checkNotSuperwallEvent allows custom event names`() =
        Given("custom event names") {
            val customNames =
                listOf(
                    "my_custom_event",
                    "purchase_button_tapped",
                    "onboarding_complete",
                    "level_completed",
                )

            customNames.forEach { name ->
                When("checking $name") {
                    var exceptionThrown = false
                    try {
                        TrackingLogic.checkNotSuperwallEvent(name)
                    } catch (e: Exception) {
                        exceptionThrown = true
                    }

                    Then("it should not throw an exception") {
                        assertFalse("Unexpected exception for $name", exceptionThrown)
                    }
                }
            }
        }
}

private object TrackingLogicTestFactory :
    InternalSuperwallEvent.PresentationRequest.Factory,
    com.superwall.sdk.dependencies.RuleAttributesFactory,
    com.superwall.sdk.dependencies.FeatureFlagsFactory,
    com.superwall.sdk.dependencies.ComputedPropertyRequestsFactory {
    override suspend fun makeRuleAttributes(
        event: EventData?,
        computedPropertyRequests: List<com.superwall.sdk.models.config.ComputedPropertyRequest>,
    ): Map<String, Any> = emptyMap()

    override fun makeFeatureFlags(): com.superwall.sdk.models.config.FeatureFlags? = null

    override fun makeComputedPropertyRequests(): List<com.superwall.sdk.models.config.ComputedPropertyRequest> = emptyList()
}

private object TrackableTestUtils {
    fun fakeTrackable(
        rawName: String,
        audienceParams: Map<String, Any> = emptyMap(),
        superwallParams: Map<String, Any>,
        canImplicitlyTrigger: Boolean = true,
    ): Trackable =
        object : Trackable {
            override val rawName: String = rawName
            override val audienceFilterParams: Map<String, Any> = audienceParams
            override val canImplicitlyTriggerPaywall: Boolean = canImplicitlyTrigger

            override suspend fun getSuperwallParameters(): Map<String, Any> = superwallParams
        }
}
