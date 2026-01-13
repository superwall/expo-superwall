package com.superwall.superapp.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.superwall.sdk.Superwall
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperwallDebugBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    var deviceTemplateMap by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val subscriptionStatus by Superwall.instance.subscriptionStatus.collectAsState()
    val userAttributes = Superwall.instance.userAttributes
    val integrationAttributes = Superwall.instance.integrationAttributes
    val configurationState = Superwall.instance.configurationState

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val templateMap = Superwall.instance.deviceAttributes()
                withContext(Dispatchers.Main) {
                    deviceTemplateMap = templateMap
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Superwall Debug Info",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Loading device data...")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Configuration Status
                    item {
                        DebugSection(
                            title = "Configuration",
                            items =
                                mapOf(
                                    "Status" to configurationState.toString(),
                                    "User ID" to Superwall.instance.userId,
                                    "External Account ID" to Superwall.instance.externalAccountId,
                                    "Is Logged In" to Superwall.instance.isLoggedIn.toString(),
                                    "Locale" to (Superwall.instance.localeIdentifier ?: "Not set"),
                                ),
                        )
                    }

                    // Subscription Status
                    item {
                        DebugSection(
                            title = "Subscription",
                            items =
                                mapOf(
                                    "Status" to
                                        subscriptionStatus.let {
                                            when (it) {
                                                is SubscriptionStatus.Active -> " Active"
                                                SubscriptionStatus.Inactive -> " Inactive"
                                                SubscriptionStatus.Unknown -> " Unknown"
                                            }
                                        },
                                    "Is Paywall Presented" to Superwall.instance.isPaywallPresented.toString(),
                                ),
                        )
                    }

                    // User Attributes
                    if (userAttributes.isNotEmpty()) {
                        item {
                            DebugSection(
                                title = "User Attributes",
                                items = userAttributes.mapValues { it.value.toString() },
                            )
                        }
                    }

                    // Integration Attributes
                    if (integrationAttributes.isNotEmpty()) {
                        item {
                            DebugSection(
                                title = "Integration Attributes",
                                items = integrationAttributes,
                            )
                        }
                    }

                    // Device Template Data
                    deviceTemplateMap?.let { templateMap ->
                        item {
                            DebugSection(
                                title = "Device Template",
                                items =
                                    templateMap
                                        .mapValues { it.value.toString() }
                                        .filterValues { it.isNotBlank() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugSection(
    title: String,
    items: Map<String, String>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { (key, value) ->
                DebugRow(label = key, value = value)
            }
        }
    }
}

@Composable
private fun DebugRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
    }
}
