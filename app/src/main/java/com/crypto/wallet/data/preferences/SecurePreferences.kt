package com.crypto.wallet.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SecurePreferences @Inject constructor(
    private val context: Context
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Save auth token securely
    fun saveAuthToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    // Get auth token
    fun getAuthToken(): String? {
        return encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
    }

    // Clear auth token
    fun clearAuthToken() {
        encryptedPrefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .apply()
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return getAuthToken() != null
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}