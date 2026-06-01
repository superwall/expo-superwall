package com.superwall.sdk.network.device

import com.superwall.sdk.analytics.superwall.SuperwallEvents
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// NOTE: not @Serializable. Polymorphic sealed-class encoding breaks under
// R8 minification in customer apps (the auto-generated $serializer reflects
// on sealedSubclasses metadata that R8 strips). We hand-build the JSON in
// `toJson` instead — same wire format, no kotlinx.serialization runtime
// reflection on consumers' obfuscated classpaths.
internal sealed class Capability(
    val name: String,
) {
    class PaywallEventReceiver : Capability("paywall_event_receiver") {
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

    object MultiplePaywallUrls : Capability("multiple_paywall_urls")

    object ConfigCaching : Capability("config_caching")
}

internal fun List<Capability>.toJson(): JsonElement =
    buildJsonArray {
        forEach { cap ->
            add(
                buildJsonObject {
                    // `type` discriminator preserves byte-compat with the
                    // previous polymorphic encoding; some downstream readers
                    // may still key off it.
                    put("type", cap.name)
                    put("name", cap.name)
                    if (cap is Capability.PaywallEventReceiver) {
                        put(
                            "event_names",
                            JsonArray(cap.eventNames.map { JsonPrimitive(it) }),
                        )
                    }
                },
            )
        }
    }

internal fun List<Capability>.namesCommaSeparated(): String = this.joinToString(",") { it.name }
