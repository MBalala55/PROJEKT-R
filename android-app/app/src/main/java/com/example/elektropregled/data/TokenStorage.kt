package com.example.elektropregled.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface TokenStorage {
    fun saveToken(token: String, expiresIn: Int)
    fun getToken(): String?
    fun isTokenValid(): Boolean
    fun clearToken()
    fun getUserId(): Int?
    fun saveUserId(userId: Int)
    fun clearUserId()
    fun getUsername(): String?
    fun saveUsername(username: String)
}

class EncryptedTokenStorage(context: Context) : TokenStorage {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedSharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "app_secure_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    override fun saveToken(token: String, expiresIn: Int) {
        encryptedSharedPreferences.edit().apply {
            putString("access_token", token)
            putLong("token_created_at", System.currentTimeMillis())
            putInt("expires_in", expiresIn)
            apply()
        }
    }
    
    override fun getToken(): String? {
        return encryptedSharedPreferences.getString("access_token", null)
    }
    
    override fun isTokenValid(): Boolean {
        val token = getToken() ?: return false
        val createdAt = encryptedSharedPreferences.getLong("token_created_at", 0L)
        val expiresIn = encryptedSharedPreferences.getInt("expires_in", 0)
        
        if (createdAt == 0L || expiresIn == 0) return false
        
        val expirationTime = createdAt + (expiresIn * 1000L)
        return System.currentTimeMillis() < expirationTime
    }
    
    override fun clearToken() {
        encryptedSharedPreferences.edit().apply {
            remove("access_token")
            remove("token_created_at")
            remove("expires_in")
            remove("user_id")
            remove("username")
            apply()
        }
    }
    
    override fun getUserId(): Int? {
        val userId = encryptedSharedPreferences.getInt("user_id", -1)
        return if (userId == -1) null else userId
    }
    
    override fun saveUserId(userId: Int) {
        encryptedSharedPreferences.edit().putInt("user_id", userId).apply()
    }
    
    override fun clearUserId() {
        encryptedSharedPreferences.edit().remove("user_id").apply()
    }
    
    override fun getUsername(): String? {
        return encryptedSharedPreferences.getString("username", null)
    }
    
    override fun saveUsername(username: String) {
        encryptedSharedPreferences.edit().putString("username", username).apply()
    }
}
