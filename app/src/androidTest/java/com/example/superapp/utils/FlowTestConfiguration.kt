package com.example.superapp.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class FlowTestConfiguration(
    val waitForConfig: Boolean = true,
    val timeout: Duration = 5.minutes,
)
