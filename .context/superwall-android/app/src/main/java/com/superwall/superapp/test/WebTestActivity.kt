package com.superwall.superapp.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.superwall.sdk.Superwall
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.superapp.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WebTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                WebUITestTable()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WebUITestTable() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val mainTextColor = Color.Black // Set the color for light mode

    val tests =
        remember {
            UITestHandler.WebTests.tests.toList()
        }

    var force by remember {
        mutableStateOf(0)
    }

    val subscriptionStatus =
        Superwall.instance.subscriptionStatus.collectAsState(initial = SubscriptionStatus.Unknown)

    key(force) {
        LazyColumn {
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "User:",
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = mainTextColor,
                        )
                        Text(
                            text =
                                Superwall.instance.userId
                                    .take(29)
                                    .plus("..."),
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = mainTextColor,
                        )
                    }

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Subscription status",
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = mainTextColor,
                        )
                        Text(
                            text =
                                subscriptionStatus.value.let {
                                    when (it) {
                                        SubscriptionStatus.Unknown -> "Unknown"
                                        is SubscriptionStatus.Active -> "Active"
                                        SubscriptionStatus.Inactive -> "Inactive"
                                    }
                                },
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = mainTextColor,
                        )
                    }
                    if (subscriptionStatus.value is SubscriptionStatus.Active) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Entitlements:",
                                style = TextStyle(fontWeight = FontWeight.Bold),
                                color = mainTextColor,
                            )
                            Text(
                                text =
                                    Superwall.instance.entitlements.active
                                        .map { " id: ${it.id}" }
                                        .joinToString("\n"),
                                style = TextStyle(fontWeight = FontWeight.Bold),
                                color = mainTextColor,
                            )
                        }
                    }
                }
            }
            items(
                tests,
            ) { item ->
                val index = tests.indexOf(item)
                Column {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .weight(2f)
                                    .padding(horizontal = 8.dp),
                        ) {
                            Text(
                                color = mainTextColor,
                                text = item.testCaseType.titleText(item.number),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            Text(
                                text = "${item.description}",
                                style = TextStyle(color = Color.Gray),
                            )
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    launch(Dispatchers.IO) {
                                        item.test(context)
                                        force++
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Launch $index")
                        }
                    }
                    Divider(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(0.5.dp),
                        color = Color.LightGray,
                    )
                }
            }
        }
    }
}
