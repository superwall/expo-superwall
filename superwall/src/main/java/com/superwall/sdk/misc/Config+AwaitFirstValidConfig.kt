package com.superwall.sdk.misc

import com.superwall.sdk.config.models.ConfigState
import com.superwall.sdk.models.config.Config
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

internal suspend fun Flow<ConfigState>.awaitFirstValidConfig(): Config =
    try {
        filterIsInstance<ConfigState.Retrieved>()
            .first()
            .config
    } catch (e: Throwable) {
        throw e
    }
