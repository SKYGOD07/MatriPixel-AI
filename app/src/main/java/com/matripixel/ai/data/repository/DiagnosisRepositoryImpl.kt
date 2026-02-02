package com.matripixel.ai.data.repository

import com.matripixel.ai.data.dao.DiagnosisDao
import com.matripixel.ai.data.model.DiagnosisScan
import com.matripixel.ai.data.model.RiskLevel
import com.matripixel.ai.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DiagnosisRepositoryImpl @Inject constructor(
    private val diagnosisDao: DiagnosisDao
) : DiagnosisRepository {
    
    override suspend fun insert(scan: DiagnosisScan): Long = diagnosisDao.insert(scan)
    
    override suspend fun update(scan: DiagnosisScan) = diagnosisDao.update(scan)
    
    override suspend fun delete(scan: DiagnosisScan) = diagnosisDao.delete(scan)
    
    override suspend fun getById(id: String): DiagnosisScan? = diagnosisDao.getById(id)
    
    override fun getByPatientFlow(patientId: String): Flow<List<DiagnosisScan>> = 
        diagnosisDao.getByPatientFlow(patientId)
    
    override suspend fun getByPatient(patientId: String): List<DiagnosisScan> = 
        diagnosisDao.getByPatient(patientId)
    
    override fun getAllFlow(): Flow<List<DiagnosisScan>> = diagnosisDao.getAllFlow()
    
    override suspend fun getRecent(limit: Int): List<DiagnosisScan> = diagnosisDao.getRecent(limit)
    
    // Sync operations
    override suspend fun getPendingSync(): List<DiagnosisScan> = 
        diagnosisDao.getBySyncStatus(SyncStatus.PENDING)
    
    override suspend fun getPendingSyncCount(): Int = 
        diagnosisDao.countBySyncStatus(SyncStatus.PENDING)
    
    override fun getPendingSyncCountFlow(): Flow<Int> = diagnosisDao.getPendingSyncCountFlow()
    
    override suspend fun markAsSynced(scanIds: List<String>) = 
        diagnosisDao.updateSyncStatusBatch(scanIds, SyncStatus.SYNCED)
    
    override suspend fun markAsFailed(scanIds: List<String>) = 
        diagnosisDao.updateSyncStatusBatch(scanIds, SyncStatus.FAILED)
    
    // Analytics
    override suspend fun countByRiskLevel(level: RiskLevel): Int = 
        diagnosisDao.countByRiskLevel(level)
    
    override suspend fun getAverageRiskScore(): Float? = diagnosisDao.getAverageRiskScore()
}
