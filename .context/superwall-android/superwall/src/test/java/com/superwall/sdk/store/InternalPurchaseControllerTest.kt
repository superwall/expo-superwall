package com.superwall.sdk.store

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.delegate.PurchaseResult
import com.superwall.sdk.delegate.RestorationResult
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.delegate.subscription_controller.PurchaseControllerJava
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InternalPurchaseControllerTest {
    private val mockContext = mockk<Context>()
    private val mockActivity = mockk<Activity>()
    private val mockProductDetails = mockk<ProductDetails>()

    @Test
    fun `test hasInternalPurchaseController returns true when using AutomaticPurchaseController`() =
        runTest {
            Given("an InternalPurchaseController with AutomaticPurchaseController") {
                val automaticController = mockk<AutomaticPurchaseController>()
                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = automaticController,
                        javaPurchaseController = null,
                        context = mockContext,
                    )

                When("checking hasInternalPurchaseController") {
                    val hasInternal = controller.hasInternalPurchaseController

                    Then("it should return true") {
                        assertTrue(hasInternal)
                    }

                    And("hasExternalPurchaseController should return false") {
                        assertFalse(controller.hasExternalPurchaseController)
                    }
                }
            }
        }

    @Test
    fun `test hasInternalPurchaseController returns false with custom controller`() =
        runTest {
            Given("an InternalPurchaseController with custom PurchaseController") {
                val customController = mockk<PurchaseController>()
                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = customController,
                        javaPurchaseController = null,
                        context = mockContext,
                    )

                When("checking hasInternalPurchaseController") {
                    val hasInternal = controller.hasInternalPurchaseController

                    Then("it should return false") {
                        assertFalse(hasInternal)
                    }

                    And("hasExternalPurchaseController should return true") {
                        assertTrue(controller.hasExternalPurchaseController)
                    }
                }
            }
        }

    @Test
    fun `test hasInternalPurchaseController returns false with no controller`() =
        runTest {
            Given("an InternalPurchaseController with no controllers") {
                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = null,
                        javaPurchaseController = null,
                        context = mockContext,
                    )

                When("checking hasInternalPurchaseController") {
                    val hasInternal = controller.hasInternalPurchaseController

                    Then("it should return false") {
                        assertFalse(hasInternal)
                    }

                    And("hasExternalPurchaseController should return true") {
                        assertTrue(controller.hasExternalPurchaseController)
                    }
                }
            }
        }

    @Test
    fun `test purchase delegates to Kotlin controller`() =
        runTest {
            Given("an InternalPurchaseController with Kotlin controller") {
                val kotlinController = mockk<PurchaseController>()
                val expectedResult = PurchaseResult.Purchased()

                coEvery {
                    kotlinController.purchase(
                        mockActivity,
                        mockProductDetails,
                        "base_plan",
                        "offer",
                    )
                } returns expectedResult

                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = kotlinController,
                        javaPurchaseController = null,
                        context = mockContext,
                    )

                When("calling purchase") {
                    val result = controller.purchase(mockActivity, mockProductDetails, "base_plan", "offer")

                    Then("it should delegate to Kotlin controller and return the result") {
                        assertEquals(expectedResult, result)
                        coVerify(exactly = 1) {
                            kotlinController.purchase(
                                mockActivity,
                                mockProductDetails,
                                "base_plan",
                                "offer",
                            )
                        }
                    }
                }
            }
        }

    @Test
    fun `test purchase delegates to Java controller`() =
        runTest {
            Given("an InternalPurchaseController with Java controller") {
                val javaController = mockk<PurchaseControllerJava>()
                val expectedResult = PurchaseResult.Purchased()
                val callbackSlot = slot<(PurchaseResult) -> Unit>()

                every {
                    javaController.purchase(
                        mockProductDetails,
                        "base_plan",
                        "offer",
                        capture(callbackSlot),
                    )
                } answers {
                    callbackSlot.captured(expectedResult)
                }

                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = null,
                        javaPurchaseController = javaController,
                        context = mockContext,
                    )

                When("calling purchase") {
                    val result = controller.purchase(mockActivity, mockProductDetails, "base_plan", "offer")

                    Then("it should delegate to Java controller and return the result") {
                        assertEquals(expectedResult, result)
                    }
                }
            }
        }

    @Test
    fun `test purchase returns Cancelled when no controller is provided`() =
        runTest {
            Given("an InternalPurchaseController with no controllers") {
                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = null,
                        javaPurchaseController = null,
                        context = mockContext,
                    )

                When("calling purchase") {
                    val result = controller.purchase(mockActivity, mockProductDetails, "base_plan", "offer")

                    Then("it should return Cancelled") {
                        assertTrue(result is PurchaseResult.Cancelled)
                    }
                }
            }
        }

    @Test
    fun `test restorePurchases delegates to Kotlin controller`() =
        runTest {
            Given("an InternalPurchaseController with Kotlin controller") {
                val kotlinController = mockk<PurchaseController>()
                val expectedResult = RestorationResult.Restored()

                coEvery { kotlinController.restorePurchases() } returns expectedResult

                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = kotlinController,
                        javaPurchaseController = null,
                        context = mockContext,
                    )

                When("calling restorePurchases") {
                    val result = controller.restorePurchases()

                    Then("it should delegate to Kotlin controller and return the result") {
                        assertEquals(expectedResult, result)
                        coVerify(exactly = 1) { kotlinController.restorePurchases() }
                    }
                }
            }
        }

    @Test
    fun `test restorePurchases delegates to Java controller with success`() =
        runTest {
            Given("an InternalPurchaseController with Java controller") {
                val javaController = mockk<PurchaseControllerJava>()
                val expectedResult = RestorationResult.Restored()
                val callbackSlot = slot<(RestorationResult, Throwable?) -> Unit>()

                every {
                    javaController.restorePurchases(capture(callbackSlot))
                } answers {
                    callbackSlot.captured(expectedResult, null)
                }

                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = null,
                        javaPurchaseController = javaController,
                        context = mockContext,
                    )

                When("calling restorePurchases") {
                    val result = controller.restorePurchases()

                    Then("it should delegate to Java controller and return the result") {
                        assertEquals(expectedResult, result)
                    }
                }
            }
        }

    @Test
    fun `test restorePurchases delegates to Java controller with error`() =
        runTest {
            Given("an InternalPurchaseController with Java controller that returns error") {
                val javaController = mockk<PurchaseControllerJava>()
                val error = RuntimeException("Restore failed")
                val callbackSlot = slot<(RestorationResult, Throwable?) -> Unit>()

                every {
                    javaController.restorePurchases(capture(callbackSlot))
                } answers {
                    callbackSlot.captured(RestorationResult.Restored(), error)
                }

                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = null,
                        javaPurchaseController = javaController,
                        context = mockContext,
                    )

                When("calling restorePurchases") {
                    val result = controller.restorePurchases()

                    Then("it should return Failed with the error") {
                        assertTrue(result is RestorationResult.Failed)
                        assertEquals(error, (result as RestorationResult.Failed).error)
                    }
                }
            }
        }

    @Test
    fun `test restorePurchases returns Failed when no controller is provided`() =
        runTest {
            Given("an InternalPurchaseController with no controllers") {
                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = null,
                        javaPurchaseController = null,
                        context = mockContext,
                    )

                When("calling restorePurchases") {
                    val result = controller.restorePurchases()

                    Then("it should return Failed with null error") {
                        assertTrue(result is RestorationResult.Failed)
                        assertEquals(null, (result as RestorationResult.Failed).error)
                    }
                }
            }
        }

    @Test
    fun `test Kotlin controller takes precedence over Java controller`() =
        runTest {
            Given("an InternalPurchaseController with both controllers") {
                val kotlinController = mockk<PurchaseController>()
                val javaController = mockk<PurchaseControllerJava>()
                val kotlinResult = PurchaseResult.Purchased()

                coEvery {
                    kotlinController.purchase(
                        mockActivity,
                        mockProductDetails,
                        null,
                        null,
                    )
                } returns kotlinResult

                val controller =
                    InternalPurchaseController(
                        kotlinPurchaseController = kotlinController,
                        javaPurchaseController = javaController,
                        context = mockContext,
                    )

                When("calling purchase") {
                    val result = controller.purchase(mockActivity, mockProductDetails, null, null)

                    Then("it should use Kotlin controller and not call Java controller") {
                        assertEquals(kotlinResult, result)
                        coVerify(exactly = 1) {
                            kotlinController.purchase(
                                mockActivity,
                                mockProductDetails,
                                null,
                                null,
                            )
                        }
                        coVerify(exactly = 0) {
                            javaController.purchase(
                                any(),
                                any(),
                                any(),
                                any(),
                            )
                        }
                    }
                }
            }
        }
}
