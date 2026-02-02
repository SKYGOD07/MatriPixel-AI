package com.matripixel.ai.data.db

import androidx.room.TypeConverter
import com.matripixel.ai.data.model.Gender
import com.matripixel.ai.data.model.RiskLevel
import com.matripixel.ai.data.model.ScanType
import com.matripixel.ai.data.model.SyncStatus

/**
 * Room type converters for enum classes
 */
class Converters {
    
    @TypeConverter
    fun fromGender(value: Gender): String = value.name
    
    @TypeConverter
    fun toGender(value: String): Gender = Gender.valueOf(value)
    
    @TypeConverter
    fun fromScanType(value: ScanType): String = value.name
    
    @TypeConverter
    fun toScanType(value: String): ScanType = ScanType.valueOf(value)
    
    @TypeConverter
    fun fromRiskLevel(value: RiskLevel): String = value.name
    
    @TypeConverter
    fun toRiskLevel(value: String): RiskLevel = RiskLevel.valueOf(value)
    
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name
    
    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
