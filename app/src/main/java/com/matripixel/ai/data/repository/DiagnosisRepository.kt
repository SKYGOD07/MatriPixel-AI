package com.matripixel.ai.data.repository

import com.matripixel.ai.data.model.DiagnosisScan
import com.matripixel.ai.data.model.RiskLevel
import com.matripixel.ai.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface DiagnosisRepository {
    suspend fun insert(scan: DiagnosisScan): Long
    suspend fun update(scan: DiagnosisScan)
    suspend fun delete(scan: DiagnosisScan)
    suspend fun getById(id: String): DiagnosisScan?
    fun getByPatientFlow(patientId: String): Flow<List<DiagnosisScan>>
    suspend fun getByPatient(patientId: String): List<DiagnosisScan>
    fun getAllFlow(): Flow<List<DiagnosisScan>>
    suspend fun getRecent(limit: Int): List<DiagnosisScan>
    
    // Sync operations
    suspend fun getPendingSync(): List<DiagnosisScan>
    suspend fun getPendingSyncCount(): Int
    fun getPendingSyncCountFlow(): Flow<Int>
    suspend fun markAsSynced(scanIds: List<String>)
    suspend fun markAsFailed(scanIds: List<String>)
    
    // Analytics
    suspend fun countByRiskLevel(level: RiskLevel): Int
    suspend fun getAverageRiskScore(): Float?
}
