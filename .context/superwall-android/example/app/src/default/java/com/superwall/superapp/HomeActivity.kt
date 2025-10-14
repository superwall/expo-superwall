package com.superwall.superapp

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.superwall.sdk.Superwall
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.PaywallPresentationHandler
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason
import com.superwall.sdk.paywall.presentation.register
import com.superwall.superapp.ui.theme.SuperwallExampleAppTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val entitlementStatus by Superwall.instance.entitlements.status
                .collectAsState()
            SuperwallExampleAppTheme {
                HomeScreen(
                    subscriptionStatus = entitlementStatus,
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
    val subscriptionText =
        when (subscriptionStatus) {
            is SubscriptionStatus.Unknown -> "Loading subscription status."
            is SubscriptionStatus.Active ->
                "You currently have an active subscription. Therefore, the " +
                    "paywall will never show. For the purposes of this app, delete and reinstall the " +
                    "app to clear subscriptions."

            is SubscriptionStatus.Inactive ->
                "You do not have an active subscription so the paywall will " +
                    "show when clicking the button."
        }

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
                text = "Presenting a Paywall",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text =
                        "The Launch Feature button below registers a placement \"campaign_trigger\".\n\n" +
                            "This placement has been added to a campaign on the Superwall dashboard.\n\n" +
                            "When this placement is registered, the rules in the campaign are evaluated.\n\n" +
                            "The rules match and cause a paywall to show.",
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = subscriptionText,
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        val handler = PaywallPresentationHandler()
                        handler.onDismiss { paywallInfo, paywallResult ->
                            println("The paywall dismissed. PaywallInfo: $paywallInfo - $paywallResult")
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
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .padding(top = 8.dp),
                ) {
                    Text(
                        text = "Launch Feature",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style =
                            TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                    )
                }
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
