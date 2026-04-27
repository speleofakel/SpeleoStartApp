package com.speleo.start.presentation

import com.speleo.start.data.local.dao.AppSettingsDao
import com.speleo.start.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManager @Inject constructor(
    private val settingsDao: AppSettingsDao
) {
    companion object {
        const val KEY_START_TIMESTAMP = "competition_start_timestamp"
        const val KEY_COMPETITION_ACTIVE = "competition_active"
    }

    private val _mainTimer = MutableStateFlow(0L)
    val mainTimer: StateFlow<Long> = _mainTimer.asStateFlow()

    private val _countdown = MutableStateFlow(120)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _started = MutableStateFlow(false)
    val started: StateFlow<Boolean> = _started.asStateFlow()

    private var mainJob: Job? = null
    private var countdownJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun restoreFromSavedState() {
        CoroutineScope(Dispatchers.IO).launch {
            val startTs = settingsDao.get(KEY_START_TIMESTAMP)?.toLongOrNull() ?: 0L
            val active = settingsDao.get(KEY_COMPETITION_ACTIVE)?.toBooleanStrictOrNull() ?: false

            if (active && startTs > 0) {
                _mainTimer.value = System.currentTimeMillis() - startTs
                _started.value = true
                startMainLoop()
            }
        }
    }

    fun startCompetition() {
        CoroutineScope(Dispatchers.IO).launch {
            val timestamp = System.currentTimeMillis()
            settingsDao.put(AppSettingsEntity(KEY_START_TIMESTAMP, timestamp.toString()))
            settingsDao.put(AppSettingsEntity(KEY_COMPETITION_ACTIVE, "true"))
        }
        _started.value = true
        _mainTimer.value = 0L
        _countdown.value = 120
        startMainLoop()
        startCountdownLoop()
    }

    private fun startMainLoop() {
        mainJob?.cancel()
        val startRef = System.currentTimeMillis() - _mainTimer.value
        mainJob = scope.launch {
            while (isActive) {
                _mainTimer.value = System.currentTimeMillis() - startRef
                delay(100)
            }
        }
    }

    private fun startCountdownLoop() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive && _countdown.value > 0) {
                if (!_isPaused.value) {
                    delay(1000)
                    _countdown.value -= 1
                } else {
                    delay(100)
                }
            }
            // Автоматически перезапускаем, когда доходит до 0
            if (_countdown.value == 0 && _started.value) {
                _countdown.value = 120
                startCountdownLoop()
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun resetCountdown(seconds: Int = 120) {
        _countdown.value = seconds
        if (_started.value) {
            startCountdownLoop()
        }
    }

    fun stop() {
        mainJob?.cancel()
        countdownJob?.cancel()
        _started.value = false
        _mainTimer.value = 0L
    }
}