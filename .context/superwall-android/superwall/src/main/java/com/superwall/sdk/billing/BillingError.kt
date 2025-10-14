package com.superwall.sdk.billing

sealed class BillingError(
    val code: Int,
    val description: String,
) : Exception(description) {
    object UnknownError : BillingError(0, "Unknown error.")

    object IllegalStateException : BillingError(1, "IllegalStateException when connecting to billing client")

    // Define a class for custom errors where you can pass a message
    class BillingNotAvailable(
        description: String,
    ) : BillingError(2, description)
}
