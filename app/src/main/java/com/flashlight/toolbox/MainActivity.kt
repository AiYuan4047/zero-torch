package com.flashlight.toolbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashlight.toolbox.data.SettingsStore
import com.flashlight.toolbox.data.ThemeMode
import com.flashlight.toolbox.data.AppSettings
import com.flashlight.toolbox.flashlight.FlashlightController
import com.flashlight.toolbox.ui.screens.MainScreen
import com.flashlight.toolbox.ui.screens.MorseSignalScreen
import com.flashlight.toolbox.ui.screens.SettingsScreen
import com.flashlight.toolbox.ui.screens.WidgetSettingsScreen
import com.flashlight.toolbox.ui.theme.FlashlightToolboxTheme

enum class Screen { Main, Settings, MorseSignal, WidgetSettings }

class MainActivity : ComponentActivity() {

    private lateinit var flashlight: FlashlightController
    private lateinit var settingsStore: SettingsStore
    // 用于从 onNewIntent 接收导航目标
    private var pendingNavigationTarget: Screen? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        flashlight = FlashlightController(this)
        settingsStore = SettingsStore(this)

        // 检查是否从快捷方式打开摩斯界面
        val openMorse = intent.getBooleanExtra("open_morse", false)
        val initialScreen = if (openMorse) Screen.MorseSignal else Screen.Main
        pendingNavigationTarget = if (openMorse) Screen.MorseSignal else null

        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }

            // 处理来自 onNewIntent 的导航请求
            LaunchedEffect(pendingNavigationTarget) {
                pendingNavigationTarget?.let {
                    currentScreen = it
                    pendingNavigationTarget = null
                }
            }

            FlashlightToolboxTheme(
                darkTheme = darkTheme,
                themeColor = Color(settings.themeColor),
            ) {
                val navBarsColor = if (darkTheme) Color(0xFF1A120B) else Color(0xFFFFF8F4)
                LaunchedEffect(navBarsColor) {
                    window.navigationBarColor = navBarsColor.toArgb()
                    window.statusBarColor = navBarsColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView)
                        .apply {
                            isAppearanceLightStatusBars = !darkTheme
                            isAppearanceLightNavigationBars = !darkTheme
                        }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = currentScreen == Screen.Main,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(200)),
                    ) {
                        MainScreen(
                            controller = flashlight,
                            settingsFlow = settingsStore.settings,
                            onOpenSettings = { currentScreen = Screen.Settings },
                            onOpenMorse = { currentScreen = Screen.MorseSignal },
                        )
                    }

                    AnimatedVisibility(
                        visible = currentScreen == Screen.Settings,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(200)),
                    ) {
                        SettingsScreen(
                            store = settingsStore,
                            onBack = { currentScreen = Screen.Main },
                            onOpenWidgetSettings = { currentScreen = Screen.WidgetSettings },
                        )
                    }

                    AnimatedVisibility(
                        visible = currentScreen == Screen.MorseSignal,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(200)),
                    ) {
                        MorseSignalScreen(
                            controller = flashlight,
                            onBack = { currentScreen = Screen.Main },
                        )
                    }

                    AnimatedVisibility(
                        visible = currentScreen == Screen.WidgetSettings,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(200)),
                    ) {
                        WidgetSettingsScreen(
                            onBack = { currentScreen = Screen.Settings },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // 退出时确保手电筒关闭
        if (::flashlight.isInitialized) {
            flashlight.turnOff()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 处理快捷方式打开摩斯界面（应用已在后台时）
        if (intent.getBooleanExtra("open_morse", false)) {
            pendingNavigationTarget = Screen.MorseSignal
        }
    }
}