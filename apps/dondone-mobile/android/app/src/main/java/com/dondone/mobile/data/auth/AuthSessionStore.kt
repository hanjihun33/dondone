package com.dondone.mobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val AUTH_PREFS_NAME = "dondone.auth.session"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_TOKEN_TYPE = "token_type"
private const val KEY_EXPIRES_AT = "expires_at_epoch_millis"
private const val KEY_USER_ID = "user_id"
private const val KEY_EMAIL = "email"
private const val KEY_NAME = "name"

class AuthSessionStore(
    context: Context
) {
    private val preferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        AUTH_PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun read(): AuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val tokenType = preferences.getString(KEY_TOKEN_TYPE, null) ?: return null
        val expiresAtEpochMillis = preferences.getLong(KEY_EXPIRES_AT, -1L)
        val userId = preferences.getLong(KEY_USER_ID, -1L)
        val email = preferences.getString(KEY_EMAIL, null) ?: return null
        val name = preferences.getString(KEY_NAME, null) ?: return null
        if (expiresAtEpochMillis <= 0L || userId <= 0L) {
            return null
        }
        return AuthSession(
            accessToken = accessToken,
            tokenType = tokenType,
            expiresAtEpochMillis = expiresAtEpochMillis,
            userId = userId,
            email = email,
            name = name
        )
    }

    fun save(session: AuthSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_TOKEN_TYPE, session.tokenType)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochMillis)
            .putLong(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_NAME, session.name)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
