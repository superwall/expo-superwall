package com.superwall.sdk.logger

enum class LogLevel(
    val level: Int,
) {
    debug(10),
    info(20),
    warn(30),
    error(40),
    none(99),
    ;

    override fun toString(): String =
        when (this) {
            debug -> "DEBUG"
            info -> "INFO"
            warn -> "WARN"
            error -> "ERROR"
            none -> "NONE"
        }

    fun getDescriptionEmoji(): String =
        when (this) {
            debug -> "💬"
            info -> "ℹ️"
            warn -> "⚠️"
            error -> "‼️"
            none -> ""
        }
}
