package com.superwall.superapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.superwall.sdk.Superwall
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionStatusTestScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription Status Test") },
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
            verticalArrangement = Arrangement.Center,
        ) {
            ElevatedButton(
                onClick = {
                    scope.launch {
                        val status = SubscriptionStatus.Unknown
                        Superwall.instance.setSubscriptionStatus(status)
                        showDialog = true
                        dialogMessage = "Subscription status: Unknown"
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                Text("Set Subscription Status Unknown")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        val status = SubscriptionStatus.Inactive
                        Superwall.instance.setSubscriptionStatus(status)
                        showDialog = true
                        dialogMessage = "Subscription status: Inactive"
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                Text("Set Subscription Status Inactive")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        val entitlements =
                            setOf(
                                Entitlement("pro"),
                                Entitlement("test_entitlement"),
                            )
                        val status = SubscriptionStatus.Active(entitlements)
                        Superwall.instance.setSubscriptionStatus(status)
                        showDialog = true
                        dialogMessage = "Subscription status: Active - Entitlements: pro, test_entitlement"
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                Text("Set Subscription Status Active")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Success") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
        )
    }
}
