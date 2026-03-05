package com.superwall.sdk.paywall.presentation

/**
 * Defines how the paywall waits for a custom callback response.
 */
enum class CustomCallbackBehavior(
    val rawValue: String,
) {
    /**
     * The paywall waits for the callback to complete before continuing
     * the tap action chain.
     */
    BLOCKING("blocking"),

    /**
     * The paywall continues immediately; the response still triggers
     * onSuccess/onFailure handlers in the paywall.
     */
    NON_BLOCKING("non-blocking"),
    ;

    companion object {
        fun fromRaw(rawValue: String): CustomCallbackBehavior? = entries.find { it.rawValue == rawValue }
    }
}

/**
 * Represents a custom callback request from the paywall.
 *
 * @property name The name of the callback being requested.
 * @property variables Optional key-value pairs passed from the paywall.
 *                     Values are type-preserved (string/number/boolean).
 */
data class CustomCallback(
    val name: String,
    val variables: Map<String, Any>?,
)

/**
 * The result status of a custom callback.
 */
enum class CustomCallbackResultStatus(
    val rawValue: String,
) {
    SUCCESS("success"),
    FAILURE("failure"),
}

/**
 * The result to return from a custom callback handler.
 *
 * @property status Whether the callback succeeded or failed.
 *                  Determines which branch (onSuccess/onFailure) executes in the paywall.
 * @property data Optional key-value pairs to return to the paywall.
 *               Values are type-preserved and accessible as callbacks.<name>.data.<key>.
 */
data class CustomCallbackResult(
    val status: CustomCallbackResultStatus,
    val data: Map<String, Any>? = null,
) {
    companion object {
        /**
         * Creates a success result with optional data.
         */
        fun success(data: Map<String, Any>? = null): CustomCallbackResult = CustomCallbackResult(CustomCallbackResultStatus.SUCCESS, data)

        /**
         * Creates a failure result with optional data.
         */
        fun failure(data: Map<String, Any>? = null): CustomCallbackResult = CustomCallbackResult(CustomCallbackResultStatus.FAILURE, data)
    }
}
