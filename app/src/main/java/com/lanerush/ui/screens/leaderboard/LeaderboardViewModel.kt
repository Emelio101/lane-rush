package com.lanerush.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanerush.domain.model.LeaderboardCategory
import com.lanerush.domain.model.Score
import com.lanerush.domain.repository.AuthRepository
import com.lanerush.domain.repository.LeaderboardRepository
import com.lanerush.data.leaderboard.LeaderboardRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val authRepository: AuthRepository,
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {

    private val _scores = MutableStateFlow<List<Score>>(emptyList())
    val scores = _scores.asStateFlow()

    private val _currentUserScore = MutableStateFlow<Score?>(null)
    val currentUserScore = _currentUserScore.asStateFlow()

    private val _userRank = MutableStateFlow<Int?>(null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _selectedCategory = MutableStateFlow(LeaderboardCategory.DISTANCE)
    val selectedCategory = _selectedCategory.asStateFlow()

    fun fetchTopScores(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            else _isLoading.value = true
            
            val category = _selectedCategory.value
            val repo = leaderboardRepository as? LeaderboardRepositoryImpl
            
            if (repo != null) {
                val top100 = repo.getTopScoresByCategory(category.name, 100)
                _scores.value = top100
                
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val index = top100.indexOfFirst { it.uid == currentUser.uid }
                    if (index != -1) {
                        _userRank.value = index + 1
                        _currentUserScore.value = null
                    } else {
                        repo.getUserData(currentUser.uid).onSuccess { user ->
                            if (user != null) {
                                _currentUserScore.value = Score(
                                    uid = user.uid,
                                    displayName = user.displayName,
                                    photoUrl = user.photoUrl,
                                    score = user.highScore,
                                    topSpeedReached = user.topSpeed,
                                    avgSpeedDuringRace = user.avgSpeed
                                )
                                _userRank.value = null
                            }
                        }
                    }
                }
            }
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    fun selectCategory(category: LeaderboardCategory) {
        _selectedCategory.value = category
        fetchTopScores()
    }

    fun getCurrentUserId(): String? = authRepository.getCurrentUser()?.uid
}
