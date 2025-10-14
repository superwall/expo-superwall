@file:Suppress("ktlint:standard:function-naming")

package com.superwall.sdk

fun assertTrue(value: Boolean) {
    if (!value) {
        throw AssertionError("Expected true, got false")
    }
}

fun assertFalse(value: Boolean) {
    if (value) {
        throw AssertionError("Expected false, got true")
    }
}

@DslMarker annotation class TestingDSL

class GivenWhenThenScope(
    val text: MutableList<String>,
)

@TestingDSL
inline fun Given(
    what: String,
    block: GivenWhenThenScope.() -> Unit,
) {
    val scope = GivenWhenThenScope(mutableListOf("Given $what"))
    try {
        block(scope)
    } catch (e: Throwable) {
        e.printStackTrace()
        println(scope.text.joinToString("\n"))
        throw e
    }
}

@TestingDSL
inline fun <T> GivenWhenThenScope.When(
    what: String,
    block: GivenWhenThenScope.() -> T,
): T {
    text.add("\tWhen $what")
    try {
        return block(this)
    } catch (e: Throwable) {
        throw e
    }
}

@TestingDSL
inline fun GivenWhenThenScope.Then(
    what: String,
    block: GivenWhenThenScope.() -> Unit,
) {
    text.add("\t\tThen $what")
    block()
}

@TestingDSL
inline fun GivenWhenThenScope.And(
    what: String,
    block: GivenWhenThenScope.() -> Unit,
) {
    text.add("\t\t\tAnd $what")
    block()
}
