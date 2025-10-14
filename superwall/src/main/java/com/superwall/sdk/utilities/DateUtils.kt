package com.superwall.sdk.utilities

import java.text.SimpleDateFormat
import java.util.Locale

internal object DateUtils {
    val ISO_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    val ISO_SECONDS = "yyyy-MM-dd'T'HH:mm:ss"
    val ISO_SECONDS_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    val SIMPLE = "yyyy-MM-dd HH:mm:ss"
    val yyyy_MM_dd = "yyyy-MM-dd"
    val HH_mm_ss = "HH:mm:ss"
    val MMM_dd_yyyy = "MMM dd, yyyy"
}

internal fun dateFormat(format: String = DateUtils.ISO_MILLIS) = SimpleDateFormat(format, Locale.US)

internal val DateFormatterUtil = dateFormat()
