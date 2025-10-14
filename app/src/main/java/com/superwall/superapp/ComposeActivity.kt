package com.superwall.superapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.superwall.sdk.compose.PaywallComposable
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult
import com.superwall.sdk.paywall.view.PaywallView
import com.superwall.sdk.paywall.view.delegate.PaywallViewCallback
import com.superwall.superapp.ui.theme.MyApplicationTheme

class ComposeActivity :
    ComponentActivity(),
    PaywallViewCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeActivityContent(this)
        }
    }

    override fun onFinished(
        paywall: PaywallView,
        result: PaywallResult,
        shouldDismiss: Boolean,
    ) {
        // In the context of a tab bar, there won't be a dismissal.
    }
}

@Preview(showBackground = true)
@Composable
fun ComposeActivityContent(
    @PreviewParameter(PreviewPaywallDelegateProvider::class) delegate: PaywallViewCallback,
) {
    val selectedTabIndex = remember { mutableStateOf(0) }
    val examplePaywallOverrides = PaywallOverrides()

    MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column {
                TabRow(selectedTabIndex = selectedTabIndex.value) {
                    Tab(
                        selected = selectedTabIndex.value == 0,
                        onClick = { selectedTabIndex.value = 0 },
                    ) {
                        Text(text = "Tab 0", modifier = Modifier.padding(16.dp))
                    }
                    Tab(
                        selected = selectedTabIndex.value == 1,
                        onClick = { selectedTabIndex.value = 1 },
                    ) {
                        Text(text = "Tab 1", modifier = Modifier.padding(16.dp))
                    }
                    Tab(
                        selected = selectedTabIndex.value == 2,
                        onClick = { selectedTabIndex.value = 2 },
                    ) {
                        Text(text = "Tab 2", modifier = Modifier.padding(16.dp))
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTabIndex.value) {
                        0 -> TabContent0(examplePaywallOverrides, delegate = delegate)
                        1 -> TabContent1(examplePaywallOverrides, delegate = delegate)
                        2 -> TabContent2()
                    }
                }
            }
        }
    }
}

@Composable
fun TabContent0(
    paywallOverrides: PaywallOverrides?,
    delegate: PaywallViewCallback,
) {
    PaywallComposable(
        placement = "no_products",
        params = mapOf("key" to "value"),
        paywallOverrides = paywallOverrides,
        delegate = delegate,
    )
}

@Composable
fun TabContent1(
    paywallOverrides: PaywallOverrides?,
    delegate: PaywallViewCallback,
) {
    PaywallComposable(
        placement = "no-existing-event",
        params = mapOf("key" to "value"),
        paywallOverrides = paywallOverrides,
        delegate = delegate,
    )
}

@Composable
fun TabContent2() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Greeting("Jetpack Compose")
            EventButton()
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Composable
fun EventButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            val app = context.applicationContext as? MainApplication
            app?.invokeRegister("another_paywall")
        },
        modifier =
            Modifier
                .width(250.dp),
    ) {
        Text("Another Paywall")
    }
}

// Mock Provider for Preview
class PreviewPaywallDelegateProvider : PreviewParameterProvider<PaywallViewCallback> {
    override val values: Sequence<PaywallViewCallback> =
        sequenceOf(
            object : PaywallViewCallback {
                // Mock implementation of PaywallViewDelegate
                override fun onFinished(
                    paywall: PaywallView,
                    result: PaywallResult,
                    shouldDismiss: Boolean,
                ) {
                    // No implementation required
                }
            },
        )
}
