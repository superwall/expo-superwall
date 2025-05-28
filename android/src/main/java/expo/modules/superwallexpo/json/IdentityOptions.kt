package expo.modules.superwallexpo.json

import com.superwall.sdk.identity.IdentityOptions

fun identityOptionsFromJson(json: Map<String, Any>): IdentityOptions {
    val restorePaywallAssignments = json["restorePaywallAssignments"] as Boolean
    return IdentityOptions(restorePaywallAssignments = restorePaywallAssignments)
}
