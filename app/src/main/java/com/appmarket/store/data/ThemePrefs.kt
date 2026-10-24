package com.appmarket.store.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers the dark-mode choice on this device. */
@Singleton
class ThemePrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _isDark = MutableStateFlow(prefs.getBoolean(KEY_DARK, false))
    val isDark: StateFlow<Boolean> = _isDark.asStateFlow()

    fun setDark(value: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, value).apply()
        _isDark.value = value
    }

    private companion object {
        const val KEY_DARK = "dark_mode"
    }
}
