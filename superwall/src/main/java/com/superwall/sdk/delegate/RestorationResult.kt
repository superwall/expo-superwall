package com.superwall.sdk.delegate

sealed class RestorationResult {
    class Restored : RestorationResult()

    data class Failed(
        val error: Throwable?,
    ) : RestorationResult()
}
