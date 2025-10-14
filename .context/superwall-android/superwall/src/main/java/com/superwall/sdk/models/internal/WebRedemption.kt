@file:OptIn(ExperimentalSerializationApi::class)

package com.superwall.sdk.models.internal

import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.Redeemable
import com.superwall.sdk.models.paywall.PaywallIdentifier
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

typealias ExperimentId = String
typealias VariantId = String
typealias RedemptionCode = String

@Serializable
data class WebRedemptionResponse(
    @SerialName("codes")
    val codes: List<RedemptionResult>,
    @SerialName("entitlements")
    val entitlements: List<Entitlement>,
    @kotlinx.serialization.Transient
    val allCodes: List<Redeemable> = codes.map { Redeemable(it.code, false) },
)

@Serializable
@JsonClassDiscriminator("status")
sealed class RedemptionResult {
    abstract val code: String

    val stripeSubscriptionId: List<String?>
        get() =
            when (this) {
                is RedemptionResult.Success ->
                    when (this.redemptionInfo.purchaserInfo?.storeIdentifiers) {
                        is StoreIdentifiers.Stripe ->
                            this.redemptionInfo.purchaserInfo
                                ?.storeIdentifiers
                                ?.subscriptionIds
                                ?: listOf()

                        else -> listOf()
                    }
                else -> listOf()
            }

    val paddleSubscriptionIds: List<String?>
        get() =
            when (this) {
                is RedemptionResult.Success ->
                    when (this.redemptionInfo.purchaserInfo?.storeIdentifiers) {
                        is StoreIdentifiers.Paddle -> {
                            this.redemptionInfo.purchaserInfo
                                ?.storeIdentifiers
                                ?.paddleSubscriptionIds ?: listOf()
                        }
                        else -> listOf()
                    }

                else -> listOf()
            }

    val subscriptionIds: List<String?>
        get() = stripeSubscriptionId + paddleSubscriptionIds

    // Represents that a redemption was successful
    @Serializable(with = DirectSuccessSerializer::class)
    @SerialName("SUCCESS")
    data class Success(
        @SerialName("code")
        override val code: String,
        @SerialName("redemption_info")
        val redemptionInfo: RedemptionInfo,
    ) : RedemptionResult()

    // Represents that a redemption failed
    @Serializable
    @SerialName("ERROR")
    data class Error(
        @SerialName("code")
        override val code: RedemptionCode,
        @SerialName("error")
        val error: ErrorInfo,
    ) : RedemptionResult()

    // Code expired
    @Serializable
    @SerialName("CODE_EXPIRED")
    data class Expired(
        @SerialName("code")
        override val code: RedemptionCode,
        @SerialName("expired")
        val expired: ExpiredInfo,
    ) : RedemptionResult()

    // Invalid code
    @Serializable
    @SerialName("INVALID_CODE")
    data class InvalidCode(
        @SerialName("code")
        override val code: RedemptionCode,
    ) : RedemptionResult()

    // Expired subscription
    @Serializable
    @SerialName("EXPIRED_SUBSCRIPTION")
    data class ExpiredSubscription(
        @SerialName("code")
        override val code: RedemptionCode,
        @SerialName("redemptionInfo")
        val redemptionInfo: RedemptionInfo,
    ) : RedemptionResult()

    @Serializable
    data class PaywallInfo(
        @SerialName("identifier")
        val identifier: PaywallIdentifier,
        @SerialName("placementName")
        val placementName: String,
        @SerialName("placementParams")
        val placementParams: Map<String, JsonElement>,
        @SerialName("variantId")
        val variantId: VariantId,
        @SerialName("experimentId")
        val experimentId: ExperimentId,
        @SerialName("productIdentifier")
        val productIdentifier: String? = null,
    )
}

@Serializable
data class RedemptionInfo(
    // Ownership of the redemption
    @SerialName("ownership")
    val ownership: RedemptionOwnership,
    // Who originally bought the subscription we're redeeming.
    @SerialName("purchaserInfo")
    val purchaserInfo: PurchaserInfo,
    // Can be null if the redemption was not tied to a paywall,
    // like when an entitlement is granted directly from the dashboard.
    // TODO: Consider merging these into one "paywallInfo"
    @SerialName("paywallInfo")
    val paywallInfo: RedemptionResult.PaywallInfo? = null,
    // We'll always have a transaction info, b/c these fields are
    // present in the transaction info.
    // transactionInfo: TransactionInfo,
    // Entitlements that were granted as a result of the redemption.
    @SerialName("entitlements")
    val entitlements: List<Entitlement>,
)

@Serializable
data class PurchaserInfo(
    // Who actually bought the subscription, can be
    // used to alias the purchaser to the app user.
    @SerialName("appUserId")
    val appUserId: String,
    @SerialName("email")
    val email: String? = null,
    @SerialName("storeIdentifiers")
    val storeIdentifiers: StoreIdentifiers,
)

//
// Store Transaction Info
//
// Unique per store, can be used to identify real identifiers within each store.
// We'd have a "superwall" type, but it's not yet implemented.

@Serializable
@JsonClassDiscriminator("store")
sealed class StoreIdentifiers {
    @Serializable
    @SerialName("STRIPE")
    data class Stripe(
        @SerialName("stripeCustomerId")
        val stripeCustomerId: String,
        @SerialName("stripeSubscriptionIds")
        val subscriptionIds: List<String>,
    ) : StoreIdentifiers()

    @Serializable
    @SerialName("PADDLE")
    data class Paddle(
        @SerialName("paddleCustomerId")
        val paddleCustomerId: String,
        @SerialName("paddleSubscriptionId")
        val paddleSubscriptionIds: List<String>,
    )

    @Serializable
    @SerialName("UNKNOWN")
    data class Unknown(
        val properties: Map<String, JsonElement?>,
    ) : StoreIdentifiers()
}

@Serializable
data class ErrorInfo(
    @SerialName("message")
    val message: String,
)

@Serializable
data class ExpiredInfo(
    @SerialName("resent")
    val resent: Boolean,
    @SerialName("obfuscatedEmail")
    val obfuscatedEmail: String? = null,
)

@Serializable
@JsonClassDiscriminator("type")
sealed class RedemptionOwnership {
    @Serializable
    @SerialName("DEVICE")
    data class Device(
        @SerialName("deviceId")
        val deviceId: String,
    ) : RedemptionOwnership()

    @Serializable
    @SerialName("APP_USER")
    data class AppUser(
        @SerialName("appUserId")
        val appUserId: String,
    ) : RedemptionOwnership()
}

@Serializable
enum class RedemptionOwnershipType {
    @SerialName("device")
    Device,

    @SerialName("app_user")
    AppUser,
}

// Custom serializer due to issue with nested polymorphic serialization
object DirectSuccessSerializer : KSerializer<RedemptionResult.Success> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("SUCCESS") {
            element<String>("code")
            element<JsonElement>("redemptionInfo")
        }

    override fun serialize(
        encoder: Encoder,
        value: RedemptionResult.Success,
    ) {
        val compositeEncoder = encoder.beginStructure(descriptor)
        compositeEncoder.encodeStringElement(descriptor, 0, value.code)

        val json =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
        val redemptionInfoJson =
            json.encodeToJsonElement(RedemptionInfo.serializer(), value.redemptionInfo)

        compositeEncoder.encodeSerializableElement(
            descriptor,
            1,
            JsonElement.serializer(),
            redemptionInfoJson,
        )

        compositeEncoder.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): RedemptionResult.Success {
        // Cast to JsonDecoder to access the JSON directly
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("Expected JSON decoder")

        // Get the input JSON object directly
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        // Extract fields directly from JSON
        val code =
            jsonObject["code"]?.jsonPrimitive?.content
                ?: throw SerializationException("Required field 'code' was not found")

        // Get the redemptionInfo as a JsonElement
        val redemptionInfoJson =
            (jsonObject["redemption_info"] ?: jsonObject["redemptionInfo"])
                ?: throw SerializationException("Required field 'redemptionInfo' was not found")

        // Then parse it with the RedemptionInfo serializer
        val redemptionInfo =
            Json.decodeFromJsonElement(
                RedemptionInfo.serializer(),
                redemptionInfoJson,
            )

        return RedemptionResult.Success(code = code, redemptionInfo = redemptionInfo)
    }
}
