package com.ftt.aichat.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ftt.aichat.data.DEFAULT_MODEL

/**
 * Secure storage for app settings.
 * API key is stored in EncryptedSharedPreferences.
 * Other settings use regular SharedPreferences.
 */
class PreferencesManager(context: Context) {

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "aichat_secure"
        private const val REGULAR_PREFS_NAME = "aichat_settings"

        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_THEME = "app_theme" // "system", "light", "dark"

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    // Encrypted prefs for sensitive data (API key)
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Regular prefs for non-sensitive settings
    private val regularPrefs: SharedPreferences =
        context.getSharedPreferences(REGULAR_PREFS_NAME, Context.MODE_PRIVATE)

    // ── API Key ──────────────────────────────────────────────────
    var apiKey: String
        get() = encryptedPrefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_API_KEY, value).apply()

    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    // ── Model ────────────────────────────────────────────────────
    var selectedModelId: String
        get() = regularPrefs.getString(KEY_MODEL, DEFAULT_MODEL.id) ?: DEFAULT_MODEL.id
        set(value) = regularPrefs.edit().putString(KEY_MODEL, value).apply()

    // ── System Prompt ────────────────────────────────────────────
    var systemPrompt: String
        get() = regularPrefs.getString(KEY_SYSTEM_PROMPT, "") ?: ""
        set(value) = regularPrefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    // ── Theme ────────────────────────────────────────────────────
    var theme: String
        get() = regularPrefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = regularPrefs.edit().putString(KEY_THEME, value).apply()
}
