package com.superwall.sdk.paywall.presentation.internal

object InternalPresentationLogic {
    fun presentationError(
        domain: String,
        code: Int,
        title: String,
        value: String,
    ): Throwable = RuntimeException("$domain: $code, $title - $value")
}
