package com.superwall.sdk.store.transactions

enum class PlayBillingErrors(
    val code: Int,
    val message: String,
) {
    NETWORK_ERROR(
        12,
        "There was a problem with the network connection. Please check your connection and try again.",
    ),
    SERVICE_TIMEOUT(
        -3,
        "The request timed out. This is usually a temporary issue. Please try again.",
    ),
    SERVICE_DISCONNECTED(
        -1,
        "The connection to the Google Play Store was lost. We are attempting to reconnect. Please try again in a few moments.",
    ),
    FEATURE_NOT_SUPPORTED(
        -2,
        "The requested feature is not supported on your device. You may need to update the Play Store app.",
    ),
    SERVICE_UNAVAILABLE(
        2,
        "The Google Play Billing service is currently unavailable. This may be a temporary network issue. Please try again.",
    ),
    BILLING_UNAVAILABLE(
        3,
        "Billing is unavailable. This could be because the Play Store app is not available, you're in an unsupported country, or there's an issue with your payment method. Please check your Google Play account and try again.",
    ),
    USER_CANCELED(
        1,
        "You have canceled the purchase.",
    ),
    ITEM_UNAVAILABLE(
        4,
        "This item is not available for purchase. It may not be available in your country or the item is no longer for sale.",
    ),
    DEVELOPER_ERROR(
        5,
        "A developer error occurred. Please check the app's configuration.",
    ),
    ERROR(
        6,
        "An internal error occurred in the Google Play Store. This might be a temporary issue. Please try again later.",
    ),
    ITEM_ALREADY_OWNED(
        7,
        "You already own this item. If you don't see your purchase, please check your purchase history in the Play Store.",
    ),
    ITEM_NOT_OWNED(
        8,
        "You do not own the item you are trying to manage. Please check your purchase history in the Play Store.",
    ),
    ;

    companion object {
        fun fromCode(code: Int): PlayBillingErrors? = values().firstOrNull { it.code == code }

        fun fromCode(code: String?): PlayBillingErrors? {
            val parsed = code?.toIntOrNull() ?: return null
            return fromCode(parsed)
        }
    }
}
