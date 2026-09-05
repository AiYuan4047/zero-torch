package com.flashlight.toolbox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import com.flashlight.toolbox.flashlight.FlashlightController
import com.flashlight.toolbox.util.MorseCode
import com.flashlight.toolbox.util.MorseSymbol
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseSignalScreen(
    controller: FlashlightController,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // 统一预设列表（可编辑、可删除，包含原内置预设）
    // 使用不可变列表，确保每次修改都触发 Compose 重组
    var presets by remember {
        mutableStateOf(
            MorseCode.BUILTIN_PRESETS.map { it.copy() }
        )
    }

    // 手动输入框文本
    var manualInput by remember { mutableStateOf("") }

    // 当前选中的预设索引（-1表示手动输入模式）
    var selectedPresetIndex by remember { mutableStateOf(0) }

    // 发送状态
    var isSending by remember { mutableStateOf(false) }
    var sendProgress by remember { mutableStateOf(0) }
    var totalSymbols by remember { mutableStateOf(0) }

    // 添加/编辑预设对话框状态
    var showPresetDialog by remember { mutableStateOf(false) }
    var editingPresetIndex by remember { mutableStateOf(-1) } // -1 表示新增
    var dialogName by remember { mutableStateOf("") }
    var dialogText by remember { mutableStateOf("") }
    var dialogDesc by remember { mutableStateOf("") }

    // 查看详情弹窗状态
    var showDetailDialog by remember { mutableStateOf(false) }
    var detailPresetIndex by remember { mutableStateOf(-1) }

    // 删除确认弹窗状态
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletePresetIndex by remember { mutableStateOf(-1) }

    // 返回键处理
    BackHandler(enabled = !isSending) {
        onBack()
    }

    // 发送摩斯码
    fun sendMorse(symbols: List<MorseSymbol>, unitMs: Int = 150) {
        if (isSending) return
        isSending = true
        totalSymbols = symbols.size
        sendProgress = 0

        scope.launch {
            controller.turnOff()
            delay(100)

            symbols.forEachIndexed { index, symbol ->
                if (!isSending) return@launch
                sendProgress = index + 1

                when (symbol) {
                    is MorseSymbol.Dot -> {
                        controller.setTorch(true)
                        delay(unitMs.toLong())
                        controller.setTorch(false)
                        delay(unitMs.toLong())
                    }
                    is MorseSymbol.Dash -> {
                        controller.setTorch(true)
                        delay(unitMs * 3L)
                        controller.setTorch(false)
                        delay(unitMs.toLong())
                    }
                    is MorseSymbol.Gap -> {
                        delay(unitMs.toLong())
                    }
                    is MorseSymbol.CharGap -> {
                        delay(unitMs * 3L)
                    }
                    is MorseSymbol.WordGap -> {
                        delay(unitMs * 7L)
                    }
                }
            }

            controller.setTorch(false)
            isSending = false
            sendProgress = 0
        }
    }

    // 停止发送
    val stopSending = {
        isSending = false
        controller.setTorch(false)
        Unit
    }

    // 获取当前要发送的符号序列
    val currentSymbols: List<MorseSymbol> = if (selectedPresetIndex == -1) {
        MorseCode.encode(manualInput)
    } else if (selectedPresetIndex in presets.indices) {
        presets[selectedPresetIndex].symbols
    } else {
        emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text("发送摩斯信号", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = { if (!isSending) onBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        ) {
            // —— 预设标题栏 ——
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "预设",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        editingPresetIndex = -1
                        dialogName = ""
                        dialogText = ""
                        dialogDesc = ""
                        showPresetDialog = true
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加预设",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // —— 预设网格（一排两个） ——
            itemsIndexed(
                items = presets,
                key = { index, preset -> preset.name + index },
            ) { index, preset ->
                PresetGridItem(
                    preset = preset,
                    isSelected = selectedPresetIndex == index && selectedPresetIndex != -1,
                    onClick = {
                        selectedPresetIndex = index
                        manualInput = ""
                        focusManager.clearFocus()
                    },
                    onViewDetail = {
                        detailPresetIndex = index
                        showDetailDialog = true
                    },
                )
            }

            // —— 手动输入区域 ——
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "手动输入",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        val textFieldFocusRequester = remember { FocusRequester() }

                        TextField(
                            value = manualInput,
                            onValueChange = { manualInput = it; selectedPresetIndex = -1 },
                            label = { Text("输入英文单词或句子") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(textFieldFocusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        selectedPresetIndex = -1
                                    }
                                },
                            singleLine = false,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        )
                        Spacer(Modifier.height(8.dp))

                        // 显示编码预览
                        if (manualInput.isNotBlank()) {
                            val previewSymbols = MorseCode.encode(manualInput)
                            Text(
                                "编码预览: ${formatMorsePreview(previewSymbols)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                            )
                        }
                    }
                }
            }

            // —— 发送进度指示 ——
            if (isSending) {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "正在发送... $sendProgress / $totalSymbols",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { sendProgress.toFloat() / totalSymbols },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = stopSending) {
                                Text(
                                    "停止发送",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        // —— 底部发送/停止按钮 ——
        if (!isSending) {
            Button(
                onClick = { sendMorse(currentSymbols) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .height(56.dp),
                enabled = currentSymbols.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("开始发送", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 停止按钮（发送时显示）
        if (isSending) {
            Button(
                onClick = stopSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("停止发送", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // 添加/编辑预设对话框
    if (showPresetDialog) {
        PresetEditDialog(
            isEdit = editingPresetIndex >= 0,
            onDismiss = { showPresetDialog = false },
            onConfirm = { name, text, desc ->
                val newPreset = MorseCode.PresetMessage(name, text, desc)
                if (editingPresetIndex >= 0) {
                    // 编辑现有预设
                    presets = presets.toMutableList().apply { set(editingPresetIndex, newPreset) }
                } else {
                    // 添加新预设
                    presets = presets + newPreset
                }
                showPresetDialog = false
            },
            name = dialogName,
            onNameChange = { dialogName = it },
            text = dialogText,
            onTextChange = { dialogText = it },
            desc = dialogDesc,
            onDescChange = { dialogDesc = it },
        )
    }

    // 查看详情弹窗
    if (showDetailDialog && detailPresetIndex in presets.indices) {
        PresetDetailDialog(
            preset = presets[detailPresetIndex],
            onDismiss = { showDetailDialog = false },
            onEdit = {
                showDetailDialog = false
                editingPresetIndex = detailPresetIndex
                dialogName = presets[detailPresetIndex].name
                dialogText = presets[detailPresetIndex].text
                dialogDesc = presets[detailPresetIndex].description
                showPresetDialog = true
            },
            onDelete = {
                deletePresetIndex = detailPresetIndex
                showDeleteConfirm = true
            },
        )
    }

    // 删除确认弹窗（二级确认）
    if (showDeleteConfirm && deletePresetIndex in presets.indices) {
        DeleteConfirmDialog(
            presetName = presets[deletePresetIndex].name,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                presets = presets.toMutableList().apply { removeAt(deletePresetIndex) }
                if (selectedPresetIndex >= presets.size) {
                    selectedPresetIndex = if (presets.isEmpty()) -1 else 0
                }
                showDeleteConfirm = false
                showDetailDialog = false
            },
        )
    }
}

@Composable
private fun PresetGridItem(
    preset: MorseCode.PresetMessage,
    isSelected: Boolean,
    onClick: () -> Unit,
    onViewDetail: () -> Unit,
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // 顶部：名称 + 查看详情按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onViewDetail,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "查看详情",
                        tint = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // 中部：描述
            if (preset.description.isNotBlank()) {
                Text(
                    preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 底部：摩斯码预览
            Text(
                formatMorsePreview(preset.symbols),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PresetEditDialog(
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    text: String,
    onTextChange: (String) -> Unit,
    desc: String,
    onDescChange: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑预设" else "添加预设") },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("预设名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text("英文单词/句子") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = desc,
                    onValueChange = onDescChange,
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && text.isNotBlank()) {
                        onConfirm(name, text, desc)
                    }
                },
                enabled = name.isNotBlank() && text.isNotBlank(),
            ) { Text(if (isEdit) "保存" else "添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun formatMorsePreview(symbols: List<MorseSymbol>): String {
    val sb = StringBuilder()
    symbols.forEach { symbol ->
        when (symbol) {
            is MorseSymbol.Dot -> sb.append("·")
            is MorseSymbol.Dash -> sb.append("—")
            is MorseSymbol.Gap -> sb.append(" ")
            is MorseSymbol.CharGap -> sb.append("  ")
            is MorseSymbol.WordGap -> sb.append("   ")
        }
    }
    return sb.toString().trim()
}

/**
 * 格式化摩斯编码为详细字符串（使用标准 . 和 - 符号）
 */
private fun formatMorseDetail(symbols: List<MorseSymbol>): String {
    val sb = StringBuilder()
    symbols.forEach { symbol ->
        when (symbol) {
            is MorseSymbol.Dot -> sb.append(".")
            is MorseSymbol.Dash -> sb.append("-")
            is MorseSymbol.Gap -> sb.append(" ")
            is MorseSymbol.CharGap -> sb.append(" / ")
            is MorseSymbol.WordGap -> sb.append(" | ")
        }
    }
    return sb.toString().trim()
}

data class MorseStats(
    val dots: Int,
    val dashes: Int,
    val gaps: Int,
    val charGaps: Int,
    val wordGaps: Int
)

/**
 * 统计摩斯编码中各类符号数量
 */
private fun getMorseStats(symbols: List<MorseSymbol>): MorseStats {
    var dots = 0
    var dashes = 0
    var gaps = 0
    var charGaps = 0
    var wordGaps = 0
    symbols.forEach { symbol ->
        when (symbol) {
            is MorseSymbol.Dot -> dots++
            is MorseSymbol.Dash -> dashes++
            is MorseSymbol.Gap -> gaps++
            is MorseSymbol.CharGap -> charGaps++
            is MorseSymbol.WordGap -> wordGaps++
        }
    }
    return MorseStats(dots, dashes, gaps, charGaps, wordGaps)
}

@Composable
private fun PresetDetailDialog(
    preset: MorseCode.PresetMessage,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("预设详情") },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    "名称",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    preset.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                Text(
                    "发送内容",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    preset.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                Text(
                    "描述",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    preset.description.ifBlank { "无" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                Text(
                    "摩斯编码",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    formatMorseDetail(preset.symbols),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )

                Text(
                    "编码统计",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val stats = getMorseStats(preset.symbols)
                Text(
                    "点: ${stats.dots}  划: ${stats.dashes}  符号间隔: ${stats.gaps}  字符间隔: ${stats.charGaps}  单词间隔: ${stats.wordGaps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    "总符号数: ${preset.symbols.size}  预计发送时间: ${preset.symbols.size * 150}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onEdit) {
                    Text("编辑")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
private fun DeleteConfirmDialog(
    presetName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = {
            Text(
                "确定要删除预设「${presetName}」吗？删除后无法恢复。",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
