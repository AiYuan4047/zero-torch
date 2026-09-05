package com.flashlight.toolbox

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.flashlight.toolbox.data.WidgetConfigStore
import com.flashlight.toolbox.flashlight.FlashlightController

class FlashlightWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.flashlight.toolbox.WIDGET_TOGGLE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, FlashlightWidgetProvider::class.java)
            )

            // 乐观更新：先获取当前状态并立即切换显示
            val controller = FlashlightController(context)
            val wasOn = controller.mode.value != FlashlightController.Mode.OFF
            val newOn = !wasOn

            // 立即更新 UI（乐观更新）
            for (widgetId in widgetIds) {
                updateWidget(context, appWidgetManager, widgetId, newOn)
            }

            // 在后台执行实际的手电筒切换
            Thread {
                controller.toggle()
            }.start()
        }
    }

    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        isOn: Boolean = false
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_flashlight)

        // 加载配置
        val configStore = WidgetConfigStore(context)
        val config = configStore.loadConfig()

        // 设置点击事件
        val intent = Intent(context, FlashlightWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_icon, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_status, pendingIntent)

        // 应用配置 - 文字颜色
        views.setTextColor(R.id.widget_status, config.textColor)

        // 根据状态切换图标和文字
        if (isOn) {
            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_flashlight_on)
            views.setTextViewText(R.id.widget_status, config.onText)
        } else {
            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_flashlight)
            views.setTextViewText(R.id.widget_status, config.offText)
        }

        // 背景颜色 + 透明度
        val bgAlpha = (config.backgroundOpacity * 255 / 100).coerceIn(0, 255)
        val bgColor = Color.argb(bgAlpha, Color.red(config.backgroundColor), Color.green(config.backgroundColor), Color.blue(config.backgroundColor))
        views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
