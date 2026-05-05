package com.speleo.start.presentation

import com.speleo.start.data.local.dao.AppSettingsDao
import com.speleo.start.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManager @Inject constructor(
    private val settingsDao: AppSettingsDao
) {
    companion object {
        const val KEY_START_TIMESTAMP = "competition_start_timestamp"
        const val KEY_COMPETITION_ACTIVE = "competition_active"
        const val KEY_COUNTDOWN_VALUE = "countdown_current_value"
        const val KEY_COUNTDOWN_PAUSED = "countdown_paused"
        const val KEY_FIRST_TEAM_STARTED = "first_team_started"
        const val KEY_CURRENT_INTERVAL = "countdown_interval"
        const val DEFAULT_START_INTERVAL = 60
        const val FIRST_START_COUNTDOWN = 10
    }

    private val _mainTimer = MutableStateFlow(0L)
    val mainTimer: StateFlow<Long> = _mainTimer.asStateFlow()

    private val _countdown = MutableStateFlow(DEFAULT_START_INTERVAL)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    // Пауза ТОЛЬКО для обратного отсчёта. Главный таймер не имеет паузы.
    private val _countdownPaused = MutableStateFlow(false)
    val countdownPaused: StateFlow<Boolean> = _countdownPaused.asStateFlow()

    private val _started = MutableStateFlow(false)
    val started: StateFlow<Boolean> = _started.asStateFlow()

    private val _isFirstStart = MutableStateFlow(true)
    val isFirstStart: StateFlow<Boolean> = _isFirstStart.asStateFlow()

    private var mainJob: Job? = null
    private var countdownJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun restoreFromSavedState() {
        CoroutineScope(Dispatchers.IO).launch {
            val startTs = settingsDao.get(KEY_START_TIMESTAMP)?.toLongOrNull() ?: 0L
            val active = settingsDao.get(KEY_COMPETITION_ACTIVE)?.toBooleanStrictOrNull() ?: false
            val savedCountdown = settingsDao.get(KEY_COUNTDOWN_VALUE)?.toIntOrNull()
            val wasPaused = settingsDao.get(KEY_COUNTDOWN_PAUSED)?.toBooleanStrictOrNull() ?: false
            val firstStarted = settingsDao.get(KEY_FIRST_TEAM_STARTED)?.toBooleanStrictOrNull() ?: false
            val savedInterval = settingsDao.get(KEY_CURRENT_INTERVAL)?.toIntOrNull() ?: DEFAULT_START_INTERVAL

            if (active && startTs > 0) {
                _mainTimer.value = System.currentTimeMillis() - startTs
                _started.value = true
                _isFirstStart.value = !firstStarted
                _countdown.value = savedCountdown ?: savedInterval
                _countdownPaused.value = wasPaused
                startMainLoop(keepStartRef = true)
                if (!wasPaused && _countdown.value > 0) {
                    startCountdownLoop()
                }
            }
        }
    }

    fun startCompetition(startInterval: Int = DEFAULT_START_INTERVAL) {
        CoroutineScope(Dispatchers.IO).launch {
            val timestamp = System.currentTimeMillis()
            settingsDao.put(AppSettingsEntity(KEY_START_TIMESTAMP, timestamp.toString()))
            settingsDao.put(AppSettingsEntity(KEY_COMPETITION_ACTIVE, "true"))
            settingsDao.put(AppSettingsEntity(KEY_COUNTDOWN_VALUE, startInterval.toString()))
            settingsDao.put(AppSettingsEntity(KEY_CURRENT_INTERVAL, startInterval.toString()))
            settingsDao.put(AppSettingsEntity(KEY_COUNTDOWN_PAUSED, "false"))
            settingsDao.put(AppSettingsEntity(KEY_FIRST_TEAM_STARTED, "false"))
        }
        _started.value = true
        _isFirstStart.value = true
        _mainTimer.value = 0L
        _countdown.value = startInterval
        _countdownPaused.value = false
        startMainLoop(keepStartRef = false)
        startCountdownLoop()
    }

    /**
     * Остановка главного таймера (по долгому тапу судьи)
     * Требует пароль администратора (1234 или devdebug)
     */
    fun stopMainTimer(password: String): Boolean {
        if (password != "1234" && password != "devdebug") {
            return false
        }

        mainJob?.cancel()
        mainJob = null
        _started.value = false
        _mainTimer.value = 0L

        CoroutineScope(Dispatchers.IO).launch {
            settingsDao.put(AppSettingsEntity(KEY_COMPETITION_ACTIVE, "false"))
            settingsDao.remove(KEY_START_TIMESTAMP)
        }

        return true
    }

    private fun startMainLoop(keepStartRef: Boolean = false) {
        mainJob?.cancel()
        val startRef = if (keepStartRef) {
            System.currentTimeMillis() - _mainTimer.value
        } else {
            System.currentTimeMillis()
        }
        mainJob = scope.launch {
            while (isActive && _started.value) {
                _mainTimer.value = System.currentTimeMillis() - startRef
                delay(100)
            }
        }
    }

    private fun startCountdownLoop() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive && _countdown.value > 0 && _started.value) {
                if (!_countdownPaused.value) {
                    delay(1000)
                    _countdown.value -= 1
                    // Сохраняем для восстановления после краша
                    settingsDao.put(AppSettingsEntity(KEY_COUNTDOWN_VALUE, _countdown.value.toString()))
                } else {
                    delay(100)
                }
            }
        }
    }

    /**
     * Пауза/возобновление обратного отсчёта.
     * Запрещено если осталось ≤ 10 секунд (звуковая индикация запущена).
     */
    fun toggleCountdownPause(canPause: Boolean = true) {
        if (!canPause && _countdown.value <= 10) {
            return
        }
        _countdownPaused.value = !_countdownPaused.value
        CoroutineScope(Dispatchers.IO).launch {
            settingsDao.put(AppSettingsEntity(KEY_COUNTDOWN_PAUSED, _countdownPaused.value.toString()))
        }
        if (!_countdownPaused.value) {
            startCountdownLoop()
        }
    }

    fun resetCountdown(seconds: Int, isFirst: Boolean = false) {
        _countdown.value = if (isFirst) FIRST_START_COUNTDOWN else seconds
        _isFirstStart.value = isFirst
        _countdownPaused.value = false
        CoroutineScope(Dispatchers.IO).launch {
            settingsDao.put(AppSettingsEntity(KEY_COUNTDOWN_VALUE, _countdown.value.toString()))
            settingsDao.put(AppSettingsEntity(KEY_COUNTDOWN_PAUSED, "false"))
            if (!isFirst) {
                settingsDao.put(AppSettingsEntity(KEY_FIRST_TEAM_STARTED, "true"))
            }
        }
        if (_started.value) {
            startCountdownLoop()
        }
    }

    fun stop() {
        mainJob?.cancel()
        countdownJob?.cancel()
        _started.value = false
        _mainTimer.value = 0L
        _countdown.value = DEFAULT_START_INTERVAL
        _countdownPaused.value = false
        _isFirstStart.value = true
        CoroutineScope(Dispatchers.IO).launch {
            settingsDao.put(AppSettingsEntity(KEY_COMPETITION_ACTIVE, "false"))
            settingsDao.remove(KEY_COUNTDOWN_VALUE)
            settingsDao.remove(KEY_COUNTDOWN_PAUSED)
            settingsDao.remove(KEY_FIRST_TEAM_STARTED)
            settingsDao.remove(KEY_CURRENT_INTERVAL)
        }
    }
}