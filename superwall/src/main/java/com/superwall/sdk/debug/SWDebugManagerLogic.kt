package com.superwall.sdk.debug

import android.net.Uri

object SWDebugManagerLogic {
    enum class Parameter(
        val value: String,
    ) {
        TOKEN("token"),
        PAYWALL_ID("paywall_id"),
        SUPERWALL_DEBUG("superwall_debug"),
    }

    fun getQueryItemValue(
        uri: Uri,
        name: Parameter,
    ): String? {
        val query = uri.query ?: return null
        val queryItems =
            query
                .split("&")
                .map {
                    val parts = it.split("=")
                    Pair(parts[0], parts.getOrElse(1) { "" })
                }.toMap()
        return queryItems[name.value]
    }
}
