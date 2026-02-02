package com.matripixel.ai.data.repository

import com.matripixel.ai.data.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    suspend fun insert(patient: Patient): Long
    suspend fun update(patient: Patient)
    suspend fun delete(patient: Patient)
    suspend fun getById(id: String): Patient?
    fun getAllFlow(): Flow<List<Patient>>
    suspend fun getAll(): List<Patient>
    suspend fun getCount(): Int
}
