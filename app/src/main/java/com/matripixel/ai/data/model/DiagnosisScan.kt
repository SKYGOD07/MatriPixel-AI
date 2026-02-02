package com.matripixel.ai.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Diagnosis scan entity storing analysis results.
 * 
 * Privacy considerations:
 * - Images are stored locally only, never synced
 * - Only anonymized scores are synced for federated learning
 */
@Entity(
    tableName = "diagnosis_scans",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId"), Index("syncStatus")]
)
data class DiagnosisScan(
    @PrimaryKey
    val scanId: String = UUID.randomUUID().toString(),
    
    val patientId: String,
    
    val scanType: ScanType,
    
    // Local file path to encrypted image (never synced)
    val localImagePath: String,
    
    @Embedded
    val vitals: Vitals,
    
    // Analysis results
    val riskScore: Float,       // 0.0 - 1.0
    val riskLevel: RiskLevel,   // RED, AMBER, GREEN
    val confidence: Float,      // Model confidence 0.0 - 1.0
    
    // Feature vector for federated learning (no raw data)
    val featureVector: String? = null,  // JSON encoded
    
    val inferenceTimeMs: Long,
    
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    
    val timestamp: Long = System.currentTimeMillis()
)

enum class ScanType {
    EYE_CONJUNCTIVA,
    NAIL_BED
}

enum class RiskLevel {
    RED,    // High risk - immediate consultation recommended
    AMBER,  // Moderate risk - schedule blood test
    GREEN   // Low risk - no immediate concern
}

enum class SyncStatus {
    PENDING,     // Awaiting sync
    SYNCED,      // Successfully synced
    FAILED       // Sync failed, will retry
}

/**
 * Vitals data embedded in DiagnosisScan
 */
data class Vitals(
    val fatigueLevel: Int? = null,           // 1-10 scale
    val shortnessOfBreath: Boolean = false,
    val knownHemoglobin: Float? = null,      // g/dL if known
    val paleSkin: Boolean = false,
    val dizziness: Boolean = false
)
