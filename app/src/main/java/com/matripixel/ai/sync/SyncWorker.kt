package com.matripixel.ai.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.matripixel.ai.data.model.SyncStatus
import com.matripixel.ai.data.repository.DiagnosisRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for syncing diagnosis results.
 * 
 * Federated Learning Pattern:
 * - Only syncs anonymized feature vectors and risk scores
 * - Never uploads raw images or patient identifiers
 * - Runs only on WiFi and when charging (to save battery)
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val diagnosisRepository: DiagnosisRepository
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val WORK_NAME = "diagnosis_sync"
        private const val SYNC_INTERVAL_HOURS = 6L
        
        /**
         * Schedule periodic sync
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
                .setRequiresCharging(true)                      // While charging
                .setRequiresBatteryNotLow(true)                 // Battery not low
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
        
        /**
         * Cancel scheduled sync
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Get pending sync items
            val pendingScans = diagnosisRepository.getPendingSync()
            
            if (pendingScans.isEmpty()) {
                return Result.success()
            }
            
            // Prepare sync payload (anonymized data only)
            val syncPayload = prepareSyncPayload(pendingScans)
            
            // Attempt to sync (mock implementation - would hit Firebase in production)
            val success = performSync(syncPayload)
            
            if (success) {
                // Mark as synced
                val scanIds = pendingScans.map { it.scanId }
                diagnosisRepository.markAsSynced(scanIds)
                Result.success()
            } else {
                Result.retry()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
    
    /**
     * Prepare anonymized sync payload
     * Privacy: No raw images, no patient names, only aggregated analytics
     */
    private fun prepareSyncPayload(
        scans: List<com.matripixel.ai.data.model.DiagnosisScan>
    ): String {
        val payload = JSONObject().apply {
            put("device_id", getAnonymousDeviceId())
            put("timestamp", System.currentTimeMillis())
            put("count", scans.size)
            
            // Anonymized scan data
            val scansArray = JSONArray()
            scans.forEach { scan ->
                val scanJson = JSONObject().apply {
                    put("scan_type", scan.scanType.name)
                    put("risk_score", scan.riskScore)
                    put("risk_level", scan.riskLevel.name)
                    put("confidence", scan.confidence)
                    put("inference_time_ms", scan.inferenceTimeMs)
                    
                    // Parse and include feature vector (already anonymized)
                    scan.featureVector?.let { fv ->
                        put("features", JSONObject(fv))
                    }
                }
                scansArray.put(scanJson)
            }
            put("scans", scansArray)
            
            // Aggregate statistics for model improvement
            put("stats", JSONObject().apply {
                put("avg_risk_score", scans.map { it.riskScore }.average())
                put("high_risk_count", scans.count { it.riskLevel == com.matripixel.ai.data.model.RiskLevel.RED })
                put("moderate_risk_count", scans.count { it.riskLevel == com.matripixel.ai.data.model.RiskLevel.AMBER })
                put("low_risk_count", scans.count { it.riskLevel == com.matripixel.ai.data.model.RiskLevel.GREEN })
            })
        }
        
        return payload.toString()
    }
    
    /**
     * Perform the actual sync to backend
     * TODO: Implement Firebase Firestore upload
     */
    private suspend fun performSync(payload: String): Boolean {
        // Mock implementation - in production, this would:
        // 1. Encrypt the payload
        // 2. Upload to Firebase Firestore
        // 3. Optionally download updated model weights
        
        android.util.Log.d("SyncWorker", "Would sync payload: ${payload.take(500)}...")
        
        // Return true to simulate successful sync
        // In production, check actual network response
        return true
    }
    
    /**
     * Get anonymous device identifier
     * Does not contain any user-identifiable information
     */
    private fun getAnonymousDeviceId(): String {
        val prefs = applicationContext.getSharedPreferences("matripixel_device", Context.MODE_PRIVATE)
        return prefs.getString("anonymous_id", null) ?: run {
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("anonymous_id", newId).apply()
            newId
        }
    }
}
