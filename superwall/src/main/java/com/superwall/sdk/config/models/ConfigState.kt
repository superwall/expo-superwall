package com.superwall.sdk.config.models

import com.superwall.sdk.models.config.Config

internal sealed class ConfigState {
    object None : ConfigState()

    object Retrieving : ConfigState()

    object Retrying : ConfigState()

    data class Retrieved(
        val config: Config,
    ) : ConfigState()

    data class Failed(
        val throwable: Throwable,
    ) : ConfigState()
}

internal fun ConfigState.getConfig(): Config? =
    when (this) {
        is ConfigState.Retrieved -> config
        else -> null
    }
