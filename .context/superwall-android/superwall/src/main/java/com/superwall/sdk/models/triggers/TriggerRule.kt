package com.superwall.sdk.models.triggers

import com.superwall.sdk.models.config.ComputedPropertyRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data class UnmatchedRule(
    val source: Source,
    val experimentId: String,
) {
    enum class Source(
        val rawValue: String,
    ) {
        EXPRESSION("EXPRESSION"),
        OCCURRENCE("OCCURRENCE"),
    }
}

data class MatchedItem(
    val rule: TriggerRule,
    val unsavedOccurrence: TriggerRuleOccurrence? = null,
)

sealed class TriggerRuleOutcome {
    data class NoMatch(
        val unmatchedRule: UnmatchedRule,
    ) : TriggerRuleOutcome()

    data class Match(
        val matchedItem: MatchedItem,
    ) : TriggerRuleOutcome()

    companion object {
        fun noMatch(
            source: UnmatchedRule.Source,
            experimentId: String,
        ): TriggerRuleOutcome = NoMatch(UnmatchedRule(source, experimentId))

        fun match(
            rule: TriggerRule,
            unsavedOccurrence: TriggerRuleOccurrence? = null,
        ): TriggerRuleOutcome = Match(MatchedItem(rule, unsavedOccurrence))
    }

    override fun equals(other: Any?): Boolean =
        when {
            this is NoMatch && other is NoMatch ->
                unmatchedRule.source == other.unmatchedRule.source &&
                    unmatchedRule.experimentId == other.unmatchedRule.experimentId
            this is Match && other is Match ->
                matchedItem.rule == other.matchedItem.rule &&
                    matchedItem.unsavedOccurrence == other.matchedItem.unsavedOccurrence
            else -> false
        }
}

@Serializable
enum class TriggerPreloadBehavior(
    val rawValue: String,
) {
    @SerialName("IF_TRUE")
    IF_TRUE("IF_TRUE"),

    @SerialName("ALWAYS")
    ALWAYS("ALWAYS"),

    @SerialName("NEVER")
    NEVER("NEVER"),
}

@Serializable
data class TriggerRule(
    @SerialName("experiment_id")
    var experimentId: String,
    @SerialName("experiment_group_id")
    var experimentGroupId: String,
    @SerialName("variants")
    var variants: List<VariantOption>,
    @SerialName("expression")
    val expression: String? = null,
    @SerialName("expression_js")
    val expressionJs: String? = null,
    @SerialName("expression_cel")
    val expressionCEL: String? = null,
    @SerialName("occurrence")
    val occurrence: TriggerRuleOccurrence? = null,
    @SerialName("computed_properties")
    val computedPropertyRequests: List<ComputedPropertyRequest> = emptyList(),
    @SerialName("preload")
    val preload: TriggerPreload,
) {
    @Serializable(with = TriggerPreloadSerializer::class)
    data class TriggerPreload(
        var behavior: TriggerPreloadBehavior,
        val requiresReEvaluation: Boolean? = null,
    )

    object TriggerPreloadSerializer : KSerializer<TriggerPreload> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("TriggerPreload") {
                element<String>("behavior")
                element<Boolean>("requiresReEvaluation", isOptional = true)
            }

        override fun serialize(
            encoder: Encoder,
            value: TriggerPreload,
        ) {
            val compositeOutput = encoder.beginStructure(descriptor)
            compositeOutput.encodeStringElement(descriptor, 0, value.behavior.rawValue)
            if (value.requiresReEvaluation != null) {
                compositeOutput.encodeBooleanElement(descriptor, 1, value.requiresReEvaluation)
            }
            compositeOutput.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): TriggerPreload {
            val dec = decoder.beginStructure(descriptor)
            var behavior: TriggerPreloadBehavior? = null
            var requiresReevaluation: Boolean? = null

            loop@ while (true) {
                when (val i = dec.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> behavior = TriggerPreloadBehavior.valueOf(dec.decodeStringElement(descriptor, i).uppercase())
                    1 -> requiresReevaluation = dec.decodeBooleanElement(descriptor, i)
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            dec.endStructure(descriptor)

            val finalBehavior = behavior ?: throw SerializationException("Behavior is missing")
            if (requiresReevaluation == true) {
                return TriggerPreload(TriggerPreloadBehavior.ALWAYS, requiresReevaluation)
            }

            return TriggerPreload(finalBehavior, requiresReevaluation)
        }
    }

    val experiment: RawExperiment
        get() {
            return RawExperiment(
                id = this.experimentId,
                groupId = this.experimentGroupId,
                variants = this.variants,
            )
        }

    companion object {
        fun stub() =
            TriggerRule(
                experimentId = "1",
                experimentGroupId = "2",
                variants =
                    listOf(
                        VariantOption(
                            type = Experiment.Variant.VariantType.HOLDOUT,
                            id = "3",
                            percentage = 20,
                            paywallId = null,
                        ),
                    ),
                expression = null,
                expressionJs = null,
                occurrence = null,
                computedPropertyRequests = emptyList(),
                preload = TriggerPreload(behavior = TriggerPreloadBehavior.ALWAYS),
            )
    }
}
