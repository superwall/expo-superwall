package com.superwall.sdk.analytics.internal.trackable

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.config.models.Survey
import com.superwall.sdk.config.models.SurveyOption
import com.superwall.sdk.config.models.SurveyShowCondition
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.config.options.toMap
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.config.FeatureFlags
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.PaywallPresentationInfo
import com.superwall.sdk.models.paywall.PaywallPresentationStyle
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.models.triggers.UnmatchedRule
import com.superwall.sdk.paywall.presentation.PaywallInfo
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatus
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.view.survey.SurveyPresentationResult
import com.superwall.sdk.paywall.view.webview.WebviewError
import com.superwall.sdk.store.abstractions.product.StoreProduct
import com.superwall.sdk.store.abstractions.transactions.GoogleBillingPurchaseTransaction
import com.superwall.sdk.store.abstractions.transactions.StorePayment
import com.superwall.sdk.store.abstractions.transactions.StoreTransaction
import com.superwall.sdk.store.abstractions.transactions.StoreTransactionState
import com.superwall.sdk.store.transactions.RestoreType
import com.superwall.sdk.store.transactions.TransactionError
import com.superwall.sdk.web.WebPaywallRedeemer
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class InternalSuperwallEventTest {
    @Test
    fun appInstall_reportsInstallTimestampAndControllerFlag() =
        runTest {
            Given("an AppInstall event") {
                val event =
                    InternalSuperwallEvent.AppInstall(
                        appInstalledAtString = "2024-01-01T00:00:00Z",
                        hasExternalPurchaseController = true,
                    )

                When("superwall parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the install timestamp is forwarded") {
                        assertEquals("2024-01-01T00:00:00Z", params["application_installed_at"])
                    }

                    And("the purchase controller flag is forwarded") {
                        assertEquals(true, params["using_purchase_controller"])
                    }
                }
            }
        }

    @Test
    fun attributes_forwardsInstallTimestamp() =
        runTest {
            Given("an Attributes event") {
                val event =
                    InternalSuperwallEvent.Attributes(
                        appInstalledAtString = "2023-12-20T12:00:00Z",
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the install timestamp is present") {
                        assertEquals("2023-12-20T12:00:00Z", params["application_installed_at"])
                    }
                }
            }
        }

    @Test
    fun configAttributes_includesOptionsAndDelegateMetadata() =
        runTest {
            Given("Superwall options with custom values") {
                val options =
                    SuperwallOptions().apply {
                        paywalls.isHapticFeedbackEnabled = false
                        paywalls.shouldShowPurchaseFailureAlert = false
                        networkEnvironment =
                            SuperwallOptions.NetworkEnvironment.Custom(
                                baseHost = "custom.superwall.dev",
                                collectorHost = "collector.superwall.dev",
                                scheme = "https",
                                port = 8443,
                            )
                        localeIdentifier = "en_GB"
                        logging.level = com.superwall.sdk.logger.LogLevel.info
                        logging.scopes = java.util.EnumSet.of(com.superwall.sdk.logger.LogScope.paywallEvents)
                    }
                val event =
                    InternalSuperwallEvent.ConfigAttributes(
                        options = options,
                        hasExternalPurchaseController = true,
                        hasDelegate = false,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the payload matches the options map with flags") {
                        assertEquals(true, params["using_purchase_controller"])
                        assertEquals(false, params["has_delegate"])
                        val base =
                            params.toMutableMap().apply {
                                remove("using_purchase_controller")
                                remove("has_delegate")
                            }
                        val expected = options.toMap()
                        assertEquals(expected.keys, base.keys)
                        expected.forEach { (key, value) ->
                            assertEquals(value, base[key])
                        }
                    }
                }
            }
        }

    @Test
    fun deviceAttributes_returnsProvidedAttributes() =
        runTest {
            Given("device attributes") {
                val attributes = hashMapOf<String, Any>("os" to "Android", "version" to 34)
                val event = InternalSuperwallEvent.DeviceAttributes(attributes)

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the same map is returned") {
                        assertEquals(attributes, params)
                    }
                }
            }
        }

    @Test
    fun integrationAttributes_populatesAudienceFiltersOnly() =
        runTest {
            Given("integration attributes") {
                val attributes = mapOf("campaign_id" to "cmp_123", "locked" to true)
                val event = InternalSuperwallEvent.IntegrationAttributes(attributes)

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("no payload is returned") {
                        assertTrue(params.isEmpty())
                    }

                    And("audience filters mirror the provided attributes") {
                        assertEquals(attributes, event.audienceFilterParams)
                    }
                }
            }
        }

    @Test
    fun deepLink_coercesQueryParametersToTypedValues() =
        runTest {
            Given("a deep link uri with assorted query params") {
                val uri = mockk<URI>(relaxed = true)
                every { uri.path } returns "/path/file.html?enabled=true&count=3&amount=4.2&label=beta"
                every { uri.host } returns "promo"
                every { uri.query } returns "enabled=true&count=3&amount=4.2&label=beta"
                every { uri.fragment } returns null
                val event = InternalSuperwallEvent.DeepLink(uri)

                When("audience filter parameters are inspected") {
                    val filters = event.audienceFilterParams

                    Then("boolean, integer and double parameters are typed correctly") {
                        assertEquals(true, filters["enabled"])
                        assertEquals(3, filters["count"])
                        assertEquals(4.2, filters["amount"])
                        assertEquals("beta", filters["label"])
                    }
                }
            }
        }

    @Test
    fun paywallLoad_completeEmitsPaywallInfoParameters() =
        runTest {
            Given("a paywall load completion") {
                val paywallInfo = stubPaywallInfo()
                val eventData = EventData.stub()
                val event =
                    InternalSuperwallEvent.PaywallLoad(
                        state = InternalSuperwallEvent.PaywallLoad.State.Complete(paywallInfo),
                        eventData = eventData,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the trigger flag is true and paywall parameters are merged") {
                        assertEquals(true, params["is_triggered_from_event"])
                        assertEquals(paywallInfo.identifier, params["paywall_identifier"])
                    }

                    And("the superwall placement references the completion event") {
                        assertEquals(
                            SuperwallEvent.PaywallResponseLoadComplete(eventData.name, paywallInfo),
                            event.superwallPlacement,
                        )
                    }
                }
            }
        }

    @Test
    fun paywallProductsLoad_failIncludesErrorMessage() =
        runTest {
            Given("a paywall products load failure") {
                val paywallInfo = stubPaywallInfo()
                val event =
                    InternalSuperwallEvent.PaywallProductsLoad(
                        state = InternalSuperwallEvent.PaywallProductsLoad.State.Fail("bad network"),
                        paywallInfo = paywallInfo,
                        eventData = null,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the failure message is attached") {
                        assertEquals("bad network", params["error_message"])
                    }

                    And("paywall context is merged") {
                        assertEquals(paywallInfo.identifier, params["paywall_identifier"])
                    }
                }
            }
        }

    @Test
    fun paywallWebviewLoad_failSerialisesUrlsAndError() =
        runTest {
            Given("a paywall webview failure") {
                val paywallInfo = stubPaywallInfo()
                val error = WebviewError.AllUrlsFailed(listOf("https://a", "https://b"))
                val event =
                    InternalSuperwallEvent.PaywallWebviewLoad(
                        state = InternalSuperwallEvent.PaywallWebviewLoad.State.Fail(error, error.urls),
                        paywallInfo = paywallInfo,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("urls are flattened and include an error message") {
                        assertEquals("All paywall URLs have failed to load.", params["error_message"])
                        assertEquals("https://a", params["url_0"])
                        assertEquals("https://b", params["url_1"])
                    }
                }
            }
        }

    @Test
    fun shimmerLoad_completeOmitsNullDuration() =
        runTest {
            Given("a completed shimmer load without duration") {
                val event =
                    InternalSuperwallEvent.ShimmerLoad(
                        state = InternalSuperwallEvent.ShimmerLoad.State.Complete,
                        paywallId = "pw123",
                        visibleDuration = null,
                        delay = 0.5,
                        preloadingEnabled = true,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("only non-null metadata is included") {
                        assertEquals(mapOf("paywall_id" to "pw123", "preloading_enabled" to true), params)
                    }
                }
            }
        }

    @Test
    fun triggerFire_paywallIncludesExperimentMetadata() =
        runTest {
            Given("a trigger result that presents a paywall") {
                val experiment =
                    Experiment(
                        id = "exp1",
                        groupId = "grp",
                        variant = Experiment.Variant(id = "var1", type = Experiment.Variant.VariantType.TREATMENT, paywallId = "pw"),
                    )
                val event =
                    InternalSuperwallEvent.TriggerFire(
                        triggerResult = InternalTriggerResult.Paywall(experiment),
                        triggerName = "app_open",
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("variant and experiment metadata are surfaced") {
                        assertEquals("app_open", params["trigger_name"])
                        assertEquals("present", params["result"])
                        assertEquals("var1", params["variant_id"])
                        assertEquals("exp1", params["experiment_id"])
                        assertEquals("pw", params["paywall_identifier"])
                    }
                }
            }
        }

    @Test
    fun triggerFire_noAudienceMatchSetsResult() =
        runTest {
            Given("a trigger result with no audience match") {
                val event =
                    InternalSuperwallEvent.TriggerFire(
                        triggerResult =
                            InternalTriggerResult.NoAudienceMatch(
                                listOf(UnmatchedRule(UnmatchedRule.Source.EXPRESSION, "exp123")),
                            ),
                        triggerName = "purchase",
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the result indicates no match") {
                        assertEquals("no_rule_match", params["result"])
                    }
                }
            }
        }

    @Test
    fun presentationRequest_includesStatusPayload() =
        runTest {
            Given("a presentation request event") {
                val eventData = EventData.stub()
                val event =
                    InternalSuperwallEvent.PresentationRequest(
                        eventData = eventData,
                        type = PresentationRequestType.Presentation,
                        status = PaywallPresentationRequestStatus.NoPresentation,
                        statusReason = PaywallPresentationRequestStatusReason.NoConfig(),
                        factory = NoopPresentationFactory,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("status fields surface human readable values") {
                        assertEquals(eventData.name, params["source_event_name"])
                        assertEquals("presentation", params["pipeline_type"])
                        assertEquals("no_presentation", params["status"])
                        assertEquals("no_config", params["status_reason"])
                    }
                }
            }
        }

    @Test
    fun surveyResponse_mergesAudienceFilters() =
        runTest {
            Given("a survey response") {
                val survey =
                    Survey(
                        id = "survey_1",
                        assignmentKey = "assign_1",
                        title = "Title",
                        message = "Message",
                        options = listOf(SurveyOption("opt", "Pick")),
                        presentationCondition = SurveyShowCondition.ON_MANUAL_CLOSE,
                        presentationProbability = 1.0,
                        includeOtherOption = false,
                        includeCloseOption = true,
                    )
                val option = survey.options.first()
                val paywallInfo = stubPaywallInfo().copy(surveys = listOf(survey))
                val event =
                    InternalSuperwallEvent.SurveyResponse(
                        survey = survey,
                        selectedOption = option,
                        customResponse = null,
                        paywallInfo = paywallInfo,
                    )

                When("audience filters are requested") {
                    val filters = event.audienceFilterParams

                    Then("survey metadata augments paywall filters") {
                        assertEquals(option.title, filters["survey_selected_option_title"])
                        assertFalse(filters.containsKey("survey_custom_response"))
                    }
                }
            }
        }

    @Test
    fun paywallOpen_addsDemandSignalsAndUserAttributes() =
        runTest {
            Given("a paywall open event with demand info") {
                val paywallInfo = stubPaywallInfo()
                val userAttributes = mapOf("favoriteColor" to "blue")
                val event =
                    InternalSuperwallEvent.PaywallOpen(
                        paywallInfo = paywallInfo,
                        userAttributes = userAttributes,
                        demandTier = "high",
                        demandScore = 99,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("user attributes and demand metadata are included") {
                        assertEquals(userAttributes, params["user_attributes"])
                        assertEquals("high", params["attr_demandTier"])
                        assertEquals(99, params["attr_demandScore"])
                    }
                }
            }
        }

    @Test
    fun paywallClose_marksSurveyPresentation() =
        runTest {
            Given("a paywall close with an attached survey") {
                val survey =
                    Survey(
                        id = "survey_1",
                        assignmentKey = "assign_1",
                        title = "Title",
                        message = "Message",
                        options = listOf(SurveyOption("opt", "Pick")),
                        presentationCondition = SurveyShowCondition.ON_MANUAL_CLOSE,
                        presentationProbability = 1.0,
                        includeOtherOption = false,
                        includeCloseOption = true,
                    )
                val paywallInfo = stubPaywallInfo().copy(surveys = listOf(survey))
                val event =
                    InternalSuperwallEvent.PaywallClose(
                        paywallInfo = paywallInfo,
                        surveyPresentationResult = SurveyPresentationResult.SHOW,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("survey metadata is surfaced") {
                        assertEquals(true, params["survey_attached"])
                        assertEquals("show", params["survey_presentation"])
                    }
                }
            }
        }

    @Test
    fun transaction_failIncludesErrorMessage() =
        runTest {
            Given("a transaction failure") {
                val paywallInfo = stubPaywallInfo()
                val product = stubStoreProduct()
                val event =
                    InternalSuperwallEvent.Transaction(
                        state = InternalSuperwallEvent.Transaction.State.Fail(TransactionError.Failure("boom", product)),
                        paywallInfo = paywallInfo,
                        product = product,
                        model = null,
                        source = InternalSuperwallEvent.Transaction.TransactionSource.INTERNAL,
                        isObserved = false,
                        demandScore = null,
                        demandTier = null,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the error message is included") {
                        assertEquals("boom", params["message"])
                    }
                }
            }
        }

    @Test
    fun transaction_restoreFlagsPurchaseAttemptWhenModelPresent() =
        runTest {
            Given("a restore event with a backing transaction") {
                val paywallInfo = stubPaywallInfo()
                val product = stubStoreProduct()
                val storeTransaction = stubStoreTransaction()
                val event =
                    InternalSuperwallEvent.Transaction(
                        state = InternalSuperwallEvent.Transaction.State.Restore(RestoreType.ViaPurchase(storeTransaction)),
                        paywallInfo = paywallInfo,
                        product = product,
                        model = storeTransaction,
                        source = InternalSuperwallEvent.Transaction.TransactionSource.INTERNAL,
                        isObserved = false,
                        demandScore = null,
                        demandTier = null,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("restore via purchase is marked") {
                        assertEquals(true, params["restore_via_purchase_attempt"])
                    }
                }
            }
        }

    @Test
    fun transaction_abandonAddsAudienceFilterForAbandonedProduct() =
        runTest {
            Given("an abandoned transaction") {
                val paywallInfo = stubPaywallInfo()
                val product = stubStoreProduct(productId = "prod_1")
                val event =
                    InternalSuperwallEvent.Transaction(
                        state = InternalSuperwallEvent.Transaction.State.Abandon(product),
                        paywallInfo = paywallInfo,
                        product = product,
                        model = null,
                        source = InternalSuperwallEvent.Transaction.TransactionSource.INTERNAL,
                        isObserved = false,
                        demandScore = null,
                        demandTier = null,
                    )

                When("audience filters are requested") {
                    val filters = event.audienceFilterParams

                    Then("the abandoned product identifier is included") {
                        assertEquals("prod_1", filters["abandoned_product_id"])
                    }
                }
            }
        }

    @Test
    fun expressionResult_defaultsNullsToEmptyStrings() =
        runTest {
            Given("an expression result without payloads") {
                val event = InternalSuperwallEvent.ExpressionResult()

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("all entries are empty strings") {
                        assertTrue(params.values.all { it == "" })
                    }
                }
            }
        }

    @Test
    fun errorThrown_marshalsExceptionMetadata() =
        runTest {
            Given("an internal error") {
                val event =
                    InternalSuperwallEvent.ErrorThrown(
                        message = "boom",
                        stacktrace = "stack",
                        occuredAt = 1234L,
                        type = "IllegalStateException",
                        isFatal = true,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the payload mirrors constructor arguments") {
                        assertEquals("boom", params["error_message"])
                        assertEquals("stack", params["error_stack_trace"])
                        assertEquals(1234L, params["occured_at"])
                        assertEquals(true, params["is_fatal"])
                    }
                }
            }
        }

    @Test
    fun configRefresh_includesCacheMetadata() =
        runTest {
            Given("a cached config refresh") {
                val event =
                    InternalSuperwallEvent.ConfigRefresh(
                        isCached = true,
                        buildId = "build_1",
                        retryCount = 2,
                        fetchDuration = 120L,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("cache metadata is emitted") {
                        assertEquals("CACHED", params["cache_status"])
                        assertEquals("build_1", params["config_build_id"])
                        assertEquals(2, params["retry_count"])
                        assertEquals(120L, params["fetch_duration"])
                    }
                }
            }
        }

    @Test
    fun restore_failureIncludesReason() =
        runTest {
            Given("a failed restore") {
                val paywallInfo = stubPaywallInfo()
                val event =
                    InternalSuperwallEvent.Restore(
                        state = InternalSuperwallEvent.Restore.State.Failure("not found"),
                        paywallInfo = paywallInfo,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the error reason is present") {
                        assertEquals("not found", params["error_message"])
                    }
                }
            }
        }

    @Test
    fun subscriptionStatusDidChange_mapsStateString() =
        runTest {
            Given("an active subscription") {
                val event =
                    InternalSuperwallEvent.SubscriptionStatusDidChange(
                        subscriptionStatus = SubscriptionStatus.Active(emptySet()),
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("the status string matches") {
                        assertEquals("active", params["subscription_status"])
                    }
                }
            }
        }

    @Test
    fun customPlacement_mergesPlacementParameters() =
        runTest {
            Given("a custom placement event") {
                val paywallInfo = stubPaywallInfo()
                val params = mapOf("placement_key" to "promo")
                val event =
                    InternalSuperwallEvent.CustomPlacement(
                        placementName = "flash_sale",
                        paywallInfo = paywallInfo,
                        params = params,
                    )

                When("parameters are requested") {
                    val payload = event.getSuperwallParameters()

                    Then("custom params and placement name are included") {
                        assertEquals("flash_sale", payload["name"])
                        assertEquals("promo", payload["placement_key"])
                    }

                    And("audience filters include placement params") {
                        assertEquals("promo", event.audienceFilterParams["placement_key"])
                    }
                }
            }
        }

    @Test
    fun enrichmentLoad_completeFlattensUserAndDeviceValues() =
        runTest {
            Given("an enrichment load completion") {
                val enrichment =
                    Enrichment(
                        _user =
                            buildJsonObject {
                                put("firstName", JsonPrimitive("Ada"))
                            },
                        _device =
                            buildJsonObject {
                                put("tier", JsonPrimitive("gold"))
                            },
                    )
                val event =
                    InternalSuperwallEvent.EnrichmentLoad(
                        state = InternalSuperwallEvent.EnrichmentLoad.State.Complete(enrichment),
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("user and device keys are flattened") {
                        assertEquals("Ada", params["user_firstName"])
                        assertEquals("gold", params["device_tier"])
                    }
                }
            }
        }

    @Test
    fun paywallResourceLoadFail_reportsUrlAndError() =
        runTest {
            Given("a resource load failure") {
                val event = InternalSuperwallEvent.PaywallResourceLoadFail("https://asset", "not found")

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("url and error are forwarded") {
                        assertEquals("https://asset", params["url"])
                        assertEquals("not found", params["error"])
                    }
                }
            }
        }

    @Test
    fun nonRecurringProductPurchase_forwardsProductContext() =
        runTest {
            Given("a non recurring purchase") {
                val paywallInfo = stubPaywallInfo()
                val product = stubStoreProduct(productId = "prod_2", fullId = "prod_2.full")
                val event =
                    InternalSuperwallEvent.NonRecurringProductPurchase(
                        paywallInfo = paywallInfo,
                        product = product,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("product id is included") {
                        assertEquals("prod_2.full", params["product_id"])
                    }
                }
            }
        }

    @Test
    fun freeTrialStart_forwardsProductContext() =
        runTest {
            Given("a free trial start event") {
                val paywallInfo = stubPaywallInfo()
                val product = stubStoreProduct(productId = "trial_prod", fullId = "trial.full")
                val event =
                    InternalSuperwallEvent.FreeTrialStart(
                        paywallInfo = paywallInfo,
                        product = product,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("product id is included") {
                        assertEquals("trial.full", params["product_id"])
                    }
                }
            }
        }

    @Test
    fun redemptions_codeTypeIncludesCodeValue() =
        runTest {
            Given("a redemption start with code") {
                val event =
                    InternalSuperwallEvent.Redemptions(
                        state = InternalSuperwallEvent.Redemptions.RedemptionState.Start,
                        type = WebPaywallRedeemer.RedeemType.Code("SW123"),
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("type and code are surfaced") {
                        assertEquals("CODE", params["type"])
                        assertEquals("SW123", params["code"])
                    }
                }
            }
        }

    @Test
    fun reviewRequested_reportsCountAndType() =
        runTest {
            Given("a review request event") {
                val event = InternalSuperwallEvent.ReviewRequested(count = 3, type = "trigger")

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("count and type are preserved") {
                        assertEquals(3, params["count"])
                        assertEquals("trigger", params["type"])
                    }
                }
            }
        }

    @Test
    fun userInitiatedEventTrack_returnsFeatureGatableFlag() =
        runTest {
            Given("a user initiated track event") {
                val event =
                    UserInitiatedEvent.Track(
                        rawName = "custom",
                        canImplicitlyTriggerPaywall = true,
                        isFeatureGatable = false,
                        customParameters = mapOf("foo" to "bar"),
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("feature gating flag is returned") {
                        assertEquals(false, params["is_feature_gatable"])
                    }
                }
            }
        }

    @Test
    fun paywallPreload_startReportsPaywallCount() =
        runTest {
            Given("a paywall preload start event") {
                val paywallCount = 5
                val event =
                    InternalSuperwallEvent.PaywallPreload(
                        state = InternalSuperwallEvent.PaywallPreload.State.Start,
                        paywallCount = paywallCount,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("paywall count is included") {
                        assertEquals(paywallCount, params["paywall_count"])
                    }

                    And("the superwall placement is paywallPreload_start") {
                        assertEquals("paywallPreload_start", event.superwallPlacement.rawName)
                    }
                }
            }
        }

    @Test
    fun paywallPreload_completeReportsPaywallCount() =
        runTest {
            Given("a paywall preload complete event") {
                val paywallCount = 3
                val event =
                    InternalSuperwallEvent.PaywallPreload(
                        state = InternalSuperwallEvent.PaywallPreload.State.Complete,
                        paywallCount = paywallCount,
                    )

                When("parameters are requested") {
                    val params = event.getSuperwallParameters()

                    Then("paywall count is included") {
                        assertEquals(paywallCount, params["paywall_count"])
                    }

                    And("the superwall placement is paywallPreload_complete") {
                        assertEquals("paywallPreload_complete", event.superwallPlacement.rawName)
                    }
                }
            }
        }

    private fun stubPaywallInfo(): PaywallInfo =
        PaywallInfo.empty().copy(
            databaseId = "db_1",
            identifier = "paywall_1",
            name = "Paywall",
            url =
                com.superwall.sdk.models.paywall
                    .PaywallURL("https://paywall.example"),
            presentedByEventWithName = "app_open",
            presentedByEventWithId = "evt_1",
            presentedBy = "event",
            isFreeTrialAvailable = true,
            paywalljsVersion = "1.2.3",
            presentation = PaywallPresentationInfo(PaywallPresentationStyle.Modal, 0),
            buildId = "build_1",
            cacheKey = "cache_1",
        )

    private fun stubStoreProduct(
        productId: String = "primary",
        fullId: String = "primary.full",
    ): StoreProduct =
        mockk(relaxed = true) {
            every { productIdentifier } returns productId
            every { fullIdentifier } returns fullId
            every { hasFreeTrial } returns false
            every { attributes } returns mapOf("price" to "5.99", "period" to "month")
        }

    private fun stubStoreTransaction(): StoreTransaction {
        val payment = StorePayment(productIdentifier = "primary", quantity = 1, discountIdentifier = null)
        val transaction =
            GoogleBillingPurchaseTransaction(
                transactionDate = java.util.Date(0),
                originalTransactionIdentifier = "orig",
                state = StoreTransactionState.Purchased,
                storeTransactionId = "store",
                originalTransactionDate = java.util.Date(0),
                webOrderLineItemID = null,
                appBundleId = "bundle",
                subscriptionGroupId = null,
                isUpgraded = null,
                expirationDate = null,
                offerId = null,
                revocationDate = null,
                appAccountToken = null,
                purchaseToken = "token",
                payment = payment,
                signature = null,
            )
        return StoreTransaction(transaction, configRequestId = "config", appSessionId = "session")
    }

    private object NoopPresentationFactory : InternalSuperwallEvent.PresentationRequest.Factory {
        override suspend fun makeRuleAttributes(
            event: EventData?,
            computedPropertyRequests: List<ComputedPropertyRequest>,
        ): Map<String, Any> = emptyMap()

        override fun makeFeatureFlags(): FeatureFlags? = null

        override fun makeComputedPropertyRequests(): List<ComputedPropertyRequest> = emptyList()
    }
}
