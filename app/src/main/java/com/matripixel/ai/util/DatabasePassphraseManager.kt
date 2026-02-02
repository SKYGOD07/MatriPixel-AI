package com.matripixel.ai.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Manages the SQLCipher database passphrase securely using Android Keystore.
 * 
 * HIPAA Compliance:
 * - Passphrase is stored in EncryptedSharedPreferences
 * - Backed by Android Keystore (hardware-backed on supported devices)
 * - Passphrase is generated using SecureRandom
 */
class DatabasePassphraseManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "matripixel_secure_prefs"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_LENGTH = 32
    }
    
    private val masterKey: MasterKey by lazy {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        MasterKey.Builder(context)
            .setKeyGenParameterSpec(spec)
            .build()
    }
    
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Gets existing passphrase or creates a new one if none exists.
     * Returns passphrase as ByteArray for SQLCipher.
     */
    fun getOrCreatePassphrase(): ByteArray {
        val existing = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
        
        return if (existing != null) {
            existing.toByteArray(Charsets.UTF_8)
        } else {
            val newPassphrase = generateSecurePassphrase()
            encryptedPrefs.edit()
                .putString(KEY_DB_PASSPHRASE, String(newPassphrase, Charsets.UTF_8))
                .apply()
            newPassphrase
        }
    }
    
    private fun generateSecurePassphrase(): ByteArray {
        val random = SecureRandom()
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        random.nextBytes(passphrase)
        
        // Convert to printable characters for SQLCipher compatibility
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
        return passphrase.map { byte ->
            chars[(byte.toInt() and 0xFF) % chars.length].code.toByte()
        }.toByteArray()
    }
}
