package com.superwall.superapp.test

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo
import com.superwall.sdk.delegate.SuperwallDelegate
import com.superwall.superapp.test.UITestHandler.tests
import com.superwall.superapp.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class UITestInfo(
    val number: Int,
    val description: String,
    val testCaseType: TestCaseType = TestCaseType.iOS,
    test: suspend Context.(testDispatcher: CoroutineScope, events: Flow<SuperwallEvent>, message: MutableSharedFlow<Any?>) -> Unit,
) {
    private val events = MutableSharedFlow<SuperwallEvent?>(extraBufferCapacity = 100, replay = 3)
    private val message = MutableSharedFlow<Any?>(extraBufferCapacity = 10, replay = 10)

    fun events() = events

    fun messages() = message

    val test: suspend Context.() -> Unit = {
        val scope = CoroutineScope(Dispatchers.IO)
        delay(100)
        Superwall.instance.delegate =
            object : SuperwallDelegate {
                override fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {
                    Log.e(
                        "\n!!SuperwallDelegate!!\n",
                        "\tEvent name:" + eventInfo.event.rawName + "" +
                            ",\n\tParams:" + eventInfo.params + "\n",
                    )
                    scope.launch {
                        events.emit(eventInfo.event)
                    }
                }
            }
        test.invoke(this, scope, events().filterNotNull(), message)
    }
}

enum class TestCaseType(
    val prefix: String,
) {
    iOS(prefix = "iOS"),
    Android("Android"),
    ;

    fun titleText(testCaseNumber: Int): String =
        when (this) {
            iOS -> "UITest $testCaseNumber"
            Android -> "$prefix UITest $testCaseNumber"
        }
}

class UITestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                UITestTable()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UITestTable() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mainTextColor =
        if (isSystemInDarkTheme()) {
            Color.White // Set the color for dark mode
        } else {
            Color.Black // Set the color for light mode
        }

    LazyColumn {
        items(UITestHandler.tests.toList()) { item ->
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
