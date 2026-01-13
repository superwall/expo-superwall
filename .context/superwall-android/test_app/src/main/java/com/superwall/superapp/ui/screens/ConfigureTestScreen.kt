package com.superwall.superapp.ui.screens

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.superwall.sdk.Superwall
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.delegate.subscription_controller.PurchaseController
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.misc.ActivityProvider
import com.superwall.superapp.BuildConfig
import com.superwall.superapp.purchase.RevenueCatPurchaseController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureTestScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    // API keys for testing
    val apiKey = "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257" // Android key

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ConfigureTest") },
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
                        try {
                            configureSuperwallWithPC(context, apiKey)
                            showDialog = true
                            dialogMessage = "Configuration completed"
                        } catch (e: Exception) {
                            showDialog = true
                            dialogMessage = "Configuration failed: ${e.message}"
                        }
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                Text("Configure with dialog shown + PC")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        try {
                            configureSuperwallWithDialogShownNoPC(context, apiKey)
                            showDialog = true
                            dialogMessage = "Configuration completed"
                        } catch (e: Exception) {
                            showDialog = true
                            dialogMessage = "Configuration failed: ${e.message}"
                        }
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                Text("Configure with dialog shown + no PC")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        try {
                            configureSuperwallWithRC(context, apiKey)
                            showDialog = true
                            dialogMessage = "Configuration completed"
                        } catch (e: Exception) {
                            showDialog = true
                            dialogMessage = "Configuration failed: ${e.message}"
                        }
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                Text("Configure with dialog shown and RC")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        try {
                            configureSuperwall(context, apiKey)
                            showDialog = true
                            dialogMessage = "Configuration completed"
                        } catch (e: Exception) {
                            showDialog = true
                            dialogMessage = "Configuration failed: ${e.message}"
                        }
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                Text("Just configure")
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

private suspend fun configureSuperwallWithPC(
    context: Context,
    apiKey: String,
) {
    val application = context.applicationContext as Application
    val options =
        SuperwallOptions().apply {
            logging.level = LogLevel.debug
            networkEnvironment = getNetworkEnvironment()
        }

    // Create a test purchase controller
    val purchaseController =
        object : PurchaseController {
            override suspend fun purchase(
                activity: android.app.Activity,
                productDetails: com.android.billingclient.api.ProductDetails,
                basePlanId: String?,
                offerId: String?,
            ): com.superwall.sdk.delegate.PurchaseResult =
                com.superwall.sdk.delegate.PurchaseResult
                    .Purchased()

            override suspend fun restorePurchases(): com.superwall.sdk.delegate.RestorationResult =
                com.superwall.sdk.delegate.RestorationResult
                    .Restored()
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
}

private suspend fun configureSuperwallWithDialogShownNoPC(
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
}

private suspend fun configureSuperwallWithRC(
    context: Context,
    apiKey: String,
) {
    val application = context.applicationContext as Application
    val options =
        SuperwallOptions().apply {
            logging.level = LogLevel.debug
            networkEnvironment = getNetworkEnvironment()
        }

    // Create a real RevenueCat purchase controller
    val rcPurchaseController = RevenueCatPurchaseController(context)

    val activityProvider =
        object : ActivityProvider {
            override fun getCurrentActivity(): android.app.Activity? = context as? android.app.Activity
        }

    Superwall.configure(
        applicationContext = application,
        apiKey = apiKey,
        purchaseController = rcPurchaseController,
        options = options,
        activityProvider = activityProvider,
    )
}

private suspend fun configureSuperwall(
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
}

private fun getNetworkEnvironment(): SuperwallOptions.NetworkEnvironment {
    // Get environment configuration from BuildConfig
    val env = BuildConfig.SUPERWALL_ENV
    val customEndpoint = BuildConfig.SUPERWALL_ENDPOINT

    return when (env.lowercase()) {
        "dev", "developer" -> {
            Log.e("TestApp", "Using environment - dev")
            SuperwallOptions.NetworkEnvironment.Developer()
        }
        "custom" -> {
            val endpoint = customEndpoint?.takeIf { it != "null" && it != "NONE" }
            if (endpoint != null && endpoint.isNotEmpty()) {
                Log.e("TestApp", "Using environment - custom - $endpoint")
                // Parse custom endpoint - this is a simplified version
                // In production, you'd want more robust URL parsing
                SuperwallOptions.NetworkEnvironment.Custom(
                    baseHost = endpoint,
                    collectorHost = endpoint.replace("api.", "collector."),
                    scheme = if (endpoint.contains("localhost") || endpoint.contains("127.0.0.1")) "http" else "https",
                    port = null,
                )
            } else {
                Log.e("TestApp", "Using environment - Release - custom endpoint not provided")
                // Fall back to release if custom endpoint not provided
                SuperwallOptions.NetworkEnvironment.Release()
            }
        }
        else -> {
            Log.e("TestApp", "Using environment - release")
            SuperwallOptions.NetworkEnvironment.Release()
        }
    }
}
