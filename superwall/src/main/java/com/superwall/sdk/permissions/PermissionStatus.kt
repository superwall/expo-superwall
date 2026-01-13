package com.superwall.sdk.permissions

/**
 * Permission status values matching OS-level permission states.
 * Maps to the paywall schema permission_result status values.
 */
enum class PermissionStatus(
    val rawValue: String,
) {
    /** User granted permission */
    GRANTED("granted"),

    /** User denied permission */
    DENIED("denied"),

    /** Platform doesn't support this permission */
    UNSUPPORTED("unsupported"),
    ;

    companion object {
        fun fromRaw(raw: String): PermissionStatus? = values().find { it.rawValue == raw }
    }
}
