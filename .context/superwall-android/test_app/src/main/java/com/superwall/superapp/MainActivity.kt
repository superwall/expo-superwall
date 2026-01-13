package com.superwall.superapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.superwall.superapp.ui.screens.ConfigureTestScreen
import com.superwall.superapp.ui.screens.DelegateTestScreen
import com.superwall.superapp.ui.screens.HandlerTestScreen
import com.superwall.superapp.ui.screens.HomeScreen
import com.superwall.superapp.ui.screens.PurchaseControllerTestScreen
import com.superwall.superapp.ui.screens.SubscriptionStatusTestScreen
import com.superwall.superapp.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                    ) {
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        composable("configureTest") {
                            ConfigureTestScreen(navController = navController)
                        }
                        composable("subscriptionStatusTest") {
                            SubscriptionStatusTestScreen(navController = navController)
                        }
                        composable("purchaseControllerTest") {
                            PurchaseControllerTestScreen(navController = navController)
                        }
                        composable("delegateTest") {
                            DelegateTestScreen(navController = navController)
                        }
                        composable("handlerTest") {
                            HandlerTestScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
