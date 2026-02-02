package com.matripixel.ai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * MatriPixel AI Application
 * 
 * Offline-first anemia detection app with:
 * - Edge AI inference via LiteRT
 * - HIPAA-compliant encrypted storage
 * - Federated learning sync pattern
 */
@HiltAndroidApp
class MatriPixelApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
