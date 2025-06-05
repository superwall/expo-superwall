package expo.modules.superwallexpo.json

import com.superwall.sdk.config.models.ConfigurationStatus

fun ConfigurationStatus.asString(): String {
    return when (this) {
        ConfigurationStatus.Pending -> "PENDING"
        ConfigurationStatus.Configured -> "CONFIGURED"
        ConfigurationStatus.Failed -> "FAILED"
    }
}
