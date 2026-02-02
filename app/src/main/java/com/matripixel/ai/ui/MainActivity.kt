package com.matripixel.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.matripixel.ai.sync.SyncWorker
import com.matripixel.ai.ui.navigation.MatriPixelNavHost
import com.matripixel.ai.ui.theme.MatriPixelTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - Entry point for MatriPixel AI
 * 
 * Features:
 * - Jetpack Compose UI
 * - Hilt Dependency Injection
 * - Camera Permission Handling
 * - WorkManager Sync Scheduling
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, camera features are available
        } else {
            // TODO: Show permission rationale UI
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request camera permission if not granted
        checkCameraPermission()
        
        // Schedule background sync
        SyncWorker.schedule(this)
        
        setContent {
            MatriPixelTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MatriPixelNavHost()
                }
            }
        }
    }
    
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // TODO: Show rationale dialog before requesting
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
