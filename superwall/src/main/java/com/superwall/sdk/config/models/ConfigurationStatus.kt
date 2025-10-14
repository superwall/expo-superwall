package com.superwall.sdk.config.models

/**
* Derived from ConfigState.kt.
* Represents the current configuration status of the SDK.
* Can be one of:
* - Pending: The configuration process is not yet completed.
* - Configured: The configuration process completed successfully.
* - Failed: The configuration process failed.
* */
sealed class ConfigurationStatus {
    object Pending : ConfigurationStatus()

    object Configured : ConfigurationStatus()

    object Failed : ConfigurationStatus()
}
