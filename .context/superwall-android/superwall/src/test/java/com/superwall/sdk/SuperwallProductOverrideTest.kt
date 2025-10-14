package com.superwall.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperwallProductOverrideTest {
    @Test
    fun `test Superwall overrideProductsByName property initialization`() {
        Given("a new Superwall instance") {
            val initialOverrides = emptyMap<String, String>()

            When("accessing overrideProductsByName property") {
                val overrides = initialOverrides

                Then("it should be empty by default") {
                    assertTrue(overrides.isEmpty())
                }
            }
        }
    }

    @Test
    fun `test Superwall overrideProductsByName property assignment`() {
        Given("product override mappings") {
            val overrides =
                mapOf(
                    "primary" to "com.example.premium_monthly",
                    "secondary" to "com.example.premium_annual",
                )

            When("assigning to overrideProductsByName") {
                val assignedOverrides = overrides

                Then("it should contain the assigned values") {
                    assertEquals(2, assignedOverrides.size)
                    assertEquals("com.example.premium_monthly", assignedOverrides["primary"])
                    assertEquals("com.example.premium_annual", assignedOverrides["secondary"])
                }
            }
        }
    }

    @Test
    fun `test Superwall overrideProductsByName property with single override`() {
        Given("a single product override") {
            val overrides = mapOf("primary" to "com.example.premium_monthly")

            When("accessing the override") {
                val primaryOverride = overrides["primary"]

                Then("it should return the correct product identifier") {
                    assertEquals("com.example.premium_monthly", primaryOverride)
                }
            }
        }
    }

    @Test
    fun `test Superwall overrideProductsByName property with multiple overrides`() {
        Given("multiple product overrides") {
            val overrides =
                mapOf(
                    "primary" to "com.example.premium_monthly",
                    "secondary" to "com.example.premium_annual",
                    "tertiary" to "com.example.premium_weekly",
                )

            When("accessing all overrides") {
                val keys = overrides.keys
                val values = overrides.values

                Then("it should contain all product names and identifiers") {
                    assertEquals(3, overrides.size)
                    assertTrue(keys.contains("primary"))
                    assertTrue(keys.contains("secondary"))
                    assertTrue(keys.contains("tertiary"))
                    assertTrue(values.contains("com.example.premium_monthly"))
                    assertTrue(values.contains("com.example.premium_annual"))
                    assertTrue(values.contains("com.example.premium_weekly"))
                }
            }
        }
    }

    @Test
    fun `test Superwall overrideProductsByName property replacement`() {
        Given("initial product overrides") {
            var overrides = mapOf("primary" to "com.example.initial_product")

            When("replacing with new overrides") {
                overrides =
                    mapOf(
                        "primary" to "com.example.new_product",
                        "secondary" to "com.example.additional_product",
                    )

                Then("it should contain the new overrides") {
                    assertEquals(2, overrides.size)
                    assertEquals("com.example.new_product", overrides["primary"])
                    assertEquals("com.example.additional_product", overrides["secondary"])
                }
            }
        }
    }
}
