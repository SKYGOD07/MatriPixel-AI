package com.matripixel.ai.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.matripixel.ai.data.model.DiagnosisScan
import com.matripixel.ai.data.model.RiskLevel
import com.matripixel.ai.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosisDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: DiagnosisScan): Long
    
    @Update
    suspend fun update(scan: DiagnosisScan)
    
    @Delete
    suspend fun delete(scan: DiagnosisScan)
    
    @Query("SELECT * FROM diagnosis_scans WHERE scanId = :id")
    suspend fun getById(id: String): DiagnosisScan?
    
    @Query("SELECT * FROM diagnosis_scans WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getByPatientFlow(patientId: String): Flow<List<DiagnosisScan>>
    
    @Query("SELECT * FROM diagnosis_scans WHERE patientId = :patientId ORDER BY timestamp DESC")
    suspend fun getByPatient(patientId: String): List<DiagnosisScan>
    
    @Query("SELECT * FROM diagnosis_scans ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<DiagnosisScan>>
    
    @Query("SELECT * FROM diagnosis_scans ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DiagnosisScan>
    
    // Sync-related queries
    @Query("SELECT * FROM diagnosis_scans WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<DiagnosisScan>
    
    @Query("SELECT COUNT(*) FROM diagnosis_scans WHERE syncStatus = :status")
    suspend fun countBySyncStatus(status: SyncStatus): Int
    
    @Query("SELECT COUNT(*) FROM diagnosis_scans WHERE syncStatus = 'PENDING'")
    fun getPendingSyncCountFlow(): Flow<Int>
    
    @Query("UPDATE diagnosis_scans SET syncStatus = :status WHERE scanId = :scanId")
    suspend fun updateSyncStatus(scanId: String, status: SyncStatus)
    
    @Query("UPDATE diagnosis_scans SET syncStatus = :status WHERE scanId IN (:scanIds)")
    suspend fun updateSyncStatusBatch(scanIds: List<String>, status: SyncStatus)
    
    // Analytics queries
    @Query("SELECT COUNT(*) FROM diagnosis_scans WHERE riskLevel = :level")
    suspend fun countByRiskLevel(level: RiskLevel): Int
    
    @Query("SELECT AVG(riskScore) FROM diagnosis_scans")
    suspend fun getAverageRiskScore(): Float?
    
    @Query("DELETE FROM diagnosis_scans")
    suspend fun deleteAll()
}
