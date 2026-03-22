package com.lanerush.ui.screens.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanerush.data.settings.SettingsRepository
import com.lanerush.data.work.ScoreSubmitRequest
import com.lanerush.domain.model.Difficulty
import com.lanerush.domain.model.Score
import com.lanerush.domain.repository.AuthRepository
import com.lanerush.engine.GameEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GameViewModel(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val appContext: Context
) : ViewModel() {

    val engine    = GameEngine(viewModelScope)
    val gameState = engine.gameState

    private val _selectedLevel      = MutableStateFlow(1)
    private val _selectedDifficulty = MutableStateFlow(Difficulty.EASY)
    val selectedLevel      = _selectedLevel.asStateFlow()
    val selectedDifficulty = _selectedDifficulty.asStateFlow()

    private val _soundEvents = MutableSharedFlow<SoundEvent>()
    val soundEvents = _soundEvents.asSharedFlow()

    init {
        gameState.onEach { state ->
            if (state.isGameOver) {
                if (state.isVictory) {
                    _soundEvents.emit(SoundEvent.VICTORY)
                    settingsRepository.updateMaxUnlockedLevel(state.level + 1)
                } else {
                    _soundEvents.emit(SoundEvent.CRASH)
                }
                submitFinalScore(
                    distance = state.distanceTravelled.toInt(),
                    topSpeed = state.peakSpeed,
                    avgSpeed = state.avgSpeed
                )
            }
        }.launchIn(viewModelScope)
    }

    fun setLevel(level: Int)            { _selectedLevel.value = level }
    fun setDifficulty(diff: Difficulty) { _selectedDifficulty.value = diff }

    fun nextLevel() {
        if (_selectedLevel.value < com.lanerush.domain.model.Levels.all.size) {
            _selectedLevel.value += 1
            startGame()
        }
    }

    fun startGame() {
        viewModelScope.launch {
            val currentSettings = settingsRepository.userSettingsFlow.first()
            engine.startGame(
                level             = _selectedLevel.value,
                difficulty        = _selectedDifficulty.value,
                targetFps         = currentSettings.targetFps,
                slipstreamEnabled = currentSettings.isSlipstreamEnabled
            )
        }
    }

    fun togglePause() = engine.togglePause()
    fun pauseGame()   = engine.pause()

    fun throttleOn() {
        viewModelScope.launch { _soundEvents.emit(SoundEvent.THROTTLE) }
        engine.setThrottle(true)
    }
    fun throttleOff() = engine.setThrottle(false)

    fun onSwipe(direction: GameEngine.SwipeDirection) = engine.onSwipe(direction)
    fun onTap(lane: Int)                              = engine.onTap(lane)

    private fun submitFinalScore(distance: Int, topSpeed: Float, avgSpeed: Float) {
        val user = authRepository.getCurrentUser() ?: return
        val score = Score(
            uid                = user.uid,
            displayName        = user.displayName,
            photoUrl           = user.photoUrl,
            score              = distance,
            topSpeedReached    = topSpeed,
            avgSpeedDuringRace = avgSpeed
        )
        ScoreSubmitRequest.enqueue(appContext, score)
    }

    enum class SoundEvent { THROTTLE, CRASH, VICTORY }
}