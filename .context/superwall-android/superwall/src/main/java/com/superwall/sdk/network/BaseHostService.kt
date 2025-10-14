package com.superwall.sdk.network

import com.superwall.sdk.dependencies.ApiFactory
import com.superwall.sdk.misc.Either
import com.superwall.sdk.models.assignment.AssignmentPostback
import com.superwall.sdk.models.assignment.ConfirmedAssignmentResponse
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.paywall.Paywalls
import com.superwall.sdk.network.session.CustomHttpUrlConnection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BaseHostService(
    override val host: String,
    override val version: String,
    val factory: ApiFactory,
    private val json: Json,
    override val customHttpUrlConnection: CustomHttpUrlConnection,
) : NetworkService() {
    override suspend fun makeHeaders(
        isForDebugging: Boolean,
        requestId: String,
    ): Map<String, String> = factory.makeHeaders(isForDebugging, requestId)

    suspend fun config(requestId: String) =
        get<Config>(
            "static_config",
            requestId = requestId,
            queryItems = listOf(URLQueryItem("pk", factory.storage.apiKey)),
        )

    suspend fun assignments() = get<ConfirmedAssignmentResponse>("assignments")

    suspend fun confirmAssignments(confirmableAssignments: AssignmentPostback) =
        post<ConfirmedAssignmentResponse>(
            "confirm_assignments",
            body = json.encodeToString(confirmableAssignments).toByteArray(),
        )

    suspend fun paywalls(isForDebugging: Boolean = false) = get<Paywalls>(path = "paywalls", isForDebugging = isForDebugging)

    suspend fun paywall(identifier: String? = null): Either<Paywall, NetworkError> {
        // WARNING: Do not modify anything about this request without considering our cache eviction code
        // we must know all the exact urls we need to invalidate so changing the order, inclusion, etc of any query
        // parameters will cause issues
        val queryItems = mutableListOf(URLQueryItem("pk", factory.storage.apiKey))

        // In the config endpoint we return all the locales, this code will check if:
        // 1. The device locale (ex: en_US) exists in the locales list
        // 2. The shortened device locale (ex: en) exists in the locale list
        // If either exist (preferring the most specific) include the locale in the
        // the url as a query param.
        factory.configManager.config?.let { config ->
            if (config.locales.contains(factory.deviceHelper.locale)) {
                val localeQuery =
                    URLQueryItem(
                        name = "locale",
                        value = factory.deviceHelper.locale,
                    )
                queryItems.add(localeQuery)
            } else {
                val shortLocale = factory.deviceHelper.locale.split("_")[0]
                if (config.locales.contains(shortLocale)) {
                    val localeQuery =
                        URLQueryItem(
                            name = "locale",
                            value = shortLocale,
                        )
                    queryItems.add(localeQuery)
                }
                return@let
            }
        }

        return get<Paywall>("paywall/$identifier", queryItems = queryItems, isForDebugging = true)
    }
}
