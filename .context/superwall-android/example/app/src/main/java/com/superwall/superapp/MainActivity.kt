package com.superwall.superapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.superwall.sdk.Superwall
import com.superwall.sdk.delegate.SuperwallDelegate
import com.superwall.sdk.identity.identify
import com.superwall.sdk.identity.setUserAttributes
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.superapp.ui.theme.SuperwallExampleAppTheme

class MainActivity :
    ComponentActivity(),
    SuperwallDelegate {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SuperwallExampleAppTheme {
                var errorMessage =
                    remember {
                        mutableStateOf<String?>(null)
                    }
                Superwall.instance.delegate =
                    object : SuperwallDelegate {
                        override fun handleLog(
                            level: String,
                            scope: String,
                            message: String?,
                            info: Map<String, Any>?,
                            error: Throwable?,
                        ) {
                            if (level == LogLevel.error.toString() &&
                                scope == LogScope.productsManager.toString()
                            ) {
                                errorMessage.value = message
                            }
                            super.handleLog(level, scope, message, info, error)
                        }
                    }
                if (errorMessage.value != null) {
                    AlertDialog(
                        onDismissRequest = { errorMessage.value = null },
                        title = { Text("Error") },
                        text = { Text(errorMessage.value ?: "") },
                        confirmButton = {
                            TextButton(onClick = { errorMessage.value = null }) {
                                Text("OK")
                            }
                        },
                    )
                }

                WelcomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WelcomeScreen() {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text =
                        "Welcome! Enter your name to get started." +
                            " Your name will be added to the Paywall user attributes," +
                            " which can then be accessed and displayed within your paywall.",
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 26.dp)
                            .heightIn(max = 50.dp)
                            .background(color = Color.White, shape = RoundedCornerShape(25.dp))
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(onDone = {
                            keyboardController?.hide()
                            startHomeActivity(context, name)
                        }),
                    textStyle =
                        TextStyle(
                            color = Color.Black,
                            textAlign = TextAlign.Start,
                        ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 16.dp),
                            // Apply horizontal padding to the Row
                            verticalAlignment = Alignment.CenterVertically, // Align text to the center vertically,
                        ) {
                            if (name.isEmpty() && !isFocused) {
                                Text(
                                    "Enter your name",
                                    style = TextStyle(color = Color.Gray),
                                )
                            }
                            innerTextField() // The inner text field will inherit the Row's padding
                        }
                    },
                )
                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { startHomeActivity(context, name) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = "Log In",
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

fun startHomeActivity(
    context: Context,
    name: String,
) {
    if (name.isNotBlank()) {
        Superwall.instance.setUserAttributes(
            mapOf(
                "firstName" to name,
            ),
        )
    }

    Superwall.instance.identify(userId = "abc")

    val intent = Intent(context, HomeActivity::class.java)
    context.startActivity(intent)
}
