package com.superwall.sdk.paywall.presentation.rule_logic

import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.CELResult
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ExecutionContext
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.PassableValue
import com.superwall.supercel.HostContext
import com.superwall.supercel.ResultCallback
import com.superwall.supercel.evaluateWithContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class CELTest {
    val json =
        Json {
        }

    @Test
    fun deserialize_cel_from_json() {
        val celState = """        {
            "variables": {
                "map": {
                    "numbers": {
                        "type":"list",
                        "value": [
                            {"type": "int", "value": 100},
                            {"type": "int", "value": 100},
                            {"type": "int", "value": 100}
                        ]
                    }
                }
            },
            "computed": {},
            "device": {},
            "expression": "numbers[0] + numbers[1] == 200"
        }
"""

        val executionContext = json.decodeFromString<ExecutionContext>(celState)
        println("Deserialized: $executionContext")

        val serializedJson = json.encodeToString(executionContext)
        println("Serialized JSON: $serializedJson")
    }

    @Test
    fun deserialize_cel_function_from_json() {
        val celState = """        {
            "variables": {
                "map": {
                    "numbers": {
                        "type":"list",
                        "value": [
                            {"type": "int", "value": 100},
                            {"type": "int", "value": 100},
                            {"type": "int", "value": 100}
                        ]
                    }
                }
            },
            "device": {},
            "computed": {},
            "expression": "numbers[0] + numbers[1] == 200"
        }
"""

        val executionContext = json.decodeFromString<ExecutionContext>(celState)
        val serializedJson = json.encodeToString(executionContext)
        val resultJSON =
            evaluateWithContext(
                celState,
                object : HostContext {
                    override fun computedProperty(
                        name: String,
                        args: String,
                        callback: ResultCallback,
                    ) {
                        callback.onResult(
                            Json.encodeToString(
                                PassableValue.UIntValue(0uL),
                            ),
                        )
                    }

                    override fun deviceProperty(
                        name: String,
                        args: String,
                        callback: ResultCallback,
                    ) {
                        callback.onResult(
                            Json.encodeToString(
                                PassableValue.UIntValue(0uL),
                            ),
                        )
                    }
                },
            )
        val result = json.decodeFromString<CELResult>(resultJSON)
        assert(result is CELResult.Ok && result.value == PassableValue.BoolValue(true))
    }

    @Test
    fun evaluate_expression_from_rules() {
        val celState = """        {
                    "variables": {
                        "map": {
                            "user": {
                                "type": "map",
                                "value": {
                                    "should_display": {
                                        "type": "bool",
                                        "value": true
                                    },
                                    "some_value": {
                                        "type": "uint",
                                        "value": 13
                                    }
                                }
                            }
                        }
                    },
                    "computed" : {
                        "daysSinceEvent": [{
                            "type": "uint",
                            "value": 9
                        }]
                    },
                    "device": {
                    },
                    "expression": "computed.daysSinceEvent(user.some_value) == 3" 
    }
"""
        val executionContext = json.decodeFromString<ExecutionContext>(celState)
        val serializedJson = json.encodeToString(executionContext)
        val resultJSON =
            evaluateWithContext(
                celState,
                object : HostContext {
                    override fun computedProperty(
                        name: String,
                        args: String,
                        callback: ResultCallback,
                    ) {
                        val res = json.encodeToString(PassableValue.UIntValue(3uL) as PassableValue)
                        callback.onResult(res)
                        TODO("Not yet implemented")
                    }

                    override fun deviceProperty(
                        name: String,
                        args: String,
                        callback: ResultCallback,
                    ) {
                        val res = json.encodeToString(PassableValue.UIntValue(3uL) as PassableValue)
                        callback.onResult(res)
                        TODO("Not yet implemented")
                    }
                },
            )
        val result = json.decodeFromString<CELResult>(resultJSON)
        assert(result is CELResult.Ok && result.value == PassableValue.BoolValue(true))
    }
}
