package com.superwall.superapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.superwall.sdk.Superwall
import com.superwall.sdk.paywall.presentation.PaywallPresentationHandler
import com.superwall.sdk.paywall.presentation.dismiss
import com.superwall.sdk.paywall.presentation.register
import com.superwall.superapp.handlers.HandlerEvent
import com.superwall.superapp.handlers.TestHandler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandlerTestScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var featureBlockExecuted by remember { mutableStateOf(false) }

    val presentationHandler = remember { PaywallPresentationHandler() }
    val testHandler =
        remember {
            TestHandler(presentationHandler).apply {
                setupHandlers()
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Handler Test") },
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
                        // Test non-gated paywall
                        Superwall.instance.register(
                            placement = "non_gated_paywall",
                            handler = presentationHandler,
                            feature = {
                                featureBlockExecuted = true
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Test Non-Gated Paywall")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        // Test gated paywall - feature block should NOT execute until purchase
                        Superwall.instance.register(
                            placement = "gated_paywall",
                            handler = presentationHandler,
                            feature = {
                                featureBlockExecuted = true
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Test Gated Paywall")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        // Test skip audience - should skip paywall and execute feature
                        Superwall.instance.register(
                            placement = "skip_audience",
                            handler = presentationHandler,
                            feature = {
                                featureBlockExecuted = true
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Test Skip Audience")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        // Test error placement - should trigger error event
                        Superwall.instance.register(
                            placement = "error_placement",
                            handler = presentationHandler,
                            feature = {
                                featureBlockExecuted = true
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Test Error Placement")
            }

            ElevatedButton(
                onClick = {
                    scope.launch {
                        // Dismiss any active paywall
                        Superwall.instance.dismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Dismiss Paywall")
            }

            ElevatedButton(
                onClick = {
                    testHandler.clearEvents()
                    featureBlockExecuted = false
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear Handler Events")
            }

            ElevatedButton(
                onClick = {
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Show Handler Events")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test Results Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Test Results",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Feature Block Executed: ${if (featureBlockExecuted) "Yes" else "No"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        text = "Event Count: ${testHandler.events.size}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Handler Events") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                ) {
                    itemsIndexed(testHandler.events) { index, event ->
                        val eventName =
                            when (event) {
                                is HandlerEvent.OnPresent -> "OnPresent"
                                is HandlerEvent.OnDismiss -> "OnDismiss"
                                is HandlerEvent.OnError -> "OnError"
                                is HandlerEvent.OnSkip -> "OnSkip"
                            }
                        Text("Event ${index + 1}: $eventName")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
        )
    }
}
