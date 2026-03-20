package com.lanerush.engine

import android.annotation.SuppressLint
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
    private var currentTargetFps: Int = 60

    fun startGame(level: Int = 1, difficulty: Difficulty = Difficulty.MEDIUM, targetFps: Int = 60) {
        val cfg = Levels.get(level)
        throttleOn = false
        currentTargetFps = targetFps
        val tickRate = (1000L / targetFps).coerceAtLeast(1L)

        _gameState.value = GameState(
            rivals            = spawnRivals(),
            obstacles         = spawnInitialObstacles(cfg),
            level             = level,
            difficulty        = difficulty,
            currentSpeed      = GameConstants.INITIAL_SPEED,
            distanceTravelled = 0f,
            peakSpeed         = 0f,
            avgSpeed          = 0f,
            throttleOn        = false,
            isStarting        = true,
            startLights       = 0
        )
        gameJob?.cancel()
        gameJob = scope.launch {
            while (isActive && !_gameState.value.isGameOver) {
                if (!_gameState.value.isPaused) updateGame()
                delay(tickRate)
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
            // Check if any rival is side-by-side in the target lane
            val isBlocked = state.rivals.any { it.lane == newLane && kotlin.math.abs(it.y - state.player.y) < 1.8f }
            if (isBlocked) return // Lane change blocked by rival

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
            // Check if any rival is side-by-side in the target lane
            val isBlocked = state.rivals.any { it.lane == lane && kotlin.math.abs(it.y - state.player.y) < 1.8f }
            if (isBlocked) return // Lane change blocked by rival

            // Speed Scrubbing on tap as well
            val minSpd = state.difficulty.minSpeed
            val scrubbedSpeed = (state.currentSpeed * 0.96f).coerceAtLeast(minSpd)
            _gameState.update { it.copy(player = it.player.copy(lane = lane), currentSpeed = scrubbedSpeed) }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateGame() {
        val state = _gameState.value
        if (state.isGameOver) return

        val fpsScale = 60f / currentTargetFps

        // ── F1 Starting Sequence ─────────────────────────────────────────
        if (state.isStarting) {
            val startTicks = state.ticks + 1
            val currentLights = state.startLights
            
            // Ultra-Fast F1: 1 light per ~0.25 second
            val interval = (currentTargetFps * 0.25f).toLong().coerceAtLeast(1L)
            val newLights = if (currentLights in 0..4) {
                if (startTicks % interval == 0L) currentLights + 1 else currentLights
            } else currentLights

            // Once 5 lights are on, wait a very short interval to "go"
            if (newLights == 5) {
                // 5 lights hit at 5 * interval. Go after a short delay (approx 0.4s)
                val goTick = 5 * interval + (currentTargetFps * 0.4f).toLong()
                if (startTicks >= goTick) {
                    _gameState.update { it.copy(isStarting = false, startLights = -1, ticks = 0) }
                    return
                }
            }

            _gameState.update { it.copy(startLights = newLights, ticks = startTicks) }
            return
        }
        
        val diff = state.difficulty
        val cfg  = Levels.get(state.level)
        val throttle = throttleOn

        // 1. Slipstreaming (Drafting) Check
        val isDrafting = state.rivals.any {
            it.lane == state.player.lane && it.y > state.player.y && (it.y - state.player.y) < 12f
        }

        // Boost top speed by 15% if drafting (and enabled)
        val currentMaxSpeed = if (isDrafting && state.rivals.isNotEmpty()) {
            diff.maxSpeed * 1.15f 
        } else diff.maxSpeed

        var newSpeed = if (throttle) {
            val speedRatio = state.currentSpeed / currentMaxSpeed
            val dragFactor = (1f - speedRatio).coerceAtLeast(0.05f)
            val dynamicAccel = diff.accelerationRate * dragFactor * fpsScale
            val finalAccel = if (isDrafting) dynamicAccel * 1.3f else dynamicAccel
            (state.currentSpeed + finalAccel).coerceAtMost(currentMaxSpeed)
        } else {
            (state.currentSpeed - diff.brakeRate * fpsScale).coerceAtLeast(diff.minSpeed)
        }

        // ── Rival Blocking Logic ─────────────────────────────────────────
        val rivalInFront = state.rivals.firstOrNull { 
            it.lane == state.player.lane && it.y > state.player.y && (it.y - state.player.y) < 2.0f 
        }
        if (rivalInFront != null) {
            val rivalPace = (0.18f + (rivalInFront.id - 1) * 0.06f) * diff.rivalSpeedMultiplier
            if (newSpeed > rivalPace) {
                newSpeed = rivalPace // Blocked by rival pace
            }
        }

        val newTicks    = state.ticks + 1
        val newDistance = state.distanceTravelled + newSpeed * fpsScale
        val newPeak     = maxOf(state.peakSpeed, newSpeed)
        val newAvg      = if (newTicks > 0) newDistance / newTicks else 0f

        val filteredObstacles = state.obstacles.filter { it.y > newDistance - 10f }
        val updatedObstacles = filteredObstacles + if (Random.nextFloat() < diff.obstacleDensity * fpsScale) listOf(
            GameEntity(Random.nextInt(), Random.nextInt(GameConstants.LANES),
                newDistance + 45f + Random.nextFloat() * 25f, EntityType.OBSTACLE)
        ) else emptyList()

        val player = state.player.copy(y = newDistance)
        val dangerY = 3.5f
        val survivors = mutableListOf<GameEntity>()

        // Process rivals from front to back to handle avoidance correctly
        val sortedInitialRivals = state.rivals.sortedByDescending { it.y }

        for (rival in sortedInitialRivals) {
            val basePace = (0.18f + (rival.id - 1) * 0.06f) * diff.rivalSpeedMultiplier * fpsScale
            
            // AI considers the player, obstacles, and already processed (ahead) rivals as barriers
            val obstaclesAhead = updatedObstacles + listOf(player) + survivors

            val blockingEntity = obstaclesAhead.firstOrNull { 
                it.lane == rival.lane && it.y > rival.y && (it.y - rival.y) < dangerY 
            }
            val needsSteer = blockingEntity != null

            val newLane = if (needsSteer) {
                val candidates = (0 until GameConstants.LANES).filter { cl ->
                    cl != rival.lane &&
                            obstaclesAhead.none { other -> other.lane == cl && other.y > rival.y && (other.y - rival.y) < dangerY }
                }
                val adj = candidates.filter { kotlin.math.abs(it - rival.lane) == 1 }
                when {
                    adj.isNotEmpty()        -> adj.random()
                    candidates.isNotEmpty() -> candidates.random()
                    else                    -> rival.lane
                }
            } else rival.lane

            // If blocked and cannot steer, match speed with the car in front (roughly)
            val finalPace = if (newLane == rival.lane && needsSteer) {
                minOf(basePace, 0.15f * diff.rivalSpeedMultiplier * fpsScale)
            } else basePace

            val movedRival = rival.copy(lane = newLane, y = rival.y + finalPace)
            
            val aiCrashed = updatedObstacles.any { checkCollision(movedRival, it) }
            if (!aiCrashed) {
                survivors.add(movedRival)
            }
        }

        // ── Real-time Interval Gaps ──────────────────────────────────────
        val sortedRivals = survivors.sortedByDescending { it.y }
        val closestAhead = sortedRivals.lastOrNull { it.y > player.y }
        val closestBehind = sortedRivals.firstOrNull { it.y < player.y }

        val gapAhead = closestAhead?.let {
            val dist = it.y - player.y
            val time = if (state.currentSpeed > 0) dist / state.currentSpeed else 0f
            "+${String.format("%.3f", time)}"
        }
        val gapBehind = closestBehind?.let {
            val dist = player.y - it.y
            val time = if (state.currentSpeed > 0) dist / state.currentSpeed else 0f
            "-${String.format("%.3f", time)}"
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
                isDrafting = isDrafting,
                gapAhead = gapAhead,
                gapBehind = gapBehind,
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

    private fun spawnRivals() = listOf(
        // Row 1 (Furthest ahead)
        GameEntity(1, 0, 14f, EntityType.AI),
        GameEntity(2, 2, 14f, EntityType.AI),
        // Row 2
        GameEntity(3, 1, 7f, EntityType.AI),
        // Row 3 (Next to player who is at Lane 1, y=0)
        GameEntity(4, 0, 0f, EntityType.AI),
        GameEntity(5, 2, 0f, EntityType.AI)
    )

    private fun spawnInitialObstacles(cfg: LevelConfig) = List(3 + cfg.extraObstacles) { i ->
        // Start obstacles a bit further out so rivals have room
        GameEntity(Random.nextInt(), i % GameConstants.LANES, 80f + i * 40f, EntityType.OBSTACLE)
    }

    enum class SwipeDirection { LEFT, RIGHT }
}