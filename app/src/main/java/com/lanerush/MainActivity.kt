package com.lanerush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lanerush.data.auth.AuthRepositoryImpl
import com.lanerush.data.leaderboard.LeaderboardRepositoryImpl
import com.lanerush.data.settings.SettingsRepository
import com.lanerush.domain.model.AppTheme
import com.lanerush.engine.SoundManager
import com.lanerush.ui.screens.game.GameScreen
import com.lanerush.ui.screens.game.GameViewModel
import com.lanerush.ui.screens.game.LevelSelectScreen
import com.lanerush.ui.screens.home.HomeScreen
import com.lanerush.ui.screens.home.HomeViewModel
import com.lanerush.ui.screens.leaderboard.LeaderboardScreen
import com.lanerush.ui.screens.leaderboard.LeaderboardViewModel
import com.lanerush.ui.screens.login.LoginScreen
import com.lanerush.ui.screens.login.LoginViewModel
import com.lanerush.ui.screens.settings.SettingsScreen
import com.lanerush.ui.screens.settings.SettingsViewModel
import com.lanerush.ui.theme.LaneRushTheme

class MainActivity : ComponentActivity() {

    private val authRepository        by lazy { AuthRepositoryImpl() }
    private val leaderboardRepository by lazy { LeaderboardRepositoryImpl() }
    private val settingsRepository    by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        enableEdgeToEdge()

        setContent {
            val settings by settingsRepository.userSettingsFlow.collectAsState(
                initial = com.lanerush.domain.model.UserSettings()
            )
            val isDarkTheme = when (settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT  -> false
                AppTheme.DARK   -> true
            }
            LaneRushTheme(darkTheme = isDarkTheme) {
                LaneRushApp(
                    authRepository        = authRepository,
                    leaderboardRepository = leaderboardRepository,
                    settingsRepository    = settingsRepository
                )
            }
        }
    }
}

@Composable
fun LaneRushApp(
    authRepository: AuthRepositoryImpl,
    leaderboardRepository: LeaderboardRepositoryImpl,
    settingsRepository: SettingsRepository
) {
    val navController = rememberNavController()
    val context       = LocalContext.current

    // FIX: single SoundManager instance for the entire app lifetime
    val soundManager  = remember { SoundManager(context) }

    val settings by settingsRepository.userSettingsFlow.collectAsState(
        initial = com.lanerush.domain.model.UserSettings()
    )

    // ── Lifecycle: pause sound when app backgrounds ──────────────────
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppInForeground = event == Lifecycle.Event.ON_RESUME ||
                    event == Lifecycle.Event.ON_START
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Sync volume / mute globally ──────────────────────────────────
    LaunchedEffect(settings.isSoundEnabled, settings.soundVolume) {
        soundManager.updateSettings(settings.isSoundEnabled, settings.soundVolume)
    }

    // ── Menu music ───────────────────────────────────────────────────
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute, isAppInForeground) {
        if (!isAppInForeground) { soundManager.stopMusic(); return@LaunchedEffect }
        val isMenuRoute = currentRoute in listOf("home", "level_select", "settings", "leaderboard")
        if (isMenuRoute) soundManager.playMusic() else soundManager.stopMusic()
    }

    // FIX: single release point for the one SoundManager instance
    DisposableEffect(Unit) { onDispose { soundManager.release() } }

    val startDest = if (authRepository.getCurrentUser() != null) "home" else "login"

    // FIX: GameViewModel no longer holds LeaderboardRepository (score goes via WorkManager)
    //      It receives appContext so it can enqueue WorkManager jobs.
    val gameViewModel: GameViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return GameViewModel(
                    authRepository    = authRepository,
                    settingsRepository = settingsRepository,
                    appContext         = context.applicationContext
                ) as T
            }
        }
    )

    NavHost(navController = navController, startDestination = startDest) {

        composable("login") {
            val vm: LoginViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return LoginViewModel(authRepository, leaderboardRepository) as T
                    }
                }
            )
            LoginScreen(viewModel = vm, onAuthSuccess = {
                navController.navigate("home") { popUpTo("login") { inclusive = true } }
            })
        }

        composable("home") {
            val vm: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return HomeViewModel(authRepository) as T
                    }
                }
            )
            HomeScreen(
                viewModel          = vm,
                onStartGame        = { navController.navigate("level_select") },
                onViewLeaderboard  = { navController.navigate("leaderboard") },
                onNavigateSettings = { navController.navigate("settings") }
            )
        }

        composable("level_select") {
            LevelSelectScreen(
                viewModel      = gameViewModel,
                settings       = settings,
                onStartGame    = { navController.navigate("game") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            val vm: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return SettingsViewModel(settingsRepository) as T
                    }
                }
            )
            val signOutVm: LoginViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return LoginViewModel(authRepository, leaderboardRepository) as T
                    }
                }
            )
            SettingsScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() },
                onSignOut      = {
                    signOutVm.signOut()
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                }
            )
        }

        composable("game") {
            // FIX: soundManager is injected — GameScreen no longer creates its own instance
            GameScreen(
                viewModel         = gameViewModel,
                settings          = settings,
                soundManager      = soundManager,
                isAppInForeground = isAppInForeground,
                onNavigateBack    = { navController.popBackStack() }
            )
        }

        composable("leaderboard") {
            val vm: LeaderboardViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        // FIX: passes the interface, not the impl — no downcast needed in VM
                        return LeaderboardViewModel(authRepository, leaderboardRepository) as T
                    }
                }
            )
            LeaderboardScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }
    }
}