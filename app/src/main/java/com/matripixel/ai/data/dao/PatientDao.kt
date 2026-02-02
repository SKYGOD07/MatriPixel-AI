package com.matripixel.ai.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.matripixel.ai.data.model.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: Patient): Long
    
    @Update
    suspend fun update(patient: Patient)
    
    @Delete
    suspend fun delete(patient: Patient)
    
    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getById(id: String): Patient?
    
    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<Patient>>
    
    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    suspend fun getAll(): List<Patient>
    
    @Query("SELECT COUNT(*) FROM patients")
    suspend fun getCount(): Int
    
    @Query("DELETE FROM patients")
    suspend fun deleteAll()
}
