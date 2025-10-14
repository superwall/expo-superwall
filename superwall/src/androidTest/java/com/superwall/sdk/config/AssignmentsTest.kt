package com.superwall.sdk.config

import Given
import Then
import When
import androidx.test.platform.app.InstrumentationRegistry
import com.superwall.sdk.misc.Either
import com.superwall.sdk.models.assignment.Assignment
import com.superwall.sdk.models.assignment.ConfirmableAssignment
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.network.Network
import com.superwall.sdk.storage.LocalStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class AssignmentsTest {
    private lateinit var storage: LocalStorage
    private lateinit var network: Network

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        storage = mockk(relaxed = true)
        network = mockk(relaxed = true)
    }

    @Test
    fun test_choosePaywallVariants() =
        runTest(timeout = 5.minutes) {
            val assignments = Assignments(storage, network, ioScope = this)
            coEvery { network.confirmAssignments(any()) } returns Either.Success(Unit)
            Given("We have a set of triggers") {
                val triggers =
                    setOf(
                        Trigger.stub(),
                        Trigger.stub(),
                    )

                When("We choose paywall variants") {
                    assignments.choosePaywallVariants(triggers)

                    Then("The assignments should be updated") {
                        verify { storage.getConfirmedAssignments() }
                        verify { storage.saveConfirmedAssignments(any()) }
                    }
                }
            }
        }

    @Test
    fun test_getAssignments() =
        runTest(timeout = 5.minutes) {
            val assignments = Assignments(storage, network, ioScope = this)
            Given("We have a set of triggers and server assignments") {
                val triggers =
                    setOf(
                        Trigger.stub(),
                        Trigger.stub(),
                    )
                val serverAssignments =
                    listOf(
                        Assignment("exp1", "var1"),
                        Assignment("exp2", "var2"),
                    )
                coEvery { network.getAssignments() } returns Either.Success(serverAssignments)

                When("We get assignments") {
                    assignments.getAssignments(triggers)

                    Then("The assignments should be updated with server data") {
                        verify { storage.getConfirmedAssignments() }
                        verify { storage.saveConfirmedAssignments(any()) }
                    }
                }
            }
        }

    @Test
    fun test_confirmAssignment() =
        runTest(timeout = 5.minutes) {
            val assignments = Assignments(storage, network, ioScope = this)
            coEvery { network.confirmAssignments(any()) } returns Either.Success(Unit)
            coEvery { network.confirmAssignments(any()) } returns Either.Success(Unit)

            Given("We have a confirmable assignment") {
                val assignment =
                    ConfirmableAssignment(
                        experimentId = "exp1",
                        variant =
                            Experiment.Variant(
                                id = "var1",
                                type = Experiment.Variant.VariantType.TREATMENT,
                                paywallId = "pw1",
                            ),
                    )

                When("We confirm the assignment") {
                    assignments.confirmAssignment(assignment)
                    advanceUntilIdle()
                    Then("The assignment should be confirmed and saved") {
                        verify { storage.getConfirmedAssignments() }
                        verify { storage.saveConfirmedAssignments(any()) }
                        coVerify { network.confirmAssignments(any()) }
                    }
                }
            }
        }

    @Test
    fun test_reset() =
        runTest(timeout = 5.minutes) {
            Given("We have some unconfirmed assignments") {
                val assignments =
                    Assignments(
                        storage,
                        network,
                        ioScope = this@runTest,
                        mapOf(
                            "exp1" to
                                Experiment.Variant(
                                    id = "var1",
                                    type = Experiment.Variant.VariantType.TREATMENT,
                                    paywallId = "pw1",
                                ),
                        ),
                    )
                When("We reset the assignments") {
                    assignments.reset()

                    Then("The unconfirmed assignments should be cleared") {
                        assertTrue(assignments.unconfirmedAssignments.isEmpty())
                    }
                }
            }
        }
}
