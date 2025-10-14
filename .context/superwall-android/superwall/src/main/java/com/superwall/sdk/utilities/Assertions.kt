package com.superwall.sdk.view

import com.superwall.sdk.BuildConfig

fun fatalAssert(
    condition: Boolean,
    message: String,
) {
    if (BuildConfig.DEBUG && !condition) {
        throw AssertionError(message)
    }
}
