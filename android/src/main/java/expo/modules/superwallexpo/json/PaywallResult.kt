package expo.modules.superwallexpo.json

import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult

fun PaywallResult.toJson(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            when (this) {
                is PaywallResult.Purchased -> {
                    this["type"] = "purchased"
                    this["productId"] = productId
                }
                is PaywallResult.Declined -> {
                    this["type"] = "declined"
                }
                is PaywallResult.Restored -> {
                    this["type"] = "restored"
                }
            }
        }
    }
