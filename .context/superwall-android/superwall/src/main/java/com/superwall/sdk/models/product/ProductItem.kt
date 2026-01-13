package com.superwall.sdk.models.product

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.entitlements.Entitlement
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class Store {
    @SerialName("PLAY_STORE")
    PLAY_STORE,

    @SerialName("APP_STORE")
    APP_STORE,

    @SerialName("STRIPE")
    STRIPE,

    @SerialName("PADDLE")
    PADDLE,

    @SerialName("SUPERWALL")
    SUPERWALL,

    @SerialName("OTHER")
    OTHER,

    ;

    companion object {
        fun fromValue(value: String): Store =
            when (value) {
                "PLAY_STORE" -> PLAY_STORE
                "APP_STORE" -> APP_STORE
                "STRIPE" -> STRIPE
                "PADDLE" -> PADDLE
                "SUPERWALL" -> SUPERWALL
                else -> OTHER
            }
    }
}

sealed class Offer {
    @Serializable
    data class Automatic(
        val type: String = "AUTOMATIC",
    ) : Offer()

    @Serializable
    data class Specified(
        val type: String = "SPECIFIED",
        val offerIdentifier: String,
    ) : Offer()
}

@Serializable(with = PlayStoreProductSerializer::class)
data class PlayStoreProduct(
    @SerialName("store")
    val store: Store = Store.PLAY_STORE,
    @SerialName("product_identifier")
    val productIdentifier: String,
    @SerialName("base_plan_identifier")
    val basePlanIdentifier: String,
    @SerialName("offer")
    val offer: Offer,
) {
    val fullIdentifier: String
        get() =
            when {
                basePlanIdentifier.isEmpty() -> productIdentifier
                offer is Offer.Automatic -> "$productIdentifier:$basePlanIdentifier:sw-auto"
                offer is Offer.Specified -> "$productIdentifier:$basePlanIdentifier:${offer.offerIdentifier}"
                else -> "$productIdentifier:$basePlanIdentifier"
            }
}

@Serializable
data class AppStoreProduct(
    @SerialName("store")
    val store: Store = Store.APP_STORE,
    @SerialName("product_identifier")
    val productIdentifier: String,
) {
    val fullIdentifier: String
        get() = productIdentifier
}

@Serializable
data class StripeProduct(
    @SerialName("store")
    val store: Store = Store.STRIPE,
    @SerialName("environment")
    val environment: String,
    @SerialName("product_identifier")
    val productIdentifier: String,
    @SerialName("trial_days")
    val trialDays: Int? = null,
) {
    val fullIdentifier: String
        get() = productIdentifier
}

@Serializable
data class PaddleProduct(
    @SerialName("store")
    val store: Store = Store.PADDLE,
    @SerialName("environment")
    val environment: String,
    @SerialName("product_identifier")
    val productIdentifier: String,
    @SerialName("trial_days")
    val trialDays: Int? = null,
) {
    val fullIdentifier: String
        get() = productIdentifier
}

@Serializable
data class UnknownStoreProduct(
    @SerialName("product_identifier")
    val productIdentifier: String,
    @SerialName("store")
    val store: Store = Store.OTHER,
)

object PlayStoreProductSerializer : KSerializer<PlayStoreProduct> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PlayStoreProduct")

    override fun serialize(
        encoder: Encoder,
        value: PlayStoreProduct,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("This class can be saved only by Json")
        val jsonObj =
            buildJsonObject {
                put("store", JsonPrimitive(value.store.name))
                put("product_identifier", JsonPrimitive(value.productIdentifier))
                put("base_plan_identifier", JsonPrimitive(value.basePlanIdentifier))
                val offer =
                    when (val offer = value.offer) {
                        is Offer.Automatic -> JsonObject(mapOf("type" to JsonPrimitive(offer.type)))
                        is Offer.Specified ->
                            JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive(offer.type),
                                    "offer_identifier" to JsonPrimitive(offer.offerIdentifier),
                                ),
                            )
                    }
                put("offer", offer)
            }
        jsonEncoder.encodeJsonElement(jsonObj)
    }

    override fun deserialize(decoder: Decoder): PlayStoreProduct {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("This class can be loaded only by Json")
        val jsonObject = jsonDecoder.decodeJsonElement() as JsonObject

        val store =
            try {
                Store.fromValue(
                    jsonObject["store"]?.jsonPrimitive?.content
                        ?: throw SerializationException("Store is missing"),
                )
            } catch (throwable: Throwable) {
                // / Default to play store
                Store.PLAY_STORE
            }
        val productIdentifier =
            jsonObject["product_identifier"]?.jsonPrimitive?.content
                ?: jsonObject["product"]
                    ?.jsonObject
                    ?.get("product_identifier")
                    ?.jsonPrimitive
                    ?.content
                ?: throw SerializationException("product_identifier is missing")
        val basePlanIdentifier =
            jsonObject["base_plan_identifier"]?.jsonPrimitive?.content ?: ""
        val offerJsonObject =
            jsonObject["offer"] as? JsonObject
        val type = offerJsonObject?.get("type")?.jsonPrimitive?.content

        val offer =
            when (type) {
                "AUTOMATIC" -> Offer.Automatic()
                "SPECIFIED" -> {
                    val offerIdentifier =
                        offerJsonObject["offer_identifier"]?.jsonPrimitive?.content
                            ?: throw SerializationException("offer_identifier is missing")
                    Offer.Specified(offerIdentifier = offerIdentifier)
                }

                else -> {
                    Logger.debug(
                        LogLevel.debug,
                        LogScope.configManager,
                        "Unknown offer type for $productIdentifier, fallback to none",
                    )
                    Offer.Specified(offerIdentifier = "sw-none")
                }
            }

        return PlayStoreProduct(store, productIdentifier, basePlanIdentifier, offer)
    }
}

object StoreProductSerializer : KSerializer<ProductItem.StoreProductType> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StoreProduct")

    override fun serialize(
        encoder: Encoder,
        value: ProductItem.StoreProductType,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("This class can be saved only by Json")
        val jsonElement =
            when (value) {
                is ProductItem.StoreProductType.PlayStore ->
                    jsonEncoder.json.encodeToJsonElement(PlayStoreProductSerializer, value.product)

                is ProductItem.StoreProductType.AppStore ->
                    jsonEncoder.json.encodeToJsonElement(
                        AppStoreProduct.serializer(),
                        value.product,
                    )

                is ProductItem.StoreProductType.Stripe ->
                    jsonEncoder.json.encodeToJsonElement(StripeProduct.serializer(), value.product)

                is ProductItem.StoreProductType.Paddle ->
                    jsonEncoder.json.encodeToJsonElement(PaddleProduct.serializer(), value.product)

                is ProductItem.StoreProductType.Other ->
                    jsonEncoder.json.encodeToJsonElement(
                        UnknownStoreProduct.serializer(),
                        value.product,
                    )
            }
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): ProductItem.StoreProductType {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("This class can be loaded only by Json")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val storeValue =
            jsonObject["store"]?.jsonPrimitive?.content
                ?: jsonObject["product"]
                    ?.jsonObject
                    ?.get("store")
                    ?.jsonPrimitive
                    ?.content
                ?: throw SerializationException("Store is missing")
        val store =
            try {
                Store.fromValue(storeValue)
            } catch (throwable: Throwable) {
                Store.PLAY_STORE
            }
        val json = jsonDecoder.json
        return when (store) {
            Store.PLAY_STORE -> {
                val product = json.decodeFromJsonElement(PlayStoreProductSerializer, jsonObject)
                ProductItem.StoreProductType.PlayStore(product)
            }

            Store.APP_STORE -> {
                val product = json.decodeFromJsonElement(AppStoreProduct.serializer(), jsonObject)
                ProductItem.StoreProductType.AppStore(product)
            }

            Store.STRIPE -> {
                val product = json.decodeFromJsonElement(StripeProduct.serializer(), jsonObject)
                ProductItem.StoreProductType.Stripe(product)
            }

            Store.PADDLE -> {
                val product = json.decodeFromJsonElement(PaddleProduct.serializer(), jsonObject)
                ProductItem.StoreProductType.Paddle(product)
            }

            Store.SUPERWALL,
            Store.OTHER,
            -> {
                val product =
                    json.decodeFromJsonElement(UnknownStoreProduct.serializer(), jsonObject)
                ProductItem.StoreProductType.Other(product)
            }
        }
    }
}

sealed interface TemplatingProduct

@Serializable(with = ProductItemSerializer::class)
data class ProductItem(
    @SerialName("sw_composite_product_id")
    val compositeId: String,
    // Note: This is used only by paywall as a reference to the object. Otherwise, it is empty.
    @SerialName("reference_name")
    val name: String,
    @SerialName("store_product")
    val type: StoreProductType,
    @SerialName("entitlements")
    val entitlements: Set<Entitlement>,
) : TemplatingProduct {
    @Serializable
    sealed class StoreProductType {
        @Serializable
        data class PlayStore(
            val product: PlayStoreProduct,
        ) : StoreProductType()

        @Serializable
        data class AppStore(
            val product: AppStoreProduct,
        ) : StoreProductType()

        @Serializable
        data class Stripe(
            val product: StripeProduct,
        ) : StoreProductType()

        @Serializable
        data class Paddle(
            val product: PaddleProduct,
        ) : StoreProductType()

        @Serializable
        data class Other(
            val product: UnknownStoreProduct,
        ) : StoreProductType()
    }

    val fullProductId: String
        get() =
            when (type) {
                is StoreProductType.PlayStore -> type.product.fullIdentifier
                is StoreProductType.AppStore -> type.product.fullIdentifier
                is StoreProductType.Stripe -> type.product.fullIdentifier
                is StoreProductType.Paddle -> type.product.fullIdentifier
                is StoreProductType.Other -> type.product.productIdentifier
            }

    companion object {
        fun fromCrossplatformProduct(product: CrossplatformProduct) =
            ProductItem(
                name = product.name,
                entitlements = product.entitlements.toSet(),
                type = product.storeProduct.toStoreProductType(),
                compositeId = product.compositeId,
            )
    }
}

@Serializer(forClass = ProductItem::class)
object ProductItemSerializer : KSerializer<ProductItem> {
    override fun serialize(
        encoder: Encoder,
        value: ProductItem,
    ) {
        // Create a JSON object with custom field names for serialization
        val jsonOutput =
            encoder as? JsonEncoder
                ?: throw SerializationException("This class can be saved only by Json")
        val jsonObject =
            buildJsonObject {
                put("product", JsonPrimitive(value.name))
                put("productId", JsonPrimitive(value.fullProductId))
                val storeProductElement =
                    jsonOutput.json.encodeToJsonElement(StoreProductSerializer, value.type)
                put("store_product", storeProductElement)
            }
        // Encode the JSON object
        jsonOutput.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): ProductItem {
        // Decode the JSON object
        val jsonInput =
            decoder as? JsonDecoder
                ?: throw SerializationException("This class can be loaded only by Json")
        val jsonObject = jsonInput.decodeJsonElement().jsonObject

        // Extract fields using the expected names during deserialization
        val name = jsonObject["reference_name"]?.jsonPrimitive?.content ?: ""
        val storeProductJsonObject =
            jsonObject["store_product"]?.jsonObject
                ?: throw SerializationException("Missing store_product")
        val entitlements =
            jsonObject["entitlements"]
                ?.jsonArray
                ?.map {
                    Json.decodeFromJsonElement<Entitlement>(it)
                }?.toSet() ?: emptySet()

        val storeProductType =
            jsonInput.json.decodeFromJsonElement(StoreProductSerializer, storeProductJsonObject)

        val compositeIdFromJson = jsonObject["sw_composite_product_id"]?.jsonPrimitive?.content
        val compositeId =
            compositeIdFromJson
                ?: when (storeProductType) {
                    is ProductItem.StoreProductType.PlayStore -> storeProductType.product.fullIdentifier
                    is ProductItem.StoreProductType.AppStore -> storeProductType.product.fullIdentifier
                    is ProductItem.StoreProductType.Stripe -> storeProductType.product.fullIdentifier
                    is ProductItem.StoreProductType.Paddle -> storeProductType.product.fullIdentifier
                    is ProductItem.StoreProductType.Other -> storeProductType.product.productIdentifier
                }

        return ProductItem(
            name = name,
            type = storeProductType,
            entitlements = entitlements,
            compositeId = compositeId,
        )
    }
}

object ProductItemsDeserializer : KSerializer<List<ProductItem>> {
    override val descriptor: SerialDescriptor =
        listSerialDescriptor(ProductItem.serializer().descriptor)

    override fun serialize(
        encoder: Encoder,
        value: List<ProductItem>,
    ) {
        encoder.encodeSerializableValue(ListSerializer(ProductItem.serializer()), value)
    }

    override fun deserialize(decoder: Decoder): List<ProductItem> {
        // Ensure we're working with JsonDecoder and thus can navigate the JSON structure
        require(decoder is JsonDecoder) // This line ensures we have a JsonDecoder

        // Decode the entire document as JsonElement
        val productsV2Element = decoder.decodeJsonElement().jsonArray

        // Use a mutable list to collect valid ProductItem instances
        val validProducts = mutableListOf<ProductItem>()

        // Process each product in the array
        for (productElement in productsV2Element) {
            try {
                val product = Json.decodeFromJsonElement<ProductItem>(productElement)
                validProducts.add(product)
            } catch (e: SerializationException) {
                // Catch and ignore items that cannot be deserialized due to unknown store types or other issues
                // Log the error or handle it as needed
            }
        }

        return validProducts
    }
}
