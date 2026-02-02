package com.matripixel.ai.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.matripixel.ai.data.dao.DiagnosisDao
import com.matripixel.ai.data.dao.PatientDao
import com.matripixel.ai.data.model.DiagnosisScan
import com.matripixel.ai.data.model.Patient

/**
 * MatriPixel AI Room Database
 * 
 * HIPAA Compliance:
 * - Encrypted at rest using SQLCipher (AES-256)
 * - Passphrase stored in Android Keystore
 * - No automatic backup enabled
 */
@Database(
    entities = [
        Patient::class,
        DiagnosisScan::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MatriPixelDatabase : RoomDatabase() {
    
    abstract fun patientDao(): PatientDao
    
    abstract fun diagnosisDao(): DiagnosisDao
}
