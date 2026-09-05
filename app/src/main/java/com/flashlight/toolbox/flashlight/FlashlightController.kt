package com.flashlight.toolbox.flashlight

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import com.flashlight.toolbox.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 手电筒控制核心：常亮 / 爆闪 / SOS / 定时开·关。
 * 所有模式互斥，切换模式时自动清理上一个模式的循环任务。
 */
class FlashlightController(context: Context) {

    enum class Mode(val displayName: String) {
        OFF("已关闭"),
        ON("已开启"),
        STROBE("爆闪中"),
        SOS("SOS 信号中"),
    }

    sealed interface TimerAction {
        data object TurnOn : TimerAction
        data object TurnOff : TimerAction
    }

    data class TimerState(
        val action: TimerAction,
        val endAtMillis: Long,
    )

    private val appContext = context.applicationContext
    private val cameraManager: CameraManager =
        appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _mode = MutableStateFlow(Mode.OFF)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _timerOn = MutableStateFlow<TimerState?>(null)
    val timerOn: StateFlow<TimerState?> = _timerOn.asStateFlow()

    private val _timerOff = MutableStateFlow<TimerState?>(null)
    val timerOff: StateFlow<TimerState?> = _timerOff.asStateFlow()

    // 兼容旧接口
    val timer: StateFlow<TimerState?> = _timerOn

    private val _torchOn = AtomicBoolean(false)
    private var blinkerJob: Job? = null
    private var timerOnJob: Job? = null
    private var timerOffJob: Job? = null

    init {
        detectCamera()
        // 同步全局状态到当前实例
        _torchOn.set(globalTorchState.get())
        _mode.value = globalMode.get()
    }

    private fun detectCamera() {
        val hasFlash = try {
            cameraManager.cameraIdList.any { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            false
        }
        _available.value = hasFlash
    }

    /** 底层真正点/灭闪光灯。 */
    fun setTorch(on: Boolean): Boolean {
        return try {
            val id = cameraManager.cameraIdList.firstOrNull { cid ->
                cameraManager.getCameraCharacteristics(cid)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cameraManager.setTorchMode(id, on)
            _torchOn.set(on)
            globalTorchState.set(on)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 主按钮：常亮开/关。爆闪/SOS 状态下点击视作关闭。 */
    fun toggle() {
        // 检查当前模式状态
        val currentMode = _mode.value
        if (currentMode == Mode.OFF) {
            turnOn()
        } else {
            turnOff()
        }
    }

    fun turnOn() {
        stopBlinker()
        if (setTorch(true)) {
            _mode.value = Mode.ON
            globalMode.set(Mode.ON)
        }
    }

    fun turnOff() {
        stopBlinker()
        setTorch(false)
        _mode.value = Mode.OFF
        globalMode.set(Mode.OFF)
        if (_timerOff.value?.action is TimerAction.TurnOff) _timerOff.value = null
        // 取消通知
        NotificationHelper.cancelNotification(appContext)
    }

    /** 爆闪开关：开启时以 intervalMs 为周期往复闪烁。 */
    fun toggleStrobe(intervalMs: Int) {
        if (_mode.value == Mode.STROBE) {
            turnOff()
            return
        }
        setTorch(false)
        _mode.value = Mode.STROBE
        globalMode.set(Mode.STROBE)
        // 显示持久通知
        NotificationHelper.showPersistentNotification(appContext, "手电筒工具箱", "爆闪中")
        // 使用全局 job 以便跨实例取消
        globalBlinkerJob = scope.launch {
            while (isActive) {
                setTorch(true)
                delay(intervalMs.coerceAtLeast(20).toLong())
                setTorch(false)
                delay(intervalMs.coerceAtLeast(20).toLong())
            }
        }
    }

    /** 爆闪开启但尚未点时，切换爆闪状态（内部状态直接开）。 */
    fun startStrobe(intervalMs: Int) {
        if (_mode.value == Mode.STROBE) return
        toggleStrobe(intervalMs)
    }

    /** SOS 莫尔斯码（... --- ...），unitMs 为基础时长。 */
    fun toggleSos() {
        if (_mode.value == Mode.SOS) {
            turnOff()
            return
        }
        setTorch(false)
        _mode.value = Mode.SOS
        globalMode.set(Mode.SOS)
        // 显示持久通知
        NotificationHelper.showPersistentNotification(appContext, "手电筒工具箱", "SOS 信号发送中")
        globalBlinkerJob = scope.launch {
            while (isActive) {
                // S: 三个点
                repeat(3) { dot(); if (!isActive) return@launch }
                // 字符间停顿：3 unit
                delay(unit * 3)
                // O: 三个划
                repeat(3) { dash(); if (!isActive) return@launch }
                delay(unit * 3)
                // S
                repeat(3) { dot(); if (!isActive) return@launch }
                // 完整一轮后的长停顿
                delay(unit * 7)
            }
        }
    }

    private suspend fun dot() {
        setTorch(true); delay(unit); setTorch(false); delay(unit)
    }

    private suspend fun dash() {
        setTorch(true); delay(unit * 3); setTorch(false); delay(unit)
    }

    /** 定时动作（定时开 / 定时关）。延迟单位毫秒。 */
    fun schedule(action: TimerAction, delayMs: Long) {
        when (action) {
            TimerAction.TurnOn -> {
                timerOnJob?.cancel()
                val end = System.currentTimeMillis() + delayMs
                _timerOn.value = TimerState(action, end)
                // 显示持久通知
                NotificationHelper.showPersistentNotification(appContext, "手电筒工具箱", "定时开启已设置")
                timerOnJob = scope.launch {
                    delay(delayMs)
                    mainHandler.post {
                        turnOn()
                        _timerOn.value = null
                        // 检查是否还有关闭任务
                        if (_timerOff.value == null) {
                            NotificationHelper.cancelNotification(appContext)
                        }
                    }
                }
            }
            TimerAction.TurnOff -> {
                timerOffJob?.cancel()
                val end = System.currentTimeMillis() + delayMs
                _timerOff.value = TimerState(action, end)
                // 显示持久通知
                NotificationHelper.showPersistentNotification(appContext, "手电筒工具箱", "定时关闭已设置")
                timerOffJob = scope.launch {
                    delay(delayMs)
                    mainHandler.post {
                        turnOff()
                        _timerOff.value = null
                        // 检查是否还有开启任务
                        if (_timerOn.value == null) {
                            NotificationHelper.cancelNotification(appContext)
                        }
                    }
                }
            }
        }
    }

    fun cancelTimer(action: TimerAction? = null) {
        when (action) {
            TimerAction.TurnOn -> {
                timerOnJob?.cancel()
                timerOnJob = null
                _timerOn.value = null
            }
            TimerAction.TurnOff -> {
                timerOffJob?.cancel()
                timerOffJob = null
                _timerOff.value = null
            }
            null -> {
                // 取消所有定时任务
                timerOnJob?.cancel()
                timerOnJob = null
                _timerOn.value = null
                timerOffJob?.cancel()
                timerOffJob = null
                _timerOff.value = null
            }
        }
        // 如果没有定时任务了，取消通知
        if (_timerOn.value == null && _timerOff.value == null) {
            NotificationHelper.cancelNotification(appContext)
        }
    }

    private fun stopBlinker() {
        blinkerJob?.cancel()
        blinkerJob = null
        globalBlinkerJob?.cancel()
        globalBlinkerJob = null
    }

    companion object {
        private const val unit = 150L
        
        // 全局手电筒状态（跨实例共享）
        private val globalTorchState = AtomicBoolean(false)
        private val globalMode = AtomicReference<Mode>(Mode.OFF)
        private var globalBlinkerJob: Job? = null
        
        fun getGlobalMode(): Mode = globalMode.get()
        fun getGlobalTorchState(): Boolean = globalTorchState.get()
    }
}