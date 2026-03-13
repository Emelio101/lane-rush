package com.lanerush.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lanerush.domain.model.LeaderboardCategory
import com.lanerush.domain.model.Score
import com.lanerush.ui.theme.LaneRushTheme

// ═══════════════════════════════════════════════════════════════════════════
//  PALETTE  (matches game dark theme)
// ═══════════════════════════════════════════════════════════════════════════
private object LBC {
    val gold        = Color(0xFFFFD600)
    val silver      = Color(0xFFB8C4D0)
    val bronze      = Color(0xFFCD7F32)
}

// ═══════════════════════════════════════════════════════════════════════════
//  ENTRY
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onNavigateBack: () -> Unit
) {
    val scores           by viewModel.scores.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val isRefreshing     by viewModel.isRefreshing.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentUserId     = viewModel.getCurrentUserId()
    val currentUserScore by viewModel.currentUserScore.collectAsState()

    LaunchedEffect(Unit) { viewModel.fetchTopScores() }

    LeaderboardContent(
        scores           = scores,
        isLoading        = isLoading,
        isRefreshing     = isRefreshing,
        selectedCategory = selectedCategory,
        currentUserId    = currentUserId,
        currentUserScore = currentUserScore,
        onRefresh        = { viewModel.fetchTopScores(isRefresh = true) },
        onCategorySelect = { viewModel.selectCategory(it) },
        onNavigateBack   = onNavigateBack
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  CONTENT
// ═══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardContent(
    scores: List<Score>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    selectedCategory: LeaderboardCategory,
    currentUserId: String?,
    currentUserScore: Score?,
    onRefresh: () -> Unit,
    onCategorySelect: (LeaderboardCategory) -> Unit,
    onNavigateBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "HALL OF FAME",
                                fontWeight    = FontWeight.Black,
                                fontSize      = 18.sp,
                                letterSpacing = 3.sp,
                                color         = colorScheme.onBackground
                            )
                            Text(
                                "Global rankings",
                                fontSize = 11.sp,
                                color    = colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colorScheme.surfaceVariant)
                                    .drawBehind {
                                        drawRoundRect(
                                            colorScheme.primary.copy(alpha = 0.2f),
                                            cornerRadius = CornerRadius(10.dp.toPx()),
                                            style        = Stroke(1.dp.toPx())
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colorScheme.onSurface, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor         = colorScheme.background,
                        titleContentColor      = colorScheme.onBackground,
                        navigationIconContentColor = colorScheme.onBackground
                    )
                )

                // ── Category tab row ───────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LeaderboardCategory.entries.forEach { cat ->
                        val selected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(3.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) colorScheme.surfaceVariant else Color.Transparent)
                                .drawBehind {
                                    if (selected) drawRoundRect(
                                        colorScheme.secondary.copy(alpha = 0.45f),
                                        cornerRadius = CornerRadius(9.dp.toPx()),
                                        style        = Stroke(1.dp.toPx())
                                    )
                                }
                                .clickableNoRipple { onCategorySelect(cat) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (cat) {
                                    LeaderboardCategory.DISTANCE  -> "Distance"
                                    LeaderboardCategory.TOP_SPEED -> "Speed"
                                    LeaderboardCategory.AVG_SPEED -> "Avg Speed"
                                },
                                fontSize      = 12.sp,
                                fontWeight    = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color         = if (selected) colorScheme.secondary else colorScheme.onSurfaceVariant,
                                letterSpacing = if (selected) 0.5.sp else 0.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Podium header for top-3 ─────────────────────────────────
                if (scores.size >= 3 && !isLoading) {
                    PodiumRow(
                        scores   = scores.take(3),
                        category = selectedCategory
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(colorScheme.background, colorScheme.surfaceVariant))
                )
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh    = onRefresh,
                modifier     = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading && !isRefreshing -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = colorScheme.secondary, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
                                Spacer(Modifier.height(14.dp))
                                Text("Loading rankings…", color = colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }
                    scores.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏁", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("No records yet", color = colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Be the first to race!", color = colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Skip top 3 — shown in podium
                            val listScores = if (scores.size >= 3) scores.drop(3) else scores
                            val offset     = if (scores.size >= 3) 4 else 1
                            itemsIndexed(listScores) { index, score ->
                                LeaderboardItem(
                                    rank          = index + offset,
                                    score         = score,
                                    category      = selectedCategory,
                                    isHighlighted = score.uid == currentUserId
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }

            // ── Pinned current-user row (if outside top 100) ──────────────
            if (currentUserScore != null && !isLoading) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, colorScheme.background.copy(alpha = 0.9f), colorScheme.background))
                        )
                        .padding(top = 16.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(colorScheme.secondary))
                        Spacer(Modifier.width(8.dp))
                        Text("YOUR BEST", color = colorScheme.secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                        LeaderboardItem(
                            rank          = 0,
                            score         = currentUserScore,
                            category      = selectedCategory,
                            isHighlighted = true
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PODIUM  (top 3 in a visually distinct block)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PodiumRow(scores: List<Score>, category: LeaderboardCategory) {
    val colorScheme = MaterialTheme.colorScheme
    // Order: 2nd, 1st, 3rd
    val order   = listOf(1, 0, 2)
    val heights = listOf(72.dp, 96.dp, 60.dp)
    val medals  = listOf(LBC.silver, LBC.gold, LBC.bronze)
    val ranks   = listOf(2, 1, 3)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.Bottom
    ) {
        order.forEachIndexed { i, scoreIdx ->
            val score    = scores[scoreIdx]
            val podiumH  = heights[i]
            val medalCol = medals[i]
            val rank     = ranks[i]
            val value    = scoreValue(score, category)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.weight(1f)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(if (rank == 1) 54.dp else 44.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceVariant)
                        .border(2.dp, medalCol, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (score.photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model             = ImageRequest.Builder(LocalContext.current).data(score.photoUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier          = Modifier.fillMaxSize(),
                            contentScale      = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, tint = medalCol, modifier = Modifier.fillMaxSize().padding(8.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    score.displayName.ifEmpty { "Driver" },
                    color     = colorScheme.onBackground,
                    fontSize  = if (rank == 1) 13.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(value, color = medalCol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))

                // Podium block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(podiumH)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(medalCol.copy(alpha = 0.3f), colorScheme.surfaceVariant)
                            )
                        )
                        .drawBehind {
                            drawRoundRect(
                                medalCol.copy(alpha = 0.4f),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style        = Stroke(1.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "#$rank",
                        color      = medalCol,
                        fontSize   = if (rank == 1) 28.sp else 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  LIST ITEM  (rank 4+)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun LeaderboardItem(
    rank: Int,
    score: Score,
    category: LeaderboardCategory,
    isHighlighted: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor    = if (isHighlighted) colorScheme.secondary.copy(alpha = 0.08f) else colorScheme.surface
    val borderCol  = if (isHighlighted) colorScheme.secondary.copy(alpha = 0.5f)  else colorScheme.onSurface.copy(alpha = 0.1f)

    Surface(
        color    = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Box(Modifier.width(36.dp), Alignment.Center) {
                Text(
                    if (rank > 0) "#$rank" else "–",
                    color      = if (isHighlighted) colorScheme.secondary else colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceVariant)
                    .border(1.5.dp, if (isHighlighted) colorScheme.secondary.copy(alpha = 0.4f) else colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (score.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model             = ImageRequest.Builder(LocalContext.current).data(score.photoUrl).crossfade(true).build(),
                        contentDescription = "Avatar",
                        modifier          = Modifier.fillMaxSize(),
                        contentScale      = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null,
                        tint     = if (isHighlighted) colorScheme.secondary.copy(alpha = 0.7f) else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            // Name
            Text(
                score.displayName.ifEmpty { "Unknown Driver" },
                color      = colorScheme.onSurface,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.weight(1f),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )

            // Value
            Text(
                scoreValue(score, category),
                color      = if (isHighlighted) colorScheme.secondary else colorScheme.primary,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HELPERS
// ═══════════════════════════════════════════════════════════════════════════
private fun scoreValue(score: Score, category: LeaderboardCategory) = when (category) {
    LeaderboardCategory.DISTANCE  -> "${score.score} m"
    LeaderboardCategory.TOP_SPEED -> "${(score.topSpeedReached * 200).toInt()} KM/H"
    LeaderboardCategory.AVG_SPEED -> "${(score.avgSpeedDuringRace * 200).toInt()} KM/H"
}

// Modifier extension to make a Box clickable without ripple
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            indication          = null,
            interactionSource   = androidx.compose.foundation.interaction.MutableInteractionSource()
        ) { onClick() }
    )

// ═══════════════════════════════════════════════════════════════════════════
//  PREVIEW
// ═══════════════════════════════════════════════════════════════════════════
@Preview(showBackground = true)
@Composable
fun LeaderboardPreview() {
    LaneRushTheme {
        LeaderboardContent(
            scores = listOf(
                Score(uid = "1", displayName = "Ghost Rider",   score = 4800, topSpeedReached = 1.9f, avgSpeedDuringRace = 1.2f),
                Score(uid = "2", displayName = "NightShift",    score = 4200, topSpeedReached = 1.7f, avgSpeedDuringRace = 1.1f),
                Score(uid = "3", displayName = "StormBlazer",   score = 3900, topSpeedReached = 1.6f, avgSpeedDuringRace = 1.0f),
                Score(uid = "4", displayName = "Emelio101",     score = 984,  topSpeedReached = 0.74f, avgSpeedDuringRace = 0.57f),
            ),
            isLoading        = false,
            isRefreshing     = false,
            selectedCategory = LeaderboardCategory.DISTANCE,
            currentUserId    = "4",
            currentUserScore = null,
            onRefresh        = {},
            onCategorySelect = {},
            onNavigateBack   = {}
        )
    }
}