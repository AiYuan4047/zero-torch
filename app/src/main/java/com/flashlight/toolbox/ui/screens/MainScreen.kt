package com.flashlight.toolbox.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashlight.toolbox.data.AppSettings
import com.flashlight.toolbox.flashlight.FlashlightController
import com.flashlight.toolbox.ui.theme.FlashOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    controller: FlashlightController,
    settingsFlow: Flow<AppSettings>,
    onOpenSettings: () -> Unit,
    onOpenMorse: () -> Unit,
) {
    val mode by controller.mode.collectAsStateWithLifecycle()
    val available by controller.available.collectAsStateWithLifecycle()
    val timerOn by controller.timerOn.collectAsStateWithLifecycle()
    val timerOff by controller.timerOff.collectAsStateWithLifecycle()
    // 兼容旧接口
    val timer: FlashlightController.TimerState? = timerOn

    var settings by remember { mutableStateOf(AppSettings()) }
    LaunchedEffect(Unit) {
        settingsFlow.collectLatest { settings = it }
    }

    var showTimerDialog by remember { mutableStateOf<TimerDialogKind?>(null) }
    var showCancelDialog by remember { mutableStateOf<CancelDialogKind?>(null) }

    // 爆闪状态
    var strobeMs by remember { mutableStateOf(settings.strobeIntervalMs) }
    LaunchedEffect(settings.strobeIntervalMs) { strobeMs = settings.strobeIntervalMs }
    val strobeActive = mode == FlashlightController.Mode.STROBE

    // 每 500ms 刷新倒计时
    var nowTick by remember { mutableStateOf(0L) }
    LaunchedEffect(timer != null) {
        while (timer != null) {
            nowTick = System.currentTimeMillis()
            kotlinx.coroutines.delay(500)
        }
    }

    // 申请相机权限
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text("手电筒工具箱", fontWeight = FontWeight.SemiBold) },
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(4.dp))

            // —— 大开关 ——
            BigPowerButton(
                isOn = mode != FlashlightController.Mode.OFF,
                enabled = available,
                onClick = { controller.toggle() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    !available -> "设备不支持闪光灯"
                    else -> mode.displayName
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (mode != FlashlightController.Mode.OFF && available)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            // —— 定时开 / 定时关 ——
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimerButton(
                    modifier = Modifier.weight(1f),
                    label = "定时打开",
                    active = timerOn != null,
                    countdownMs = remainingMs(timerOn, nowTick),
                    onClick = {
                        if (timerOn != null) {
                            // 点击已激活的定时开启 → 确认取消
                            showCancelDialog = CancelDialogKind.TurnOn
                        } else {
                            showTimerDialog = TimerDialogKind.TurnOn
                        }
                    },
                )
                TimerButton(
                    modifier = Modifier.weight(1f),
                    label = "定时关闭",
                    active = timerOff != null,
                    countdownMs = remainingMs(timerOff, nowTick),
                    onClick = {
                        if (timerOff != null) {
                            // 点击已激活的定时关闭 → 确认取消
                            showCancelDialog = CancelDialogKind.TurnOff
                        } else {
                            showTimerDialog = TimerDialogKind.TurnOff
                        }
                    },
                )
            }
            // 取消提示
            if (timerOn != null || timerOff != null) {
                Text(
                    "点击卡片可取消任务",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            // —— 爆闪延迟滑条 ——
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "爆闪延迟",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${strobeMs} ms",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = strobeMs.toFloat(),
                        onValueChange = { strobeMs = it.roundToInt().coerceIn(20, 2000) },
                        onValueChangeFinished = {
                            if (strobeActive) controller.toggleStrobe(strobeMs)
                        },
                        valueRange = 20f..2000f,
                    )
                    Text(
                        "每次闪烁的间隔时长，可在设置中选择预设",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { controller.toggleStrobe(strobeMs) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (strobeActive)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (strobeActive)
                                MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Text(if (strobeActive) "停止爆闪" else "开启爆闪")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // —— 发送摩斯信号（底部） ——
            SosButton(
                onClick = onOpenMorse,
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    // —— 定时对话框 ——
    showTimerDialog?.let { kind ->
        val title = if (kind == TimerDialogKind.TurnOn) "定时打开" else "定时关闭"
        TimerPickerDialog(
            title = title,
            onConfirm = { ms ->
                controller.schedule(
                    if (kind == TimerDialogKind.TurnOn) FlashlightController.TimerAction.TurnOn
                    else FlashlightController.TimerAction.TurnOff,
                    ms,
                )
                showTimerDialog = null
            },
            onDismiss = { showTimerDialog = null },
        )
    }

    // —— 取消确认对话框 ——
    showCancelDialog?.let { kind ->
        AlertDialog(
            onDismissRequest = { showCancelDialog = null },
            title = { Text("确认取消") },
            text = {
                Text(
                    if (kind == CancelDialogKind.TurnOn) "确定要取消定时开启任务吗？"
                    else "确定要取消定时关闭任务吗？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    controller.cancelTimer(
                        if (kind == CancelDialogKind.TurnOn) FlashlightController.TimerAction.TurnOn
                        else FlashlightController.TimerAction.TurnOff
                    )
                    showCancelDialog = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = null }) { Text("取消") }
            },
        )
    }
}

private enum class TimerDialogKind { TurnOn, TurnOff }
private enum class CancelDialogKind { TurnOn, TurnOff }

private fun remainingMs(
    timer: FlashlightController.TimerState?,
    now: Long,
): Long {
    if (timer == null) return 0
    return (timer.endAtMillis - now).coerceAtLeast(0)
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> "%d:%02d:%02d".format(h, m, s)
        m > 0 -> "%d:%02d".format(m, s)
        else -> "${s}s"
    }
}

@Composable
private fun BigPowerButton(
    isOn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(targetValue = if (isOn) 1f else 0.96f, label = "powerScale")
    val color by animateColorAsState(
        targetValue = if (isOn) FlashOn else MaterialTheme.colorScheme.primary,
        label = "powerColor",
    )
    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.4f)
            .fillMaxSize()
            .clickable(enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color, CircleShape)
                .border(6.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isOn) "关" else "开",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOn) MaterialTheme.colorScheme.primary else Color.White,
                )
                Text(
                    text = "手电筒",
                    fontSize = 14.sp,
                    color = if (isOn) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun TimerButton(
    modifier: Modifier = Modifier,
    label: String,
    active: Boolean,
    countdownMs: Long,
    onClick: () -> Unit,
) {
    val container = if (active) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (active) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Timer, contentDescription = label, tint = content)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = content)
            if (active) {
                Text(
                    formatDuration(countdownMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SosButton(
    onClick: () -> Unit,
) {
    val container = MaterialTheme.colorScheme.primary
    val content = MaterialTheme.colorScheme.onPrimary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Send, contentDescription = null, tint = content)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "发送摩斯信号",
                style = MaterialTheme.typography.titleMedium,
                color = content,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TimerPickerDialog(
    title: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var h by remember { mutableIntStateOf(0) }
    var m by remember { mutableIntStateOf(1) }
    var s by remember { mutableIntStateOf(0) }
    val ms = ((h * 3600L + m * 60L + s) * 1000L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 小时行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("小时", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp))
                    IconButton(onClick = { h = (h - 1).coerceIn(0, 23) }, modifier = Modifier.size(28.dp)) {
                        Text("−", fontSize = 14.sp)
                    }
                    Slider(
                        value = h.toFloat(),
                        onValueChange = { h = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    IconButton(onClick = { h = (h + 1).coerceIn(0, 23) }, modifier = Modifier.size(28.dp)) {
                        Text("+", fontSize = 14.sp)
                    }
                    Text("%02d".format(h), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp))
                }
                Spacer(Modifier.height(4.dp))
                // 分钟行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("分钟", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp))
                    IconButton(onClick = { m = (m - 1).coerceIn(0, 59) }, modifier = Modifier.size(28.dp)) {
                        Text("−", fontSize = 14.sp)
                    }
                    Slider(
                        value = m.toFloat(),
                        onValueChange = { m = it.toInt() },
                        valueRange = 0f..59f,
                        steps = 58,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    IconButton(onClick = { m = (m + 1).coerceIn(0, 59) }, modifier = Modifier.size(28.dp)) {
                        Text("+", fontSize = 14.sp)
                    }
                    Text("%02d".format(m), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp))
                }
                Spacer(Modifier.height(4.dp))
                // 秒行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("秒", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp))
                    IconButton(onClick = { s = (s - 1).coerceIn(0, 59) }, modifier = Modifier.size(28.dp)) {
                        Text("−", fontSize = 14.sp)
                    }
                    Slider(
                        value = s.toFloat(),
                        onValueChange = { s = it.toInt() },
                        valueRange = 0f..59f,
                        steps = 58,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    IconButton(onClick = { s = (s + 1).coerceIn(0, 59) }, modifier = Modifier.size(28.dp)) {
                        Text("+", fontSize = 14.sp)
                    }
                    Text("%02d".format(s), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "将在 ${formatDuration(ms)} 后执行",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(ms) }, enabled = ms > 0) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
