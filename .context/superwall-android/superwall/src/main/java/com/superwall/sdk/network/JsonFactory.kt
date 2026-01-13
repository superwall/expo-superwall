package com.superwall.sdk.network

import com.superwall.sdk.models.serialization.DateSerializer
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.util.Date

interface JsonFactory {
    companion object {
        val JSON =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                namingStrategy = JsonNamingStrategy.SnakeCase
                serializersModule =
                    SerializersModule {
                        contextual(Date::class, DateSerializer)
                    }
            }

        val JSON_POLYMORPHIC =
            Json(JSON) {
                classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
                classDiscriminator = "type"
            }
    }

    fun json() = JSON
}
