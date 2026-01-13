package com.superwall.superapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.superwall.sdk.Superwall
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.register
import com.superwall.superapp.delegates.TestDelegate
import com.superwall.superapp.delegates.TestDelegateEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegateTestScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    val subscriptionStatus by Superwall.instance.subscriptionStatus.collectAsState()
    val testDelegate = remember { TestDelegate() }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Delegate Test") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                color = Color.Black,
                text = "Subscription status is ${
                    when (subscriptionStatus){
                        is SubscriptionStatus.Active -> "Active"
                        SubscriptionStatus.Inactive -> "Inactve"
                        SubscriptionStatus.Unknown -> "Unknown"
                    }
                }",
            )
            ElevatedButton(
                onClick = {
                    Superwall.instance.delegate = testDelegate
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set Test Delegate")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        Superwall.instance.register(placement = "campaign_trigger")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Show Paywall")
            }

            ElevatedButton(
                onClick = {
                    showEventsDialog(testDelegate.eventsWithoutLog) { title, content ->
                        dialogTitle = title
                        dialogContent = content
                        showDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Show Delegate Events without log")
            }

            ElevatedButton(
                onClick = {
                    val logEvents =
                        testDelegate.events.filter {
                            it is TestDelegateEvent.HandleLog
                        }
                    showEventsDialog(logEvents) { title, content ->
                        dialogTitle = title
                        dialogContent = content
                        showDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Show Delegate Events with log")
            }

            ElevatedButton(
                onClick = {
                    testDelegate.clearEvents()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear Delegate Events")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        val status = SubscriptionStatus.Active(setOf(Entitlement("test")))
                        Superwall.instance.setSubscriptionStatus(status)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Change Subscription Status")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        val status = SubscriptionStatus.Inactive
                        Superwall.instance.setSubscriptionStatus(status)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set Status Inactive")
            }

            ElevatedButton(
                onClick = {
                    testDelegate.clearEvents()
                    Superwall.instance.delegate = null
                    scope.launch {
                        val status = SubscriptionStatus.Inactive
                        Superwall.instance.setSubscriptionStatus(status)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear Delegate and Change Status")
            }

            ElevatedButton(
                onClick = {
                    testDelegate.clearEvents()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear Delegate Events")
            }

            ElevatedButton(
                onClick = {
                    val eventsWithoutLogAndPresentation =
                        testDelegate.eventsWithoutLog.filter { event ->
                            event !is TestDelegateEvent.WillPresentPaywall &&
                                event !is TestDelegateEvent.DidPresentPaywall &&
                                event !is TestDelegateEvent.WillDismissPaywall &&
                                event !is TestDelegateEvent.DidDismissPaywall
                        }
                    showEventsDialog(eventsWithoutLogAndPresentation) { title, content ->
                        dialogTitle = title
                        dialogContent = content
                        showDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Events without log and presentation")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle) },
            text = dialogContent,
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
        )
    }
}

private fun showEventsDialog(
    events: List<TestDelegateEvent>,
    onShow: (title: String, content: @Composable () -> Unit) -> Unit,
) {
    onShow("Delegate Events") {
        LazyColumn {
            itemsIndexed(events) { index, event ->
                val eventName =
                    when (event) {
                        is TestDelegateEvent.DidDismissPaywall -> "DidDismissPaywall"
                        is TestDelegateEvent.DidPresentPaywall -> "DidPresentPaywall"
                        is TestDelegateEvent.HandleCustomPaywallAction -> "HandleCustomPaywallAction"
                        is TestDelegateEvent.HandleLog -> "HandleLog"
                        is TestDelegateEvent.PaywallWillOpenDeepLink -> "PaywallWillOpenDeepLink"
                        is TestDelegateEvent.PaywallWillOpenURL -> "PaywallWillOpenURL"
                        is TestDelegateEvent.SubscriptionStatusDidChange -> "SubscriptionStatusDidChange"
                        is TestDelegateEvent.WillDismissPaywall -> "WillDismissPaywall"
                        is TestDelegateEvent.WillPresentPaywall -> "WillPresentPaywall"
                    }
                Text("Event ${index + 1}: $eventName")
            }
        }
    }
}
