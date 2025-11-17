package expo.modules.superwallexpo.json

import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult

fun PaywallResult.toJson(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    
    when (this) {  
        is PaywallResult.Purchased -> {
            map["type"] = "purchased"
            map["productId"] = productId
        }
        is PaywallResult.Declined -> {
            map["type"] = "declined"
        }
        is PaywallResult.Restored -> {
            map["type"] = "restored"
        }
    }
    
    return map
}