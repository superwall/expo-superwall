package com.superwall.sdk.debug.localizations

data class LocalizationOption(
    val language: String,
    val country: String?,
    val locale: String,
    private val popularLocales: List<String>,
) {
    val description: String = if (country != null) "$language ($country)" else language

    val isPopular: Boolean
        get() = locale == "en" || popularLocales.contains(locale)

    val sectionTitle: String
        get() =
            when {
                isPopular -> "Localized"
                language.isNotEmpty() -> language.first().uppercaseChar().toString()
                else -> "Unknown"
            }

    val sortDescription: String
        get() = "${if (isPopular) "a" else "b"} $description"

    fun included(query: String): Boolean {
        val lowerCaseQuery = query.lowercase()
        return description.lowercase().contains(lowerCaseQuery) ||
            locale.lowercase().contains(lowerCaseQuery)
    }
}
