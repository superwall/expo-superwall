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
    ;

    companion object {
        fun fromValue(value: String): Store =
            when (value) {
                "PLAY_STORE" -> PLAY_STORE
                else -> throw SerializationException("Store must be PLAY_STORE, found: $value")
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
            when (offer) {
                is Offer.Automatic -> "$productIdentifier:$basePlanIdentifier:sw-auto"
                is Offer.Specified -> "$productIdentifier:$basePlanIdentifier:${offer.offerIdentifier}"
            }
}

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

@Serializable(with = ProductItemSerializer::class)
data class ProductItem(
    // Note: This is used only by paywall as a reference to the object. Otherwise, it is empty.
    @SerialName("reference_name")
    val name: String,
    @SerialName("store_product")
    val type: StoreProductType,
    @SerialName("entitlements")
    val entitlements: Set<Entitlement>,
) {
    @Serializable
    sealed class StoreProductType {
        @Serializable
        data class PlayStore(
            val product: PlayStoreProduct,
        ) : StoreProductType()
    }

    val fullProductId: String
        get() =
            when (type) {
                is StoreProductType.PlayStore -> type.product.fullIdentifier
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
                put("store_product", encoder.json.encodeToJsonElement(value.type))
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

        // Deserialize 'storeProduct' JSON object into the expected Kotlin data class
        val storeProduct = Json.decodeFromJsonElement<PlayStoreProduct>(storeProductJsonObject)

        return ProductItem(
            name = name,
            type = ProductItem.StoreProductType.PlayStore(storeProduct),
            entitlements = entitlements,
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
                // Check the store type and add to the list if it matches the criteria
                if (product.type is ProductItem.StoreProductType.PlayStore) {
                    validProducts.add(product)
                }
                // If the type is APP_STORE or anything else, it will simply skip adding it
            } catch (e: SerializationException) {
                // Catch and ignore items that cannot be deserialized due to unknown store types or other issues
                // Log the error or handle it as needed
            }
        }

        return validProducts
    }
}
