package com.superwall.sdk.debug.localizations

import java.util.Locale

class LocalizationManager {
    val popularLocales = listOf("de_DE", "es_US")
    val localizationGroupings: List<LocalizationGrouping>

    init {
        val localeIds = Locale.getAvailableLocales().map { it.toString() }
        val sortedLocalizations =
            LocalizationLogic.getSortedLocalizations(
                localeIds,
                popularLocales,
            )
        localizationGroupings = LocalizationLogic.getGroupings(sortedLocalizations)
    }

    fun localizationGroupings(searchTerm: String?): List<LocalizationGrouping> {
        val query = searchTerm?.lowercase() ?: ""

        if (query.isEmpty()) {
            return localizationGroupings
        }

        return localizationGroupings.mapNotNull { grouping ->
            val filteredLocalizations =
                grouping.localizations
                    .filter {
                        it.included(query = query)
                    }.toMutableList()
            if (filteredLocalizations.isNotEmpty()) {
                LocalizationGrouping(
                    localizations = filteredLocalizations,
                    title = grouping.title,
                )
            } else {
                null
            }
        }
    }
}
