package com.lanerush.domain.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val highScore: Int = 0,
    val topSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val totalDistance: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

data class Score(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val score: Int = 0,
    val topSpeedReached: Float = 0f,
    val avgSpeedDuringRace: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LeaderboardCategory {
    DISTANCE, TOP_SPEED, AVG_SPEED
}

enum class AppTheme { SYSTEM, LIGHT, DARK }
enum class SpeedUnit { KMH, MPH }

data class UserSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val isSoundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val swipeSensitivity: Float = 0.5f,
    val isSlipstreamEnabled: Boolean = true,
    val targetFps: Int = 60,
    val showFps: Boolean = false,
    val maxUnlockedLevel: Int = 1
)

data class GameEntity(
    val id: Int,
    val lane: Int,
    val y: Float,
    val type: EntityType
)

enum class EntityType {
    PLAYER, AI, OBSTACLE
}
