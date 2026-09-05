package com.flashlight.toolbox

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.flashlight.toolbox.flashlight.FlashlightController

/**
 * 快捷方式处理 Activity
 * 接收快捷方式点击，执行对应操作后立即关闭
 */
class ShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val controller = FlashlightController(this)

        when (action) {
            "com.flashlight.toolbox.TOGGLE_TORCH" -> {
                controller.toggle()
                val isOn = FlashlightController.getGlobalTorchState()
                val msg = if (isOn) "手电筒已开启" else "手电筒已关闭"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            "com.flashlight.toolbox.TOGGLE_STROBE" -> {
                // 使用 toggleStrobe 统一处理开关
                controller.toggleStrobe(300)
                val isStrobeOn = FlashlightController.getGlobalMode() == FlashlightController.Mode.STROBE
                val msg = if (isStrobeOn) "爆闪已开启" else "爆闪已关闭"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            "com.flashlight.toolbox.OPEN_MORSE" -> {
                // 打开主界面并跳转到摩斯界面
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("open_morse", true)
                }
                startActivity(mainIntent)
            }
        }

        // 更新小组件状态
        updateWidgets()

        // 立即关闭 Activity
        finish()
    }

    private fun updateWidgets() {
        // 通知小组件更新
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(this, FlashlightWidgetProvider::class.java)
        )
        val isOn = FlashlightController.getGlobalTorchState()
        for (widgetId in widgetIds) {
            FlashlightWidgetProvider().updateWidget(this, appWidgetManager, widgetId, isOn)
        }
    }
}
