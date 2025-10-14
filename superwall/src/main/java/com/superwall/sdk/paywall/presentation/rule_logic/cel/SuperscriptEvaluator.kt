package com.superwall.sdk.paywall.presentation.rule_logic.cel

import com.superwall.sdk.Superwall
import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.asyncWithTracking
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.TriggerRule
import com.superwall.sdk.models.triggers.TriggerRuleOutcome
import com.superwall.sdk.models.triggers.UnmatchedRule
import com.superwall.sdk.paywall.presentation.rule_logic.cel.SuperscriptHostContext.ComputedProperties.availableComputedProperties
import com.superwall.sdk.paywall.presentation.rule_logic.cel.SuperscriptHostContext.ComputedProperties.availableDeviceProperties
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.CELResult
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ExecutionContext
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.PassableMap
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.PassableValue
import com.superwall.sdk.paywall.presentation.rule_logic.expression_evaluator.ExpressionEvaluating
import com.superwall.sdk.paywall.presentation.rule_logic.tryToMatchOccurrence
import com.superwall.sdk.storage.core_data.CoreDataManager
import com.superwall.sdk.utilities.trackError
import com.superwall.supercel.evaluateWithContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

internal class SuperscriptEvaluator(
    private val json: Json,
    private val ioScope: IOScope,
    private val storage: CoreDataManager,
    private val factory: RuleAttributesFactory,
    private val hostContext: SuperscriptHostContext =
        SuperscriptHostContext(
            json,
            storage,
        ),
) : ExpressionEvaluating {
    class NotError(
        val string: String,
    ) : Throwable(string)

    override suspend fun evaluateExpression(
        rule: TriggerRule,
        eventData: EventData?,
    ): TriggerRuleOutcome {
        if (rule.expressionCEL == null) {
            return rule.tryToMatchOccurrence(storage, true)
        }

        return ioScope
            .asyncWithTracking {
                val factory = factory.makeRuleAttributes(eventData, rule.computedPropertyRequests)
                val userAttributes = factory.toPassableValue()
                val expression = rule.expressionCEL

                val executionContext =
                    ExecutionContext(
                        variables = PassableMap(map = userAttributes.value.toMap()),
                        expression = expression,
                        device =
                            availableDeviceProperties.associate {
                                it to listOf(PassableValue.StringValue("event_name"))
                            },
                        computed =
                            availableComputedProperties.associate {
                                it to listOf(PassableValue.StringValue("event_name"))
                            },
                    )

                val ctx = json.encodeToString(executionContext)
                val result = evaluateWithContext(ctx, hostContext)

                val celResult = json.decodeFromString<CELResult>(result)
                return@asyncWithTracking when (celResult) {
                    is CELResult.Err -> {
                        Logger.debug(
                            LogLevel.error,
                            LogScope.jsEvaluator,
                            "Superscript evaluation failed for expression $result: ${celResult.message}",
                        )
                        TriggerRuleOutcome.noMatch(
                            UnmatchedRule.Source.EXPRESSION,
                            rule.experiment.id,
                        )
                    }

                    is CELResult.Ok -> {
                        if (celResult.value is PassableValue.BoolValue && celResult.value.value
                        ) {
                            rule.tryToMatchOccurrence(storage, true)
                        } else {
                            TriggerRuleOutcome.noMatch(
                                UnmatchedRule.Source.EXPRESSION,
                                rule.experiment.id,
                            )
                        }
                    }
                }
            }.await()
            .let {
                it.getSuccess() ?: run {
                    val e =
                        if (it.getThrowable() is Throwable) {
                            it.getThrowable() as Throwable
                        } else {
                            NotError(
                                "Unknown error ${it.getThrowable()}",
                            )
                        }
                    Superwall.instance.trackError(e)
                    TriggerRuleOutcome.noMatch(
                        UnmatchedRule.Source.EXPRESSION,
                        rule.experiment.id,
                    )
                }
            }
    }
}

// PassableValues match the types in our Rust package
internal fun <T : Any> Map<String, T>.toPassableValue(): PassableValue.MapValue {
    val passableMap =
        this
            .mapValues { (_, value) ->
                value.toPassableValue()
            }.toMap()
    return PassableValue.MapValue(passableMap)
}

internal fun Any.toPassableValue(): PassableValue =
    when (this) {
        is Int -> PassableValue.IntValue(this)
        is Long -> PassableValue.UIntValue(this.toULong())
        is ULong -> PassableValue.UIntValue(this)
        is Float -> PassableValue.FloatValue(this.toDouble())
        is Double -> PassableValue.FloatValue(this)
        is String -> PassableValue.StringValue(this.replace("$", ""))
        is ByteArray -> PassableValue.BytesValue(this)
        is Boolean -> PassableValue.BoolValue(this)
        is List<*> ->
            PassableValue.ListValue(
                this.map { it?.toPassableValue() ?: PassableValue.NullValue },
            )

        is LinkedHashMap<*, *> -> {
            // Due to issues with Kotlin 2.0 compatibility we have to use this workaround
            val stringKeyMap =
                this
                    .filterKeys { it is String }
                    .mapKeys { it.key as String }
            PassableValue.MapValue(
                stringKeyMap
                    .mapValues { it.value?.toPassableValue() ?: PassableValue.NullValue }
                    .toMap(),
            )
        }

        is Map<*, *> -> {
            val stringKeyMap =
                this
                    .filterKeys { it is String }
                    .mapKeys { it.key as String }
            PassableValue.MapValue(
                stringKeyMap
                    .mapValues { it.value?.toPassableValue() ?: PassableValue.NullValue }
                    .toMap(),
            )
        }

        is JsonElement -> this.toPassableValue()
        is PassableValue -> this
        else -> {
            try {
                val map = this as Map<*, *>
                PassableValue.MapValue(
                    map
                        .map {
                            it.key.toString() to (it.value?.toPassableValue() ?: PassableValue.NullValue)
                        }.toMap(),
                )
            } catch (e: Exception) {
                try {
                    val jsonElement = Json.encodeToJsonElement(this)
                    jsonElement.toPassableValue()
                } catch (e: Exception) {
                    Logger.debug(
                        LogLevel.warn,
                        LogScope.jsEvaluator,
                        "Cannot serialize $this::class, evaluating as string",
                    )
                    PassableValue.StringValue(this.toString())
                }
            }
        }
    }

private fun JsonElement.toPassableValue(): PassableValue =
    when (this) {
        is JsonObject ->
            PassableValue.MapValue(
                this.mapValues { (_, value) -> value.toPassableValue() }.toMap(),
            )

        is JsonArray ->
            PassableValue.ListValue(
                this.map { it.toPassableValue() },
            )

        is JsonPrimitive ->
            when {
                this.isString -> PassableValue.StringValue(this.content)
                this.booleanOrNull != null -> PassableValue.BoolValue(this.boolean)
                this.intOrNull != null -> PassableValue.IntValue(this.int)
                this.longOrNull != null -> PassableValue.UIntValue(this.long.toULong())
                this.doubleOrNull != null -> PassableValue.FloatValue(this.double)
                else -> PassableValue.StringValue(this.content)
            }
    }
