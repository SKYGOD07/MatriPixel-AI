package com.matripixel.ai.di

import android.content.Context
import com.matripixel.ai.ml.AnemiaDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MlModule {
    
    @Provides
    @Singleton
    fun provideAnemiaDetector(
        @ApplicationContext context: Context
    ): AnemiaDetector {
        return AnemiaDetector(context)
    }
}
