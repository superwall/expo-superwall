package com.superwall.sdk.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.superwall.sdk.paywall.presentation.get_paywall.builder.PaywallBuilder
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.delegate.PaywallViewCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PaywallComposable(
    modifier: Modifier = Modifier.fillMaxSize(),
    placement: String,
    params: Map<String, Any>? = null,
    paywallOverrides: PaywallOverrides? = null,
    delegate: PaywallViewCallback,
    errorComposable: @Composable ((Throwable) -> Unit) = { error: Throwable ->
        // Default error composable
        Text(text = "No paywall to display")
    },
    loadingComposable: @Composable (() -> Unit) = {
        // Default loading composable
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        }
    },
) {
    val viewState = remember { mutableStateOf<PaywallView?>(null) }
    val errorState = remember { mutableStateOf<Throwable?>(null) }
    val context = LocalContext.current.findNearestActivity()

    @Composable
    fun rememberThemeChanged(): Boolean {
        val isDark = isSystemInDarkTheme()

        var lastTheme by rememberSaveable { mutableStateOf(isDark) }
        var hasChanged by remember { mutableStateOf(false) }

        LaunchedEffect(isDark) {
            if (isDark != lastTheme) {
                hasChanged = true
                lastTheme = isDark
            } else {
                hasChanged = false
            }
        }

        return hasChanged
    }
    LaunchedEffect(Unit) {
        PaywallBuilder(placement)
            .params(params)
            .overrides(paywallOverrides)
            .delegate(delegate)
            .activity(context)
            .build()
            .fold(onSuccess = {
                viewState.value = it
            }, onFailure = {
                errorState.value = it
            })
    }

    when {
        viewState.value != null -> {
            viewState.value?.let { viewToRender ->
                LaunchedEffect(viewToRender) {
                    viewToRender.onViewCreated()
                }
                val themeChanged = rememberThemeChanged()
                AndroidView(
                    modifier = modifier,
                    update = {
                        if (themeChanged && it.state.isPresented) {
                            it.onThemeChanged()
                        }
                    },
                    factory = { context ->
                        viewToRender
                    },
                    onRelease = {
                        viewToRender.beforeOnDestroy()
                        viewToRender.encapsulatingActivity = null

                        CoroutineScope(Dispatchers.Main).launch {
                            viewToRender.destroyed()
                            viewToRender.cleanup()
                        }
                    },
                )
            }
        }

        errorState.value != null -> {
            errorComposable(errorState.value!!)
        }

        else -> {
            loadingComposable()
        }
    }
}

fun Context.findNearestActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findNearestActivity()
        else -> throw IllegalStateException("No Activity attached - activity required")
    }
