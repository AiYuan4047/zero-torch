package com.flashlight.toolbox.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 小组件配置存储
 * 使用 SharedPreferences 保存用户自定义的小组件外观设置
 */
class WidgetConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("widget_config", Context.MODE_PRIVATE)

    data class WidgetConfig(
        val iconColor: Int = Color.White.toArgb(),
        val textColor: Int = Color.White.toArgb(),
        val backgroundOpacity: Int = 100, // 0-100
        val cornerRadius: Int = 16, // dp
        val backgroundColor: Int = Color(0xFF1A1A2E).toArgb(),
        val onText: String = "已开启",
        val offText: String = "已关闭",
    )

    fun saveConfig(config: WidgetConfig) {
        prefs.edit()
            .putInt(KEY_ICON_COLOR, config.iconColor)
            .putInt(KEY_TEXT_COLOR, config.textColor)
            .putInt(KEY_BACKGROUND_OPACITY, config.backgroundOpacity)
            .putInt(KEY_CORNER_RADIUS, config.cornerRadius)
            .putInt(KEY_BACKGROUND_COLOR, config.backgroundColor)
            .putString(KEY_ON_TEXT, config.onText)
            .putString(KEY_OFF_TEXT, config.offText)
            .apply()
    }

    fun loadConfig(): WidgetConfig {
        return WidgetConfig(
            iconColor = prefs.getInt(KEY_ICON_COLOR, Color.White.toArgb()),
            textColor = prefs.getInt(KEY_TEXT_COLOR, Color.White.toArgb()),
            backgroundOpacity = prefs.getInt(KEY_BACKGROUND_OPACITY, 100),
            cornerRadius = prefs.getInt(KEY_CORNER_RADIUS, 16),
            backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color(0xFF1A1A2E).toArgb()),
            onText = prefs.getString(KEY_ON_TEXT, "已开启") ?: "已开启",
            offText = prefs.getString(KEY_OFF_TEXT, "已关闭") ?: "已关闭",
        )
    }

    fun resetToDefaults() {
        saveConfig(WidgetConfig())
    }

    companion object {
        private const val KEY_ICON_COLOR = "icon_color"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_BACKGROUND_OPACITY = "background_opacity"
        private const val KEY_CORNER_RADIUS = "corner_radius"
        private const val KEY_BACKGROUND_COLOR = "background_color"
        private const val KEY_ON_TEXT = "on_text"
        private const val KEY_OFF_TEXT = "off_text"
    }
}
