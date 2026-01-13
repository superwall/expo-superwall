package com.superwall.sdk.review

import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewErrorTest {
    @Test
    fun `ReviewError base class can be created with message and cause`() =
        Given("a message and a cause throwable") {
            val message = "Test error message"
            val cause = RuntimeException("Root cause")

            When("creating a GenericError") {
                val error = ReviewError.GenericError(message, cause)

                Then("it has the correct message and cause") {
                    assertEquals(message, error.message)
                    assertEquals(cause, error.cause)
                }
            }
        }

    @Test
    fun `ReviewError base class can be created with just message`() =
        Given("just a message") {
            val message = "Test error message"

            When("creating a GenericError with no cause") {
                val error = ReviewError.GenericError(message)

                Then("it has the message and null cause") {
                    assertEquals(message, error.message)
                    assertNull(error.cause)
                }
            }
        }

    @Test
    fun `RequestFlowError holds error code and cause`() =
        Given("an error code and cause") {
            val errorCode = 404
            val cause = RuntimeException("Request failed")

            When("creating a RequestFlowError") {
                val error = ReviewError.RequestFlowError(errorCode, cause)

                Then("it has the correct error code, message, and cause") {
                    assertEquals(errorCode, error.errorCode)
                    assertTrue(error.message!!.contains("404"))
                    assertEquals(cause, error.cause)
                }
            }
        }

    @Test
    fun `RequestFlowError can be created without cause`() =
        Given("just an error code") {
            val errorCode = 500

            When("creating a RequestFlowError without cause") {
                val error = ReviewError.RequestFlowError(errorCode)

                Then("it has the error code and no cause") {
                    assertEquals(errorCode, error.errorCode)
                    assertNull(error.cause)
                }
            }
        }

    @Test
    fun `RequestFlowError message includes error code`() =
        Given("an error code") {
            val errorCode = 123

            When("creating a RequestFlowError") {
                val error = ReviewError.RequestFlowError(errorCode)

                Then("the message includes the error code") {
                    assertTrue(error.message!!.contains("123"))
                    assertTrue(error.message!!.contains("Failed to request review flow"))
                }
            }
        }

    @Test
    fun `LaunchFlowError can be created with cause`() =
        Given("a cause throwable") {
            val cause = RuntimeException("Launch failed")

            When("creating a LaunchFlowError") {
                val error = ReviewError.LaunchFlowError(cause)

                Then("it has the correct message and cause") {
                    assertEquals("Failed to launch review flow", error.message)
                    assertEquals(cause, error.cause)
                }
            }
        }

    @Test
    fun `LaunchFlowError can be created without cause`() =
        Given("no parameters") {
            When("creating a LaunchFlowError without cause") {
                val error = ReviewError.LaunchFlowError()

                Then("it has the message and null cause") {
                    assertEquals("Failed to launch review flow", error.message)
                    assertNull(error.cause)
                }
            }
        }

    @Test
    fun `PlayServicesUnavailable is a singleton object`() =
        Given("the PlayServicesUnavailable object") {
            When("accessing it") {
                val error = ReviewError.PlayServicesUnavailable

                Then("it has the correct message") {
                    assertEquals("Google Play Services not available", error.message)
                    assertNull(error.cause)
                }
            }
        }

    @Test
    fun `PlayServicesUnavailable is consistent reference`() =
        Given("multiple references to PlayServicesUnavailable") {
            When("comparing them") {
                val error1 = ReviewError.PlayServicesUnavailable
                val error2 = ReviewError.PlayServicesUnavailable

                Then("they are the same object") {
                    assertTrue(error1 === error2)
                }
            }
        }

    @Test
    fun `GenericError holds custom message and cause`() =
        Given("a custom message and cause") {
            val message = "Custom error occurred"
            val cause = IllegalStateException("Bad state")

            When("creating a GenericError") {
                val error = ReviewError.GenericError(message, cause)

                Then("it has the correct message and cause") {
                    assertEquals(message, error.message)
                    assertEquals(cause, error.cause)
                }
            }
        }

    @Test
    fun `GenericError can be created with just message`() =
        Given("just a message") {
            val message = "Something went wrong"

            When("creating a GenericError") {
                val error = ReviewError.GenericError(message)

                Then("it has the message and no cause") {
                    assertEquals(message, error.message)
                    assertNull(error.cause)
                }
            }
        }

    @Test
    fun `All ReviewError types are Exception subclasses`() =
        Given("different ReviewError types") {
            When("checking their inheritance") {
                val requestError = ReviewError.RequestFlowError(100)
                val launchError = ReviewError.LaunchFlowError()
                val playServicesError = ReviewError.PlayServicesUnavailable
                val genericError = ReviewError.GenericError("test")

                Then("they are all Exceptions") {
                    assertTrue(requestError is Exception)
                    assertTrue(launchError is Exception)
                    assertTrue(playServicesError is Exception)
                    assertTrue(genericError is Exception)
                }
            }
        }

    @Test
    fun `ReviewError types can be caught as ReviewError`() =
        Given("different ReviewError instances") {
            val errors: List<ReviewError> =
                listOf(
                    ReviewError.RequestFlowError(1),
                    ReviewError.LaunchFlowError(RuntimeException()),
                    ReviewError.PlayServicesUnavailable,
                    ReviewError.GenericError("test"),
                )

            When("processing them as ReviewError") {
                Then("all are ReviewError instances") {
                    errors.forEach { error ->
                        assertTrue(error is ReviewError)
                        assertNotNull(error.message)
                    }
                }
            }
        }
}
