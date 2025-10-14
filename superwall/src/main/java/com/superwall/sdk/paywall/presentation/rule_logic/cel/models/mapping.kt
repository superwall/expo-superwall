package com.superwall.sdk.paywall.presentation.rule_logic.cel.models

import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ast.CELAtom
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ast.CELExpression

internal fun CELExpression.toPassableValue(): PassableValue =
    when (this) {
        is CELExpression.Atom -> {
            when (val atomValue = this.value) {
                is CELAtom.Int -> PassableValue.IntValue(atomValue.value.toInt())
                is CELAtom.UInt -> PassableValue.UIntValue(atomValue.value)
                is CELAtom.Float -> PassableValue.FloatValue(atomValue.value)
                is CELAtom.String -> PassableValue.StringValue(atomValue.value)
                is CELAtom.Bytes -> PassableValue.BytesValue(atomValue.value)
                is CELAtom.Bool -> PassableValue.BoolValue(atomValue.value)
                CELAtom.Null -> PassableValue.NullValue
            }
        }

        is CELExpression.List -> PassableValue.ListValue(this.elements.map { it.toPassableValue() })
        is CELExpression.Map ->
            PassableValue.MapValue(
                this.entries
                    .map { (key, value) ->
                        val key =
                            (key as? CELExpression.Atom)?.value?.let {
                                when (it) {
                                    is CELAtom.String -> it.value
                                    else -> it.toString()
                                }
                            } ?: key.toString()
                        val value = value.toPassableValue()
                        key to value
                    }.toMap(),
            )

        else -> TODO("Not yet implemented")
    }

internal fun PassableValue.toCELExpression(): CELExpression =
    when (this) {
        is PassableValue.IntValue -> CELExpression.Atom(CELAtom.Int(value.toLong()))
        is PassableValue.UIntValue -> CELExpression.Atom(CELAtom.UInt(value))
        is PassableValue.FloatValue -> CELExpression.Atom(CELAtom.Float(value))
        is PassableValue.StringValue -> CELExpression.Atom(CELAtom.String(value))
        is PassableValue.BytesValue -> CELExpression.Atom(CELAtom.Bytes(value))
        is PassableValue.BoolValue -> CELExpression.Atom(CELAtom.Bool(value))
        is PassableValue.TimestampValue -> CELExpression.Atom(CELAtom.Int(value))
        PassableValue.NullValue -> CELExpression.Atom(CELAtom.Null)
        is PassableValue.ListValue -> CELExpression.List(value.map { it.toCELExpression() })
        is PassableValue.MapValue ->
            CELExpression.Map(
                this.value
                    .map { (key, value) ->
                        CELExpression.Atom(CELAtom.String(key)) to value.toCELExpression()
                    }.toList(),
            )

        is PassableValue.FunctionValue -> {
            TODO("Not yet implemented")
        }
    }
