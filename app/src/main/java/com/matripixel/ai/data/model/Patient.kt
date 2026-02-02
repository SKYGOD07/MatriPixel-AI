package com.matripixel.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Patient entity for Room database.
 * 
 * Note: All data is encrypted at the database level via SQLCipher.
 * Patient name is optional to support anonymous screening.
 */
@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val name: String? = null, // Optional for privacy
    
    val age: Int,
    
    val gender: Gender,
    
    val createdAt: Long = System.currentTimeMillis()
)

enum class Gender {
    MALE,
    FEMALE,
    OTHER
}
