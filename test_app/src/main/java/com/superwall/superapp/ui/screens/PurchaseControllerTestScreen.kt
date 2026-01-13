package com.superwall.superapp.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.superwall.sdk.Superwall
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.misc.ActivityProvider
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.register
import com.superwall.superapp.BuildConfig
import com.superwall.superapp.purchase.TestingPurchaseController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseControllerTestScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isConfigured by remember { mutableStateOf(false) }
    var showFeatureDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    val purchaseController = remember { TestingPurchaseController() }
    val apiKey = "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257" // Android key
    var purchaseEnabled by remember { mutableStateOf(purchaseController.purchasesEnabled) }
    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Mock PC Test") },
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
            ElevatedButton(
                onClick = {
                    scope.launch {
                        try {
                            configureWithPC(context, apiKey, purchaseController)
                            isConfigured = true
                        } catch (e: Exception) {
                            // Handle configuration error
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Configure with PC")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        try {
                            configureWithoutPC(context, apiKey)
                            isConfigured = true
                        } catch (e: Exception) {
                            // Handle configuration error
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Configure without PC")
            }

            if (isConfigured) {
                Column {
                    val status by Superwall.instance.subscriptionStatus.collectAsState()
                    Text(
                        color = Color.Black,
                        text = "Subscription status is ${
                            when (status){
                                is SubscriptionStatus.Active -> "Active"
                                SubscriptionStatus.Inactive -> "Inactve"
                                SubscriptionStatus.Unknown -> "Unknown"
                            }
                        }",
                    )
                    ElevatedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    // Trigger paywall
                                    Superwall.instance.register(
                                        placement = "campaign_trigger",
                                        feature = {
                                            showFeatureDialog = true
                                        },
                                    )
                                } catch (e: Exception) {
                                    showErrorDialog = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Trigger Paywall")
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
                            val currentlyEnabled =
                                purchaseController.purchasesEnabled

                            purchaseController.purchasesEnabled = !currentlyEnabled
                            purchaseEnabled = !currentlyEnabled
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (purchaseEnabled) "Disable purchases" else "Enable purchases")
                    }

                    ElevatedButton(
                        onClick = {
                            purchaseController.restoreEnabled = !purchaseController.restoreEnabled
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (purchaseController.restoreEnabled) "Disable restore" else "Enable restore")
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
                        Text("Reset status")
                    }
                }
            }
        }
    }

// Dialogs
    if (showFeatureDialog) {
        AlertDialog(
            onDismissRequest = { showFeatureDialog = false },
            title = { Text("Feature") },
            text = { Text("Feature triggered") },
            confirmButton = {
                TextButton(onClick = { showFeatureDialog = false }) {
                    Text("OK")
                }
            },
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success") },
            text = { Text("Subscribed") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            },
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text("An error occurred") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("DONE")
                }
            },
        )
    }
}

private suspend fun configureWithPC(
    context: Context,
    apiKey: String,
    purchaseController: TestingPurchaseController,
) {
    val application = context.applicationContext as Application
    val options =
        SuperwallOptions().apply {
            logging.level = LogLevel.debug
            networkEnvironment = getNetworkEnvironment()
        }
    val activityProvider =
        object : ActivityProvider {
            override fun getCurrentActivity(): android.app.Activity? = context as? android.app.Activity
        }

    Superwall.configure(
        applicationContext = application,
        apiKey = apiKey,
        purchaseController = purchaseController,
        options = options,
        activityProvider = activityProvider,
    )

    // Set subscription status to inactive
    Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
}

private suspend fun configureWithoutPC(
    context: Context,
    apiKey: String,
) {
    val application = context.applicationContext as Application
    val options =
        SuperwallOptions().apply {
            logging.level = LogLevel.debug
            networkEnvironment = getNetworkEnvironment()
        }
    val activityProvider =
        object : ActivityProvider {
            override fun getCurrentActivity(): android.app.Activity? = context as? android.app.Activity
        }

    Superwall.configure(
        applicationContext = application,
        apiKey = apiKey,
        purchaseController = null,
        options = options,
        activityProvider = activityProvider,
    )

    // Set subscription status to inactive
    Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
}

private fun getNetworkEnvironment(): SuperwallOptions.NetworkEnvironment {
    // Get environment configuration from BuildConfig
    val env = BuildConfig.SUPERWALL_ENV
    val customEndpoint = BuildConfig.SUPERWALL_ENDPOINT

    return when (env.lowercase()) {
        "dev", "developer" -> SuperwallOptions.NetworkEnvironment.Developer()
        "custom" -> {
            val endpoint = customEndpoint?.takeIf { it != "null" && it != "NONE" }
            if (endpoint != null && endpoint.isNotEmpty()) {
                // Parse custom endpoint - this is a simplified version
                // In production, you'd want more robust URL parsing
                SuperwallOptions.NetworkEnvironment.Custom(
                    baseHost = endpoint,
                    collectorHost = endpoint.replace("api.", "collector."),
                    scheme = if (endpoint.contains("localhost") || endpoint.contains("127.0.0.1")) "http" else "https",
                    port = null,
                )
            } else {
                // Fall back to release if custom endpoint not provided
                SuperwallOptions.NetworkEnvironment.Release()
            }
        }

        else -> SuperwallOptions.NetworkEnvironment.Release()
    }
}
