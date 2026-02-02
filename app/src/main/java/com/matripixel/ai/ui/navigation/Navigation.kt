package com.matripixel.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matripixel.ai.ui.screens.*

/**
 * MatriPixel Navigation Routes
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Capture : Screen("capture")
    object Result : Screen("result/{riskScore}") {
        fun createRoute(riskScore: Float) = "result/$riskScore"
    }
}

/**
 * Main Navigation Host
 */
@Composable
fun MatriPixelNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                pendingSyncs = 3, // TODO: Get from ViewModel
                highRiskAlerts = listOf(
                    HighRiskAlert("1", "Priya Devi", "2 hours ago", 0.92f),
                    HighRiskAlert("2", "Sunita Kumari", "5 hours ago", 0.85f)
                ), // TODO: Get from ViewModel
                onNewScreeningClick = {
                    navController.navigate(Screen.Capture.route)
                },
                onPendingSyncsClick = {
                    // TODO: Navigate to sync screen
                },
                onAlertClick = { alert ->
                    navController.navigate(Screen.Result.createRoute(alert.riskScore))
                }
            )
        }
        
        // Capture Screen
        composable(Screen.Capture.route) {
            CaptureScreen(
                onCapture = { imageProxy ->
                    // TODO: Process image with TFLite model
                    imageProxy.close()
                },
                onClose = {
                    navController.popBackStack()
                },
                onCaptureComplete = {
                    // TODO: Navigate with actual risk score from ML model
                    // For demo, using random score
                    val demoRiskScore = (0..100).random() / 100f
                    navController.navigate(Screen.Result.createRoute(demoRiskScore)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        
        // Result Screen
        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("riskScore") {
                    type = NavType.FloatType
                    defaultValue = 0.5f
                }
            )
        ) { backStackEntry ->
            val riskScore = backStackEntry.arguments?.getFloat("riskScore") ?: 0.5f
            
            ResultScreen(
                riskScore = riskScore,
                patientName = "Patient", // TODO: Get from ViewModel
                onGenerateReferral = {
                    // TODO: Generate referral ticket
                },
                onNewScreening = {
                    navController.navigate(Screen.Capture.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onViewHistory = {
                    // TODO: Navigate to history
                },
                onClose = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
