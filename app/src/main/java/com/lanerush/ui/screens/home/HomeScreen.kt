package com.lanerush.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanerush.domain.model.User
import com.lanerush.ui.theme.LaneRushTheme

// ═══════════════════════════════════════════════════════════════════════════
//  PALETTE
// ═══════════════════════════════════════════════════════════════════════════
private object HC {
    val bg0         = Color(0xFF060912)
    val bg1         = Color(0xFF0A0E1A)
    val bg2         = Color(0xFF0E1428)
    val surfaceHigh = Color(0xFF1A2236)
    val cyan        = Color(0xFF00D4FF)
    val gold        = Color(0xFFFFD600)
    // "online" green — intentionally NOT colorScheme.tertiary (which is pinkish-red)
    val onlineGreen = Color(0xFF00E676)
    val white       = Color.White
    val dim         = Color.White.copy(alpha = 0.55f)
}

// ═══════════════════════════════════════════════════════════════════════════
//  ENTRY
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartGame: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    HomeContent(
        user               = user,
        onStartGame        = onStartGame,
        onViewLeaderboard  = onViewLeaderboard,
        onNavigateSettings = onNavigateSettings
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  CONTENT
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun HomeContent(
    user: User?,
    onStartGame: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    var showHowToPlay by remember { mutableStateOf(false) }
    
    val inf = rememberInfiniteTransition(label = "bg")
    val glowX by inf.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), "gx"
    )
    val glowPulse by inf.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "gp"
    )
    // Pulsing animation for the online dot
    val dotPulse by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), "dot"
    )

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(containerColor = colorScheme.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HomeBackgroundCanvas(glowX, glowPulse)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(80.dp))

                HomeLogoBlock(glowPulse)

                Spacer(Modifier.height(52.dp))

                // ── Greeting pill ─────────────────────────────────────────
                Surface(
                    color    = colorScheme.surfaceVariant,
                    shape    = RoundedCornerShape(50),
                    modifier = Modifier.drawBehind {
                        drawRoundRect(
                            // Use cyan border instead of primary (which bleeds tertiary red)
                            HC.cyan.copy(alpha = 0.25f),
                            cornerRadius = CornerRadius(50.dp.toPx()),
                            style        = Stroke(1.dp.toPx())
                        )
                    }
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(HC.onlineGreen.copy(alpha = dotPulse))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Ready to race, ", color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text(
                            user?.displayName?.ifEmpty { "Driver" } ?: "Driver",
                            color      = colorScheme.onSurface,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                HomePrimaryButton(
                    icon    = Icons.Default.PlayArrow,
                    onClick = onStartGame
                )

                Spacer(Modifier.height(12.dp))

                HomeSecondaryButton(
                    icon    = Icons.Default.Leaderboard,
                    onClick = onViewLeaderboard
                )

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = { showHowToPlay = true },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = HC.cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("HOW TO PLAY", color = HC.cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                }

                Spacer(Modifier.height(48.dp))
            }

            // ── Settings gear (top-right) ──────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 12.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.9f))
                    .drawBehind {
                        drawRoundRect(
                            HC.cyan.copy(alpha = 0.25f),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style        = Stroke(1.dp.toPx())
                        )
                    }
                    .clickable { onNavigateSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, "Settings", tint = colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showHowToPlay) {
        HowToPlayDialog(onDismiss = { showHowToPlay = false })
    }
}

@Composable
fun HowToPlayDialog(onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        HC.cyan.copy(alpha = 0.3f),
                        cornerRadius = CornerRadius(24.dp.toPx()),
                        style        = Stroke(1.5.dp.toPx())
                    )
                }
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏎️", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "HOW TO PLAY",
                    fontSize      = 22.sp,
                    fontWeight    = FontWeight.Black,
                    color         = colorScheme.onSurface,
                    letterSpacing = 2.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                InstructionItem("🔥", "THROTTLE", "Hold your finger anywhere on the screen to accelerate.")
                InstructionItem("🛑", "BRAKE", "Release your finger to slow down and avoid obstacles.")
                InstructionItem("↔️", "STEER", "Swipe Left or Right to quickly switch between lanes.")
                InstructionItem("🏁", "GOAL", "Reach the finish distance. You MUST be in Rank 1 to win and unlock the next level!")
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick  = onDismiss,
                    colors   = ButtonDefaults.buttonColors(containerColor = HC.cyan),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("GOT IT!", color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun InstructionItem(emoji: String, title: String, desc: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = HC.cyan, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(desc, color = colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BACKGROUND
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun HomeBackgroundCanvas(glowX: Float, glowPulse: Float) {
    val colorScheme = MaterialTheme.colorScheme
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(colorScheme.background, colorScheme.surfaceVariant)))
        drawCircle(
            Brush.radialGradient(
                listOf(colorScheme.primary.copy(alpha = 0.07f * glowPulse), Color.Transparent),
                Offset(size.width * glowX, size.height * 0.15f),
                size.width * 0.7f
            ),
            size.width * 0.7f,
            Offset(size.width * glowX, size.height * 0.15f)
        )
        drawCircle(
            Brush.radialGradient(
                listOf(colorScheme.secondary.copy(alpha = 0.04f), Color.Transparent),
                Offset(size.width * 0.5f, size.height * 0.85f),
                size.width * 0.6f
            ),
            size.width * 0.6f,
            Offset(size.width * 0.5f, size.height * 0.85f)
        )
        val laneW = size.width / 3f
        for (i in 1..2) {
            drawLine(
                colorScheme.onBackground.copy(alpha = 0.04f),
                Offset(i * laneW, size.height * 0.6f),
                Offset(i * laneW, size.height), 2f
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  LOGO
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun HomeLogoBlock(glowPulse: Float) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.width(160.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(colorScheme.primary.copy(alpha = 0.4f)))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(colorScheme.primary))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f).height(1.dp).background(colorScheme.primary.copy(alpha = 0.4f)))
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "LANE",
            fontSize      = 58.sp,
            fontWeight    = FontWeight.Black,
            color         = colorScheme.onBackground,
            letterSpacing = 14.sp
        )

        Box(contentAlignment = Alignment.BottomCenter) {
            Text(
                "RUSH",
                fontSize      = 76.sp,
                fontWeight    = FontWeight.Black,
                color         = colorScheme.secondary,
                letterSpacing = 8.sp
            )
            Spacer(
                Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .offset(y = 4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, colorScheme.primary.copy(alpha = glowPulse), Color.Transparent)
                        ),
                        RoundedCornerShape(50)
                    )
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "STREET RACING CHAMPIONSHIP",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Medium,
            color         = colorScheme.onSurfaceVariant,
            letterSpacing = 3.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BUTTONS
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun HomePrimaryButton(icon: ImageVector, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Button(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .drawBehind {
                drawRoundRect(
                    colorScheme.tertiary.copy(alpha = 0.25f),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style        = Stroke(6.dp.toPx())
                )
            },
        shape  = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.tertiary)
    ) {
        Icon(icon, null, tint = colorScheme.onTertiary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text("START RACE", color = colorScheme.onTertiary, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 2.sp)
    }
}

@Composable
private fun HomeSecondaryButton(icon: ImageVector, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape    = RoundedCornerShape(16.dp),
        border   = BorderStroke(1.dp, HC.cyan.copy(alpha = 0.4f)),
        colors   = ButtonDefaults.outlinedButtonColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor   = colorScheme.onSurface
        )
    ) {
        Icon(icon, null, tint = HC.cyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("LEADERBOARD", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 1.5.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PREVIEW
// ═══════════════════════════════════════════════════════════════════════════
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    LaneRushTheme {
        HomeContent(
            user               = User(uid = "1", displayName = "Emelio101"),
            onStartGame        = {},
            onViewLeaderboard  = {},
            onNavigateSettings = {}
        )
    }
}
