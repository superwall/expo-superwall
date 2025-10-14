package com.superwall.sdk.models

import java.time.Instant

data class LoadingInfo(
    val startAt: Instant? = null,
    val endAt: Instant? = null,
    val failAt: Instant? = null,
)
