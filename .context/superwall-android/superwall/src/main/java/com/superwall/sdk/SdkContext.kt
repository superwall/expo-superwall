package com.superwall.sdk

import com.superwall.sdk.config.ConfigManager
import com.superwall.sdk.misc.awaitFirstValidConfig
import com.superwall.sdk.models.config.Config

/**
 * Cross-slice bridge used by the identity actor to call into other managers.
 *
 * Keeps the identity slice decoupled from concrete manager types.
 */
interface SdkContext {
    suspend fun reevaluateTestMode(appUserId: String?, aliasId: String?)

    suspend fun fetchAssignments()

    suspend fun awaitConfig(): Config?
}

class SdkContextImpl(
    private val configManager: () -> ConfigManager,
) : SdkContext {
    override suspend fun reevaluateTestMode(appUserId: String?, aliasId: String?) {
        configManager().reevaluateTestMode(appUserId = appUserId, aliasId = aliasId)
    }

    override suspend fun fetchAssignments() {
        configManager().getAssignments()
    }

    override suspend fun awaitConfig(): Config? =
        configManager().configState.awaitFirstValidConfig()
}
