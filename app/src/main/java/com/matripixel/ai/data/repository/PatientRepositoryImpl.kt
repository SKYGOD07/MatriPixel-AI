package com.matripixel.ai.data.repository

import com.matripixel.ai.data.dao.PatientDao
import com.matripixel.ai.data.model.Patient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PatientRepositoryImpl @Inject constructor(
    private val patientDao: PatientDao
) : PatientRepository {
    
    override suspend fun insert(patient: Patient): Long = patientDao.insert(patient)
    
    override suspend fun update(patient: Patient) = patientDao.update(patient)
    
    override suspend fun delete(patient: Patient) = patientDao.delete(patient)
    
    override suspend fun getById(id: String): Patient? = patientDao.getById(id)
    
    override fun getAllFlow(): Flow<List<Patient>> = patientDao.getAllFlow()
    
    override suspend fun getAll(): List<Patient> = patientDao.getAll()
    
    override suspend fun getCount(): Int = patientDao.getCount()
}
