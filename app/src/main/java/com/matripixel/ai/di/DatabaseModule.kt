package com.matripixel.ai.di

import android.content.Context
import androidx.room.Room
import com.matripixel.ai.data.db.MatriPixelDatabase
import com.matripixel.ai.data.repository.DiagnosisRepository
import com.matripixel.ai.data.repository.DiagnosisRepositoryImpl
import com.matripixel.ai.data.repository.PatientRepository
import com.matripixel.ai.data.repository.PatientRepositoryImpl
import com.matripixel.ai.util.DatabasePassphraseManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabasePassphraseManager(
        @ApplicationContext context: Context
    ): DatabasePassphraseManager {
        return DatabasePassphraseManager(context)
    }
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphraseManager: DatabasePassphraseManager
    ): MatriPixelDatabase {
        val passphrase = passphraseManager.getOrCreatePassphrase()
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            MatriPixelDatabase::class.java,
            "matripixel_encrypted.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun providePatientRepository(
        database: MatriPixelDatabase
    ): PatientRepository {
        return PatientRepositoryImpl(database.patientDao())
    }
    
    @Provides
    @Singleton
    fun provideDiagnosisRepository(
        database: MatriPixelDatabase
    ): DiagnosisRepository {
        return DiagnosisRepositoryImpl(database.diagnosisDao())
    }
}
