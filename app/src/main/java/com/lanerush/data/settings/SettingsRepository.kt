package com.lanerush.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lanerush.domain.model.AppTheme
import com.lanerush.domain.model.SpeedUnit
import com.lanerush.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val SWIPE_SENSITIVITY = floatPreferencesKey("swipe_sensitivity")
        val SLIPSTREAM_ENABLED = booleanPreferencesKey("slipstream_enabled")
        val TARGET_FPS = intPreferencesKey("target_fps")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val MAX_UNLOCKED_LEVEL = intPreferencesKey("max_unlocked_level")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            theme = AppTheme.valueOf(preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name),
            speedUnit = SpeedUnit.valueOf(preferences[PreferencesKeys.SPEED_UNIT] ?: SpeedUnit.KMH.name),
            isSoundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true,
            soundVolume = preferences[PreferencesKeys.SOUND_VOLUME] ?: 0.7f,
            swipeSensitivity = preferences[PreferencesKeys.SWIPE_SENSITIVITY] ?: 0.5f,
            isSlipstreamEnabled = preferences[PreferencesKeys.SLIPSTREAM_ENABLED] ?: true,
            targetFps = preferences[PreferencesKeys.TARGET_FPS] ?: 60,
            showFps = preferences[PreferencesKeys.SHOW_FPS] ?: false,
            maxUnlockedLevel = preferences[PreferencesKeys.MAX_UNLOCKED_LEVEL] ?: 1
        )
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { it[PreferencesKeys.THEME] = theme.name }
    }

    suspend fun updateSpeedUnit(unit: SpeedUnit) {
        context.dataStore.edit { it[PreferencesKeys.SPEED_UNIT] = unit.name }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SOUND_ENABLED] = enabled }
    }

    suspend fun updateSoundVolume(volume: Float) {
        context.dataStore.edit { it[PreferencesKeys.SOUND_VOLUME] = volume }
    }

    suspend fun updateSwipeSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[PreferencesKeys.SWIPE_SENSITIVITY] = sensitivity }
    }

    suspend fun updateSlipstreamEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SLIPSTREAM_ENABLED] = enabled }
    }

    suspend fun updateTargetFps(fps: Int) {
        context.dataStore.edit { it[PreferencesKeys.TARGET_FPS] = fps }
    }

    suspend fun updateShowFps(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_FPS] = show }
    }

    suspend fun updateMaxUnlockedLevel(level: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.MAX_UNLOCKED_LEVEL] ?: 1
            if (level > current) {
                preferences[PreferencesKeys.MAX_UNLOCKED_LEVEL] = level
            }
        }
    }
}
