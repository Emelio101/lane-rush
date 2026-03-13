package com.lanerush.ui.screens.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanerush.domain.model.Difficulty
import com.lanerush.domain.model.Levels
import com.lanerush.domain.model.UserSettings
import com.lanerush.ui.theme.LaneRushTheme

// ── Palette ───────────────────────────────────────────────────────────────
private object LSC {
    val bg       = Color(0xFF060912)
    val surface  = Color(0xFF0E1428)
    val cyan     = Color(0xFF00D4FF)
    val white    = Color.White
    val dim      = Color.White.copy(alpha = 0.55f)
    val dimmer   = Color.White.copy(alpha = 0.18f)

    val difficultyColor = mapOf(
        Difficulty.EASY   to Color(0xFF00E676),
        Difficulty.MEDIUM to Color(0xFFFFD600),
        Difficulty.HARD   to Color(0xFFFF2D55)
    )
    val difficultyEmoji = mapOf(
        Difficulty.EASY   to "🟢",
        Difficulty.MEDIUM to "🟡",
        Difficulty.HARD   to "🔴"
    )
    val difficultyDesc = mapOf(
        Difficulty.EASY   to "Relaxed traffic, gentle acceleration",
        Difficulty.MEDIUM to "Standard challenge",
        Difficulty.HARD   to "Dense obstacles, lightning rivals"
    )
}

@Composable
fun LevelSelectScreen(
    viewModel: GameViewModel,
    settings: UserSettings = UserSettings(),
    onStartGame: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val selectedLevel      by viewModel.selectedLevel.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()

    LevelSelectContent(
        selectedLevel      = selectedLevel,
        selectedDifficulty = selectedDifficulty,
        maxUnlockedLevel   = settings.maxUnlockedLevel,
        onLevelSelect      = { viewModel.setLevel(it) },
        onDifficultySelect = { viewModel.setDifficulty(it) },
        onStartGame        = onStartGame,
        onNavigateBack     = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectContent(
    selectedLevel: Int,
    selectedDifficulty: Difficulty,
    maxUnlockedLevel: Int,
    onLevelSelect: (Int) -> Unit,
    onDifficultySelect: (Difficulty) -> Unit,
    onStartGame: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = LSC.bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "RACE SETUP",
                            fontWeight    = FontWeight.Black,
                            fontSize      = 18.sp,
                            letterSpacing = 3.sp,
                            color         = LSC.white
                        )
                        Text("Choose your challenge", fontSize = 11.sp, color = LSC.dim)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .drawBehind {
                                    drawRoundRect(
                                        LSC.cyan.copy(alpha = 0.2f),
                                        cornerRadius = CornerRadius(10.dp.toPx()),
                                        style = Stroke(1.dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, "Back",
                                tint = LSC.white, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LSC.bg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(LSC.bg, Color(0xFF0A0E1A))))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {

            // ── DIFFICULTY ────────────────────────────────────────────────
            SectionHeader("Difficulty")
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Difficulty.entries.forEach { diff ->
                    val accent  = LSC.difficultyColor[diff]!!
                    val emoji   = LSC.difficultyEmoji[diff]!!
                    val desc    = LSC.difficultyDesc[diff]!!
                    val selected = diff == selectedDifficulty
                    DifficultyCard(
                        emoji    = emoji,
                        label    = diff.label,
                        desc     = desc,
                        accent   = accent,
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick  = { onDifficultySelect(diff) }
                    )
                }
            }

            // ── LEVEL ─────────────────────────────────────────────────────
            SectionHeader("Level Selection  (Rank 1 to unlock next)")
            val levels = Levels.all
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                levels.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { lvl ->
                            val isLocked = lvl.level > maxUnlockedLevel
                            LevelCard(
                                level    = lvl.level,
                                distance = "${lvl.maxDistance.toInt()}m",
                                selected = lvl.level == selectedLevel,
                                isLocked = isLocked,
                                modifier = Modifier.weight(1f),
                                onClick  = { if (!isLocked) onLevelSelect(lvl.level) }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── START ─────────────────────────────────────────────────────
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .drawBehind {
                        drawRoundRect(
                            LSC.cyan.copy(alpha = 0.3f),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(6.dp.toPx())
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LSC.cyan)
            ) {
                Text(
                    "START RACE",
                    color      = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize   = 16.sp,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color         = LSC.dim,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier      = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun DifficultyCard(
    emoji: String,
    label: String,
    desc: String,
    accent: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (selected) accent.copy(alpha = 0.12f) else LSC.surface,
        tween(250), label = "bg"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .drawBehind {
                drawRoundRect(
                    if (selected) accent else LSC.white.copy(alpha = 0.05f),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )
            },
        color = bgColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(label, color = if (selected) accent else LSC.white, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                desc,
                color = if (selected) accent.copy(alpha = 0.7f) else LSC.dimmer,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LevelCard(
    level: Int,
    distance: String,
    selected: Boolean,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = if (isLocked) Color.Gray else levelAccent(level)
    val bgColor by animateColorAsState(
        if (selected) accent.copy(alpha = 0.15f) else LSC.surface,
        tween(200), label = "bg"
    )

    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isLocked) LSC.bg.copy(alpha = 0.5f) else bgColor)
            .drawBehind {
                drawRoundRect(
                    if (selected) accent else LSC.white.copy(alpha = 0.05f),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )
            }
            .clickable(enabled = !isLocked) { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isLocked) Color.DarkGray else if (selected) accent else LSC.dimmer),
                contentAlignment = Alignment.Center
            ) {
                if (isLocked) {
                    Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                } else {
                    Text(
                        level.toString(),
                        color      = if (selected) Color.Black else LSC.white,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Level $level",
                color      = if (isLocked) Color.Gray else if (selected) LSC.white else LSC.dim,
                fontSize   = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (!isLocked) {
            Text(
                distance,
                color      = if (selected) accent else LSC.dimmer,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Returns a color that grades from green (level 1) through yellow to red (level 10). */
private fun levelAccent(level: Int): Color = when {
    level <= 2  -> Color(0xFF00E676)  // green
    level <= 4  -> Color(0xFF69F0AE)  // light green
    level <= 6  -> Color(0xFFFFD600)  // gold
    level <= 8  -> Color(0xFFFF6D00)  // orange
    else        -> Color(0xFFFF2D55)  // red
}

// ═══════════════════════════════════════════════════════════════════════════
//  PREVIEW
// ═══════════════════════════════════════════════════════════════════════════
@Preview(showBackground = true)
@Composable
fun LevelSelectPreview() {
    LaneRushTheme {
        LevelSelectContent(
            selectedLevel      = 3,
            selectedDifficulty = Difficulty.MEDIUM,
            maxUnlockedLevel   = 4,
            onLevelSelect      = {},
            onDifficultySelect = {},
            onStartGame        = {},
            onNavigateBack     = {}
        )
    }
}
