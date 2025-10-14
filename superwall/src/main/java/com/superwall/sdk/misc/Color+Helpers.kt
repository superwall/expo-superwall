package com.superwall.sdk.misc

import android.graphics.Color

fun Int.isDarkColor(): Boolean {
    val red = Color.red(this) / 255.0
    val green = Color.green(this) / 255.0
    val blue = Color.blue(this) / 255.0

    val lum = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return lum < 0.50
}

fun Int.isLightColor(): Boolean = !isDarkColor()

fun Int.readableOverlayColor(): Int = if (isDarkColor()) Color.WHITE else Color.BLACK

fun Int.inverseReadableOverlayColor(): Int = if (isDarkColor()) Color.BLACK else Color.WHITE
