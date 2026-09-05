package com.flashlight.toolbox.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode(val storedName: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.storedName == name } ?: SYSTEM
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val strobeIntervalMs: Int = 120,
    val themeColor: Int = 0xFFB26A00.toInt(), // 默认橘黄色
)

class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_STROBE_MS = intPreferencesKey("strobe_interval_ms")
        private val KEY_THEME_COLOR = intPreferencesKey("theme_color")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.fromName(prefs[KEY_THEME]),
            strobeIntervalMs = prefs[KEY_STROBE_MS] ?: 120,
            themeColor = prefs[KEY_THEME_COLOR] ?: 0xFFB26A00.toInt(),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.storedName }
    }

    suspend fun setStrobeInterval(ms: Int) {
        context.dataStore.edit { it[KEY_STROBE_MS] = ms }
    }

    suspend fun setThemeColor(color: Int) {
        context.dataStore.edit { it[KEY_THEME_COLOR] = color }
    }
}