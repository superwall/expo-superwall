package com.superwall.superapp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
            val subscriptionStatus by Superwall.instance.subscriptionStatus
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
    val subscriptionText =
        when (subscriptionStatus) {
            is SubscriptionStatus.Unknown -> "Loading entitlement status."
            is SubscriptionStatus.Active ->
                "You currently have active entitlemements: ${
                    subscriptionStatus.entitlements.map { it.id }.joinToString()
                }."

            is SubscriptionStatus.Inactive ->
                "You do not have any active entitlements so the paywall will " +
                    "show when clicking the button."
        }

    var dropdownState =
        remember {
            mutableStateOf(false)
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
            Text(
                text = "Subscription Status: $subscriptionText",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
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
                DropdownMenu(
                    modifier =
                        Modifier
                            .padding(start = 8.dp)
                            .background(Color.DarkGray),
                    expanded = dropdownState.value,
                    onDismissRequest = { dropdownState.value = false },
                ) {
                    DropdownMenuItem({ Text("Inactive") }, onClick = {
                        Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                        dropdownState.value = false
                    })
                    DropdownMenuItem({ Text("Pro") }, onClick = {
                        Superwall.instance.setSubscriptionStatus("pro")
                        dropdownState.value = false
                    })
                    DropdownMenuItem({ Text("diamond") }, onClick = {
                        Superwall.instance.setSubscriptionStatus("diamond")
                        dropdownState.value = false
                    })
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text =
                        "Each button below registers a placement. " +
                            "Each placement has been added to a campaign on the Superwall dashboard." +
                            " When the placement is registered, the audience filters in the campaign " +
                            "are evaluated and attempt to show a paywall." +
                            "Each product on the paywall is associated with an entitlement and Pro" +
                            " and Diamond features are gated behind their respective entitlements.",
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SWButton("Launch Non-gated feature", {
                    Superwall.instance.register("non_gated") {
                        context.dialog("Feature Launched", "The feature block was called")
                    }
                })
                SWButton("Launch Pro feature", {
                    Superwall.instance.register("pro") {
                        context.dialog("Pro Feature Launched", "The Pro feature block was called")
                    }
                })
                SWButton("Launch Diamond feature", {
                    Superwall.instance.register("diamond") {
                        context.dialog(
                            "Diamond Feature Launched",
                            "The Diamond feature block was called",
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
