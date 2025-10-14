package com.superwall.sdk.misc

fun String.camelCaseToSnakeCase(): String {
    val acronymPattern = "([A-Z]+)([A-Z][a-z]|[0-9])"
    val fullWordsPattern = "([a-z])([A-Z]|[0-9])"
    val digitsFirstPattern = "([0-9])([A-Z])"

    return this
        .processCamelCaseRegex(acronymPattern)
        ?.processCamelCaseRegex(fullWordsPattern)
        ?.processCamelCaseRegex(digitsFirstPattern)
        ?.lowercase() ?: this.lowercase()
}

private fun String.processCamelCaseRegex(pattern: String): String? {
    val regex = Regex(pattern)
    return regex
        .replace(this) { matchResult ->
            "${matchResult.groups[1]?.value}_${matchResult.groups[2]?.value}"
        }.takeIf { it.isNotBlank() }
}
