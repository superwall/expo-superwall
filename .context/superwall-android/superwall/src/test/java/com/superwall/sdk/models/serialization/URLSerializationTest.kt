package com.superwall.sdk.models.serialization

import com.superwall.sdk.models.paywall.PaywallURL
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class PaywallURLSerializerTest {
    private val json =
        Json {
        }

    @Serializable
    data class TestData(
        val url: PaywallURL,
    )

    @Test
    fun testSerializeDeserialize() {
        val originalUrl = PaywallURL("https://example.com")
        val data = TestData(originalUrl)

        val jsonString = json.encodeToString(data)
        val decodedData = json.decodeFromString<TestData>(jsonString)

        assertEquals(originalUrl, decodedData.url)
    }
}
