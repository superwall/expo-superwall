package com.superwall.sdk.paywall.manager

import android.content.Context
import com.superwall.sdk.misc.ActivityProvider
import com.superwall.sdk.models.paywall.PaywallIdentifier
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.paywall.view.LoadingView
import com.superwall.sdk.paywall.view.PaywallPurchaseLoadingView
import com.superwall.sdk.paywall.view.PaywallShimmerView
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.ShimmerView
import com.superwall.sdk.paywall.view.ViewStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PaywallViewCache(
    private val appCtx: Context,
    private val store: ViewStorage,
    private val activityProvider: ActivityProvider,
    private val deviceHelper: DeviceHelper,
) {
    private val ctx: Context
        get() = activityProvider.getCurrentActivity() ?: appCtx
    private var _activePaywallVcKey: String? = null
    private val loadingView: LoadingView = LoadingView(context = ctx)
    private val shimmerView: ShimmerView = ShimmerView(context = ctx)

    init {
        store.storeView(LoadingView.TAG, loadingView)
        store.storeView(ShimmerView.TAG, shimmerView)
    }

    private val singleThreadContext = Dispatchers.IO

    val entries =
        store.views.entries.associate { it.key to it.value }

    fun getAllPaywallViews(): List<PaywallView> =
        runBlocking(singleThreadContext) {
            store.all().filterIsInstance<PaywallView>().toList()
        }

    var activePaywallVcKey: String?
        get() = runBlocking(singleThreadContext) { _activePaywallVcKey }
        set(value) {
            CoroutineScope(singleThreadContext).launch { _activePaywallVcKey = value }.apply { }
        }

    val activePaywallView: PaywallView?
        get() = runBlocking(singleThreadContext) { _activePaywallVcKey?.let { store.retrieveView(it) as PaywallView? } }

    fun save(
        paywallView: PaywallView,
        identifier: PaywallIdentifier,
    ) {
        CoroutineScope(singleThreadContext).launch {
            store.storeView(
                PaywallCacheLogic.key(
                    identifier,
                    locale = deviceHelper.locale,
                ),
                paywallView,
            )
        }
    }

    fun acquireLoadingView(): PaywallPurchaseLoadingView {
        return store.retrieveView(LoadingView.TAG)?.let {
            it as PaywallPurchaseLoadingView
        } ?: run {
            val view = LoadingView(ctx)
            store.storeView(LoadingView.TAG, view)
            return view
        }
    }

    fun acquireShimmerView(): PaywallShimmerView {
        return store.retrieveView(ShimmerView.TAG)?.let {
            it as PaywallShimmerView
        } ?: run {
            val view = ShimmerView(ctx)
            store.storeView(ShimmerView.TAG, view)
            return view
        }
    }

    fun getPaywallView(key: String): PaywallView? =
        runBlocking(singleThreadContext) {
            try {
                store.retrieveView(key) as PaywallView?
            } catch (e: Throwable) {
                null
            }
        }

    fun removePaywallView(identifier: PaywallIdentifier) {
        CoroutineScope(singleThreadContext).launch {
            store.removeView(
                PaywallCacheLogic.key(
                    identifier,
                    locale = deviceHelper.locale,
                ),
            )
        }
    }

    fun removeAll() {
        CoroutineScope(singleThreadContext).launch {
            store.views.entries.forEach { it ->
                val key = it.key
                if (key != _activePaywallVcKey) {
                    store.removeView(key)
                }
            }
        }
    }
}
