package com.superwall.sdk.identity

//  File.kt
//
//
//  Created by Yusuf Tör on 16/09/2022.
//

import java.util.*

object IdentityLogic {
    enum class IdentityConfigurationAction {
        RESET,
        LOAD_ASSIGNMENTS,
    }

    fun generateAlias(): String = "\$SuperwallAlias:${UUID.randomUUID()}"

    fun generateSeed(): Int = (0 until 100).random()

    fun mergeAttributes(
        newAttributes: Map<String, Any?>,
        oldAttributes: Map<String, Any>,
        appInstalledAtString: String,
    ): MutableMap<String, Any> {
        val mergedAttributes = oldAttributes.toMutableMap()

        for (entry in newAttributes.entries) {
            val key = entry.key
            if (key == "\$is_standard_event" || key == "\$application_installed_at") {
                continue
            }

            var transformedKey = key
            if (transformedKey.startsWith("\$")) { // replace dollar signs
                transformedKey = transformedKey.replace("\$", "")
            }

            when (val value = entry.value) {
                is List<*> -> mergedAttributes[transformedKey] = value.filterNotNull()
                is Map<*, *> -> {
                    val nonNullMap = value.filterValues { it != null }
                    mergedAttributes[transformedKey] = nonNullMap.filterKeys { it != null }
                }
                null -> mergedAttributes.remove(transformedKey)
                else -> mergedAttributes[transformedKey] = value
            }
        }

        // we want camel case
        mergedAttributes["applicationInstalledAt"] = appInstalledAtString

        return mergedAttributes
    }

    /**
     * Logic to figure out whether to get assignments before firing triggers.
     *
     * The logic is:
     * - If is logged in to account that existed PRE static config, get assignments.
     * - If anonymous, is NOT first app open since install, and existed PRE static config, get assignments.
     * - If logged in POST static config, don't get assignments.
     * - If anonymous, is NOT first app open since install, and existed POST static config, don't get assignments.
     * - If anonymous, IS first app open since install, existed PRE static, don't get assignments.
     * - If anonymous, IS first app open since install, existed POST static, don't get assignments.
     */
    fun shouldGetAssignments(
        isLoggedIn: Boolean,
        neverCalledStaticConfig: Boolean,
        isFirstAppOpen: Boolean,
    ): Boolean {
        if (neverCalledStaticConfig) {
            if (isLoggedIn || !isFirstAppOpen) {
                return true
            }
        }

        return false
    }

    /**
     * Removes white spaces and new lines
     *
     * - Returns: An optional `String` of the trimmed `userId`. This is `null`
     * if the `userId` is empty.
     */
    fun sanitize(userId: String): String? {
        val userId = userId.trim()
        return if (userId.isEmpty()) {
            null
        } else {
            userId
        }
    }
}
