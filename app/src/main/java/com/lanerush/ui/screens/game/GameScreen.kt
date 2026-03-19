package com.lanerush.ui.screens.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanerush.domain.model.*
import com.lanerush.engine.GameEngine
import com.lanerush.engine.SoundManager
import com.lanerush.ui.theme.LaneRushTheme
import kotlin.math.abs

// ═══════════════════════════════════════════════════════════════════════════
//  PALETTE
// ═══════════════════════════════════════════════════════════════════════════
private object C {
    val roadBase       = Color(0xFF080C14)
    val roadSurface    = Color(0xFF0D1220)
    val roadAsphalt    = Color(0xFF10141E)
    val laneGlow       = Color(0xFF00D4FF)
    val laneMark       = Color(0xFFFFFFFF)
    val playerBody     = Color(0xFF00E5FF)
    val playerAccent   = Color(0xFF007A99)
    val playerLight    = Color(0xFFFFFFFF)
    val playerGlow     = Color(0x6600E5FF)
    val rivalBody      = Color(0xFFFF2D55)
    val rivalAccent    = Color(0xFF880018)
    val rivalLight     = Color(0xFFFF8099)
    val rivalGlow      = Color(0x55FF2D55)
    val obstacle       = Color(0xFFFF9500)
    val obstacleStripe = Color(0x99000000)
    val obstacleGlow   = Color(0x55FF9500)
    val hudBg          = Color(0xBB060912)
    val hudBorder      = Color(0xFF00D4FF)
    val green          = Color(0xFF00E676)
    val yellow         = Color(0xFFFFD600)
    val blue           = Color(0xFF40C4FF)
    val white          = Color.White
    val overlayDark    = Color(0xEE030508)
    val rumbleRed      = Color(0xCCCC2222)
    val rumbleWhite    = Color(0xCCDDDDDD)
    val throttleOn     = Color(0xFF00E676)
    val throttleOff    = Color(0xFFFF2D55)
}

private fun Float.toDisplaySpeed(unit: SpeedUnit) =
    if (unit == SpeedUnit.KMH) (this * 200).toInt() else (this * 124).toInt()
private fun SpeedUnit.label() = if (this == SpeedUnit.KMH) "KM/H" else "MPH"

// ═══════════════════════════════════════════════════════════════════════════
//  ENTRY
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    settings: UserSettings = UserSettings(),
    isAppInForeground: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val gameState    by viewModel.gameState.collectAsState()
    val context       = LocalContext.current
    val soundManager  = remember { SoundManager(context) }

    // ── Start the game loop when this screen enters ───────────────
    LaunchedEffect(Unit) { 
        viewModel.startGame() 
    }

    // ── Audio Management & Settings Sync ────────────────────────────
    LaunchedEffect(
        gameState.isPaused,
        gameState.isGameOver,
        isAppInForeground,
        settings.isSoundEnabled,
        settings.soundVolume
    ) {
        // 1. ALWAYS apply the latest volume/mute settings FIRST
        soundManager.updateSettings(settings.isSoundEnabled, settings.soundVolume)

        // 2. THEN decide if the engine should be roaring
        if (isAppInForeground && !gameState.isPaused && !gameState.isGameOver && settings.isSoundEnabled) {
            soundManager.playEngine()
        } else {
            soundManager.stopEngine()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.soundEvents.collect { event ->
            when (event) {
                GameViewModel.SoundEvent.THROTTLE -> { /* SFX for throttle? */ }
                GameViewModel.SoundEvent.CRASH    -> {
                    soundManager.stopEngine()
                    soundManager.playCrash()
                }
                GameViewModel.SoundEvent.VICTORY  -> {
                    soundManager.stopEngine()
                    soundManager.playVictory()
                }
            }
        }
    }

    // ── Engine Auto Pause Management When Minimized (Respects lifecycle and game state) ──
    LaunchedEffect(isAppInForeground) {
        if (!isAppInForeground && !gameState.isPaused && !gameState.isGameOver) {
            viewModel.pauseGame()
        }
    }

    DisposableEffect(Unit) { onDispose { soundManager.release() } }

    GameContent(
        gameState      = gameState,
        settings       = settings,
        onThrottleOn   = { viewModel.throttleOn() },
        onThrottleOff  = { viewModel.throttleOff() },
        onSwipe        = { viewModel.onSwipe(it) },
        onTap          = { viewModel.onTap(it) },
        onTogglePause  = {
            soundManager.playClick()
            viewModel.togglePause()
        },
        onRestart      = {
            soundManager.playClick()
            viewModel.startGame()
        },
        onNextLevel    = {
            soundManager.playClick()
            viewModel.nextLevel()
        },
        onNavigateBack = {
            soundManager.playClick()
            onNavigateBack()
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  GAME CONTENT
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun GameContent(
    gameState: GameState,
    settings: UserSettings,
    onThrottleOn: () -> Unit,
    onThrottleOff: () -> Unit,
    onSwipe: (GameEngine.SwipeDirection) -> Unit,
    onTap: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onNextLevel: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var dragStartX by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val inf = rememberInfiniteTransition(label = "anim")
    val animTick by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), "tick"
    )
    val glowPulse by inf.animateFloat(
        0.55f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), "glow"
    )

    // ── ANIMATED LANE STATE ──────────────────────────────────────────
    // This smoothly interpolates the player's visual lane position
    val animatedPlayerLane by animateFloatAsState(
        targetValue = gameState.player.lane.toFloat(),
        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
        label = "playerLaneAnim"
    )

    // Calculate tilt/steering angle based on how far the visual car is from the target lane
    val laneDiff = gameState.player.lane.toFloat() - animatedPlayerLane
    val playerTiltAngle = laneDiff * 25f // Max 25 degrees of tilt when switching lanes

    val speedFraction = (gameState.currentSpeed / gameState.difficulty.maxSpeed).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize().background(C.roadBase)) {

        // ── Main canvas + interaction ──────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gameState.isGameOver, gameState.isPaused, settings.swipeSensitivity) {
                    if (gameState.isGameOver || gameState.isPaused) return@pointerInput

                    val baseThresholdPx = with(density) { 30.dp.toPx() }
                    val sensitivityRangePx = with(density) { 120.dp.toPx() }

                    awaitPointerEventScope {
                        while (true) {
                            val down   = awaitPointerEvent()
                            val press  = down.changes.firstOrNull() ?: continue
                            if (!press.pressed || press.isConsumed) continue

                            dragStartX = press.position.x
                            onThrottleOn()

                            var totalX = 0f; var totalY = 0f; var moved = false
                            while (true) {
                                val move   = awaitPointerEvent()
                                val change = move.changes.firstOrNull() ?: break
                                if (change.isConsumed) break
                                if (!change.pressed) {
                                    onThrottleOff()
                                    if (!moved) {
                                        val lw   = size.width / GameConstants.LANES.toFloat()
                                        val lane = (dragStartX / lw).toInt().coerceIn(0, GameConstants.LANES - 1)
                                        onTap(lane)
                                    }
                                    break
                                }
                                val d = change.position - change.previousPosition
                                totalX += d.x; totalY += d.y
                                if (abs(totalX) > 8f || abs(totalY) > 8f) moved = true

                                val swipeThreshold = baseThresholdPx + (1.0f - settings.swipeSensitivity) * sensitivityRangePx

                                if (abs(totalX) > swipeThreshold && abs(totalX) > abs(totalY) * 1.5f) {
                                    if (totalX > 0) onSwipe(GameEngine.SwipeDirection.RIGHT)
                                    else            onSwipe(GameEngine.SwipeDirection.LEFT)

                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    totalX = 0f; totalY = 0f
                                }

                                change.consume()
                            }
                        }
                    }
                }
        ) {
            drawScene(gameState, animTick, glowPulse, speedFraction, animatedPlayerLane, playerTiltAngle, settings)
        }
        // ── Speed vignette ──────────────────────────────────────────────
        if (speedFraction > 0.3f) {
            Canvas(Modifier.fillMaxSize()) {
                val vigAlpha = ((speedFraction - 0.3f) / 0.7f) * 0.5f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, C.roadBase.copy(alpha = vigAlpha)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.9f
                    )
                )
            }
        }

        // ── HUD ───────────────────────────────────────────────────────────
        HUDOverlay(
            state         = gameState,
            unit          = settings.speedUnit,
            settings      = settings,
            onTogglePause = onTogglePause
        )

        // ── Throttle bar ──────────────────────────────────────────────────
        if (!gameState.isGameOver && !gameState.isPaused) {
            ThrottleBar(
                speedFraction = speedFraction,
                throttleOn    = gameState.throttleOn,
                modifier      = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }

        // ── Rank dots ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .pointerInput(Unit) { detectTapGestures { } },
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(120.dp))
            for (pos in 1..6) {
                val dotColor = when {
                    pos == gameState.rank -> C.yellow
                    pos < gameState.rank  -> C.green
                    else                  -> C.white.copy(alpha = 0.18f)
                }
                Box(
                    Modifier
                        .size(if (pos == gameState.rank) 11.dp else 7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(dotColor)
                )
            }
        }

        if (gameState.isPaused && !gameState.isGameOver) PausedOverlay(onTogglePause)
        if (gameState.isGameOver) GameOverUI(gameState, settings.speedUnit, onRestart, onNextLevel, onNavigateBack)
    }
}

@Composable
private fun ThrottleBar(
    speedFraction: Float,
    throttleOn: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor by animateColorAsState(
        targetValue = if (throttleOn) C.throttleOn else C.throttleOff,
        animationSpec = tween(120), label = "throttleColor"
    )
    val filledFraction by animateFloatAsState(
        targetValue = speedFraction, animationSpec = tween(80), label = "speedFill"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (throttleOn) "▲ THROTTLE" else "▼ BRAKE",
            color = barColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(140.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.white.copy(alpha = 0.1f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(filledFraction).background(Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.6f), barColor)), RoundedCornerShape(3.dp)))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SCENE DRAWING
// ═══════════════════════════════════════════════════════════════════════════
private fun DrawScope.drawScene(
    state: GameState,
    tick: Float,
    glowPulse: Float,
    speedFraction: Float,
    animatedPlayerLane: Float,
    playerTiltAngle: Float,
    settings: UserSettings
) {
    val laneW    = size.width / GameConstants.LANES
    val viewH    = size.height
    val yScale   = 130f
    val scroll   = (state.distanceTravelled * yScale) % 80.dp.toPx()
    val horizonY = viewH * 0.07f

    drawRect(Brush.verticalGradient(listOf(Color(0xFF040810), Color(0xFF060A16), C.roadAsphalt), 0f, viewH))
    drawRect(Brush.verticalGradient(listOf(C.laneGlow.copy(alpha = 0.05f * glowPulse), Color.Transparent), 0f, horizonY + 100f), size = size)
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0C1018), Color(0xFF101520), C.roadSurface), horizonY, viewH), Offset(0f, horizonY), Size(size.width, viewH - horizonY))

    for (row in 0..14) {
        val ry = horizonY + (row / 14f) * (viewH - horizonY) + (scroll * 0.25f) % ((viewH - horizonY) / 14f)
        drawLine(C.white.copy(alpha = 0.03f), Offset(0f, ry), Offset(size.width, ry), 1f)
    }

    val rumbleCount = 14; val rumbleH = (viewH - horizonY) / rumbleCount; val rumbleW = 16.dp.toPx()
    for (i in 0..rumbleCount) {
        val ry = horizonY + i * rumbleH - (scroll % (rumbleH * 2))
        val col = if (i % 2 == 0) C.rumbleRed else C.rumbleWhite
        drawRect(col, Offset(0f, ry), Size(rumbleW, rumbleH))
        drawRect(col, Offset(size.width - rumbleW, ry), Size(rumbleW, rumbleH))
    }

    val dashLen = 30.dp.toPx(); val gapLen = 26.dp.toPx(); val period = dashLen + gapLen
    for (i in 0..GameConstants.LANES) {
        val x = i * laneW
        if (i == 0 || i == GameConstants.LANES) {
            val wx = if (i == 0) x + rumbleW else x - rumbleW
            drawLine(C.laneGlow.copy(alpha = 0.1f * glowPulse), Offset(wx, horizonY), Offset(wx, viewH), 16.dp.toPx())
            drawLine(C.laneGlow.copy(alpha = 0.75f), Offset(wx, horizonY), Offset(wx, viewH), 2.dp.toPx())
        } else {
            var y = horizonY - (scroll % period)
            while (y < viewH) {
                val yEnd = (y + dashLen).coerceAtMost(viewH)
                if (y >= horizonY) drawLine(C.laneMark.copy(alpha = 0.2f), Offset(x, y), Offset(x, yEnd), 2.dp.toPx())
                y += period
            }
        }
    }

    if (speedFraction > 0.2f) {
        val alpha = ((speedFraction - 0.2f) / 0.8f) * 0.55f
        val rng = kotlin.random.Random(99)
        repeat(18) {
            val sx = rng.nextFloat() * size.width
            val sy = (rng.nextFloat() * (viewH - horizonY) + horizonY + tick * viewH * 0.35f * speedFraction) % (viewH - horizonY) + horizonY
            val len = rng.nextFloat() * 55f * speedFraction + 15f
            drawLine(
                Brush.verticalGradient(listOf(Color.Transparent, C.laneGlow.copy(alpha = alpha * (0.3f + rng.nextFloat() * 0.5f)), Color.Transparent), sy - len / 2f, sy + len / 2f),
                Offset(sx, sy - len / 2f), Offset(sx, sy + len / 2f), 1.5f.dp.toPx()
            )
        }
    }

    // Draw Rivals (pass their logical lane)
    state.rivals.forEach { drawCar(it, state.player.y, laneW, viewH, yScale, false, glowPulse, it.lane.toFloat(), 0f) }

    // Draw Obstacles
    state.obstacles.forEach { obs ->
        val screenY = (viewH - 290f) - (obs.y - state.player.y) * yScale
        if (screenY in -220f..viewH + 220f) drawBarricade(Offset(obs.lane * laneW + laneW / 2f, screenY), laneW, tick)
    }

    // Draw Player (pass animated lane and tilt angle)
    drawCar(state.player, state.player.y, laneW, viewH, yScale, true, glowPulse, animatedPlayerLane, playerTiltAngle)

    // Player exhaust glow (tied to animated lane)
    val px = animatedPlayerLane * laneW + laneW / 2f; val pyBot = viewH - 290f + 170f
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, C.playerGlow.copy(alpha = 0.55f * glowPulse), Color.Transparent), pyBot, pyBot + 90f), Offset(px - 22f, pyBot), Size(44f, 90f))

    // ── SLIPSTREAM VISUAL EFFECT ──────────────────────────────────────
    if (state.isDrafting && settings.isSlipstreamEnabled) {
        val carH = 175f
        val carCenterY = viewH - 290f
        val carTop = carCenterY - carH / 2f
        
        // Use nanoseconds for ultra-smooth high-speed flicker
        val timeNano = System.nanoTime()
        val draftRng = kotlin.random.Random(timeNano / 1_000_000) 
        
        repeat(30) {
            // Randomly pick side or center streaks
            val isSide = draftRng.nextFloat() > 0.3f
            val sideOffset = if (isSide) (laneW * 0.42f) else (laneW * 0.15f)
            val lineX = px + (if (draftRng.nextBoolean()) sideOffset else -sideOffset) + (draftRng.nextFloat() * 12f - 6f)
            
            // Very fast downward "warp speed" flow
            val flowSpeed = 1500f // pixels per second-ish
            val flowTick = (timeNano / 1_000_000_000f) % 1f
            val yOffset = (flowTick * flowSpeed + draftRng.nextFloat() * 1000f) % 1000f
            
            // Start lines well ahead of the car and blow past it
            val startY = carTop - 600f + yOffset
            val lineLen = 100f + draftRng.nextFloat() * 200f
            
            if (startY < viewH && startY + lineLen > 0) {
                val alpha = (0.4f + draftRng.nextFloat() * 0.6f) * glowPulse
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent, 
                            Color.Cyan.copy(alpha = alpha), 
                            Color.Transparent
                        )
                    ),
                    start = Offset(lineX, startY),
                    end = Offset(lineX, startY + lineLen),
                    strokeWidth = 3.5f.dp.toPx()
                )
            }
        }
        
        // Add some "speed particles" flying straight at the screen
        repeat(8) {
            val pX = px + (draftRng.nextFloat() - 0.5f) * laneW * 1.5f
            val pY = (carTop + (draftRng.nextFloat() * 400f - 200f))
            drawCircle(
                color = Color.White.copy(alpha = 0.5f * glowPulse),
                radius = 2f.dp.toPx(),
                center = Offset(pX, pY)
            )
        }
    }
}

private fun DrawScope.drawCar(
    entity: GameEntity,
    playerY: Float,
    laneW: Float,
    viewH: Float,
    yScale: Float,
    isPlayer: Boolean,
    glowPulse: Float,
    visualLane: Float,
    tiltAngle: Float
) {
    val relY    = (entity.y - playerY) * yScale
    val screenY = if (isPlayer) viewH - 290f else (viewH - 290f) - relY
    if (screenY !in -380f..viewH + 380f) return

    val carW = (laneW * 0.60f).coerceAtMost(125f); val carH = 175f
    // Calculate the X center point using the animated/visual lane
    val cx = visualLane * laneW + laneW / 2f; val left = cx - carW / 2f; val top = screenY - carH / 2f

    val bodyCol = if (isPlayer) C.playerBody else C.rivalBody
    val accentCol = if (isPlayer) C.playerAccent else C.rivalAccent
    val glowCol = if (isPlayer) C.playerGlow else C.rivalGlow
    val lightCol = if (isPlayer) C.playerLight else C.rivalLight
    val rimCol = if (isPlayer) C.laneGlow else C.rivalLight

    // Rotate the canvas context based on the calculated tilt angle so the car "steers"
    rotate(degrees = tiltAngle, pivot = Offset(cx, top + carH / 2f)) {
        drawRect(Brush.radialGradient(listOf(glowCol.copy(alpha = glowPulse * 0.65f), Color.Transparent), Offset(cx, screenY), carW * 1.6f), Offset(cx - carW * 1.6f, top - 35f), Size(carW * 3.2f, carH + 70f))
        drawOval(Color.Black.copy(alpha = 0.5f), Offset(left + 8f, top + carH - 8f), Size(carW - 16f, 20f))
        val wW = carW * 0.23f; val wH = carH * 0.20f
        listOf(Offset(left - wW * 0.35f, top + carH * 0.11f), Offset(left + carW - wW * 0.65f, top + carH * 0.11f),
            Offset(left - wW * 0.35f, top + carH * 0.63f), Offset(left + carW - wW * 0.65f, top + carH * 0.63f)).forEach { wp ->
            drawRoundRect(Color(0xFF181820), wp, Size(wW, wH), CornerRadius(5f))
            drawOval(rimCol.copy(alpha = 0.75f), Offset(wp.x + wW * 0.18f, wp.y + wH * 0.18f), Size(wW * 0.64f, wH * 0.64f))
            drawCircle(rimCol.copy(alpha = 0.9f), 3f, Offset(wp.x + wW * 0.5f, wp.y + wH * 0.5f))
        }
        val noseL = left + carW * 0.12f; val noseR = left + carW * 0.88f
        val bodyPath = Path().apply {
            val r = 14f
            moveTo(noseL + r, top); lineTo(noseR - r, top); quadraticTo(noseR, top, noseR, top + r)
            lineTo(left + carW, top + carH * 0.88f); quadraticTo(left + carW, top + carH, left + carW - r, top + carH)
            lineTo(left + r, top + carH); quadraticTo(left, top + carH, left, top + carH * 0.88f)
            lineTo(noseL, top + r); quadraticTo(noseL, top, noseL + r, top); close()
        }
        drawPath(bodyPath, Brush.verticalGradient(listOf(bodyCol, accentCol), top, top + carH))
        val cL = left + carW * 0.24f; val cR = left + carW * 0.76f; val cT = top + carH * 0.17f; val cB = top + carH * 0.54f
        val cockpit = Path().apply {
            val r = 9f
            moveTo(cL + r, cT); lineTo(cR - r, cT); quadraticTo(cR, cT, cR, cT + r)
            lineTo(cR - 5f, cB - r); quadraticTo(cR - 5f, cB, cR - 5f - r, cB)
            lineTo(cL + 5f + r, cB); quadraticTo(cL + 5f, cB, cL + 5f, cB - r)
            lineTo(cL, cT + r); quadraticTo(cL, cT, cL + r, cT); close()
        }
        drawPath(cockpit, Color(0xFF07090F).copy(alpha = 0.88f))
        drawPath(cockpit, Brush.verticalGradient(listOf(C.white.copy(alpha = 0.17f), Color.Transparent), cT, cT + (cB - cT) * 0.4f))
        drawCircle(bodyCol.copy(alpha = 0.45f), (cR - cL) * 0.21f, Offset((cL + cR) / 2f, (cT + cB) / 2f + 5f))
        drawRect(accentCol.copy(alpha = 0.9f), Offset(left - 5f, top + 2f), Size(carW + 10f, 11f))
        drawLine(accentCol.copy(alpha = 0.5f), Offset(left - 5f, top + 7f), Offset(left + carW + 5f, top + 7f), 2f)
        drawRect(accentCol.copy(alpha = 0.9f), Offset(left - 7f, top + carH * 0.84f), Size(carW + 14f, 13f))
        drawRect(accentCol, Offset(left - 7f, top + carH * 0.78f), Size(9f, 22f))
        drawRect(accentCol, Offset(left + carW - 2f, top + carH * 0.78f), Size(9f, 22f))
        drawRect(C.white.copy(alpha = if (isPlayer) 0.22f else 0.12f), Offset(cx - 4f, top + carH * 0.04f), Size(8f, carH * 0.92f))
        val hlY = top + carH * 0.025f; val tlY = top + carH * 0.88f
        fun light(lx: Float, ly: Float, col: Color) {
            drawRoundRect(col.copy(alpha = 0.9f), Offset(lx - 11f, ly - 5f), Size(22f, 10f), CornerRadius(4f))
            drawCircle(col.copy(alpha = 0.3f * glowPulse), 16f, Offset(lx, ly))
        }
        light(left + carW * 0.22f, hlY + 5f, lightCol); light(left + carW * 0.78f, hlY + 5f, lightCol)
        val tailCol = if (isPlayer) Color(0xFFFF2020) else Color(0xFFFF4000)
        light(left + carW * 0.22f, tlY + 5f, tailCol); light(left + carW * 0.78f, tlY + 5f, tailCol)
        drawRect(Brush.verticalGradient(listOf(C.white.copy(alpha = 0.14f), Color.Transparent), top + carH * 0.04f, top + carH * 0.24f), Offset(left + carW * 0.12f, top + carH * 0.04f), Size(carW * 0.76f, carH * 0.2f))
    }
}

private fun DrawScope.drawBarricade(center: Offset, laneW: Float, tick: Float) {
    val w = (laneW * 0.72f).coerceAtMost(145f); val h = 48f; val l = center.x - w / 2f; val t = center.y
    drawRect(Brush.radialGradient(listOf(C.obstacleGlow, Color.Transparent), Offset(center.x, t + h / 2f), w * 0.85f), Offset(l - 18f, t - 10f), Size(w + 36f, h + 20f))
    val body = Path().apply { addRoundRect(RoundRect(l, t, l + w, t + h, CornerRadius(7f))) }
    drawPath(body, C.obstacle)
    clipRect(l, t, l + w, t + h) {
        val sw = 26f; val offset = (tick * sw * 0.6f) % sw
        for (k in -1..(w / sw).toInt() + 2) if (k % 2 == 0) drawRect(C.obstacleStripe, Offset(l + k * sw - offset, t), Size(sw / 2f, h))
    }
    drawPath(body, C.white.copy(alpha = 0.35f), style = Stroke(2.dp.toPx()))
    listOf(l + w * 0.15f, l + w * 0.5f, l + w * 0.85f).forEach { cx2 ->
        val cone = Path().apply { moveTo(cx2, t - 20f); lineTo(cx2 - 10f, t); lineTo(cx2 + 10f, t); close() }
        drawPath(cone, Color(0xFFFF6600)); drawPath(cone, C.white.copy(alpha = 0.25f), style = Stroke(1.5f.dp.toPx()))
        drawLine(C.white.copy(alpha = 0.6f), Offset(cx2 - 5f, t - 8f), Offset(cx2 + 5f, t - 8f), 2f)
    }
}

@Composable
fun HUDOverlay(state: GameState, unit: SpeedUnit, settings: UserSettings, onTogglePause: () -> Unit) {
    val displaySpeed = state.currentSpeed.toDisplaySpeed(unit)
    val diffColor = when (state.difficulty) { Difficulty.EASY -> Color(0xFF00E676); Difficulty.MEDIUM -> Color(0xFFFFD600); Difficulty.HARD -> Color(0xFFFF2D55) }
    
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures { } }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                HUDCard(Icons.AutoMirrored.Filled.TrendingUp, "RANK", "${state.rank}/6", C.green)
                HUDCard(Icons.Default.Timer, "DIST", "${state.distanceTravelled.toInt()}m", C.yellow)
                HUDCard(Icons.Default.Speed, "SPEED", "$displaySpeed ${unit.label()}", C.blue)
                Box(Modifier.clip(RoundedCornerShape(13.dp)).background(C.hudBg).drawBehind { drawRoundRect(diffColor.copy(alpha = 0.4f), cornerRadius = CornerRadius(13.dp.toPx()), style = Stroke(1.dp.toPx())) }.padding(horizontal = 7.dp, vertical = 6.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("LVL ${state.level}", color = diffColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp); Text(state.difficulty.label.uppercase(), color = C.white.copy(alpha = 0.55f), fontSize = 7.sp, letterSpacing = 0.8.sp) }
                }
            }
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(C.hudBg).drawBehind { drawRoundRect(C.hudBorder.copy(alpha = 0.3f), cornerRadius = CornerRadius(13.dp.toPx()), style = Stroke(1.dp.toPx())) }, Alignment.Center) {
                IconButton(onClick = onTogglePause, modifier = Modifier.fillMaxSize()) { Icon(if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, if (state.isPaused) "Resume" else "Pause", tint = C.white, modifier = Modifier.size(20.dp)) }
            }
        }

        // ── Drafting Indicator ──────────────────────────────────────
        if (state.isDrafting && settings.isSlipstreamEnabled) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = C.laneGlow.copy(alpha = 0.15f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .drawBehind { drawRoundRect(C.laneGlow.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx())) }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = C.laneGlow, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Slipstream - SPEED BOOST ACTIVE", color = C.laneGlow, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun HUDCard(icon: ImageVector, label: String, value: String, accent: Color) {
    Surface(color = C.hudBg, modifier = Modifier.clip(RoundedCornerShape(13.dp)).width(80.dp).drawBehind { drawRoundRect(accent.copy(alpha = 0.3f), cornerRadius = CornerRadius(13.dp.toPx()), style = Stroke(1.dp.toPx())) }) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = accent, modifier = Modifier.size(13.dp)); Spacer(Modifier.height(2.dp)); Text(label, color = C.white.copy(alpha = 0.45f), fontSize = 8.sp, letterSpacing = 0.8.sp); Text(value, color = C.white, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "ring")
    val ringScale by inf.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), "rs")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).pointerInput(Unit) { detectTapGestures { onResume() } }, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size((100 * ringScale).dp).drawBehind { drawCircle(C.laneGlow.copy(alpha = 0.12f)); drawCircle(C.laneGlow, style = Stroke(2.dp.toPx())) }, Alignment.Center) { Icon(Icons.Default.PlayArrow, "Resume", tint = C.white, modifier = Modifier.size(50.dp)) }
            Spacer(Modifier.height(22.dp)); Text("PAUSED", color = C.white, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
            Spacer(Modifier.height(6.dp)); Text("tap to resume", color = C.white.copy(alpha = 0.38f), fontSize = 13.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun GameOverUI(gameState: GameState, speedUnit: SpeedUnit, onRestart: () -> Unit, onNextLevel: () -> Unit, onNavigateBack: () -> Unit) {
    val isVictory = gameState.isVictory; val accent = if (isVictory) C.green else Color(0xFFFF2D55)
    val hasNextLevel = isVictory && gameState.level < Levels.all.size
    val diffCol = when (gameState.difficulty) { Difficulty.EASY -> Color(0xFF00E676); Difficulty.MEDIUM -> Color(0xFFFFD600); Difficulty.HARD -> Color(0xFFFF2D55) }
    Box(Modifier.fillMaxSize().background(C.overlayDark), Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawCircle(accent.copy(alpha = 0.06f), size.width * 0.85f, Offset(size.width / 2f, size.height / 2f)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF080C16)))).drawBehind { drawRoundRect(accent.copy(alpha = 0.55f), cornerRadius = CornerRadius(28.dp.toPx()), style = Stroke(2.dp.toPx())) }.padding(28.dp)) {
            Text(if (isVictory) "🏆" else "💥", fontSize = 52.sp); Spacer(Modifier.height(8.dp)); Text(if (isVictory) "VICTORY!" else "CRASHED!", color = accent, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Text("Level ${gameState.level}", color = C.white.copy(alpha = 0.6f), fontSize = 13.sp); Box(Modifier.clip(RoundedCornerShape(6.dp)).background(diffCol.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text(gameState.difficulty.label, color = diffCol, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
            Spacer(Modifier.height(4.dp)); Text(if (isVictory) "Race complete — well driven!" else "Back to the pits…", color = C.white.copy(alpha = 0.4f), fontSize = 13.sp); Spacer(Modifier.height(22.dp))
            StatRow("Distance", "${gameState.distanceTravelled.toInt()} m", C.yellow); StatRow("Top Speed", "${gameState.peakSpeed.toDisplaySpeed(speedUnit)} ${speedUnit.label()}", C.blue); StatRow("Avg Speed", "${gameState.avgSpeed.toDisplaySpeed(speedUnit)} ${speedUnit.label()}", C.laneGlow); StatRow("Finish Rank", "${gameState.rank} / 6", C.green)
            Spacer(Modifier.height(28.dp))

            if (hasNextLevel) {
                Button(onNextLevel, colors = ButtonDefaults.buttonColors(containerColor = C.laneGlow), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text("NEXT LEVEL", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onRestart, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, C.white.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("RETRY", color = C.white.copy(alpha = 0.65f), fontSize = 13.sp, letterSpacing = 1.5.sp)
                }
            } else {
                Button(onRestart, colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text("RETRY", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(10.dp)); OutlinedButton(onNavigateBack, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, C.white.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("MAIN MENU", color = C.white.copy(alpha = 0.65f), fontSize = 13.sp, letterSpacing = 1.5.sp) }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, accent: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.white.copy(alpha = 0.05f)).padding(horizontal = 14.dp, vertical = 11.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = C.white.copy(alpha = 0.55f), fontSize = 13.sp); Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun GamePreview() { LaneRushTheme { GameContent(gameState = GameState(distanceTravelled = 1200f, currentSpeed = 1.2f, rank = 3, level = 5, difficulty = Difficulty.HARD, throttleOn = true), settings = UserSettings(), onThrottleOn = {}, onThrottleOff = {}, onSwipe = {}, onTap = {}, onTogglePause = {}, onRestart = {}, onNextLevel = {}, onNavigateBack = {}) } }