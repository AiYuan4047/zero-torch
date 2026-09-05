package com.flashlight.toolbox.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

// 默认主题色（橘黄色）
val DefaultThemeColor = Color(0xFFB26A00)

// 动态取色标识
val DynamicColor = Color(-1)

// Functional colors
val FlashOn = Color(0xFFFFF9E0)
val StrobeAccent = Color(0xFFFFE082)

// 动态生成主题色
private fun generateLightColors(primary: Color): androidx.compose.material3.ColorScheme {
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.15f).compositeOver(Color.White),
        onPrimaryContainer = primary.copy(alpha = 0.8f).compositeOver(Color.Black),
        secondary = primary.copy(alpha = 0.7f).compositeOver(Color(0xFF715A40)),
        onSecondary = Color.White,
        secondaryContainer = primary.copy(alpha = 0.1f).compositeOver(Color.White),
        onSecondaryContainer = primary.copy(alpha = 0.7f).compositeOver(Color.Black),
        background = Color(0xFFFFF8F4),
        onBackground = Color(0xFF221A10),
        surface = Color(0xFFFFF8F4),
        onSurface = Color(0xFF221A10),
        surfaceVariant = Color(0xFFDED5CC),
        onSurfaceVariant = Color(0xFF51443A),
        outline = Color(0xFF847468),
        outlineVariant = Color(0xFFD7C3B0),
    )
}

private fun generateDarkColors(primary: Color): androidx.compose.material3.ColorScheme {
    return darkColorScheme(
        primary = primary.copy(alpha = 0.85f).compositeOver(Color(0xFFFFB86B)),
        onPrimary = Color(0xFF5B3000),
        primaryContainer = primary.copy(alpha = 0.6f).compositeOver(Color(0xFF7F4700)),
        onPrimaryContainer = Color(0xFFFFDCBD),
        secondary = primary.copy(alpha = 0.6f).compositeOver(Color(0xFFE0C1A1)),
        onSecondary = Color(0xFF3F2C16),
        secondaryContainer = primary.copy(alpha = 0.4f).compositeOver(Color(0xFF57422A)),
        onSecondaryContainer = Color(0xFFFDDDBB),
        background = Color(0xFF1A120B),
        onBackground = Color(0xFFF0DFCE),
        surface = Color(0xFF1A120B),
        onSurface = Color(0xFFF0DFCE),
        surfaceVariant = Color(0xFF3A332B),
        onSurfaceVariant = Color(0xFFD7C3B0),
        outline = Color(0xFF9F8E80),
        outlineVariant = Color(0xFF51443A),
    )
}

// 颜色混合辅助函数
private fun Color.compositeOver(background: Color): Color {
    val bgA = background.alpha
    val fgA = this.alpha
    val a = fgA + bgA * (1f - fgA)
    val r = (this.red * fgA + background.red * bgA * (1f - fgA)) / a
    val g = (this.green * fgA + background.green * bgA * (1f - fgA)) / a
    val b = (this.blue * fgA + background.blue * bgA * (1f - fgA)) / a
    return Color(r, g, b, a)
}

// 从壁纸提取动态颜色（Android 12+）
fun extractDynamicColor(context: Context): Color {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        try {
            val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            if (colors != null) {
                // 使用主要颜色
                val primaryColor = colors.primaryColor.toArgb()
                if (primaryColor != android.graphics.Color.TRANSPARENT) {
                    return Color(primaryColor)
                }
            }
        } catch (e: Exception) {
            // 权限问题或无法获取
        }
    }
    return DefaultThemeColor
}

@Composable
fun FlashlightToolboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resolvedColor = if (themeColor == DynamicColor) {
        extractDynamicColor(context)
    } else {
        themeColor
    }
    val colors = if (darkTheme) generateDarkColors(resolvedColor) else generateLightColors(resolvedColor)
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
