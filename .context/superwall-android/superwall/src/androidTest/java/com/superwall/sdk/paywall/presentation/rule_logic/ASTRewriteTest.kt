package com.superwall.sdk.paywall.presentation.rule_logic

import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.PassableValue
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ast.CELAtom
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ast.CELExpression
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ast.CELExpression.*
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ast.CELMember
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.ast.CELRelationOp.*
import com.superwall.sdk.paywall.presentation.rule_logic.cel.rewriteASTWith
import org.junit.Assert.assertEquals
import org.junit.Test

class ASTRewriteTest {
    @Test
    fun rewrite_ast_platform_references() {
        val expression: CELExpression =
            And(
                left =
                    Relation(
                        left =
                            FunctionCall(
                                function = Ident("myMethod"),
                                receiver = Ident("platform"),
                                arguments =
                                    listOf(
                                        Atom(CELAtom.String("test")),
                                    ),
                            ),
                        op = Equals,
                        right =
                            Member(
                                expr = Ident("platform"),
                                member = CELMember.Attribute("name"),
                            ),
                    ),
                right =
                    Relation(
                        left =
                            Member(
                                expr = Ident("user"),
                                member = CELMember.Attribute("test"),
                            ),
                        op = Equals,
                        right = Atom(CELAtom.Int(1)),
                    ),
            )

        val transformedExpression =
            expression.rewriteASTWith({ name, args -> PassableValue.StringValue("mapped:$name") })

        val expectedExpression =
            And(
                Relation(
                    Atom(CELAtom.String("mapped:myMethod")),
                    Equals,
                    Atom(CELAtom.String("mapped:name")),
                ),
                Relation(
                    Member(
                        Ident("user"),
                        CELMember.Attribute("test"),
                    ),
                    Equals,
                    Atom(CELAtom.Int(1)),
                ),
            )

        println("Expected: $expectedExpression")
        println("Transformed: $transformedExpression")
        assertEquals(expectedExpression, transformedExpression)
    }
}
