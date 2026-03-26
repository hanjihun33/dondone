package com.dondone.mobile.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.dondone.mobile.core.i18n.AppLanguage

private const val APP_LANGUAGE_PREFS_NAME = "dondone.app.language"
private const val KEY_APP_LANGUAGE = "selected_language"

interface AppLanguageStore {
    fun read(): AppLanguage
    fun save(language: AppLanguage)
}

class SharedPreferencesAppLanguageStore(
    context: Context
) : AppLanguageStore {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(APP_LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): AppLanguage =
        AppLanguage.fromCode(preferences.getString(KEY_APP_LANGUAGE, AppLanguage.KOREAN.code))

    override fun save(language: AppLanguage) {
        preferences.edit()
            .putString(KEY_APP_LANGUAGE, language.code)
            .apply()
    }
}

class InMemoryAppLanguageStore(
    initialLanguage: AppLanguage = AppLanguage.KOREAN
) : AppLanguageStore {
    private var currentLanguage: AppLanguage = initialLanguage

    override fun read(): AppLanguage = currentLanguage

    override fun save(language: AppLanguage) {
        currentLanguage = language
    }
}
