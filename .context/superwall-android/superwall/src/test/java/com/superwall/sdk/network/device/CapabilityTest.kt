package com.superwall.sdk.network.device

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityTest {
    @Test
    fun `capability names match the wire-format discriminators the server expects`() {
        // Server-side feature gates key off these strings — drift breaks routing.
        assertEquals("paywall_event_receiver", Capability.PaywallEventReceiver().name)
        assertEquals("multiple_paywall_urls", Capability.MultiplePaywallUrls.name)
        assertEquals("config_caching", Capability.ConfigCaching.name)
    }

    @Test
    fun `toJson emits objects with type and name discriminators`() {
        // Hand-rolled to avoid kotlinx.serialization sealed-class polymorphic
        // discovery, which breaks under R8 full-mode in customer apps. The
        // wire shape must stay byte-compatible with the previous polymorphic
        // encoding (both `type` and `name` keys present).
        val list: List<Capability> =
            listOf(
                Capability.MultiplePaywallUrls,
                Capability.ConfigCaching,
            )
        val arr = list.toJson() as JsonArray
        assertEquals(2, arr.size)

        val first = arr[0] as JsonObject
        assertEquals(JsonPrimitive("multiple_paywall_urls"), first["type"])
        assertEquals(JsonPrimitive("multiple_paywall_urls"), first["name"])
        assertNull(first["event_names"])

        val second = arr[1] as JsonObject
        assertEquals(JsonPrimitive("config_caching"), second["type"])
        assertEquals(JsonPrimitive("config_caching"), second["name"])
    }

    @Test
    fun `toJson includes event_names for PaywallEventReceiver`() {
        val arr = listOf<Capability>(Capability.PaywallEventReceiver()).toJson() as JsonArray
        val obj = arr.single() as JsonObject
        assertEquals(JsonPrimitive("paywall_event_receiver"), obj["type"])
        val events = obj["event_names"] as JsonArray
        assertTrue("event_names must not be empty", events.isNotEmpty())
        // Sanity: contains a known event the server expects.
        assertTrue(events.any { it == JsonPrimitive("paywall_open") })
    }

    @Test
    fun `namesCommaSeparated returns canonical name strings`() {
        val s = listOf(Capability.MultiplePaywallUrls, Capability.ConfigCaching).namesCommaSeparated()
        assertEquals("multiple_paywall_urls,config_caching", s)
    }
}
