package com.flashlight.toolbox.ui.screens

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashlight.toolbox.FlashlightWidgetProvider
import com.flashlight.toolbox.data.WidgetConfigStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val configStore = remember { WidgetConfigStore(context) }
    val currentConfig = remember { configStore.loadConfig() }

    var iconColor by remember { mutableIntStateOf(currentConfig.iconColor) }
    var textColor by remember { mutableIntStateOf(currentConfig.textColor) }
    var backgroundOpacity by remember { mutableFloatStateOf(currentConfig.backgroundOpacity.toFloat()) }
    var cornerRadius by remember { mutableFloatStateOf(currentConfig.cornerRadius.toFloat()) }
    var backgroundColor by remember { mutableIntStateOf(currentConfig.backgroundColor) }
    var onText by remember { mutableStateOf(currentConfig.onText) }
    var offText by remember { mutableStateOf(currentConfig.offText) }
    var showSavedToast by remember { mutableIntStateOf(0) }

    // 预设颜色
    val presetColors = listOf(
        Color.White,
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF1A1A2E), // Dark
        Color(0xFF37474F), // Blue Grey
    )

    // 返回键处理
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text("小组件设置", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // —— 预览区域 ——
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("预览", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))

                    // 模拟小组件预览 - 使用 1:1 比例
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(cornerRadius.dp))
                            .background(
                                Color(backgroundColor).copy(alpha = backgroundOpacity / 100f)
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Default.FlashlightOn,
                                contentDescription = null,
                                tint = Color(iconColor),
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                offText,
                                color = Color(textColor),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // —— 图标颜色 ——
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("图标颜色", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        presetColors.forEach { color ->
                            ColorItem(
                                color = color,
                                isSelected = iconColor == color.toArgb(),
                                onClick = { iconColor = color.toArgb() }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // —— 文字颜色 ——
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("文字颜色", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        presetColors.forEach { color ->
                            ColorItem(
                                color = color,
                                isSelected = textColor == color.toArgb(),
                                onClick = { textColor = color.toArgb() }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // —— 背景颜色 ——
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("背景颜色", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        presetColors.forEach { color ->
                            ColorItem(
                                color = color,
                                isSelected = backgroundColor == color.toArgb(),
                                onClick = { backgroundColor = color.toArgb() }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // —— 背景透明度 ——
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("背景透明度", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${backgroundOpacity.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = backgroundOpacity,
                        onValueChange = { backgroundOpacity = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // —— 圆角半径 ——
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("圆角半径", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${cornerRadius.toInt()} dp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = cornerRadius,
                        onValueChange = { cornerRadius = it },
                        valueRange = 0f..32f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // —— 自定义文字 ——
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("自定义文字", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("开启文字", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = onText,
                                onValueChange = { onText = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("关闭文字", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = offText,
                                onValueChange = { offText = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // —— 恢复默认 ——
            OutlinedButton(
                onClick = {
                    configStore.resetToDefaults()
                    val defaults = configStore.loadConfig()
                    iconColor = defaults.iconColor
                    textColor = defaults.textColor
                    backgroundOpacity = defaults.backgroundOpacity.toFloat()
                    cornerRadius = defaults.cornerRadius.toFloat()
                    backgroundColor = defaults.backgroundColor
                    onText = defaults.onText
                    offText = defaults.offText
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("恢复默认设置")
            }
            Spacer(Modifier.height(12.dp))

            // —— 保存按钮 ——
            Button(
                onClick = {
                    val config = WidgetConfigStore.WidgetConfig(
                        iconColor = iconColor,
                        textColor = textColor,
                        backgroundOpacity = backgroundOpacity.toInt(),
                        cornerRadius = cornerRadius.toInt(),
                        backgroundColor = backgroundColor,
                        onText = onText,
                        offText = offText,
                    )
                    configStore.saveConfig(config)
                    // 刷新已添加的小组件
                    refreshWidgets(context)
                    showSavedToast++
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("保存设置")
            }
            Spacer(Modifier.height(12.dp))

            // —— 添加小组件按钮 ——
            Button(
                onClick = {
                    // 先保存配置
                    val config = WidgetConfigStore.WidgetConfig(
                        iconColor = iconColor,
                        textColor = textColor,
                        backgroundOpacity = backgroundOpacity.toInt(),
                        cornerRadius = cornerRadius.toInt(),
                        backgroundColor = backgroundColor,
                        onText = onText,
                        offText = offText,
                    )
                    configStore.saveConfig(config)
                    // 然后添加小组件
                    requestPinAppWidget(context)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加小组件到桌面")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // 保存成功提示
    if (showSavedToast > 0) {
        LaunchedEffect(showSavedToast) {
            android.widget.Toast.makeText(context, "设置已保存", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

private fun requestPinAppWidget(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val myWidget = ComponentName(context, FlashlightWidgetProvider::class.java)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val successCallback = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, FlashlightWidgetProvider::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        appWidgetManager.requestPinAppWidget(myWidget, null, successCallback)
        android.widget.Toast.makeText(context, "小组件添加成功", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun refreshWidgets(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val widgetIds = appWidgetManager.getAppWidgetIds(
        ComponentName(context, FlashlightWidgetProvider::class.java)
    )
    val isOn = com.flashlight.toolbox.flashlight.FlashlightController.getGlobalTorchState()
    for (widgetId in widgetIds) {
        FlashlightWidgetProvider().updateWidget(context, appWidgetManager, widgetId, isOn)
    }
}
