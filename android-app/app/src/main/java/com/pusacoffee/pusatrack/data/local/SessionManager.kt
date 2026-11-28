package com.pusacoffee.pusatrack.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pusacoffee.pusatrack.data.model.User

class SessionManager(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        createEncryptedPrefs() ?: appContext.getSharedPreferences(
            FALLBACK_PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    private fun createEncryptedPrefs(): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            Log.w(TAG, "EncryptedSharedPreferences unavailable, using fallback storage.", t)
            null
        }
    }

    fun saveSession(user: User) {
        prefs.edit()
            .putInt(KEY_USER_ID, user.userId)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_FULL_NAME, user.fullName)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) &&
            !prefs.getString(KEY_USERNAME, null).isNullOrBlank()
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)

    fun getUser(): User? {
        if (!isLoggedIn()) return null
        val userId = getUserId()
        val username = getUsername() ?: return null
        val fullName = getFullName() ?: username
        return User(
            userId = userId,
            username = username,
            fullName = fullName,
            isActive = true
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val TAG = "SessionManager"
        private const val ENCRYPTED_PREFS_NAME = "pusatrack_secure_session"
        private const val FALLBACK_PREFS_NAME = "pusatrack_session_fallback"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
}
