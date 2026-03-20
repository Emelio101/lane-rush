package com.lanerush.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanerush.data.settings.SettingsRepository
import com.lanerush.domain.model.AppTheme
import com.lanerush.domain.model.SpeedUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings = repository.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.lanerush.domain.model.UserSettings()
    )

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch { repository.updateTheme(theme) }
    }

    fun updateSpeedUnit(unit: SpeedUnit) {
        viewModelScope.launch { repository.updateSpeedUnit(unit) }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSoundEnabled(enabled) }
    }

    fun updateSoundVolume(volume: Float) {
        viewModelScope.launch { repository.updateSoundVolume(volume) }
    }

    fun updateSwipeSensitivity(sensitivity: Float) {
        viewModelScope.launch { repository.updateSwipeSensitivity(sensitivity) }
    }

    fun updateSlipstreamEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSlipstreamEnabled(enabled) }
    }

    fun updateTargetFps(fps: Int) {
        viewModelScope.launch { repository.updateTargetFps(fps) }
    }

    fun updateShowFps(show: Boolean) {
        viewModelScope.launch { repository.updateShowFps(show) }
    }
}
