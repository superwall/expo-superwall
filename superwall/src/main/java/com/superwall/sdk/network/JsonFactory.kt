package com.superwall.sdk.network

import com.superwall.sdk.models.paywall.LocalNotificationTypeSerializer
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.serializersModuleOf

interface JsonFactory {
    companion object {
        val JSON =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                namingStrategy = JsonNamingStrategy.SnakeCase
                serializersModule =
                    serializersModuleOf(
                        LocalNotificationTypeSerializer,
                    )
            }

        val JSON_POLYMORPHIC =
            Json(JSON) {
                classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
                classDiscriminator = "type"
            }
    }

    fun json() = JSON
}
