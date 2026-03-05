package com.superwall.sdk.paywall.view.webview.templating

import com.superwall.sdk.And
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.dependencies.VariablesFactory
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.CrossplatformProduct
import com.superwall.sdk.models.product.Offer
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.paywall.view.webview.templating.models.JsonVariables
import com.superwall.sdk.paywall.view.webview.templating.models.Variables
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class TemplateLogicTest {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    private class TestVariablesFactory(
        private val userAttributes: Map<String, Any?>,
        private val deviceAttributes: Map<String, Any?>,
    ) : VariablesFactory {
        override suspend fun makeJsonVariables(
            products: List<ProductVariable>?,
            computedPropertyRequests: List<ComputedPropertyRequest>,
            event: EventData?,
        ): JsonVariables =
            Variables(
                products = products,
                params = event?.parameters,
                userAttributes = userAttributes,
                templateDeviceDictionary = deviceAttributes,
            ).templated()
    }

    private fun parseOutput(result: String): JsonArray = json.parseToJsonElement(result).jsonArray

    private fun variablesFromResult(result: String): JsonObject = parseOutput(result)[1].jsonObject["variables"]!!.jsonObject

    // region Variables / attribute mapping

    @Test
    fun `user attributes appear in output`() =
        runTest {
            Given("a paywall with user attributes name and age") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = mapOf("name" to "John", "age" to 30),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("user map contains name and age") {
                    val variables = variablesFromResult(result)
                    val user = variables["user"]!!.jsonObject
                    assertEquals("John", user["name"]!!.jsonPrimitive.content)
                    assertEquals(30, user["age"]!!.jsonPrimitive.content.toInt())
                }
            }
        }

    @Test
    fun `device attributes appear in output`() =
        runTest {
            Given("a paywall with device attributes os and version") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = mapOf("os" to "android", "version" to "14"),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("device map contains os and version") {
                    val variables = variablesFromResult(result)
                    val device = variables["device"]!!.jsonObject
                    assertEquals("android", device["os"]!!.jsonPrimitive.content)
                    assertEquals("14", device["version"]!!.jsonPrimitive.content)
                }
            }
        }

    @Test
    fun `event params appear in output`() =
        runTest {
            Given("an event with source=home parameter") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()
                val event =
                    EventData(
                        name = "test_event",
                        parameters = mapOf("source" to "home"),
                        createdAt = Date(),
                    )

                val result =
                    When("getBase64EncodedTemplates is called with the event") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = event,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("params map contains source=home") {
                    val variables = variablesFromResult(result)
                    val params = variables["params"]!!.jsonObject
                    assertEquals("home", params["source"]!!.jsonPrimitive.content)
                }
            }
        }

    @Test
    fun `null event yields empty params`() =
        runTest {
            Given("no event data") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called with null event") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("params map is empty") {
                    val variables = variablesFromResult(result)
                    val params = variables["params"]!!.jsonObject
                    assertTrue(params.isEmpty())
                }
            }
        }

    @Test
    fun `product variables map to primary secondary tertiary`() =
        runTest {
            Given("a paywall with primary, secondary, and tertiary product variables") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val paywall =
                    Paywall.stub().apply {
                        productVariables =
                            listOf(
                                ProductVariable("primary", mapOf("price" to "9.99")),
                                ProductVariable("secondary", mapOf("price" to "19.99")),
                                ProductVariable("tertiary", mapOf("price" to "29.99")),
                            )
                    }

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("primary, secondary, tertiary contain the correct prices") {
                    val variables = variablesFromResult(result)
                    assertEquals("9.99", variables["primary"]!!.jsonObject["price"]!!.jsonPrimitive.content)

                    And("secondary has price 19.99") {
                        assertEquals("19.99", variables["secondary"]!!.jsonObject["price"]!!.jsonPrimitive.content)
                    }

                    And("tertiary has price 29.99") {
                        assertEquals("29.99", variables["tertiary"]!!.jsonObject["price"]!!.jsonPrimitive.content)
                    }
                }
            }
        }

    @Test
    fun `empty product variables yield empty primary secondary tertiary`() =
        runTest {
            Given("a paywall with no product variables") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val paywall =
                    Paywall.stub().apply {
                        productVariables = emptyList()
                    }

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("primary, secondary, tertiary are empty objects") {
                    val variables = variablesFromResult(result)
                    assertTrue(variables["primary"]!!.jsonObject.isEmpty())
                    assertTrue(variables["secondary"]!!.jsonObject.isEmpty())
                    assertTrue(variables["tertiary"]!!.jsonObject.isEmpty())
                }
            }
        }

    @Test
    fun `empty user and device attributes produce empty maps`() =
        runTest {
            Given("empty user and device attribute maps") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("user and device maps are empty") {
                    val variables = variablesFromResult(result)
                    assertTrue(variables["user"]!!.jsonObject.isEmpty())
                    assertTrue(variables["device"]!!.jsonObject.isEmpty())
                }
            }
        }

    // endregion

    // region TemplateLogic orchestration

    @Test
    fun `products mapped correctly from productItemsV3`() =
        runTest {
            Given("a paywall with a PlayStore product in productItemsV3") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val crossplatformProduct =
                    CrossplatformProduct(
                        compositeId = "com.test:monthly:sw-auto",
                        storeProduct =
                            CrossplatformProduct.StoreProduct.PlayStore(
                                productIdentifier = "com.test",
                                basePlanIdentifier = "monthly",
                                offer = Offer.Automatic(),
                            ),
                        entitlements = listOf(Entitlement("pro")),
                        name = "primary",
                    )
                val paywall =
                    Paywall.stub().apply {
                        _productItemsV3 = listOf(crossplatformProduct)
                    }

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("products template contains the product with correct fields") {
                    val productsTemplate = parseOutput(result)[0].jsonObject
                    assertEquals("products", productsTemplate["event_name"]!!.jsonPrimitive.content)
                    val products = productsTemplate["products"]!!.jsonArray
                    assertEquals(1, products.size)
                    val product = products[0].jsonObject
                    assertEquals("primary", product["product"]!!.jsonPrimitive.content)
                    assertEquals("com.test:monthly:sw-auto", product["productId"]!!.jsonPrimitive.content)
                }
            }
        }

    @Test
    fun `free trial available sets prefix to freeTrial`() =
        runTest {
            Given("a paywall with isFreeTrialAvailable = true") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val paywall =
                    Paywall.stub().apply {
                        isFreeTrialAvailable = true
                    }

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("free trial template has prefix=freeTrial") {
                    val freeTrialTemplate = parseOutput(result)[2].jsonObject
                    assertEquals("template_substitutions_prefix", freeTrialTemplate["event_name"]!!.jsonPrimitive.content)
                    assertEquals("freeTrial", freeTrialTemplate["prefix"]!!.jsonPrimitive.content)
                }
            }
        }

    @Test
    fun `free trial unavailable sets prefix to null`() =
        runTest {
            Given("a paywall with isFreeTrialAvailable = false") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val paywall =
                    Paywall.stub().apply {
                        isFreeTrialAvailable = false
                    }

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("free trial template has null prefix") {
                    val freeTrialTemplate = parseOutput(result)[2].jsonObject
                    assertEquals("template_substitutions_prefix", freeTrialTemplate["event_name"]!!.jsonPrimitive.content)
                    assertEquals(
                        JsonPrimitive(null as String?),
                        freeTrialTemplate["prefix"],
                    )
                }
            }
        }

    @Test
    fun `experiment present maps fields correctly`() =
        runTest {
            Given("a paywall with an experiment") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val paywall =
                    Paywall.stub().apply {
                        experiment =
                            Experiment(
                                id = "exp-123",
                                groupId = "group-456",
                                variant =
                                    Experiment.Variant(
                                        id = "variant-789",
                                        type = Experiment.Variant.VariantType.TREATMENT,
                                        paywallId = "paywall-abc",
                                    ),
                            )
                    }

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("experiment template has the correct fields") {
                    val experimentTemplate = parseOutput(result)[3].jsonObject
                    assertEquals("experiment", experimentTemplate["eventName"]!!.jsonPrimitive.content)
                    assertEquals("exp-123", experimentTemplate["experimentId"]!!.jsonPrimitive.content)
                    assertEquals("variant-789", experimentTemplate["variantId"]!!.jsonPrimitive.content)
                    assertEquals("group-456", experimentTemplate["campaignId"]!!.jsonPrimitive.content)
                }
            }
        }

    @Test
    fun `null experiment defaults all fields to empty strings`() =
        runTest {
            Given("a paywall with no experiment") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = emptyMap(),
                        deviceAttributes = emptyMap(),
                    )
                val paywall =
                    Paywall.stub().apply {
                        experiment = null
                    }

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("experiment template fields default to empty strings") {
                    val experimentTemplate = parseOutput(result)[3].jsonObject
                    assertEquals("", experimentTemplate["experimentId"]!!.jsonPrimitive.content)
                    assertEquals("", experimentTemplate["variantId"]!!.jsonPrimitive.content)
                    assertEquals("", experimentTemplate["campaignId"]!!.jsonPrimitive.content)
                }
            }
        }

    @Test
    fun `output structure is a JSON array with 4 elements in correct order`() =
        runTest {
            Given("a standard paywall configuration") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = mapOf("key" to "value"),
                        deviceAttributes = mapOf("platform" to "android"),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("output is an array of 4 elements") {
                    val array = parseOutput(result)
                    assertEquals(4, array.size)

                    And("elements are in order: products, variables, free trial, experiment") {
                        assertEquals("products", array[0].jsonObject["event_name"]!!.jsonPrimitive.content)
                        assertEquals("template_variables", array[1].jsonObject["event_name"]!!.jsonPrimitive.content)
                        assertEquals(
                            "template_substitutions_prefix",
                            array[2].jsonObject["event_name"]!!.jsonPrimitive.content,
                        )
                        assertEquals("experiment", array[3].jsonObject["eventName"]!!.jsonPrimitive.content)
                    }
                }
            }
        }

    // endregion

    // region User map serialization edge cases

    @Test
    fun `mixed primitive types in user map serialize correctly`() =
        runTest {
            Given("user attributes with String, Int, Boolean, Double, and Long values") {
                val factory =
                    TestVariablesFactory(
                        userAttributes =
                            mapOf(
                                "name" to "Jane",
                                "age" to 25,
                                "premium" to true,
                                "balance" to 99.95,
                                "loginCount" to 100_000_000_000L,
                            ),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("each value serializes with its correct JSON type") {
                    val user = variablesFromResult(result)["user"]!!.jsonObject
                    assertEquals("Jane", user["name"]!!.jsonPrimitive.content)
                    assertTrue(user["name"]!!.jsonPrimitive.isString)

                    And("age is a number") {
                        assertEquals(25, user["age"]!!.jsonPrimitive.content.toInt())
                    }

                    And("premium is a boolean") {
                        assertEquals("true", user["premium"]!!.jsonPrimitive.content)
                    }

                    And("balance is a double") {
                        assertEquals(99.95, user["balance"]!!.jsonPrimitive.content.toDouble(), 0.001)
                    }

                    And("loginCount is a long") {
                        assertEquals(100_000_000_000L, user["loginCount"]!!.jsonPrimitive.content.toLong())
                    }
                }
            }
        }

    @Test
    fun `null values in user map serialize as JSON null`() =
        runTest {
            Given("user attributes containing a null value") {
                val factory =
                    TestVariablesFactory(
                        userAttributes = mapOf("name" to "John", "email" to null),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("the null value is present as JSON null") {
                    val user = variablesFromResult(result)["user"]!!.jsonObject
                    assertEquals("John", user["name"]!!.jsonPrimitive.content)
                    assertTrue("email key should be present", user.containsKey("email"))
                    assertEquals(JsonPrimitive(null as String?), user["email"])
                }
            }
        }

    @Test
    fun `nested map in user attributes serializes correctly`() =
        runTest {
            Given("user attributes with a nested map") {
                val factory =
                    TestVariablesFactory(
                        userAttributes =
                            mapOf(
                                "name" to "Jane",
                                "address" to mapOf("city" to "NYC", "zip" to "10001"),
                            ),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("nested map is serialized as a JSON object") {
                    val user = variablesFromResult(result)["user"]!!.jsonObject
                    val address = user["address"]!!.jsonObject
                    assertEquals("NYC", address["city"]!!.jsonPrimitive.content)
                    assertEquals("10001", address["zip"]!!.jsonPrimitive.content)
                }
            }
        }

    @Test
    fun `list values in user attributes serialize correctly`() =
        runTest {
            Given("user attributes containing a list") {
                val factory =
                    TestVariablesFactory(
                        userAttributes =
                            mapOf(
                                "tags" to listOf("vip", "premium", "early_adopter"),
                            ),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("list is serialized as a JSON array") {
                    val user = variablesFromResult(result)["user"]!!.jsonObject
                    val tags = user["tags"]!!.jsonArray
                    assertEquals(3, tags.size)
                    assertEquals("vip", tags[0].jsonPrimitive.content)
                    assertEquals("premium", tags[1].jsonPrimitive.content)
                    assertEquals("early_adopter", tags[2].jsonPrimitive.content)
                }
            }
        }

    @Test
    fun `null values inside nested map are filtered out`() =
        runTest {
            Given("user attributes with a nested map containing a null value") {
                val factory =
                    TestVariablesFactory(
                        userAttributes =
                            mapOf(
                                "prefs" to mapOf("theme" to "dark", "lang" to null),
                            ),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("the null entry is dropped from the nested map") {
                    val user = variablesFromResult(result)["user"]!!.jsonObject
                    val prefs = user["prefs"]!!.jsonObject
                    assertEquals("dark", prefs["theme"]!!.jsonPrimitive.content)
                    assertTrue("lang key should be filtered out", !prefs.containsKey("lang"))
                }
            }
        }

    @Test
    fun `null values inside list are filtered out`() =
        runTest {
            Given("user attributes with a list containing null elements") {
                val factory =
                    TestVariablesFactory(
                        userAttributes =
                            mapOf(
                                "scores" to listOf(100, null, 200),
                            ),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("null elements are removed from the list") {
                    val user = variablesFromResult(result)["user"]!!.jsonObject
                    val scores = user["scores"]!!.jsonArray
                    assertEquals(2, scores.size)
                    assertEquals(100, scores[0].jsonPrimitive.content.toInt())
                    assertEquals(200, scores[1].jsonPrimitive.content.toInt())
                }
            }
        }

    @Test
    fun `unsupported type in user map serializes as null`() =
        runTest {
            Given("user attributes containing an unsupported type") {
                val factory =
                    TestVariablesFactory(
                        userAttributes =
                            mapOf(
                                "name" to "John",
                                "created" to Date(),
                            ),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("the unsupported value falls back to null") {
                    val user = variablesFromResult(result)["user"]!!.jsonObject
                    assertEquals("John", user["name"]!!.jsonPrimitive.content)
                    assertEquals(JsonPrimitive(null as String?), user["created"])
                }
            }
        }

    @Test
    fun `deeply nested structure serializes correctly`() =
        runTest {
            Given("user attributes with nested map containing a list of maps") {
                val factory =
                    TestVariablesFactory(
                        userAttributes =
                            mapOf(
                                "profile" to
                                    mapOf(
                                        "orders" to
                                            listOf(
                                                mapOf("id" to 1, "total" to 29.99),
                                                mapOf("id" to 2, "total" to 49.99),
                                            ),
                                    ),
                            ),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("the nested structure is fully preserved") {
                    val user = variablesFromResult(result)["user"]!!.jsonObject
                    val orders = user["profile"]!!.jsonObject["orders"]!!.jsonArray
                    assertEquals(2, orders.size)
                    assertEquals(
                        1,
                        orders[0]
                            .jsonObject["id"]!!
                            .jsonPrimitive.content
                            .toInt(),
                    )
                    assertEquals(
                        29.99,
                        orders[0]
                            .jsonObject["total"]!!
                            .jsonPrimitive.content
                            .toDouble(),
                        0.001,
                    )
                    assertEquals(
                        2,
                        orders[1]
                            .jsonObject["id"]!!
                            .jsonPrimitive.content
                            .toInt(),
                    )
                    assertEquals(
                        49.99,
                        orders[1]
                            .jsonObject["total"]!!
                            .jsonPrimitive.content
                            .toDouble(),
                        0.001,
                    )
                }
            }
        }

    @Test
    fun `top-level nulls preserved but nested map nulls are dropped`() =
        runTest {
            Given("user attributes with null at top level and null inside a nested map") {
                val factory =
                    TestVariablesFactory(
                        userAttributes =
                            mapOf(
                                "email" to null,
                                "prefs" to mapOf("email" to null, "theme" to "dark"),
                            ),
                        deviceAttributes = emptyMap(),
                    )
                val paywall = Paywall.stub()

                val result =
                    When("getBase64EncodedTemplates is called") {
                        TemplateLogic.getBase64EncodedTemplates(
                            json = json,
                            paywall = paywall,
                            event = null,
                            factory = factory,
                            encodeToBase64 = { it },
                        )
                    }

                Then("top-level null is preserved as JSON null") {
                    val user = variablesFromResult(result)["user"]!!.jsonObject
                    assertTrue("email key exists at top level", user.containsKey("email"))
                    assertEquals(JsonPrimitive(null as String?), user["email"])

                    And("nested map null is dropped by AnySerializer") {
                        val prefs = user["prefs"]!!.jsonObject
                        assertTrue("nested email key is filtered out", !prefs.containsKey("email"))
                        assertEquals("dark", prefs["theme"]!!.jsonPrimitive.content)
                    }
                }
            }
        }

    // endregion
}
