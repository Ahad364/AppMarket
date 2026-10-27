package com.appmarket.store.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers the chosen language on this device: "bn" (default) or "en". */
@Singleton
class LanguagePrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _lang = MutableStateFlow(prefs.getString(KEY_LANG, "bn") ?: "bn")
    val lang: StateFlow<String> = _lang.asStateFlow()

    fun setLang(value: String) {
        prefs.edit().putString(KEY_LANG, value).apply()
        _lang.value = value
    }

    private companion object {
        const val KEY_LANG = "app_language"
    }
}
