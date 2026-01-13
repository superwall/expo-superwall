package com.superwall.sdk.paywall.view.webview.messaging

import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.paywall.LocalNotificationType
import com.superwall.sdk.permissions.PermissionType
import com.superwall.sdk.storage.core_data.convertFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.URI

private val json = Json { ignoreUnknownKeys = true }

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
        var browserType: BrowserType?,
    ) : PaywallMessage() {
        enum class BrowserType(
            val rawName: String,
        ) {
            PAYMENT_SHEET("payment_sheet"),
        }
    }

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
        val params: JsonObject,
    ) : PaywallMessage()

    object PaywallOpen : PaywallMessage()

    object PaywallClose : PaywallMessage()

    object TransactionStart : PaywallMessage()

    data class TrialStarted(
        val trialEndDate: Long?,
        val productIdentifier: String,
    ) : PaywallMessage()

    data class UserAttributesUpdated(
        val data: Map<String, Any>,
    ) : PaywallMessage()

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

    data class ScheduleNotification(
        val id: String,
        val type: LocalNotificationType,
        val title: String,
        val subtitle: String,
        val body: String,
        val delay: Long,
    ) : PaywallMessage()

    data class RequestPermission(
        val permissionType: PermissionType,
        val requestId: String,
    ) : PaywallMessage()
}

fun parseWrappedPaywallMessages(jsonString: String): Result<WrappedPaywallMessages> =
    try {
        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "SWWebViewInterface $jsonString",
        )
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val version = jsonObject["version"]?.jsonPrimitive?.intOrNull ?: 1
        val payloadJson = jsonObject["payload"]!!.jsonObject
        val messagesJsonArray = payloadJson["events"]!!.jsonArray
        val messages = mutableListOf<PaywallMessage>()

        for (element in messagesJsonArray) {
            val messageJson = element.jsonObject
            messages.add(parsePaywallMessage(messageJson))
        }

        Result.success(WrappedPaywallMessages(version, PayloadMessages(messages)))
    } catch (e: Throwable) {
        Result.failure(e)
    }

private fun parsePaywallMessage(json: JsonObject): PaywallMessage {
    val eventName = json["event_name"]!!.jsonPrimitive.content

    return when (eventName) {
        "ping" -> PaywallMessage.OnReady(json["version"]!!.jsonPrimitive.content)
        "close" -> PaywallMessage.Close
        "restore" -> PaywallMessage.Restore
        "open_url" ->
            PaywallMessage.OpenUrl(
                URI(json["url"]!!.jsonPrimitive.content),
                json["browser_type"]?.jsonPrimitive?.contentOrNull?.let {
                    when (it) {
                        PaywallMessage.OpenUrl.BrowserType.PAYMENT_SHEET.rawName -> PaywallMessage.OpenUrl.BrowserType.PAYMENT_SHEET
                        else -> null
                    }
                },
            )

        "open_url_external" -> PaywallMessage.OpenUrlInBrowser(URI(json["url"]!!.jsonPrimitive.content))
        "open_deep_link" -> PaywallMessage.OpenDeepLink(URI(json["link"]!!.jsonPrimitive.content))
        "purchase" ->
            PaywallMessage.Purchase(
                json["product"]!!.jsonPrimitive.content,
                json["product_identifier"]!!.jsonPrimitive.content,
            )

        "custom" -> PaywallMessage.Custom(json["data"]!!.jsonPrimitive.content)
        "custom_placement" ->
            PaywallMessage.CustomPlacement(
                json["name"]!!.jsonPrimitive.content,
                json["params"]!!.jsonObject,
            )

        "request_store_review" ->
            PaywallMessage.RequestReview(
                when (json["review_type"]!!.jsonPrimitive.content) {
                    "external" -> PaywallMessage.RequestReview.Type.EXTERNAL
                    "in-app" -> PaywallMessage.RequestReview.Type.INAPP
                    else -> PaywallMessage.RequestReview.Type.INAPP
                },
            )

        "user_attribute_updated" -> {
            PaywallMessage.UserAttributesUpdated(
                (json["attributes"]!!.jsonArray.convertFromJsonElement() as List<Map<String, Any>>)
                    .associate {
                        it["key"] as String to ((it["value"] as Any?) ?: "")
                    },
            )
        }

        "schedule_notification" ->
            PaywallMessage.ScheduleNotification(
                type =
                    when (json["type"]?.jsonPrimitive?.contentOrNull) {
                        "TRIAL_STARTED" -> LocalNotificationType.TrialStarted
                        else -> LocalNotificationType.Unsupported
                    },
                id = json["id"]?.jsonPrimitive?.contentOrNull ?: "",
                title = json["title"]?.jsonPrimitive?.contentOrNull ?: "",
                subtitle = json["subtitle"]?.jsonPrimitive?.contentOrNull ?: "",
                body = json["body"]?.jsonPrimitive?.contentOrNull ?: "",
                delay = json["delay"]?.jsonPrimitive?.longOrNull ?: 0L,
            )

        "request_permission" -> {
            val permissionTypeRaw =
                json["permission_type"]?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("request_permission missing permission_type")
            val permissionType =
                PermissionType.fromRaw(permissionTypeRaw)
                    ?: throw IllegalArgumentException("Unknown permission_type: $permissionTypeRaw")
            val requestId =
                json["request_id"]?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("request_permission missing request_id")
            PaywallMessage.RequestPermission(
                permissionType = permissionType,
                requestId = requestId,
            )
        }

        else -> {
            throw IllegalArgumentException("Unknown event name: $eventName")
        }
    }
}
