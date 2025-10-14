package com.superwall.sdk.paywall.view.webview.templating.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExperimentTemplate(
    @SerialName("eventName")
    val eventName: String = "experiment",
    @SerialName("experimentId")
    val experimentId: String,
    @SerialName("variantId")
    val variantId: String,
    @SerialName("campaignId")
    val campaignId: String,
)
