package com.superwall.sdk.identity

import com.superwall.sdk.Superwall

fun Superwall.identify(
    userId: String,
    options: IdentityOptions? = null,
) {
    // Call to dependency controller
    dependencyContainer.identityManager.identify(userId, options)
}

// / Sets user attributes for use in paywalls and the Superwall dashboard.
// /
// / If the existing user attributes dictionary already has a value for a given property, the old
// / value is overwritten. Existing properties will not be affected.
// / Useful for analytics and conditional paywall rules you may define in the Superwall Dashboard.
// / They should **not** be used as a source of truth for sensitive information.
// /
// / Here's how you might set user attributes after retrieving your user's data:
// /  ```swift
// /  var attributes: [String: Any] = [
// /   "name": user.name,
// /   "apnsToken": user.apnsTokenString,
// /   "email": user.email,
// /   "username": user.username,
// /   "profilePic": user.profilePicUrl
// /  ]
// /  await Superwall.instance.setUserAttributes(attributes)
// /  ```
// / See [Setting User Attributes](https://docs.superwall.com/docs/setting-user-properties) for more.
// /
// / - Parameter attributes: A `[String: Any?]` dictionary used to describe any custom
// / attributes you'd like to store for the user. Values can be any JSON encodable value, `URL`s or `Date`s.
// / Note: Keys beginning with `$` are reserved for Superwall and will be dropped. Arrays and dictionaries
// / as values are not supported at this time, and will be dropped.
fun Superwall.setUserAttributes(attributes: Map<String, Any?>) {
    // Call to dependency controller
    val cleanAttributes = cleanAttributes(attributes)
    dependencyContainer.identityManager.mergeUserAttributes(cleanAttributes)
}

fun cleanAttributes(attributes: Map<String, Any?>): Map<String, Any?> {
    val customAttributes = mutableMapOf<String, Any?>()

    for (key in attributes.keys) {
        if (key.startsWith("$")) {
            // preserve $ for Superwall-only values
            continue
        }
        val value = attributes[key]
        customAttributes[key] = value
    }
    return customAttributes.toMap()
}
