package com.superwall.sdk.paywall.presentation.rule_logic.cel

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.assertTrue
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.PassableValue
import com.superwall.sdk.storage.core_data.CoreDataManager
import com.superwall.supercel.ResultCallback
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CELHostContextTest {
    private lateinit var json: Json
    private lateinit var storage: CoreDataManager
    private lateinit var superscriptHostContext: SuperscriptHostContext

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setup() {
        json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                classDiscriminatorMode = ClassDiscriminatorMode.ALL_JSON_OBJECTS
                classDiscriminator = "type"
            }
        storage = mockk(relaxed = true)
        superscriptHostContext = SuperscriptHostContext(json, storage)
    }

    @Test
    fun test_computedProperty_daysSince_withValidEvent() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val eventName = "user_registered"
                val daysSinceValue = 5
                val args = json.encodeToString(listOf(PassableValue.StringValue(eventName)))
                val callback = mockk<ResultCallback>(relaxed = true)
                val computedPropertyRequestSlot = slot<ComputedPropertyRequest>()

                coEvery { storage.getComputedPropertySinceEvent(null, capture(computedPropertyRequestSlot)) } returns daysSinceValue

                When("computedProperty is called with daysSince function") {
                    superscriptHostContext.computedProperty("daysSince", args, callback)

                    Then("it should query storage with correct parameters") {
                        coVerify {
                            storage.getComputedPropertySinceEvent(
                                null,
                                any<ComputedPropertyRequest>(),
                            )
                        }
                        assertEquals(
                            ComputedPropertyRequest.ComputedPropertyRequestType.DAYS_SINCE,
                            computedPropertyRequestSlot.captured.type,
                        )
                        assertEquals(eventName, computedPropertyRequestSlot.captured.eventName)
                    }

                    And("it should return the correct PassableValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(daysSinceValue))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_minutesSince_withValidEvent() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val eventName = "button_clicked"
                val minutesSinceValue = 120
                val args = json.encodeToString(listOf(PassableValue.StringValue(eventName)))
                val callback = mockk<ResultCallback>(relaxed = true)
                val computedPropertyRequestSlot = slot<ComputedPropertyRequest>()

                coEvery { storage.getComputedPropertySinceEvent(null, capture(computedPropertyRequestSlot)) } returns minutesSinceValue

                When("computedProperty is called with minutesSince function") {
                    superscriptHostContext.computedProperty("minutesSince", args, callback)

                    Then("it should query storage with correct parameters") {
                        coVerify {
                            storage.getComputedPropertySinceEvent(
                                null,
                                any<ComputedPropertyRequest>(),
                            )
                        }
                        assertEquals(
                            ComputedPropertyRequest.ComputedPropertyRequestType.MINUTES_SINCE,
                            computedPropertyRequestSlot.captured.type,
                        )
                        assertEquals(eventName, computedPropertyRequestSlot.captured.eventName)
                    }

                    And("it should return the correct PassableValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(minutesSinceValue))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_hoursSince_withValidEvent() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val eventName = "session_start"
                val hoursSinceValue = 48
                val args = json.encodeToString(listOf(PassableValue.StringValue(eventName)))
                val callback = mockk<ResultCallback>(relaxed = true)
                val computedPropertyRequestSlot = slot<ComputedPropertyRequest>()

                coEvery { storage.getComputedPropertySinceEvent(null, capture(computedPropertyRequestSlot)) } returns hoursSinceValue

                When("computedProperty is called with hoursSince function") {
                    superscriptHostContext.computedProperty("hoursSince", args, callback)

                    Then("it should query storage with correct parameters") {
                        coVerify {
                            storage.getComputedPropertySinceEvent(
                                null,
                                any<ComputedPropertyRequest>(),
                            )
                        }
                        assertEquals(
                            ComputedPropertyRequest.ComputedPropertyRequestType.HOURS_SINCE,
                            computedPropertyRequestSlot.captured.type,
                        )
                        assertEquals(eventName, computedPropertyRequestSlot.captured.eventName)
                    }

                    And("it should return the correct PassableValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(hoursSinceValue))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_monthsSince_withValidEvent() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val eventName = "first_purchase"
                val monthsSinceValue = 3
                val args = json.encodeToString(listOf(PassableValue.StringValue(eventName)))
                val callback = mockk<ResultCallback>(relaxed = true)
                val computedPropertyRequestSlot = slot<ComputedPropertyRequest>()

                coEvery { storage.getComputedPropertySinceEvent(null, capture(computedPropertyRequestSlot)) } returns monthsSinceValue

                When("computedProperty is called with monthsSince function") {
                    superscriptHostContext.computedProperty("monthsSince", args, callback)

                    Then("it should query storage with correct parameters") {
                        coVerify {
                            storage.getComputedPropertySinceEvent(
                                null,
                                any<ComputedPropertyRequest>(),
                            )
                        }
                        assertEquals(
                            ComputedPropertyRequest.ComputedPropertyRequestType.MONTHS_SINCE,
                            computedPropertyRequestSlot.captured.type,
                        )
                        assertEquals(eventName, computedPropertyRequestSlot.captured.eventName)
                    }

                    And("it should return the correct PassableValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(monthsSinceValue))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_placementsSinceInstall_withValidPlacement() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val placementName = "main_paywall"
                val placementCount = 10
                val args = json.encodeToString(listOf(PassableValue.StringValue(placementName)))
                val callback = mockk<ResultCallback>(relaxed = true)

                coEvery { storage.countEventsByNameInPeriod(placementName, any(), any()) } returns placementCount

                When("computedProperty is called with placementsSinceInstall function") {
                    superscriptHostContext.computedProperty("placementsSinceInstall", args, callback)

                    Then("it should query storage with correct parameters") {
                        coVerify {
                            storage.countEventsByNameInPeriod(placementName, any(), any())
                        }
                    }

                    And("it should return the correct PassableValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(placementCount))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_placementsInDay_withValidPlacement() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val placementName = "daily_paywall"
                val placementCount = 3
                val args = json.encodeToString(listOf(PassableValue.StringValue(placementName)))
                val callback = mockk<ResultCallback>(relaxed = true)

                coEvery { storage.countEventsByNameInPeriod(placementName, any(), any()) } returns placementCount

                When("computedProperty is called with placementsInDay function") {
                    superscriptHostContext.computedProperty("placementsInDay", args, callback)

                    Then("it should query storage with correct parameters") {
                        coVerify {
                            storage.countEventsByNameInPeriod(placementName, any(), any())
                        }
                    }

                    And("it should return the correct PassableValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(placementCount))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_placementsInHour_withValidPlacement() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val placementName = "hourly_paywall"
                val placementCount = 2
                val args = json.encodeToString(listOf(PassableValue.StringValue(placementName)))
                val callback = mockk<ResultCallback>(relaxed = true)

                coEvery { storage.countEventsByNameInPeriod(placementName, any(), any()) } returns placementCount

                When("computedProperty is called with placementsInHour function") {
                    superscriptHostContext.computedProperty("placementsInHour", args, callback)

                    Then("it should query storage with correct parameters") {
                        coVerify {
                            storage.countEventsByNameInPeriod(placementName, any(), any())
                        }
                    }

                    And("it should return the correct PassableValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(placementCount))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_withUnknownProperty() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val args = json.encodeToString(listOf(PassableValue.StringValue("test_event")))
                val callback = mockk<ResultCallback>(relaxed = true)

                When("computedProperty is called with unknown property name") {
                    superscriptHostContext.computedProperty("unknownProperty", args, callback)

                    Then("it should return false without querying storage") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.BoolValue(false))
                        assertEquals(expectedResult, resultSlot.captured)
                    }

                    And("it should not query storage") {
                        coVerify(exactly = 0) { storage.getComputedPropertySinceEvent(any(), any()) }
                        coVerify(exactly = 0) { storage.countTriggerRuleOccurrences(any()) }
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_withNullResult() =
        runTest {
            Given("a CELHostContext with storage returning null") {
                val eventName = "nonexistent_event"
                val args = json.encodeToString(listOf(PassableValue.StringValue(eventName)))
                val callback = mockk<ResultCallback>(relaxed = true)

                coEvery { storage.getComputedPropertySinceEvent(null, any()) } returns null

                When("computedProperty is called with daysSince function") {
                    superscriptHostContext.computedProperty("daysSince", args, callback)

                    Then("it should return NullValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(0))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_deviceProperty_daysSince_withValidEvent() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val eventName = "device_registered"
                val daysSinceValue = 7
                val args = json.encodeToString(listOf(PassableValue.StringValue(eventName)))
                val callback = mockk<ResultCallback>(relaxed = true)
                val computedPropertyRequestSlot = slot<ComputedPropertyRequest>()

                coEvery { storage.getComputedPropertySinceEvent(null, capture(computedPropertyRequestSlot)) } returns daysSinceValue

                When("deviceProperty is called with daysSince function") {
                    superscriptHostContext.deviceProperty("daysSince", args, callback)

                    Then("it should query storage with correct parameters") {
                        coVerify {
                            storage.getComputedPropertySinceEvent(
                                null,
                                any<ComputedPropertyRequest>(),
                            )
                        }
                        assertEquals(
                            ComputedPropertyRequest.ComputedPropertyRequestType.DAYS_SINCE,
                            computedPropertyRequestSlot.captured.type,
                        )
                        assertEquals(eventName, computedPropertyRequestSlot.captured.eventName)
                    }

                    And("it should return the correct PassableValue as IntValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(daysSinceValue))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_deviceProperty_withUnknownProperty() =
        runTest {
            Given("a CELHostContext with storage configured") {
                val args = json.encodeToString(listOf(PassableValue.StringValue("test_event")))
                val callback = mockk<ResultCallback>(relaxed = true)

                When("deviceProperty is called with unknown property name") {
                    superscriptHostContext.deviceProperty("unknownProperty", args, callback)

                    Then("it should return false without querying storage") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.BoolValue(false))
                        assertEquals(expectedResult, resultSlot.captured)
                    }

                    And("it should not query storage") {
                        coVerify(exactly = 0) { storage.getComputedPropertySinceEvent(any(), any()) }
                    }
                }
            }
        }

    @Test
    fun test_deviceProperty_withNullResult() =
        runTest {
            Given("a CELHostContext with storage returning null") {
                val eventName = "nonexistent_device_event"
                val args = json.encodeToString(listOf(PassableValue.StringValue(eventName)))
                val callback = mockk<ResultCallback>(relaxed = true)

                coEvery { storage.getComputedPropertySinceEvent(null, any()) } returns null

                When("deviceProperty is called with hoursSince function") {
                    superscriptHostContext.deviceProperty("hoursSince", args, callback)

                    Then("it should return IntValue with 0") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(0))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_availableComputedProperties_containsExpectedProperties() =
        runTest {
            Given("the CELHostContext companion object") {
                When("accessing availableComputedProperties") {
                    val properties = SuperscriptHostContext.availableComputedProperties

                    Then("it should contain all expected computed properties") {
                        assertTrue(properties.contains("daysSince"))
                        assertTrue(properties.contains("minutesSince"))
                        assertTrue(properties.contains("hoursSince"))
                        assertTrue(properties.contains("monthsSince"))
                        assertTrue(properties.contains("placementsInHour"))
                        assertTrue(properties.contains("placementsInDay"))
                        assertTrue(properties.contains("placementsInWeek"))
                        assertTrue(properties.contains("placementsInMonth"))
                        assertTrue(properties.contains("placementsSinceInstall"))
                    }

                    And("it should have exactly 9 properties") {
                        assertEquals(16, properties.size)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_withComplexArgs() =
        runTest {
            Given("a CELHostContext with complex argument structure") {
                val eventName = "complex_event"
                val extraArg = PassableValue.IntValue(42)
                val args = json.encodeToString(listOf(PassableValue.StringValue(eventName), extraArg))
                val callback = mockk<ResultCallback>(relaxed = true)
                val daysSinceValue = 15

                coEvery { storage.getComputedPropertySinceEvent(null, any()) } returns daysSinceValue

                When("computedProperty is called with multiple arguments") {
                    superscriptHostContext.computedProperty("daysSince", args, callback)

                    Then("it should use the first argument as event name") {
                        val computedPropertyRequestSlot = slot<ComputedPropertyRequest>()
                        coVerify {
                            storage.getComputedPropertySinceEvent(null, capture(computedPropertyRequestSlot))
                        }
                        assertEquals(eventName, computedPropertyRequestSlot.captured.eventName)
                    }

                    And("it should return the correct result") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(daysSinceValue))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_placementsSinceInstall_withZeroResult() =
        runTest {
            Given("a CELHostContext with storage returning zero count") {
                val placementName = "unused_placement"
                val args = json.encodeToString(listOf(PassableValue.StringValue(placementName)))
                val callback = mockk<ResultCallback>(relaxed = true)

                coEvery { storage.countTriggerRuleOccurrences(any()) } returns 0

                When("computedProperty is called with placementsSinceInstall function") {
                    superscriptHostContext.computedProperty("placementsSinceInstall", args, callback)

                    Then("it should return IntValue with 0") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(0))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }

    @Test
    fun test_computedProperty_placementsSinceInstall_withValidCount() =
        runTest {
            Given("a CELHostContext with storage returning a valid count") {
                val placementName = "valid_placement"
                val placementCount = 15
                val args = json.encodeToString(listOf(PassableValue.StringValue(placementName)))
                val callback = mockk<ResultCallback>(relaxed = true)

                coEvery { storage.countEventsByNameInPeriod(placementName, any(), any()) } returns placementCount

                When("computedProperty is called with placementsSinceInstall function") {
                    superscriptHostContext.computedProperty("placementsSinceInstall", args, callback)

                    Then("it should return the correct IntValue") {
                        val resultSlot = slot<String>()
                        verify { callback.onResult(capture(resultSlot)) }

                        val expectedResult = json.encodeToString(PassableValue.IntValue(placementCount))
                        assertEquals(expectedResult, resultSlot.captured)
                    }
                }
            }
        }
}
