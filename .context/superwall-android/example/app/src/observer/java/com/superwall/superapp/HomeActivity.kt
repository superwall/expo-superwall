package com.superwall.superapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.superwall.sdk.Superwall
import com.superwall.sdk.billing.observer.SuperwallBillingFlowParams
import com.superwall.sdk.billing.observer.launchBillingFlowWithSuperwall
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.PaywallPresentationHandler
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason
import com.superwall.sdk.paywall.presentation.register
import com.superwall.sdk.store.PurchasingObserverState
import com.superwall.superapp.ui.theme.SuperwallExampleAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val subscriptionStatus by Superwall.instance.entitlements.status
                .collectAsState()
            SuperwallExampleAppTheme {
                HomeScreen(
                    subscriptionStatus = subscriptionStatus,
                    onLogOutClicked = {
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    subscriptionStatus: SubscriptionStatus,
    onLogOutClicked: () -> Unit,
) {
    val context = LocalContext.current
    var dropdownState =
        remember {
            mutableStateOf(false)
        }

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Observing purchases",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp),
            )
            Row(
                Modifier
                    .padding(8.dp)
                    .clickable {
                        dropdownState.value = true
                    },
            ) {
                Text(text = "Change entitlement status")
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White)
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text =
                        "The buttons below trigger the purchase flow. " +
                            "The first button will automatically observe the purchase flow. " +
                            "The other buttons will manually observe the purchase flow." +
                            "Please ensure you have your products set correctly" +
                            " and are added as a tester to the Google Play Console.",
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SWButton("Purchase with automatic observation", {
                    val billingClient =
                        BillingClient
                            .newBuilder(context)
                            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                            .setListener { billingResult, purchases -> }
                            .build()
                    scope.launch(Dispatchers.IO) {
                        // Replace with your product id
                        val product =
                            Superwall.instance
                                .getProducts("com.ui_tests.monthly:com-ui-tests-montly:sw-auto")
                                .getOrNull()!!
                                .values
                                .first()

                        // Launch the billing flow with the product and ensure it is observed by superwall
                        billingClient.launchBillingFlowWithSuperwall(
                            (context as Activity),
                            SuperwallBillingFlowParams
                                .newBuilder()
                                .setProductDetailsParamsList(
                                    listOf(
                                        SuperwallBillingFlowParams.ProductDetailsParams
                                            .newBuilder()
                                            .setProductDetails(product.rawStoreProduct.underlyingProductDetails)
                                            .setOfferToken(
                                                product.rawStoreProduct.selectedOffer?.offerToken
                                                    ?: "",
                                            ).build(),
                                    ),
                                ).build(),
                        )
                    }
                })
                SWButton("Observe purchase start manually", {
                    scope.launch(Dispatchers.IO) {
                        val product =
                            Superwall.instance
                                .getProducts("com.ui_tests.monthly:com-ui-tests-montly:sw-auto")
                                .getOrNull()!!
                                .values
                                .first()

                        Superwall.instance.observe(
                            PurchasingObserverState.PurchaseWillBegin(product.rawStoreProduct.underlyingProductDetails),
                        )
                    }
                })
                SWButton("Observe purchase complete manually", {
                    scope.launch(Dispatchers.IO) {
                        val product =
                            Superwall.instance
                                .getProducts("com.ui_tests.monthly:com-ui-tests-montly:sw-auto")
                                .getOrNull()!!
                                .values
                                .first()

                        Superwall.instance.observe(
                            PurchasingObserverState.PurchaseResult(
                                BillingResult
                                    .newBuilder()
                                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                                    .build(),
                                purchases = listOf(PurchaseMockBuilder.createDefaultPurchase("com.ui_tests.monthly")),
                            ),
                        )
                    }
                })
                SWButton("Observe purchase failed manually", {
                    scope.launch(Dispatchers.IO) {
                        val product =
                            Superwall.instance
                                .getProducts("com.ui_tests.monthly:com-ui-tests-montly:sw-auto")
                                .getOrNull()!!
                                .values
                                .first()

                        Superwall.instance.observe(
                            PurchasingObserverState.PurchaseError(
                                product.rawStoreProduct.underlyingProductDetails,
                                IllegalStateException("Purchase Abandoned"),
                            ),
                        )
                    }
                })

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        Superwall.instance.reset()
                        onLogOutClicked()
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = "Log Out",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style =
                            TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                    )
                }
            }
        }
    }
}

fun showPaywall(
    context: Context,
    placement: String,
) {
    val handler = PaywallPresentationHandler()
    handler.onDismiss { paywallInfo, paywallResult ->
        println("The paywall dismissed. PaywallInfo: $paywallInfo")
    }
    handler.onPresent { paywallInfo ->
        println("The paywall presented. PaywallInfo: $paywallInfo")
    }
    handler.onError { error ->
        println("The paywall presentation failed with error $error")
    }
    handler.onSkip { reason ->
        when (reason) {
            is PaywallSkippedReason.PlacementNotFound -> {
                print("Paywall not shown because this placement isn't part of a campaign.")
            }

            is PaywallSkippedReason.Holdout -> {
                print(
                    "Paywall not shown because user is in a holdout group in " +
                        "Experiment: ${reason.experiment.id}",
                )
            }

            is PaywallSkippedReason.NoAudienceMatch -> {
                print("Paywall not shown because user doesn't match any rules.")
            }

            is PaywallSkippedReason.UserIsSubscribed -> {
                print("Paywall not shown because user is subscribed.")
            }
        }
    }

    Superwall.instance.register(placement = "campaign_trigger", handler = handler) {
        // code in here can be remotely configured to execute. Either
        // (1) always after presentation or
        // (2) only if the user pays
        // code is always executed if no paywall is configured to show
        val builder =
            AlertDialog
                .Builder(context)
                .setTitle("Feature Launched")
                .setMessage("The feature block was called")

        builder.setPositiveButton("Ok") { _, _ -> }

        val alertDialog = builder.create()
        alertDialog.show()
    }
}

fun Context.dialog(
    title: String,
    text: String,
) {
    val builder =
        AlertDialog
            .Builder(this)
            .setTitle(title)
            .setMessage(text)

    builder.setPositiveButton("Ok") { _, _ -> }

    val alertDialog = builder.create()
    alertDialog.show()
}

@Composable
fun SWButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .padding(top = 8.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style =
                TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
        )
    }
}
