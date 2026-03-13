package com.lanerush.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Work
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanerush.BuildConfig
import com.lanerush.domain.model.AppTheme
import com.lanerush.domain.model.SpeedUnit
import com.lanerush.domain.model.UserSettings
import com.lanerush.ui.theme.LaneRushTheme

// ═══════════════════════════════════════════════════════════════════════════
//  ENTRY
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    SettingsContent(
        settings       = settings,
        onThemeChange  = { viewModel.updateTheme(it) },
        onUnitChange   = { viewModel.updateSpeedUnit(it) },
        onSoundToggle  = { viewModel.updateSoundEnabled(it) },
        onVolumeChange = { viewModel.updateSoundVolume(it) },
        onNavigateBack = onNavigateBack,
        onSignOut      = onSignOut
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  CONTENT
// ═══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settings: UserSettings,
    onThemeChange: (AppTheme) -> Unit,
    onUnitChange: (SpeedUnit) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "SETTINGS",
                            fontWeight    = FontWeight.Black,
                            fontSize      = 18.sp,
                            letterSpacing = 3.sp,
                            color         = colorScheme.onBackground
                        )
                        Text("Preferences", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
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
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, "Back",
                                tint     = colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = colorScheme.background,
                    titleContentColor      = colorScheme.onBackground,
                    navigationIconContentColor = colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(colorScheme.background, colorScheme.surfaceVariant)))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── APPEARANCE ────────────────────────────────────────────────
            SettingsSection(
                icon  = Icons.Default.Palette,
                title = "Appearance",
                accent = colorScheme.primary
            ) {
                ThemeOptions(current = settings.theme, onSelect = onThemeChange)
            }

            // ── SOUND & MUSIC ─────────────────────────────────────────────
            SettingsSection(
                icon  = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Sound & Music",
                accent = colorScheme.primary
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Master Sound", color = colorScheme.onSurface, fontSize = 14.sp)
                    Switch(
                        checked = settings.isSoundEnabled,
                        onCheckedChange = onSoundToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Volume", color = colorScheme.onSurface, fontSize = 14.sp)
                        Text("${(settings.soundVolume * 100).toInt()}%", color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Slider(
                        value = settings.soundVolume,
                        onValueChange = onVolumeChange,
                        enabled = settings.isSoundEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = colorScheme.primary,
                            activeTrackColor = colorScheme.primary
                        )
                    )
                }
            }

            // ── METRICS ───────────────────────────────────────────────────
            SettingsSection(
                icon  = Icons.Default.Speed,
                title = "Speed Units",
                accent = colorScheme.secondary
            ) {
                SpeedUnitOptions(current = settings.speedUnit, onSelect = onUnitChange)
            }

            // ── ABOUT DEVELOPER ───────────────────────────────────────────
            SettingsSection(
                icon = Icons.Outlined.AccountCircle,
                title = "About Developer",
                accent = colorScheme.primary
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
                                tint = colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Emmanuel C. Phiri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            "Mobile Apps Developer",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(Modifier.height(8.dp))

                SocialLinkRow(
                    icon = Icons.Outlined.Code,
                    label = "GitHub",
                    handle = "@Emelio101",
                    onClick = { uriHandler.openUri("https://github.com/Emelio101") }
                )
                SocialLinkRow(
                    icon = Icons.Outlined.Work,
                    label = "LinkedIn",
                    handle = "Emmanuel C. Phiri",
                    onClick = { uriHandler.openUri("https://www.linkedin.com/in/emmanuel-c-phiri-13420315b") }
                )
                SocialLinkRow(
                    icon = Icons.Outlined.Timer,
                    label = "WakaTime",
                    handle = "@Emelio101",
                    onClick = { uriHandler.openUri("https://wakatime.com/@Emelio101") }
                )
            }

            // ── ABOUT ─────────────────────────────────────────────────────
            SettingsSection(
                icon  = Icons.Default.Info,
                title = "About App",
                accent = colorScheme.onSurfaceVariant
            ) {
                AboutRow(label = "Version",     value = BuildConfig.VERSION_NAME)
                Spacer(Modifier.height(6.dp))
                AboutRow(label = "Build",       value = BuildConfig.VERSION_CODE.toString())
            }

            // ── SIGN OUT ──────────────────────────────────────────────────
            OutlinedButton(
                onClick  = onSignOut,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.1f)),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = colorScheme.surface,
                    contentColor   = colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sign Out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SECTION WRAPPER
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colorScheme.surface)
            .drawBehind {
                drawRoundRect(
                    colorScheme.onSurface.copy(alpha = 0.1f),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style        = Stroke(1.dp.toPx())
                )
            }
    ) {
        // Section header
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .drawBehind {
                        drawRoundRect(
                            accent.copy(alpha = 0.25f),
                            cornerRadius = CornerRadius(9.dp.toPx()),
                            style        = Stroke(1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, color = colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        // Divider
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colorScheme.onSurface.copy(alpha = 0.1f))
        )

        // Content
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            content  = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SOCIAL LINK ROW
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SocialLinkRow(
    icon: ImageVector,
    label: String,
    handle: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(20.dp),
            tint = colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colorScheme.onSurface
            )
            Text(
                handle,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.OpenInNew,
            null,
            modifier = Modifier.size(14.dp),
            tint = colorScheme.primary
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  THEME OPTIONS
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ThemeOptions(current: AppTheme, onSelect: (AppTheme) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val options = listOf(
        AppTheme.SYSTEM to (Icons.Default.BrightnessAuto  to "Follow System"),
        AppTheme.LIGHT  to (Icons.Default.WbSunny          to "Light Mode"),
        AppTheme.DARK   to (Icons.Default.NightlightRound  to "Dark Mode")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (theme, pair) ->
            val (icon, label) = pair
            SelectionTile(
                icon     = icon,
                label    = label,
                selected = current == theme,
                accent   = colorScheme.primary,
                onClick  = { onSelect(theme) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SPEED UNIT OPTIONS
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SpeedUnitOptions(current: SpeedUnit, onSelect: (SpeedUnit) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val options = listOf(
        SpeedUnit.KMH to "Kilometres per hour  •  KM/H",
        SpeedUnit.MPH to "Miles per hour  •  MPH"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (unit, label) ->
            SelectionTile(
                icon     = Icons.Default.Speed,
                label    = label,
                selected = current == unit,
                accent   = colorScheme.secondary,
                onClick  = { onSelect(unit) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SELECTION TILE
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SelectionTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor by animateColorAsState(
        if (selected) accent.copy(alpha = 0.1f) else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tween(200), label = "bg"
    )
    val borderColor by animateColorAsState(
        if (selected) accent.copy(alpha = 0.6f) else colorScheme.onSurface.copy(alpha = 0.1f),
        tween(200), label = "border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .drawBehind {
                drawRoundRect(borderColor, cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                tint     = if (selected) accent else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color      = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                fontSize   = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        // Check indicator
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) accent else Color.Transparent)
                .drawBehind {
                    if (!selected) drawCircle(colorScheme.onSurfaceVariant.copy(alpha = 0.3f), style = Stroke(1.5.dp.toPx()))
                },
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check, null,
                    tint     = colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ABOUT ROW
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AboutRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = colorScheme.onSurfaceVariant,   fontSize = 13.sp)
        Text(value, color = colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PREVIEW
// ═══════════════════════════════════════════════════════════════════════════
@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    LaneRushTheme {
        SettingsContent(
            settings       = UserSettings(),
            onThemeChange  = {},
            onUnitChange   = {},
            onSoundToggle  = {},
            onVolumeChange = {},
            onNavigateBack = {},
            onSignOut      = {}
        )
    }
}
