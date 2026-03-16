package com.lanerush.engine

import com.lanerush.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

/**
 * Throttle / brake physics:
 * • Finger held down  → throttle ON  → speed climbs toward maxSpeed
 * • Finger released   → throttle OFF → speed falls toward minSpeed (coasting/braking)
 * • Lane changes (swipe or tap) work independently of throttle state but apply a slight speed penalty.
 * • Slipstreaming behind rivals temporarily increases max speed and acceleration.
 */
class GameEngine(private val scope: CoroutineScope) {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    @Volatile private var throttleOn: Boolean = false
    private var gameJob: Job? = null

    fun startGame(level: Int = 1, difficulty: Difficulty = Difficulty.MEDIUM) {
        val cfg = Levels.get(level)
        throttleOn = false
        _gameState.value = GameState(
            rivals            = spawnRivals(),
            obstacles         = spawnInitialObstacles(cfg),
            level             = level,
            difficulty        = difficulty,
            currentSpeed      = GameConstants.INITIAL_SPEED,
            distanceTravelled = 0f,
            peakSpeed         = 0f,
            avgSpeed          = 0f,
            throttleOn        = false
        )
        gameJob?.cancel()
        gameJob = scope.launch {
            while (isActive && !_gameState.value.isGameOver) {
                if (!_gameState.value.isPaused) updateGame()
                delay(GameConstants.TICK_RATE_MS)
            }
        }
    }

    fun togglePause() = _gameState.update { it.copy(isPaused = !it.isPaused) }

    fun pause() {
        if (!_gameState.value.isPaused) {
            _gameState.update { it.copy(isPaused = true) }
        }
    }

    fun setThrottle(on: Boolean) {
        throttleOn = on
        _gameState.update { it.copy(throttleOn = on) }
    }

    fun onSwipe(direction: SwipeDirection) {
        val state = _gameState.value
        if (state.isGameOver || state.isPaused) return
        val lane = state.player.lane
        val newLane = when (direction) {
            SwipeDirection.LEFT  -> (lane - 1).coerceAtLeast(0)
            SwipeDirection.RIGHT -> (lane + 1).coerceAtMost(GameConstants.LANES - 1)
        }

        if (lane != newLane) {
            // Speed Scrubbing: Lose 4% of speed when swerving, bounded by minSpeed
            val minSpd = state.difficulty.minSpeed
            val scrubbedSpeed = (state.currentSpeed * 0.96f).coerceAtLeast(minSpd)
            _gameState.update { it.copy(player = it.player.copy(lane = newLane), currentSpeed = scrubbedSpeed) }
        }
    }

    fun onTap(lane: Int) {
        val state = _gameState.value
        if (state.isGameOver || state.isPaused) return
        if (lane in 0 until GameConstants.LANES && lane != state.player.lane) {
            // Speed Scrubbing on tap as well
            val minSpd = state.difficulty.minSpeed
            val scrubbedSpeed = (state.currentSpeed * 0.96f).coerceAtLeast(minSpd)
            _gameState.update { it.copy(player = it.player.copy(lane = lane), currentSpeed = scrubbedSpeed) }
        }
    }

    private fun updateGame() {
        val state = _gameState.value
        if (state.isGameOver) return
        val diff = state.difficulty
        val cfg  = Levels.get(state.level)
        val throttle = throttleOn

        // --- PHYSICS IMPROVEMENTS START ---

        // 1. Slipstreaming (Drafting) Check
        // If a rival is in the same lane and within 20 units ahead, engage draft
        val isDrafting = state.rivals.any {
            it.lane == state.player.lane && it.y > state.player.y && (it.y - state.player.y) < 20f
        }

        // Boost top speed by 15% if drafting
        val currentMaxSpeed = if (isDrafting) diff.maxSpeed * 1.15f else diff.maxSpeed

        val newSpeed = if (throttle) {
            // 2. Non-Linear Acceleration (Drag)
            val speedRatio = state.currentSpeed / currentMaxSpeed
            // As speed approaches max, acceleration curve flattens. Coerce to at least 0.05 so it doesn't completely stall out.
            val dragFactor = (1f - speedRatio).coerceAtLeast(0.05f)
            val dynamicAccel = diff.accelerationRate * dragFactor

            // Give a slight extra kick to acceleration if slipstreaming
            val finalAccel = if (isDrafting) dynamicAccel * 1.3f else dynamicAccel

            (state.currentSpeed + finalAccel).coerceAtMost(currentMaxSpeed)
        } else {
            (state.currentSpeed - diff.brakeRate).coerceAtLeast(diff.minSpeed)
        }

        // --- PHYSICS IMPROVEMENTS END ---

        val newTicks    = state.ticks + 1
        val newDistance = state.distanceTravelled + newSpeed
        val newPeak     = maxOf(state.peakSpeed, newSpeed)
        val newAvg      = if (newTicks > 0) newDistance / newTicks else 0f

        val filtered = state.obstacles.filter { it.y > newDistance - 10f }
        val updatedObstacles = filtered + if (Random.nextFloat() < diff.obstacleDensity) listOf(
            GameEntity(Random.nextInt(), Random.nextInt(GameConstants.LANES),
                newDistance + 45f + Random.nextFloat() * 25f, EntityType.OBSTACLE)
        ) else emptyList()

        val player = state.player.copy(y = newDistance)
        val dangerY = 3.5f
        val survivors = mutableListOf<GameEntity>()

        for (rival in state.rivals) {
            val pace = (0.18f + (rival.id - 1) * 0.06f) * diff.rivalSpeedMultiplier
            val needsSteer = (rival.lane == player.lane && player.y > rival.y && (player.y - rival.y) < dangerY) ||
                    updatedObstacles.any { obs -> obs.lane == rival.lane && obs.y > rival.y && (obs.y - rival.y) < dangerY }

            val newLane = if (needsSteer) {
                val candidates = (0 until GameConstants.LANES).filter { cl ->
                    cl != rival.lane &&
                            !(cl == player.lane && kotlin.math.abs(player.y - rival.y) < dangerY) &&
                            updatedObstacles.none { obs -> obs.lane == cl && obs.y > rival.y && (obs.y - rival.y) < dangerY }
                }
                val adj = candidates.filter { kotlin.math.abs(it - rival.lane) == 1 }
                when {
                    adj.isNotEmpty()        -> adj.random()
                    candidates.isNotEmpty() -> candidates.random()
                    else                    -> rival.lane
                }
            } else rival.lane

            val moved = rival.copy(lane = newLane, y = rival.y + pace)
            if (!checkCollision(moved, player) && updatedObstacles.none { checkCollision(moved, it) })
                survivors.add(moved)
        }

        val crashed   = updatedObstacles.any { checkCollision(player, it) }
        val rank      = survivors.count { it.y > player.y } + 1
        val finished  = newDistance >= cfg.maxDistance
        val isVictory = finished && rank == 1
        val gameOver  = crashed || finished

        _gameState.update {
            it.copy(
                player = player, rivals = survivors, obstacles = updatedObstacles,
                distanceTravelled = newDistance, currentSpeed = newSpeed,
                peakSpeed = newPeak, avgSpeed = newAvg, rank = rank,
                ticks = newTicks, throttleOn = throttle,
                isGameOver = gameOver, isVictory = isVictory,
                message = when {
                    crashed -> "Crashed!"
                    finished && rank == 1 -> "Victory! Rank 1!"
                    finished -> "Rank $rank - Lose!"
                    else -> ""
                }
            )
        }
    }

    private fun checkCollision(a: GameEntity, b: GameEntity) =
        a.lane == b.lane && kotlin.math.abs(a.y - b.y) < 1.5f

    private fun spawnRivals() = List(5) { i ->
        GameEntity(i + 1, i % GameConstants.LANES, -(i * 12f), EntityType.AI)
    }

    private fun spawnInitialObstacles(cfg: LevelConfig) = List(3 + cfg.extraObstacles) { i ->
        GameEntity(Random.nextInt(), i % GameConstants.LANES, 60f + i * 35f, EntityType.OBSTACLE)
    }

    enum class SwipeDirection { LEFT, RIGHT }
}