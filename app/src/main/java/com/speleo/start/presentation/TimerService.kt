package com.speleo.start.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerService @Inject constructor() {

    private val _mainTimer = MutableStateFlow(0L)
    val mainTimer: StateFlow<Long> = _mainTimer.asStateFlow()

    private val _countdown = MutableStateFlow(120)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _started = MutableStateFlow(false)
    val started: StateFlow<Boolean> = _started.asStateFlow()

    private var timerJob: Job? = null
    private var countdownJob: Job? = null

    fun startCompetition() {
        _started.value = true
        timerJob?.cancel()
        countdownJob?.cancel()

        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(100)
                _mainTimer.value += 100
            }
        }

        countdownJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && _countdown.value > 0) {
                if (!_isPaused.value) {
                    delay(1000)
                    _countdown.value -= 1
                } else delay(100)
            }
        }
    }

    fun togglePause() { _isPaused.value = !_isPaused.value }
    fun resetCountdown() { _countdown.value = 120 }

    fun stop() {
        timerJob?.cancel()
        countdownJob?.cancel()
        _started.value = false
    }
}