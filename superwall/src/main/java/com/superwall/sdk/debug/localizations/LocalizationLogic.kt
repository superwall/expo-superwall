package com.superwall.sdk.debug.localizations

import java.util.Locale

object LocalizationLogic {
    fun getSortedLocalizations(
        localeIds: List<String>,
        popularLocales: List<String>,
    ): List<LocalizationOption> {
        val localizations = mutableListOf<LocalizationOption>()

        for (localeId in localeIds) {
            // Get language
            var localizedLanguage = ""
            val locale = Locale(localeId)
            val localeIdComponents = localeId.split("_")

            when {
                locale.displayLanguage.isNotEmpty() -> localizedLanguage = locale.displayLanguage
                localeIdComponents.size > 1 -> localizedLanguage = Locale("", localeIdComponents.first()).displayLanguage
            }

            // Get country
            var country: String? = null

            when {
                locale.displayCountry.isNotEmpty() -> country = locale.displayCountry
                localeIdComponents.size > 1 -> country = Locale("", localeIdComponents.last()).displayCountry
            }

            val localizationOption =
                LocalizationOption(
                    language = localizedLanguage,
                    country = country,
                    locale = localeId,
                    popularLocales = popularLocales,
                )
            localizations.add(localizationOption)
        }

        // Sort in ascending manner
        localizations.sortBy { it.sortDescription }
        return localizations
    }

    fun getGroupings(localizationOptions: List<LocalizationOption>): List<LocalizationGrouping> {
        val groupings = mutableListOf<LocalizationGrouping>()

        for (localizationOption in localizationOptions) {
            val currentTitle = localizationOption.sectionTitle
            val currentGrouping = groupings.lastOrNull()

            if (currentGrouping?.title != currentTitle) {
                groupings.add(LocalizationGrouping(localizations = mutableListOf(), title = currentTitle))
            }

            groupings.last().localizations.add(localizationOption)
        }

        return groupings
    }
}
