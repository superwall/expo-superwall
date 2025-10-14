package com.superwall.sdk.config.models

import kotlinx.serialization.Serializable

@Serializable
data class SurveyOption(
    val id: String,
    val title: String,
)
