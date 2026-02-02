package com.matripixel.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.matripixel.ai.ui.navigation.MatriPixelNavHost
import com.matripixel.ai.ui.theme.MatriPixelTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - Entry point for MatriPixel App
 * Designed for ASHA workers performing anemia screenings in the field
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MatriPixelTheme {
                val navController = rememberNavController()
                
                Surface(modifier = Modifier.fillMaxSize()) {
                    MatriPixelNavHost(
                        navController = navController
                    )
                }
            }
        }
    }
}
