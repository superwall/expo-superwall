package com.superwall.sdk.utilities

import android.app.Application
import com.superwall.sdk.Superwall
import com.superwall.sdk.config.options.PaywallOptions
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.misc.ActivityProvider

class SuperwallBuilder {
    var purchaseController: PurchaseController? = null
    var options: SuperwallOptions? = null
        private set
    var activityProvider: ActivityProvider? = null
    var completion: ((Result<Unit>) -> Unit)? = null

    @SuperwallDSL
    fun options(action: SuperwallOptions.() -> Unit) {
        options = SuperwallOptions().apply(action)
    }
}

/**
 * Configures a shared instance of [Superwall] for use throughout your app.
 *
 * Call this as soon as your app finishes launching in `onCreate` in your `MainApplication`
 * class.
 * Check out [Configuring the SDK](https://docs.superwall.com/docs/configuring-the-sdk) for
 * information about how to configure the SDK.
 *
 * @param apiKey Your Public API Key that you can get from the Superwall dashboard
 * settings. If you don't have an account, you can
 * [sign up for free](https://superwall.com/sign-up).
 * @param configure A lambda that allows you to configure the SDK. Inside the configuration block,
 * you can setup your [PurchaseController], [ActivityProvider], a completion
 * closure and [SuperwallOptions] via [SuperwallBuilder.options] closure.
 * @return The configured [Superwall] instance.
 */

@SuperwallDSL
fun Application.configureSuperwall(
    apiKey: String,
    configure: SuperwallBuilder.() -> Unit,
): Superwall {
    val builder = SuperwallBuilder().apply(configure)
    Superwall.configure(
        this,
        apiKey,
        builder.purchaseController,
        builder.options,
        builder.activityProvider,
        builder.completion,
    )
    return Superwall.instance
}

/**
 * Enables you to define [PaywallOptions] via closure.
 * */
@SuperwallDSL
fun SuperwallOptions.paywalls(action: PaywallOptions.() -> Unit) {
    paywalls = this.paywalls.apply(action)
}

/**
 * Enables you to define [SuperwallOptions.Logging] via closure.
 * */
@SuperwallDSL
fun SuperwallOptions.logging(action: SuperwallOptions.Logging.() -> Unit) {
    logging = this.logging.apply(action)
}
