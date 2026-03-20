package com.lanerush.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanerush.domain.model.Difficulty
import com.lanerush.domain.model.Score
import com.lanerush.domain.repository.AuthRepository
import com.lanerush.domain.repository.LeaderboardRepository
import com.lanerush.engine.GameEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GameViewModel(
    private val authRepository: AuthRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val settingsRepository: com.lanerush.data.settings.SettingsRepository
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
                    // Unlock next level if currently winning current max level
                    settingsRepository.updateMaxUnlockedLevel(state.level + 1)
                } else {
                    _soundEvents.emit(SoundEvent.CRASH)
                }
                submitFinalScore(state.distanceTravelled.toInt(), state.peakSpeed, state.avgSpeed)
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

    fun startGame(targetFps: Int = 60) {
        engine.startGame(
            level      = _selectedLevel.value,
            difficulty = _selectedDifficulty.value,
            targetFps  = targetFps
        )
    }

    fun togglePause() = engine.togglePause()

    fun pauseGame() = engine.pause()

    /** Throttle pressed (finger down) */
    fun throttleOn()  {
        viewModelScope.launch { _soundEvents.emit(SoundEvent.THROTTLE) }
        engine.setThrottle(true)
    }

    /** Throttle released (finger up) */
    fun throttleOff() = engine.setThrottle(false)

    fun onSwipe(direction: GameEngine.SwipeDirection) {
        engine.onSwipe(direction)
    }

    fun onTap(lane: Int) {
        engine.onTap(lane)
    }

    private fun submitFinalScore(distance: Int, topSpeed: Float, avgSpeed: Float) {
        val user = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            leaderboardRepository.submitScore(
                Score(
                    uid                = user.uid,
                    displayName        = user.displayName,
                    photoUrl           = user.photoUrl,
                    score              = distance,
                    topSpeedReached    = topSpeed,
                    avgSpeedDuringRace = avgSpeed
                )
            )
        }
    }

    enum class SoundEvent { THROTTLE, CRASH, VICTORY }
}