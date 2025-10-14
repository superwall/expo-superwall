package com.superwall.sdk.config

import com.superwall.sdk.models.assignment.Assignment
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.config.PreloadingDisabled
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.triggers.*
import com.superwall.sdk.paywall.presentation.rule_logic.expression_evaluator.ExpressionEvaluating
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

internal class ConfigLogicTest {
    private var expressionEvaluator = ExpressionEvaluatorMock()

    class ExpressionEvaluatorMock : ExpressionEvaluating {
        override suspend fun evaluateExpression(
            rule: TriggerRule,
            eventData: EventData?,
        ): TriggerRuleOutcome =
            TriggerRuleOutcome.match(
                rule =
                    TriggerRule(
                        experimentId = "1",
                        experimentGroupId = "2",
                        variants = listOf(),
                        preload = TriggerRule.TriggerPreload(behavior = TriggerPreloadBehavior.ALWAYS),
                    ),
            )
    }

    @Test
    fun test_chooseVariant_noVariants() {
        try {
            ConfigLogic.chooseVariant(listOf())
            fail("Should have produced an error")
        } catch (error: ConfigLogic.TriggerRuleError) {
            assertEquals(error, ConfigLogic.TriggerRuleError.NoVariantsFound)
        } catch (error: Throwable) {
            fail("Should have produced a no variant error")
        }
    }

    @Test
    fun test_chooseVariant_onlyOneVariant_zeroSum() {
        try {
            val options =
                listOf(
                    VariantOption.stub().apply { percentage = 0 },
                )
            val variant = ConfigLogic.chooseVariant(options)
            assertEquals(options.first().toVariant(), variant)
        } catch (error: ConfigLogic.TriggerRuleError) {
            assertEquals(error, ConfigLogic.TriggerRuleError.InvalidState)
        } catch (error: Throwable) {
            fail("Should have produced a no variant error")
        }
    }

    @Test
    fun test_chooseVariant_manyVariants_zeroSum() {
        val variants =
            listOf(
                VariantOption.stub().apply { percentage = 0 },
                VariantOption.stub().apply { percentage = 0 },
            )

        try {
            val variant = ConfigLogic.chooseVariant(variants, randomiser = { 0 })
            assertEquals(variants.first().toVariant(), variant)
        } catch (error: Throwable) {
            fail("Should have worked")
        }
    }

    @Test
    fun test_chooseVariant_manyVariants_one_percent_sum() {
        val variants =
            listOf(
                VariantOption.stub().apply { percentage = 1 },
                VariantOption.stub().apply { percentage = 0 },
                VariantOption.stub().apply { percentage = 0 },
            )

        try {
            val variant = ConfigLogic.chooseVariant(variants)
            assertEquals(variants.first().toVariant(), variant)
        } catch (error: Throwable) {
            fail("Should have worked")
        }
    }

    @Test
    fun testChooseVariantDistribution() {
        // Given: Create variants with the desired distribution.
        val variants =
            listOf(
                VariantOption.stub().apply {
                    id = "A"
                    percentage = 85
                },
                VariantOption.stub().apply {
                    id = "B"
                    percentage = 5
                },
                VariantOption.stub().apply {
                    id = "C"
                    percentage = 5
                },
                VariantOption.stub().apply {
                    id = "D"
                    percentage = 5
                },
            )

        // Initialize counters for each variant.
        val selectionCounts = mutableMapOf("A" to 0, "B" to 0, "C" to 0, "D" to 0)

        // Number of iterations.
        val iterations = 100_000

        // When: Run chooseVariant multiple times and count selections.
        repeat(iterations) {
            // Call the function under test.
            val selectedVariant = ConfigLogic.chooseVariant(variants)
            // Increment the counter for the selected variant.
            selectionCounts[selectedVariant.id] = selectionCounts.getOrDefault(selectedVariant.id, 0) + 1
        }

        // Then: Calculate observed percentages.
        val observedPercentages = selectionCounts.mapValues { (it.value.toDouble() / iterations) * 100.0 }
        // Define expected percentages.
        val expectedPercentages = mapOf("A" to 85.0, "B" to 5.0, "C" to 5.0, "D" to 5.0)
        // Define acceptable margin of error (±1%).
        val marginOfError = 1.0

        // Assert that each observed percentage is within the acceptable range.
        expectedPercentages.forEach { (variantID, expectedPercentage) ->
            val observedPercentage = observedPercentages[variantID]
            assertNotNull("Variant $variantID was not selected at all.", observedPercentage)
            observedPercentage?.let {
                assertEquals(
                    "Variant $variantID selection percentage $it% is not within $marginOfError% of expected $expectedPercentage%.",
                    expectedPercentage,
                    it,
                    marginOfError,
                )
            }
        }

        // Optional: Print the results for debugging purposes.
        println("Variant Selection Distribution after $iterations iterations:")
        selectionCounts.forEach { (variantID, count) ->
            val percentage = observedPercentages[variantID] ?: 0.0
            println("Variant $variantID: $count selections (${String.format("%.2f", percentage)}%)")
        }
    }

    @Test
    fun test_chooseVariant_oneActiveVariant_chooseFirst() {
        try {
            val options: List<VariantOption> =
                listOf(
                    VariantOption
                        .stub()
                        .apply { percentage = 100 },
                    VariantOption
                        .stub()
                        .apply { percentage = 0 },
                    VariantOption
                        .stub()
                        .apply { percentage = 0 },
                )

            val variant = ConfigLogic.chooseVariant(options)
            assertEquals(options.first().toVariant(), variant)
        } catch (e: Throwable) {
            fail("Should have produced a no variant error")
        }
    }

    @Test
    fun test_chooseVariant_99PercentSumVariants_chooseLast() {
        try {
            val options =
                listOf(
                    VariantOption.stub().apply { percentage = 33 },
                    VariantOption.stub().apply { percentage = 33 },
                    VariantOption.stub().apply { percentage = 33 },
                )

            val variant =
                ConfigLogic.chooseVariant(
                    options,
                    randomiser = { range ->
                        assertEquals(0 until 99, range)
                        98
                    },
                )

            assertEquals(options.last().toVariant(), variant)
        } catch (e: Throwable) {
            fail("Should not fail")
        }
    }

    @Test
    fun test_chooseVariant_99PercentSumVariants_chooseMiddle() {
        try {
            val options =
                listOf(
                    VariantOption.stub().apply { percentage = 33 },
                    VariantOption.stub().apply { percentage = 33 },
                    VariantOption.stub().apply { percentage = 33 },
                )

            val variant =
                ConfigLogic.chooseVariant(
                    options,
                    randomiser = { range ->
                        assertEquals(0 until 99, range)
                        65
                    },
                )

            assertEquals(options[1].toVariant(), variant)
        } catch (e: Throwable) {
            fail("Should not fail")
        }
    }

    @Test
    fun test_chooseVariant_99PercentSumVariants_chooseFirst() {
        try {
            val options =
                listOf(
                    VariantOption.stub().apply { percentage = 33 },
                    VariantOption.stub().apply { percentage = 33 },
                    VariantOption.stub().apply { percentage = 33 },
                )

            val variant =
                ConfigLogic.chooseVariant(
                    options,
                    randomiser = { range ->
                        assertEquals(0 until 99, range)
                        0
                    },
                )

            assertEquals(options[0].toVariant(), variant)
        } catch (e: Throwable) {
            fail("Should not fail")
        }
    }

    @Test
    fun test_getRulesPerTriggerGroup_noTriggers() {
        val rules =
            ConfigLogic.getRulesPerCampaign(
                emptySet(),
            )
        assertTrue(rules.isEmpty())
    }

    @Test
    fun test_getRulesPerTriggerGroup_noRules() {
        val trigger =
            Trigger.stub().apply {
                rules = emptyList()
            }
        val rules = ConfigLogic.getRulesPerCampaign(setOf(trigger))
        assertTrue(rules.isEmpty())
    }

    @Test
    fun test_getRulesPerTriggerGroup_threeTriggersTwoWithSameGroupId() {
        val trigger1 =
            Trigger.stub().apply {
                rules =
                    listOf(
                        TriggerRule.stub().apply {
                            experimentGroupId = "1"
                        },
                    )
            }
        val trigger2 =
            Trigger.stub().apply {
                rules =
                    listOf(
                        TriggerRule.stub().apply {
                            experimentGroupId = "1"
                        },
                    )
            }
        val trigger3 =
            Trigger.stub().apply {
                rules =
                    listOf(
                        TriggerRule.stub().apply {
                            experimentGroupId = "2"
                        },
                    )
            }
        val rules = ConfigLogic.getRulesPerCampaign(setOf(trigger1, trigger2, trigger3))
        assertEquals(2, rules.size)
        assertTrue(rules.contains(trigger3.rules))
        assertTrue(rules.contains(trigger1.rules))
    }

    // MARK: - Choose Variants
    @Test
    fun test_assignVariants_noTriggers() {
        // Given
        val confirmedAssignments =
            mutableMapOf(
                "exp1" to
                    Experiment.Variant(
                        id = "1",
                        type = Experiment.Variant.VariantType.TREATMENT,
                        paywallId = "abc",
                    ),
            )

        // When
        val variant =
            ConfigLogic.chooseAssignments(
                fromTriggers = emptySet(),
                confirmedAssignments = confirmedAssignments,
            )

        // Then
        assert(variant.unconfirmed.isEmpty())
        assert(variant.confirmed == confirmedAssignments)
    }

    @Test
    fun test_chooseAssignments_noRules() {
        // Given
        val confirmedAssignments =
            mutableMapOf(
                "exp1" to
                    Experiment.Variant(
                        id = "1",
                        type = Experiment.Variant.VariantType.TREATMENT,
                        paywallId = "abc",
                    ),
            )

        // When
        val variant =
            ConfigLogic.chooseAssignments(
                fromTriggers = setOf(Trigger.stub().apply { rules = emptyList() }),
                confirmedAssignments = confirmedAssignments,
            )

        // Then
        assert(variant.unconfirmed.isEmpty())
        assert(variant.confirmed == confirmedAssignments)
    }

    @Test
    fun test_chooseAssignments_variantAsOfYetUnconfirmed() {
        // Given
        var variantId = "abc"
        var paywallId = "edf"
        var experimentId = "3"
        var experimentGroupId = "13"
        val variantOption =
            VariantOption.stub().apply {
                id = variantId
                paywallId = paywallId
                type = Experiment.Variant.VariantType.TREATMENT
            }

        var rules =
            listOf(
                TriggerRule.stub().apply {
                    this.experimentGroupId = experimentGroupId
                    this.experimentId = experimentId
                    this.variants = listOf(variantOption)
                },
            )

        val fromTriggers =
            setOf(
                Trigger.stub().apply {
                    this.rules = rules
                },
            )

        // When
        val variant =
            ConfigLogic.chooseAssignments(
                fromTriggers = fromTriggers,
                confirmedAssignments = mutableMapOf(),
            )

        // Then
        assertEquals(1, variant.unconfirmed.count())
        println(variant.unconfirmed)
        assertEquals(variantOption.toVariant(), variant.unconfirmed[experimentId])
        assertTrue(variant.confirmed.isEmpty())
    }

    @Test
    fun test_chooseAssignments_variantAlreadyConfirmed() {
        // Given
        var variantId = "abc"
        var paywallId = "edf"
        var experimentId = "3"
        var experimentGroupId = "13"
        val variantOption =
            VariantOption.stub().apply {
                this.id = variantId
                this.paywallId = paywallId
                this.type = Experiment.Variant.VariantType.TREATMENT
            }

        // When
        val variant =
            ConfigLogic.chooseAssignments(
                fromTriggers =
                    setOf(
                        Trigger.stub().apply {
                            rules =
                                listOf(
                                    TriggerRule.stub().apply {
                                        this.experimentGroupId = experimentGroupId
                                        this.experimentId = experimentId
                                        this.variants = listOf(variantOption)
                                    },
                                )
                        },
                    ),
                confirmedAssignments = mutableMapOf(experimentId to variantOption.toVariant()),
            )

        // Then
        assertEquals(1, variant.confirmed.count())
        assertEquals(variantOption.toVariant(), variant.confirmed[experimentId])
        assertTrue(variant.unconfirmed.isEmpty())
    }

    @Test
    fun `test chooseAssignments variantAlreadyConfirmed nowUnavailable`() {
        // Given
        val paywallId = "edf"
        val experimentId = "3"
        val experimentGroupId = "13"
        val newVariantOption =
            VariantOption.stub().apply {
                this.id = "newVariantId"
                this.paywallId = paywallId
                this.type = Experiment.Variant.VariantType.TREATMENT
            }
        val oldVariantOption =
            VariantOption.stub().apply {
                this.id = "oldVariantId"
                this.paywallId = paywallId
                this.type = Experiment.Variant.VariantType.TREATMENT
            }

        // When
        val variant =
            ConfigLogic.chooseAssignments(
                fromTriggers =
                    setOf(
                        Trigger.stub().apply {
                            this.rules =
                                listOf(
                                    TriggerRule.stub().apply {
                                        this.experimentGroupId = experimentGroupId
                                        this.experimentId = experimentId
                                        this.variants = listOf(newVariantOption)
                                    },
                                )
                        },
                    ),
                confirmedAssignments = mapOf(experimentId to oldVariantOption.toVariant()),
            )

        // Then
        assertEquals(1, variant.unconfirmed.size)
        assertEquals(newVariantOption.toVariant(), variant.unconfirmed[experimentId])
        assertTrue(variant.confirmed.isEmpty())
    }

    @Test
    fun `test chooseAssignments variantAlreadyConfirmed nowNoVariants`() {
        // Given
        val paywallId = "edf"
        val experimentId = "3"
        val experimentGroupId = "13"
        val oldVariantOption =
            VariantOption.stub().apply {
                this.id = "oldVariantId"
                this.paywallId = paywallId
                this.type = Experiment.Variant.VariantType.TREATMENT
            }

        // When
        val variant =
            ConfigLogic.chooseAssignments(
                fromTriggers =
                    setOf(
                        Trigger.stub().apply {
                            this.rules =
                                listOf(
                                    TriggerRule.stub().apply {
                                        this.experimentGroupId = experimentGroupId
                                        this.experimentId = experimentId
                                        this.variants = emptyList()
                                    },
                                )
                        },
                    ),
                confirmedAssignments = mapOf(experimentId to oldVariantOption.toVariant()),
            )

        // Then
        assertTrue(variant.unconfirmed.isEmpty())
        assertTrue(variant.confirmed.isEmpty())
    }

    // MARK: - processAssignmentsFromServer

    @Test
    fun test_processAssignmentsFromServer_noAssignments() {
        val confirmedVariant =
            Experiment.Variant("def", Experiment.Variant.VariantType.TREATMENT, "ghi")
        val unconfirmedVariant =
            Experiment.Variant("mno", Experiment.Variant.VariantType.TREATMENT, "pqr")
        val result =
            ConfigLogic.transferAssignmentsFromServerToDisk(
                assignments = emptyList(),
                triggers = setOf(Trigger.stub()),
                confirmedAssignments = mapOf("abc" to confirmedVariant),
                unconfirmedAssignments = mapOf("jkl" to unconfirmedVariant),
            )
        assertEquals(result.confirmed["abc"], confirmedVariant)
        assertEquals(result.unconfirmed["jkl"], unconfirmedVariant)
    }

    @Test
    fun test_processAssignmentsFromServer_overwriteConfirmedAssignment() {
        val experimentId = "abc"
        val variantId = "def"

        val assignments: List<Assignment> =
            listOf(
                Assignment(experimentId, variantId),
            )
        val oldVariantOption = VariantOption.stub()
        val variantOption =
            VariantOption
                .stub()
                .apply { id = variantId }
        val triggers =
            setOf(
                Trigger
                    .stub()
                    .apply {
                        rules =
                            listOf(
                                TriggerRule
                                    .stub()
                                    .apply {
                                        this.experimentId = experimentId
                                        this.variants = listOf(variantOption)
                                    },
                            )
                    },
            )

        val unconfirmedVariant =
            Experiment.Variant("mno", Experiment.Variant.VariantType.TREATMENT, "pqr")
        val result =
            ConfigLogic.transferAssignmentsFromServerToDisk(
                assignments = assignments,
                triggers = triggers,
                confirmedAssignments =
                    mapOf(
                        experimentId to
                            oldVariantOption.toVariant(),
                    ),
                unconfirmedAssignments = mapOf("jkl" to unconfirmedVariant),
            )

        assertEquals(result.confirmed[experimentId], variantOption.toVariant())
        assertEquals(result.unconfirmed["jkl"], unconfirmedVariant)
    }

    @Test
    fun test_processAssignmentsFromServer_multipleAssignments() {
        val experimentId1 = "abc"
        val variantId1 = "def"

        val experimentId2 = "ghi"
        val variantId2 = "klm"

        val assignments: List<Assignment> =
            listOf(
                Assignment(
                    experimentId = experimentId1,
                    variantId = variantId1,
                ),
                Assignment(
                    experimentId = experimentId2,
                    variantId = variantId2,
                ),
            )
        val unusedVariantOption1 =
            VariantOption.stub().apply {
                this.id = "unusedOption1"
            }
        val variantOption1 =
            VariantOption
                .stub()
                .apply { id = variantId1 }
        val variantOption2 =
            VariantOption
                .stub()
                .apply { id = variantId2 }
        val unusedVariantOption2 =
            VariantOption
                .stub()
                .apply { id = "unusedOption2" }

        val triggers: Set<Trigger> =
            setOf(
                Trigger
                    .stub()
                    .apply {
                        this.rules =
                            listOf(
                                TriggerRule.stub().apply {
                                    this.experimentId = experimentId1
                                    this.variants = listOf(variantOption1, unusedVariantOption1)
                                },
                                TriggerRule.stub().apply {
                                    this.experimentId = experimentId2
                                    this.variants = listOf(variantOption2, unusedVariantOption2)
                                },
                            )
                    },
            )

        val unconfirmedVariant =
            Experiment.Variant(
                id = "mno",
                type = Experiment.Variant.VariantType.TREATMENT,
                paywallId = "pqr",
            )
        val result =
            ConfigLogic.transferAssignmentsFromServerToDisk(
                assignments = assignments,
                triggers = triggers,
                confirmedAssignments = emptyMap(),
                unconfirmedAssignments =
                    mapOf(
                        "jkl" to
                            Experiment.Variant(
                                id = "mno",
                                type = Experiment.Variant.VariantType.TREATMENT,
                                paywallId = "pqr",
                            ),
                    ),
            )
        assertEquals(result.confirmed.size, 2)
        assertEquals(result.confirmed[experimentId1], variantOption1.toVariant())
        assertEquals(result.confirmed[experimentId2], variantOption2.toVariant())
        assertEquals(result.unconfirmed["jkl"], unconfirmedVariant)
    }

    // MARK: - getStaticPaywall

    @Test
    fun test_getStaticPaywall_noPaywallId() {
        val response =
            ConfigLogic.getStaticPaywall(
                withId = null,
                config = Config.stub(),
                deviceLocale = "en_GB",
            )
        assertNull(response)
    }

    @Test
    fun test_getStaticPaywall_noConfig() {
        val response =
            ConfigLogic.getStaticPaywall(
                withId = "abc",
                config = null,
                deviceLocale = "en_GB",
            )
        assertNull(response)
    }

    @Test
    fun test_getStaticPaywall_deviceLocaleSpecifiedInConfig() {
        val locale = "en_GB"
        val response =
            ConfigLogic.getStaticPaywall(
                withId = "abc",
                config =
                    Config
                        .stub()
                        .apply { this.locales = setOf(locale) },
                deviceLocale = locale,
            )
        assertNull(response)
    }

    @Test
    fun test_getStaticPaywall_shortLocaleContainsEn() {
        val paywallId = "abc"
        val locale = "en_GB"
        val config: Config =
            Config
                .stub()
                .apply {
                    locales = setOf("de_DE")
                    paywalls =
                        listOf(
                            Paywall.stub(),
                            Paywall
                                .stub()
                                .apply { this.identifier = paywallId },
                        )
                }

        val response =
            ConfigLogic.getStaticPaywall(
                withId = paywallId,
                config = config,
                deviceLocale = locale,
            )

        assertEquals(response, config.paywalls[1])
    }

    @Test
    fun test_getStaticPaywall_shortLocaleNotContainedInConfig() {
        val paywallId = "abc"
        val locale = "de_DE"
        val config: Config =
            Config.stub().apply {
                this.locales = setOf()
                this.paywalls =
                    listOf(
                        Paywall.stub(),
                        Paywall
                            .stub()
                            .apply { this.identifier = paywallId },
                    )
            }

        val response =
            ConfigLogic.getStaticPaywall(
                withId = paywallId,
                config = config,
                deviceLocale = locale,
            )

        assertEquals(response, config.paywalls[1])
    }

    @Test
    fun test_GetStaticPaywallResponse_ShortLocaleContainedInConfig() {
        val paywallId = "abc"
        val locale = "de_DE"
        val config: Config =
            Config
                .stub()
                .apply {
                    this.locales = setOf(locale)
                    this.paywalls =
                        listOf(
                            Paywall.stub(),
                            Paywall
                                .stub()
                                .apply { this.identifier = paywallId },
                        )
                }

        val response =
            ConfigLogic.getStaticPaywall(
                withId = paywallId,
                config = config,
                deviceLocale = locale,
            )

        assertNull(response)
    }

    @Test
    fun test_getAllActiveTreatmentPaywallIds_onlyConfirmedAssignments_treatment() =
        runTest {
            val paywallId1 = "abc"
            val experiment1 = "def"

            val triggers =
                setOf(
                    Trigger
                        .stub()
                        .apply {
                            rules =
                                listOf(
                                    TriggerRule
                                        .stub()
                                        .apply {
                                            this.experimentId = experiment1
                                        },
                                )
                        },
                )
            val confirmedAssignments: Map<ExperimentID, Experiment.Variant> =
                mapOf(
                    experiment1 to
                        Experiment.Variant(
                            paywallId1,
                            Experiment.Variant.VariantType.TREATMENT,
                            paywallId1,
                        ),
                )
            val ids =
                ConfigLogic.getAllActiveTreatmentPaywallIds(
                    triggers = triggers,
                    confirmedAssignments = confirmedAssignments,
                    unconfirmedAssignments = emptyMap(),
                    expressionEvaluator = expressionEvaluator,
                )
            assertEquals(ids, setOf(paywallId1))
        }

    @Test
    fun test_getAllActiveTreatmentPaywallIds_onlyConfirmedAssignments_treatment_multipleTriggerSameGroupId() =
        runTest {
            val paywallId1 = "abc"
            val experiment1 = "def"

            val triggers =
                setOf(
                    Trigger
                        .stub()
                        .apply {
                            rules =
                                listOf(
                                    TriggerRule
                                        .stub()
                                        .apply {
                                            this.experimentId = experiment1
                                        },
                                )
                        },
                    Trigger
                        .stub()
                        .apply {
                            rules =
                                listOf(
                                    TriggerRule
                                        .stub()
                                        .apply {
                                            this.experimentId = experiment1
                                        },
                                )
                        },
                )
            val confirmedAssignments: Map<String, Experiment.Variant> =
                mapOf(
                    experiment1 to
                        Experiment.Variant(
                            paywallId1,
                            Experiment.Variant.VariantType.TREATMENT,
                            paywallId1,
                        ),
                )
            val ids =
                ConfigLogic.getAllActiveTreatmentPaywallIds(
                    triggers = triggers,
                    confirmedAssignments = confirmedAssignments,
                    unconfirmedAssignments = emptyMap(),
                    expressionEvaluator = expressionEvaluator,
                )
            assertEquals(ids, setOf(paywallId1))
        }

    @Test
    fun test_getAllActiveTreatmentPaywallIds_onlyConfirmedAssignments_holdout() =
        runTest {
            val experiment1 = "def"

            val triggers =
                setOf(
                    Trigger
                        .stub()
                        .apply {
                            rules =
                                listOf(
                                    TriggerRule
                                        .stub()
                                        .apply {
                                            this.experimentId = experiment1
                                        },
                                )
                        },
                )
            val confirmedAssignments: Map<String, Experiment.Variant> =
                mapOf(
                    experiment1 to
                        Experiment.Variant(
                            "variantId1",
                            Experiment.Variant.VariantType.HOLDOUT,
                            null,
                        ),
                )
            val ids =
                ConfigLogic.getAllActiveTreatmentPaywallIds(
                    triggers = triggers,
                    confirmedAssignments = confirmedAssignments,
                    unconfirmedAssignments = emptyMap(),
                    expressionEvaluator = expressionEvaluator,
                )
            assertTrue(ids.isEmpty())
        }

    @Test
    fun test_getAllActiveTreatmentPaywallIds_onlyConfirmedAssignments_filterOldOnes() =
        runTest {
            val paywallId1 = "abc"
            val experiment1 = "def"
            val paywallId2 = "efg"
            val experiment2 = "ghi"

            val triggers =
                setOf(
                    Trigger
                        .stub()
                        .apply {
                            rules =
                                listOf(
                                    TriggerRule
                                        .stub()
                                        .apply {
                                            this.experimentId = experiment1
                                        },
                                )
                        },
                )
            val confirmedAssignments: Map<String, Experiment.Variant> =
                mapOf(
                    experiment1 to
                        Experiment.Variant(
                            "variantId1",
                            Experiment.Variant.VariantType.TREATMENT,
                            paywallId1,
                        ),
                    experiment2 to
                        Experiment.Variant(
                            "variantId2",
                            Experiment.Variant.VariantType.TREATMENT,
                            paywallId2,
                        ),
                )
            val ids =
                ConfigLogic.getAllActiveTreatmentPaywallIds(
                    triggers = triggers,
                    confirmedAssignments = confirmedAssignments,
                    unconfirmedAssignments = emptyMap(),
                    expressionEvaluator = expressionEvaluator,
                )
            assertEquals(ids, setOf(paywallId1))
        }

    @Test
    fun test_getAllActiveTreatmentPaywallIds_confirmedAndUnconfirmedAssignments_filterOldOnes() =
        runTest {
            val paywallId1 = "abc"
            val experiment1 = "def"
            val paywallId2 = "efg"
            val experiment2 = "ghi"
            val paywallId3 = "jik"
            val experiment3 = "klo"

            val triggers =
                setOf(
                    Trigger
                        .stub()
                        .apply {
                            rules =
                                listOf(
                                    TriggerRule
                                        .stub()
                                        .apply {
                                            this.experimentId = experiment1
                                            this.experimentGroupId = "a"
                                        },
                                )
                        },
                    Trigger
                        .stub()
                        .apply {
                            rules =
                                listOf(
                                    TriggerRule
                                        .stub()
                                        .apply {
                                            this.experimentId = experiment3
                                            this.experimentGroupId = "b"
                                        },
                                )
                        },
                )
            val confirmedAssignments: Map<String, Experiment.Variant> =
                mapOf(
                    experiment1 to
                        Experiment.Variant(
                            "variantId1",
                            Experiment.Variant.VariantType.TREATMENT,
                            paywallId1,
                        ),
                    experiment2 to
                        Experiment.Variant(
                            "variantId2",
                            Experiment.Variant.VariantType.TREATMENT,
                            paywallId2,
                        ),
                )
            val ids =
                ConfigLogic.getAllActiveTreatmentPaywallIds(
                    triggers = triggers,
                    confirmedAssignments = confirmedAssignments,
                    unconfirmedAssignments =
                        mapOf(
                            experiment3 to
                                Experiment.Variant(
                                    "variantId3",
                                    Experiment.Variant.VariantType.TREATMENT,
                                    paywallId3,
                                ),
                        ),
                    expressionEvaluator = expressionEvaluator,
                )
            assertEquals(ids, setOf(paywallId1, paywallId3))
        }

    @Test
    fun test_getActiveTreatmentPaywallIds() {
        val paywallId1 = "abc"
        val experiment1 = "def"
        val experiment2 = "sdf"
        val paywallId2 = "wer"

        val triggers =
            setOf(
                Trigger
                    .stub()
                    .apply {
                        rules =
                            listOf(
                                TriggerRule
                                    .stub()
                                    .apply {
                                        this.experimentId = experiment1
                                    },
                            )
                    },
            )
        val confirmedAssignments: Map<String, Experiment.Variant> =
            mapOf(
                experiment1 to
                    Experiment.Variant(
                        experiment2,
                        Experiment.Variant.VariantType.TREATMENT,
                        paywallId1,
                    ),
                experiment2 to
                    Experiment.Variant(
                        experiment2,
                        Experiment.Variant.VariantType.TREATMENT,
                        paywallId2,
                    ),
            )
        val ids =
            ConfigLogic.getActiveTreatmentPaywallIds(
                forTriggers = triggers,
                confirmedAssignments = confirmedAssignments,
                unconfirmedAssignments = emptyMap(),
            )
        assertEquals(ids, setOf(paywallId1))
    }

    @Test
    fun test_getActiveTreatmentPaywallIds_holdout() {
        val paywallId1 = "abc"
        val experiment1 = "def"
        val experiment2 = "sdf"
        val paywallId2 = "wer"

        val triggers =
            setOf(
                Trigger
                    .stub()
                    .apply {
                        rules =
                            listOf(
                                TriggerRule
                                    .stub()
                                    .apply {
                                        this.experimentId = experiment1
                                    },
                            )
                    },
            )
        val confirmedAssignments: Map<String, Experiment.Variant> =
            mapOf(
                experiment1 to
                    Experiment.Variant(
                        experiment2,
                        Experiment.Variant.VariantType.HOLDOUT,
                        paywallId1,
                    ),
                experiment2 to
                    Experiment.Variant(
                        experiment2,
                        Experiment.Variant.VariantType.TREATMENT,
                        paywallId2,
                    ),
            )
        val ids =
            ConfigLogic.getActiveTreatmentPaywallIds(
                forTriggers = triggers,
                confirmedAssignments = confirmedAssignments,
                unconfirmedAssignments = emptyMap(),
            )
        assertTrue(ids.isEmpty())
    }

    @Test
    fun test_getActiveTreatmentPaywallIds_confirmedAndUnconfirmedAssignments() {
        val paywallId1 = "abc"
        val experiment1 = "def"
        val experiment2 = "sdf"
        val paywallId2 = "wer"

        val triggers =
            setOf(
                Trigger
                    .stub()
                    .apply {
                        rules =
                            listOf(
                                TriggerRule
                                    .stub()
                                    .apply {
                                        this.experimentId = experiment1
                                    },
                            )
                    },
            )
        val confirmedAssignments: Map<String, Experiment.Variant> =
            mapOf(
                experiment1 to
                    Experiment.Variant(
                        experiment2,
                        Experiment.Variant.VariantType.TREATMENT,
                        paywallId1,
                    ),
            )
        val unconfirmedAssignments: Map<String, Experiment.Variant> =
            mapOf(
                experiment2 to
                    Experiment.Variant(
                        experiment2,
                        Experiment.Variant.VariantType.TREATMENT,
                        paywallId2,
                    ),
            )
        val ids =
            ConfigLogic.getActiveTreatmentPaywallIds(
                forTriggers = triggers,
                confirmedAssignments = confirmedAssignments,
                unconfirmedAssignments = unconfirmedAssignments,
            )
        assertEquals(ids, setOf(paywallId1))
    }

    @Test
    fun test_getActiveTreatmentPaywallIds_confirmedAndUnconfirmedAssignments_removeDuplicateRules() {
        val paywallId1 = "abc"
        val experiment1 = "def"
        val experiment2 = "sdf"
        val paywallId2 = "wer"

        val triggers =
            setOf(
                Trigger
                    .stub()
                    .apply {
                        rules =
                            listOf(
                                TriggerRule
                                    .stub()
                                    .apply {
                                        this.experimentGroupId = "abc"
                                        this.experimentId = experiment1
                                    },
                            )
                    },
                Trigger
                    .stub()
                    .apply {
                        rules =
                            listOf(
                                TriggerRule
                                    .stub()
                                    .apply {
                                        this.experimentId = experiment1
                                        this.experimentGroupId = "abc"
                                    },
                            )
                    },
            )
        val confirmedAssignments: Map<String, Experiment.Variant> =
            mapOf(
                experiment1 to
                    Experiment.Variant(
                        experiment2,
                        Experiment.Variant.VariantType.TREATMENT,
                        paywallId1,
                    ),
            )
        val unconfirmedAssignments: Map<String, Experiment.Variant> =
            mapOf(
                experiment2 to
                    Experiment.Variant(
                        experiment2,
                        Experiment.Variant.VariantType.TREATMENT,
                        paywallId2,
                    ),
            )
        val ids =
            ConfigLogic.getActiveTreatmentPaywallIds(
                forTriggers = triggers,
                confirmedAssignments = confirmedAssignments,
                unconfirmedAssignments = unconfirmedAssignments,
            )
        assertEquals(ids, setOf(paywallId1))
    }

    @Test
    fun test_getTriggerDictionary() {
        val firstTrigger = Trigger.stub().apply { eventName = "abc" }
        val secondTrigger = Trigger.stub().apply { eventName = "def" }
        val triggers = setOf(firstTrigger, secondTrigger)
        val dictionary = ConfigLogic.getTriggersByEventName(from = triggers)
        assertEquals(dictionary["abc"], firstTrigger)
        assertEquals(dictionary["def"], secondTrigger)
    }

    @Test
    fun test_filterTriggers_noTriggers() {
        val disabled =
            PreloadingDisabled(
                all = true,
                triggers = setOf("app_open"),
            )
        val triggers =
            ConfigLogic.filterTriggers(
                emptySet(),
                disabled,
            )
        assertTrue(triggers.isEmpty())
    }

    @Test
    fun test_filterTriggers_disableAll() {
        val disabled =
            PreloadingDisabled(
                all = true,
                triggers = emptySet(),
            )
        val triggers: Set<Trigger> =
            setOf(
                Trigger("app_open", emptyList()),
                Trigger("campaign_trigger", listOf(TriggerRule.stub())),
            )
        val filteredTriggers =
            ConfigLogic.filterTriggers(
                triggers,
                disabled,
            )
        assertTrue(filteredTriggers.isEmpty())
    }

    @Test
    fun test_filterTriggers_disableSome() {
        val disabled =
            PreloadingDisabled(
                all = false,
                triggers = setOf("app_open"),
            )
        val triggers: Set<Trigger> =
            setOf(
                Trigger("app_open", emptyList()),
                Trigger("campaign_trigger", listOf(TriggerRule.stub())),
            )
        val filteredTriggers =
            ConfigLogic.filterTriggers(
                triggers,
                disabled,
            )
        assertEquals(1, filteredTriggers.size)
        assertEquals("campaign_trigger", filteredTriggers.first().eventName)
    }

    @Test
    fun test_filterTriggers_disableNone() {
        val disabled =
            PreloadingDisabled(
                all = false,
                triggers = emptySet(),
            )
        val triggers: Set<Trigger> =
            setOf(
                Trigger("app_open", emptyList()),
                Trigger("campaign_trigger", listOf(TriggerRule.stub())),
            )
        val filteredTriggers =
            ConfigLogic.filterTriggers(
                triggers,
                disabled,
            )
        assertEquals(2, filteredTriggers.size)
    }
}
