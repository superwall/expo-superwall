package com.superwall.superapp.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.superwall.superapp.ui.theme.MyApplicationTheme

class SuperwallDebugActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val bottomSheetState = rememberModalBottomSheetState()
                var showBottomSheet by remember { mutableStateOf(false) }

                // Show bottom sheet immediately when activity opens
                LaunchedEffect(Unit) {
                    showBottomSheet = true
                }

                // Transparent background - only show the bottom sheet
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Transparent),
                ) {
                    if (showBottomSheet) {
                        SuperwallDebugBottomSheet(
                            sheetState = bottomSheetState,
                            onDismiss = {
                                showBottomSheet = false
                                finish() // Close activity when bottom sheet is dismissed
                            },
                        )
                    }
                }
            }
        }
    }
}
