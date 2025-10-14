package com.superwall.sdk.identity

//  File.kt
//
//
//  Created by Yusuf Tör on 02/02/2023.
//

/**
 * Options passed in when calling `Superwall.identify(userId, options)`.
 */
data class IdentityOptions(
    /**
     * Determines whether the SDK should wait to restore paywall assignments from the server
     * before presenting any paywalls.
     *
     * This should only be used in advanced use cases. By setting this to `true`, it prevents
     * paywalls from showing until after paywall assignments have been restored. If you expect
     * users of your app to switch accounts or delete/reinstall a lot, you'd set this when users log
     * in to an existing account.
     */
    val restorePaywallAssignments: Boolean = false,
)
