package com.superwall.sdk.paywall.view.webview

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import org.json.JSONObject
import java.net.URI

data class WrappedPaywallMessages(
    var version: Int = 1,
    val payload: PayloadMessages,
)

data class PayloadMessages(
    val messages: List<PaywallMessage>,
)

sealed class PaywallMessage {
    data class OnReady(
        val paywallJsVersion: String,
    ) : PaywallMessage()

    object TemplateParamsAndUserAttributes : PaywallMessage()

    object Close : PaywallMessage()

    object Restore : PaywallMessage()

    data class RestoreFailed(
        val message: String,
    ) : PaywallMessage()

    data class OpenUrl(
        val url: URI,
    ) : PaywallMessage()

    data class OpenUrlInBrowser(
        val url: URI,
    ) : PaywallMessage()

    data class OpenDeepLink(
        val url: URI,
    ) : PaywallMessage()

    data class Purchase(
        val product: String,
        val productId: String,
    ) : PaywallMessage()

    data class Custom(
        val data: String,
    ) : PaywallMessage()

    data class CustomPlacement(
        val name: String,
        val params: JSONObject,
    ) : PaywallMessage()

    object PaywallOpen : PaywallMessage()

    object PaywallClose : PaywallMessage()

    data class RequestReview(
        val type: Type,
    ) : PaywallMessage() {
        enum class Type(
            val rawName: String,
        ) {
            INAPP("in-app"),
            EXTERNAL("external"),
        }
    }
}

fun parseWrappedPaywallMessages(jsonString: String): Result<WrappedPaywallMessages> =
    try {
        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "SWWebViewInterface $jsonString",
        )
        val jsonObject = JSONObject(jsonString)
        val version = jsonObject.optInt("version", 1)
        val payloadJson = jsonObject.getJSONObject("payload")
        val messagesJsonArray = payloadJson.getJSONArray("events")
        val messages = mutableListOf<PaywallMessage>()

        for (i in 0 until messagesJsonArray.length()) {
            val messageJson = messagesJsonArray.getJSONObject(i)
            messages.add(parsePaywallMessage(messageJson))
        }

        Result.success(WrappedPaywallMessages(version, PayloadMessages(messages)))
    } catch (e: Throwable) {
        Result.failure(e)
    }

private fun parsePaywallMessage(json: JSONObject): PaywallMessage {
    val eventName = json.getString("event_name")

    return when (eventName) {
        "ping" -> PaywallMessage.OnReady(json.getString("version"))
        "close" -> PaywallMessage.Close
        "restore" -> PaywallMessage.Restore
        "open_url" -> PaywallMessage.OpenUrl(URI(json.getString("url")))
        "open_url_external" -> PaywallMessage.OpenUrlInBrowser(URI(json.getString("url")))
        "open_deep_link" -> PaywallMessage.OpenDeepLink(URI(json.getString("link")))
        "purchase" ->
            PaywallMessage.Purchase(
                json.getString("product"),
                json.getString("product_identifier"),
            )

        "custom" -> PaywallMessage.Custom(json.getString("data"))
        "custom_placement" ->
            PaywallMessage.CustomPlacement(
                json.getString("name"),
                json.getJSONObject("params"),
            )

        "request_store_review" ->
            PaywallMessage.RequestReview(
                when (json.getString("review_type")) {
                    "external" -> PaywallMessage.RequestReview.Type.EXTERNAL
                    "in-app" -> PaywallMessage.RequestReview.Type.INAPP
                    else -> PaywallMessage.RequestReview.Type.INAPP
                },
            )

        else -> {
            throw IllegalArgumentException("Unknown event name: $eventName")
        }
    }
}
