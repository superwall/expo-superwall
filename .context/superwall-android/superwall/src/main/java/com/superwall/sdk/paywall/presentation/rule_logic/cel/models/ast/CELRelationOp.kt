package com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ast

import kotlinx.serialization.*

// Used to rewrite the AST to replace all the CELExpressions with the proper values
internal sealed interface CELNode {
    // When implementing, ensure that the expression is transformed leaf to node
    abstract fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression
}

internal interface ToExprString {
    fun toExprString(): String
}

@Serializable
@SerialName("CELRelationOp")
@Polymorphic
internal sealed class CELRelationOp : ToExprString {
    @SerialName("LessThan")
    @Serializable
    object LessThan : CELRelationOp() {
        override fun toExprString() = "<"
    }

    @SerialName("LessThanEq")
    @Serializable
    object LessThanEq : CELRelationOp() {
        override fun toExprString() = "<="
    }

    @SerialName("GreaterThan")
    @Serializable
    object GreaterThan : CELRelationOp() {
        override fun toExprString() = ">"
    }

    @SerialName("GreaterThanEq")
    @Serializable
    object GreaterThanEq : CELRelationOp() {
        override fun toExprString() = ">="
    }

    @SerialName("Equals")
    @Serializable
    object Equals : CELRelationOp() {
        override fun toExprString() = "=="
    }

    @SerialName("NotEquals")
    @Serializable
    object NotEquals : CELRelationOp() {
        override fun toExprString() = "!="
    }

    @SerialName("In")
    @Serializable
    object In : CELRelationOp() {
        override fun toExprString() = "in"
    }
}

@Serializable
@SerialName("CELArithmeticOp")
@Polymorphic
internal sealed class CELArithmeticOp : ToExprString {
    @SerialName("Add")
    @Serializable
    object Add : CELArithmeticOp() {
        override fun toExprString() = "+"
    }

    @SerialName("Subtract")
    @Serializable
    object Subtract : CELArithmeticOp() {
        override fun toExprString() = "-"
    }

    @SerialName("Divide")
    @Serializable
    object Divide : CELArithmeticOp() {
        override fun toExprString() = "/"
    }

    @SerialName("Multiply")
    @Serializable
    object Multiply : CELArithmeticOp() {
        override fun toExprString() = "*"
    }

    @SerialName("Modulus")
    @Serializable
    object Modulus : CELArithmeticOp() {
        override fun toExprString() = "%"
    }
}

@Serializable
@SerialName("CELUnaryOp")
@Polymorphic
internal sealed class CELUnaryOp : ToExprString {
    @SerialName("Not")
    @Serializable
    object Not : CELUnaryOp() {
        override fun toExprString() = "!"
    }

    @SerialName("DoubleNot")
    @Serializable
    object DoubleNot : CELUnaryOp() {
        override fun toExprString() = "!!"
    }

    @SerialName("Minus")
    @Serializable
    object Minus : CELUnaryOp() {
        override fun toExprString() = "-"
    }

    @SerialName("DoubleMinus")
    @Serializable
    object DoubleMinus : CELUnaryOp() {
        override fun toExprString() = "--"
    }
}

@Serializable
@SerialName("CELExpression")
@Polymorphic
internal sealed class CELExpression :
    CELNode,
    ToExprString {
    @SerialName("Arithmetic")
    @Serializable
    data class Arithmetic(
        @SerialName("left") val left: CELExpression,
        @SerialName("op") val op: CELArithmeticOp,
        @SerialName("right") val right: CELExpression,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                Arithmetic(
                    left.mapAll(transform),
                    op,
                    right.mapAll(transform),
                ),
            )

        override fun toExprString(): String = "(${left.toExprString()} ${op.toExprString()} ${right.toExprString()})"
    }

    @SerialName("Relation")
    @Serializable
    data class Relation(
        @SerialName("left") val left: CELExpression,
        @SerialName("op") val op: CELRelationOp,
        @SerialName("right") val right: CELExpression,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                Relation(
                    left.mapAll(transform),
                    op,
                    right.mapAll(transform),
                ),
            )

        override fun toExprString(): String = "(${left.toExprString()} ${op.toExprString()} ${right.toExprString()})"
    }

    @SerialName("Ternary")
    @Serializable
    data class Ternary(
        @SerialName("condition") val condition: CELExpression,
        @SerialName("trueExpr") val trueExpr: CELExpression,
        @SerialName("falseExpr") val falseExpr: CELExpression,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                Ternary(
                    condition.mapAll(transform),
                    trueExpr.mapAll(transform),
                    falseExpr.mapAll(transform),
                ),
            )

        override fun toExprString(): String = "(${condition.toExprString()} ? ${trueExpr.toExprString()} : ${falseExpr.toExprString()})"
    }

    @SerialName("Or")
    @Serializable
    data class Or(
        @SerialName("left") val left: CELExpression,
        @SerialName("right") val right: CELExpression,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                Or(
                    left.mapAll(transform),
                    right.mapAll(transform),
                ),
            )

        override fun toExprString(): String = "(${left.toExprString()} || ${right.toExprString()})"
    }

    @SerialName("And")
    @Serializable
    data class And(
        @SerialName("left") val left: CELExpression,
        @SerialName("right") val right: CELExpression,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                And(
                    left.mapAll(transform),
                    right.mapAll(transform),
                ),
            )

        override fun toExprString(): String = "(${left.toExprString()} && ${right.toExprString()})"
    }

    @SerialName("Unary")
    @Serializable
    data class Unary(
        @SerialName("op") val op: CELUnaryOp,
        @SerialName("expr") val expr: CELExpression,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                Unary(
                    op,
                    expr.mapAll(transform),
                ),
            )

        override fun toExprString(): String = "${op.toExprString()}(${expr.toExprString()})"
    }

    @SerialName("Member")
    @Serializable
    data class Member(
        @SerialName("expr") val expr: CELExpression,
        @SerialName("member") val member: CELMember,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                Member(
                    expr.mapAll(transform),
                    member,
                ),
            )

        override fun toExprString(): String = "${expr.toExprString()}${member.toExprString()}"
    }

    @SerialName("FunctionCall")
    @Serializable
    data class FunctionCall(
        @SerialName("function") val function: CELExpression,
        @SerialName("receiver") val receiver: CELExpression?,
        @SerialName("arguments") val arguments: kotlin.collections.List<CELExpression>,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                FunctionCall(
                    function.mapAll(transform),
                    receiver?.mapAll(transform),
                    arguments.map { it.mapAll(transform) },
                ),
            )

        override fun toExprString(): String {
            val receiverStr = receiver?.toExprString()?.plus(".") ?: ""
            val argsStr = arguments.joinToString(", ") { it.toExprString() }
            return "$receiverStr${function.toExprString()}($argsStr)"
        }
    }

    @SerialName("List")
    @Serializable
    data class List(
        @SerialName("elements") val elements: kotlin.collections.List<CELExpression>,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                List(
                    elements.map { it.mapAll(transform) },
                ),
            )

        override fun toExprString(): String = "[${elements.joinToString(", ") { it.toExprString() }}]"
    }

    @SerialName("Map")
    @Serializable
    data class Map(
        @SerialName("entries") val entries: kotlin.collections.List<Pair<CELExpression, CELExpression>>,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression =
            transform(
                Map(
                    entries.map { (key, value) ->
                        key.mapAll(transform) to value.mapAll(transform)
                    },
                ),
            )

        override fun toExprString(): String =
            "{${entries.joinToString(", ") { (key, value) -> "${key.toExprString()}: ${value.toExprString()}" }}}"
    }

    @SerialName("Atom")
    @Serializable
    data class Atom(
        @SerialName("value") val value: CELAtom,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression = transform(this)

        override fun toExprString(): String = value.toExprString()
    }

    @SerialName("Ident")
    @Serializable
    data class Ident(
        @SerialName("name") val name: String,
    ) : CELExpression() {
        override fun mapAll(transform: (CELExpression) -> CELExpression): CELExpression = transform(this)

        override fun toExprString(): String = name
    }
}

@Serializable
@SerialName("CELMember")
@Polymorphic
internal sealed class CELMember : ToExprString {
    @SerialName("Attribute")
    @Serializable
    data class Attribute(
        @SerialName("name") val name: String,
    ) : CELMember() {
        override fun toExprString(): String = ".$name"
    }

    @SerialName("Index")
    @Serializable
    data class Index(
        @SerialName("expr") val expr: CELExpression,
    ) : CELMember() {
        override fun toExprString(): String = "[${expr.toExprString()}]"
    }

    @SerialName("Fields")
    @Serializable
    data class Fields(
        @SerialName("fields") val fields: List<Pair<String, CELExpression>>,
    ) : CELMember() {
        override fun toExprString(): String = "{${fields.joinToString(", ") { (key, value) -> "$key: ${value.toExprString()}" }}}"
    }
}

@Serializable
@SerialName("CELAtom")
internal sealed class CELAtom : ToExprString {
    @SerialName("Int")
    @Serializable
    data class Int(
        @SerialName("value") val value: Long,
    ) : CELAtom() {
        override fun toExprString() = value.toString()
    }

    @SerialName("UInt")
    @Serializable
    data class UInt(
        @SerialName("value") val value: ULong,
    ) : CELAtom() {
        override fun toExprString() = "${value}u"
    }

    @SerialName("Float")
    @Serializable
    data class Float(
        @SerialName("value") val value: Double,
    ) : CELAtom() {
        override fun toExprString() = value.toString()
    }

    @SerialName("String")
    @Serializable
    data class String(
        @SerialName("value") val value: kotlin.String,
    ) : CELAtom() {
        override fun toExprString() = "\"$value\""
    }

    @SerialName("Bytes")
    @Serializable
    data class Bytes(
        @SerialName("value") val value: ByteArray,
    ) : CELAtom() {
        override fun toExprString() = "b\"${value.joinToString("") { "%02x".format(it) }}\""

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Bytes
            return value.contentEquals(other.value)
        }
    }

    @SerialName("Bool")
    @Serializable
    data class Bool(
        @SerialName("value") val value: Boolean,
    ) : CELAtom() {
        override fun toExprString() = value.toString()
    }

    @SerialName("Null")
    @Serializable
    object Null : CELAtom() {
        override fun toExprString() = "null"
    }
}
