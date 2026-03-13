package com.lanerush.domain.repository

import com.lanerush.domain.model.Score
import com.lanerush.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): User?
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User>
    suspend fun signOut()
    fun authStateFlow(): Flow<User?>
}

interface LeaderboardRepository {
    suspend fun submitScore(score: Score): Result<Unit>
    suspend fun getTopScores(limit: Int = 10): Result<List<Score>>
    suspend fun getUserData(uid: String): Result<User?>
    suspend fun createOrUpdateUser(user: User): Result<Unit>
}
