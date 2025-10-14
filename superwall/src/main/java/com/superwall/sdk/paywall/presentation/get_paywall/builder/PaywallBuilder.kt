package com.superwall.sdk.paywall.presentation.get_paywall.builder

import android.app.Activity
import android.view.View
import com.superwall.sdk.Superwall
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.paywall.presentation.get_paywall.getPaywall
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.paywall.view.LoadingView
import com.superwall.sdk.paywall.view.PaywallPurchaseLoadingView
import com.superwall.sdk.paywall.view.PaywallShimmerView
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.ShimmerView
import com.superwall.sdk.paywall.view.delegate.PaywallViewCallback
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import kotlin.jvm.Throws

/**
 * Builder class for creating a PaywallView. This is useful in case you want to present a paywall yourself
 * as an Android View, allowing you to customize the paywall's appearance and behavior,
 * for example by passing in a custom Shimmer or Loading view.
 *
 * @param placement The placement name for the paywall you want to display.
 * @property params the parameters to pass into the event
 * @property paywallOverrides the paywall overrides to apply to the paywall, i.e. substitutions or presentation styles
 * @property delegate the [SuperwallDelegate] to handle the paywall's callbacks
 * @property shimmmerView a view to display while the paywall is loading
 * @property purchaseLoadingView the loading view to display while the purchase/restoration is loading
 * @property activity the activity to attach the paywall to
 * @see PaywallView
 * Example:
 * ```
 * val paywallView = PaywallBuilder("placement_name")
 *    .params(mapOf("key" to "value"))
 *    .overrides(PaywallOverrides())
 *    .delegate(mySuperwallDelegate)
 *    .shimmerView(MyShimmerView(context))
 *    .purchaseLoadingView(MyPurchaseLoadingView(context))
 *    .activity(activity)
 *    .build()
 */

class PaywallBuilder(
    val placement: String,
) {
    private var params: Map<String, Any>? = null
    private var paywallOverrides: PaywallOverrides? = null
    private var delegate: PaywallViewCallback? = null
    private var shimmmerView: PaywallShimmerView? = null
    private var purchaseLoadingView: PaywallPurchaseLoadingView? = null
    private var activity: Activity? = null

    fun params(params: Map<String, Any>?): PaywallBuilder {
        this.params = params
        return this
    }

    fun overrides(paywallOverrides: PaywallOverrides?): PaywallBuilder {
        this.paywallOverrides = paywallOverrides
        return this
    }

    fun delegate(delegate: PaywallViewCallback): PaywallBuilder {
        this.delegate = delegate
        return this
    }

    fun <T> shimmerView(shimmerView: T): PaywallBuilder
            where T : PaywallShimmerView, T : View {
        this.shimmmerView = shimmerView
        return this
    }

    fun activity(activity: Activity): PaywallBuilder {
        this.activity = activity
        return this
    }

    fun <T> purchaseLoadingView(purchaseLoadingView: T): PaywallBuilder
            where T : PaywallPurchaseLoadingView, T : View {
        this.purchaseLoadingView = purchaseLoadingView
        return this
    }

    suspend fun build(): Result<PaywallView> =
        Superwall.instance
            .getPaywall(placement, params, paywallOverrides, delegate!!)
            .onSuccess { newView ->
                newView.encapsulatingActivity = WeakReference(activity)
                val shimmer = shimmmerView ?: ShimmerView(activity!!)
                val loading = purchaseLoadingView ?: LoadingView(activity!!)
                newView.setupWith(shimmer, loading)
                newView.setupShimmer(shimmer)
                newView.setupLoading(loading)
                newView.beforeViewCreated()
            }

    @JvmName("buildWithCallback")
    fun build(
        onSuccess: (PaywallView) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        IOScope().launch {
            val res =
                Superwall.instance
                    .getPaywall(placement, params, paywallOverrides, delegate!!)
            MainScope().launch {
                res
                    .onSuccess { newView ->
                        newView.encapsulatingActivity = WeakReference(activity)
                        val shimmer = shimmmerView ?: ShimmerView(activity!!)
                        val loading = purchaseLoadingView ?: LoadingView(activity!!)
                        newView.setupWith(shimmer, loading)
                        newView.setupShimmer(shimmer)
                        newView.setupLoading(loading)
                        newView.beforeViewCreated()
                        onSuccess(newView)
                    }.onFailure {
                        onError(it)
                    }
            }
        }
    }

    @Throws
    fun buildSync(): PaywallView {
        return runBlocking {
            Superwall.instance
                .getPaywall(placement, params, paywallOverrides, delegate!!)
                .onSuccess { newView ->
                    newView.encapsulatingActivity = WeakReference(activity)
                    val shimmer = shimmmerView ?: ShimmerView(activity!!)
                    val loading = purchaseLoadingView ?: LoadingView(activity!!)
                    newView.setupWith(shimmer, loading)
                    newView.setupShimmer(shimmer)
                    newView.setupLoading(loading)
                    newView.beforeViewCreated()
                    return@runBlocking newView
                }.getOrThrow()
        }
    }
}
