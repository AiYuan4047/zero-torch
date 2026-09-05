package com.flashlight.toolbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.flashlight.toolbox.flashlight.FlashlightController

/**
 * 快捷方式广播接收器：处理一键开关手电筒和爆闪模式
 * 不打开应用界面，直接执行操作
 */
class QuickActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE_TORCH = "com.flashlight.toolbox.TOGGLE_TORCH"
        const val ACTION_TOGGLE_STROBE = "com.flashlight.toolbox.TOGGLE_STROBE"
        const val ACTION_OPEN_MORSE = "com.flashlight.toolbox.OPEN_MORSE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_TORCH -> {
                val controller = FlashlightController(context)
                controller.toggle()
                val isOn = FlashlightController.getGlobalMode() != FlashlightController.Mode.OFF
                val msg = if (isOn) "手电筒已开启" else "手电筒已关闭"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                updateWidgets(context)
            }
            ACTION_TOGGLE_STROBE -> {
                val controller = FlashlightController(context)
                val currentMode = FlashlightController.getGlobalMode()
                if (currentMode == FlashlightController.Mode.STROBE) {
                    controller.turnOff()
                    Toast.makeText(context, "爆闪已关闭", Toast.LENGTH_SHORT).show()
                } else {
                    controller.toggleStrobe(300)
                    Toast.makeText(context, "爆闪已开启", Toast.LENGTH_SHORT).show()
                }
                updateWidgets(context)
            }
            ACTION_OPEN_MORSE -> {
                // 打开应用并跳转到摩斯界面
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("open_morse", true)
                }
                context.startActivity(launchIntent)
            }
        }
    }

    private fun updateWidgets(context: Context) {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, FlashlightWidgetProvider::class.java)
        )
        val isOn = FlashlightController.getGlobalMode() != FlashlightController.Mode.OFF
        for (widgetId in widgetIds) {
            FlashlightWidgetProvider().updateWidget(context, appWidgetManager, widgetId, isOn)
        }
    }
}
