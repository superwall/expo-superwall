package com.superwall.sdk.models.internal

import com.superwall.sdk.models.paywall.LocalNotificationTypeSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.serializersModuleOf
import org.junit.Test

class WebRedemptionResultTest {
    @Test
    fun test_serialising_of_results() {
        val json =
            """
            {
  "codes": [
    {
      "status": "SUCCESS",
      "code": "redemption_c6358802-ab07-4642-a361-d7352473505a",
      "redemptionInfo": {
        "ownership": {
          "type": "APP_USER",
          "appUserId": "Alias:d1ba57dc-15bb-46e3-864c-a1f8805fe746"
        },
        "purchaserInfo": {
          "appUserId": "Alias:d1ba57dc-15bb-46e3-864c-a1f8805fe746",
          "email": "ian@superwall.com",
          "storeIdentifiers": {
            "store": "STRIPE",
            "stripeCustomerId" : "asdasf",
            "stripeSubscriptionIds": ["sub_1R3bMoBitwqMmwU0Wm4OgMsj"]
          }
        },
        "paywallInfo": null,
        "entitlements": [
          {
            "identifier": "pro",
            "type": "SERVICE_LEVEL"
          }
        ]
      }
    }
  ],
  "customerInfo": {
    "subscriptions": [],
    "nonSubscriptions": [],
    "userId": "",
    "entitlements": [
      {
        "identifier": "pro",
        "type": "SERVICE_LEVEL"
      }
    ]
  }
}
            """.trimIndent()

        val result =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                namingStrategy = JsonNamingStrategy.SnakeCase
                serializersModule =
                    serializersModuleOf(
                        LocalNotificationTypeSerializer,
                    )
            }.decodeFromString<WebRedemptionResponse>(json)
        assert(result != null)
    }

    @Test
    fun test_code_expired() {
        val json = """{
  "codes": [
    {
      "status": "CODE_EXPIRED",
      "code": "redemption_296db3d9-0f38-4399-a809-25b50849c753",
      "expired": {
        "resent": false,
        "obfuscatedEmail": null
      }
    }
  ]
}"""

        val result = Json { ignoreUnknownKeys = true }.decodeFromString<WebRedemptionResponse>(json)
        assert(result != null)
    }
}
