package com.lanerush.domain.model

// ── Difficulty ────────────────────────────────────────────────────────────
enum class Difficulty {
    EASY, MEDIUM, HARD;

    val label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }

    // Speed the car gains per tick (60fps) when throttle is held
    val accelerationRate: Float get() = when (this) {
        EASY   -> 0.0008f   // gentle ramp
        MEDIUM -> 0.0014f   // standard
        HARD   -> 0.0022f   // punchy
    }

    // Speed lost per tick when throttle is released (coasting / braking)
    val brakeRate: Float get() = when (this) {
        EASY   -> 0.0004f   // very forgiving coast
        MEDIUM -> 0.0010f   // satisfying brake
        HARD   -> 0.0018f   // aggressive — must hold throttle to maintain speed
    }

    // Floor: car never stops completely even when braking
    val minSpeed: Float get() = when (this) {
        EASY   -> 0.10f
        MEDIUM -> 0.06f
        HARD   -> 0.02f
    }

    // Ceiling
    val maxSpeed: Float get() = when (this) {
        EASY   -> 1.4f
        MEDIUM -> 2.0f
        HARD   -> 2.8f
    }

    // Obstacle spawn probability per tick
    val obstacleDensity: Float get() = when (this) {
        EASY   -> 0.010f
        MEDIUM -> 0.018f
        HARD   -> 0.030f
    }

    // Rival speed multiplier
    val rivalSpeedMultiplier: Float get() = when (this) {
        EASY   -> 0.70f
        MEDIUM -> 1.00f
        HARD   -> 1.45f
    }
}

// ── Per-level configuration ───────────────────────────────────────────────
data class LevelConfig(
    val level: Int,
    val maxDistance: Float,
    val extraObstacles: Int,
    val description: String
)

object Levels {
    val all: List<LevelConfig> = listOf(
        LevelConfig(1,  1000f, 0, "Short sprint"),
        LevelConfig(2,  1500f, 0, "City streets"),
        LevelConfig(3,  2000f, 1, "Night circuit"),
        LevelConfig(4,  2500f, 1, "Desert highway"),
        LevelConfig(5,  3000f, 2, "Mountain pass"),
        LevelConfig(6,  3500f, 2, "Storm corridor"),
        LevelConfig(7,  4000f, 3, "Neon avenue"),
        LevelConfig(8,  4500f, 3, "Ice crossing"),
        LevelConfig(9,  4800f, 4, "Death valley"),
        LevelConfig(10, 5000f, 5, "Grand finale")
    )
    fun get(level: Int): LevelConfig = all.getOrElse(level - 1) { all.last() }
}

// ── Constants ─────────────────────────────────────────────────────────────
object GameConstants {
    const val LANES         = 3
    const val TICK_RATE_MS  = 16L
    const val INITIAL_SPEED = 0.0f   // always starts at rest
}

// ── State ─────────────────────────────────────────────────────────────────
data class GameState(
    val player: GameEntity          = GameEntity(0, 1, 0f, EntityType.PLAYER),
    val rivals: List<GameEntity>    = emptyList(),
    val obstacles: List<GameEntity> = emptyList(),
    val currentSpeed: Float         = GameConstants.INITIAL_SPEED,
    val peakSpeed: Float            = 0f,
    val avgSpeed: Float             = 0f,
    val distanceTravelled: Float    = 0f,
    val rank: Int                   = 1,
    val ticks: Long                 = 0,
    val isGameOver: Boolean         = false,
    val isPaused: Boolean           = false,
    val isVictory: Boolean          = false,
    val message: String             = "",
    val level: Int                  = 1,
    val difficulty: Difficulty      = Difficulty.MEDIUM,
    // Whether the player's finger is currently held down (throttle)
    val throttleOn: Boolean         = false
)