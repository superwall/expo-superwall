package com.superwall.sdk.network.device

import com.superwall.sdk.analytics.superwall.SuperwallEvents
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
internal sealed class Capability(
    @SerialName("name")
    val name: String,
) {
    @Serializable
    @SerialName("paywall_event_receiver")
    class PaywallEventReceiver : Capability("paywall_event_receiver") {
        @SerialName("event_names")
        val eventNames =
            listOf(
                SuperwallEvents.TransactionStart,
                SuperwallEvents.TransactionRestore,
                SuperwallEvents.TransactionComplete,
                SuperwallEvents.RestoreStart,
                SuperwallEvents.RestoreFail,
                SuperwallEvents.RestoreComplete,
                SuperwallEvents.TransactionFail,
                SuperwallEvents.TransactionAbandon,
                SuperwallEvents.TransactionTimeout,
                SuperwallEvents.PaywallOpen,
                SuperwallEvents.PaywallClose,
            ).map { it.rawName }
    }

    @Serializable
    @SerialName("multiple_paywall_urls")
    object MultiplePaywallUrls : Capability("multiple_paywall_urls")

    @Serializable
    @SerialName("config_caching")
    object ConfigCaching : Capability("config_caching")
}

internal fun List<Capability>.toJson(json: Json): JsonElement =
    json.encodeToJsonElement(
        ListSerializer(Capability.serializer()),
        this,
    )

internal fun List<Capability>.namesCommaSeparated(): String = this.joinToString(",") { it.name }
