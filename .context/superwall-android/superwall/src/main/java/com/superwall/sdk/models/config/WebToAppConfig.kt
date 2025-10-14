package com.superwall.sdk.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WebToAppConfig {
    @SerialName("entitlements_max_age_ms")
    var entitlementsMaxAgeMs: Long = 0

    @SerialName("restore_access_url")
    var restoreAccesUrl: String? = null
}
